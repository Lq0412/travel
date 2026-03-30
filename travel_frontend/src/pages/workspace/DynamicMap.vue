<template>
  <div class="dynamic-map-shell">
    <div class="dynamic-map-container" ref="mapContainer"></div>
    <div v-if="showNoDataHint" class="map-empty-hint">
      当前行程没有可定位的地址信息，地图先展示底图。可在对话中补充更具体的地点名称或地址。
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted, shallowRef } from 'vue'
import AMapLoader from '@amap/amap-jsapi-loader'
import type { StructuredItinerary } from '@/types/itinerary'

const props = defineProps<{
  itinerary: StructuredItinerary | null
}>()

const mapContainer = ref<HTMLElement | null>(null)
const showNoDataHint = ref(false)

// 为了避免高德地图实例被 Proxy 代理导致性能或渲染极其卡顿，必须用 shallowRef
const mapInstance = shallowRef<any>(null)
let AMapObj: any = null
const geocoder = shallowRef<any>(null)
const placeSearch = shallowRef<any>(null)
let markerList: any[] = []
let polylineLayer: any = null
let renderVersion = 0

onMounted(async () => {
  if (!mapContainer.value) return

  // 设置安全密钥，需在加载前设置
  ;(window as any)._AMapSecurityConfig = {
    securityJsCode: import.meta.env.VITE_AMAP_SECURITY_CODE || '这里填你的安全密钥',
  }

  try {
    AMapObj = await AMapLoader.load({
      key: import.meta.env.VITE_AMAP_KEY || '这里填你的API-KEY', // 申请好的Web端开发者Key
      version: '2.0', // 指定要加载的 JSAPI 的版本
      plugins: ['AMap.Marker', 'AMap.Polyline', 'AMap.InfoWindow', 'AMap.Geocoder', 'AMap.PlaceSearch'],
    })

    mapInstance.value = new AMapObj.Map(mapContainer.value, {
      center: [104.195397, 35.86166], // 中国中心点坐标
      zoom: 4, // 初始地图级别
    })

    geocoder.value = new AMapObj.Geocoder({
      city: '全国',
    })

    placeSearch.value = new AMapObj.PlaceSearch({
      city: '全国',
      citylimit: false,
      pageSize: 1,
    })

    if (props.itinerary) {
      await drawItinerary(props.itinerary)
    }
  } catch (e) {
    console.error('高德地图加载失败', e)
  }
})

onUnmounted(() => {
  if (mapInstance.value) {
    mapInstance.value.destroy()
    mapInstance.value = null
  }
  geocoder.value = null
  placeSearch.value = null
})

watch(() => props.itinerary, async (newItinerary) => {
  if (mapInstance.value && AMapObj) {
    await drawItinerary(newItinerary)
  }
}, { deep: true })

function clearMap() {
  if (!mapInstance.value) {
    return
  }

  if (markerList.length > 0) {
    mapInstance.value.remove(markerList)
    markerList = []
  }
  if (polylineLayer) {
    mapInstance.value.remove(polylineLayer)
    polylineLayer = null
  }
}

function toNumber(value: unknown): number | null {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value
  }
  if (typeof value === 'string') {
    const parsed = Number(value)
    return Number.isFinite(parsed) ? parsed : null
  }
  return null
}

function extractLngLat(location: any): [number, number] | null {
  if (!location) {
    return null
  }

  if (Array.isArray(location) && location.length >= 2) {
    const lng = toNumber(location[0])
    const lat = toNumber(location[1])
    if (lng !== null && lat !== null) {
      return [lng, lat]
    }
  }

  const lng = toNumber(location.lng) ?? (typeof location.getLng === 'function' ? toNumber(location.getLng()) : null)
  const lat = toNumber(location.lat) ?? (typeof location.getLat === 'function' ? toNumber(location.getLat()) : null)

  if (lng !== null && lat !== null) {
    return [lng, lat]
  }

  return null
}

function parseRawCoordinate(activity: any): [number, number] | null {
  const rawLon = activity?.location?.longitude ?? activity?.longitude ?? activity?.['location.longitude'] ?? activity?.lng
  const rawLat = activity?.location?.latitude ?? activity?.latitude ?? activity?.['location.latitude'] ?? activity?.lat

  let lon = toNumber(rawLon)
  let lat = toNumber(rawLat)

  if (lon === null || lat === null) {
    return null
  }

  // 容错：如果模型把经纬度写反，则自动交换
  if (Math.abs(lat) > 90 && Math.abs(lon) <= 90) {
    const temp = lon
    lon = lat
    lat = temp
  }

  return [lon, lat]
}

function buildQueryCandidates(itinerary: StructuredItinerary, activity: any): string[] {
  const destination = (itinerary.destination || '').trim()
  const address = (activity?.location?.address || '').trim()
  const name = (activity?.name || '').trim()
  const candidates = [
    [destination, address].filter(Boolean).join(' '),
    address,
    [destination, name].filter(Boolean).join(' '),
    name,
  ]

  return Array.from(new Set(candidates.filter((item) => item.length > 0)))
}

function geocodeByAddress(query: string): Promise<[number, number] | null> {
  return new Promise((resolve) => {
    const instance = geocoder.value
    if (!instance || !query.trim()) {
      resolve(null)
      return
    }

    instance.getLocation(query, (status: string, result: any) => {
      if (status !== 'complete') {
        resolve(null)
        return
      }

      const geocode = result?.geocodes?.[0]
      resolve(extractLngLat(geocode?.location))
    })
  })
}

function searchByPoi(keyword: string, city: string): Promise<[number, number] | null> {
  return new Promise((resolve) => {
    const instance = placeSearch.value
    if (!instance || !keyword.trim()) {
      resolve(null)
      return
    }

    if (typeof instance.setCity === 'function' && city) {
      instance.setCity(city)
    }

    instance.search(keyword, (status: string, result: any) => {
      if (status !== 'complete') {
        resolve(null)
        return
      }

      const poi = result?.poiList?.pois?.[0]
      resolve(extractLngLat(poi?.location))
    })
  })
}

async function resolveActivityPosition(itinerary: StructuredItinerary, activity: any): Promise<[number, number] | null> {
  const candidates = buildQueryCandidates(itinerary, activity)

  for (const query of candidates) {
    const point = await geocodeByAddress(query)
    if (point) {
      return point
    }
  }

  const keyword = (activity?.name || activity?.location?.address || '').trim()
  const poiPoint = await searchByPoi(keyword, (itinerary.destination || '').trim())
  if (poiPoint) {
    return poiPoint
  }

  // 最终兜底：若模型确实给了坐标，仍可显示，避免整日程无点位
  const rawCoordinate = parseRawCoordinate(activity)
  if (rawCoordinate) {
    const [gcjLon, gcjLat] = wgs84togcj02(rawCoordinate[0], rawCoordinate[1])
    return [gcjLon, gcjLat]
  }

  return null
}

async function drawItinerary(itinerary: StructuredItinerary | null) {
  const currentRender = ++renderVersion
  clearMap()
  showNoDataHint.value = false

  if (!itinerary || !itinerary.dailyPlans) return

  const coords: [number, number][] = []
  const nextMarkerList: any[] = []
  let dayIndex = 0

  for (const plan of itinerary.dailyPlans) {
    dayIndex++
    const currentDay = dayIndex
    if (!plan.activities) continue

    for (const activity of plan.activities) {
      const point = await resolveActivityPosition(itinerary, activity)

      if (currentRender !== renderVersion) {
        return
      }

      if (!point) {
        continue
      }

      const [lng, lat] = point
      coords.push([lng, lat])

      const customHtml = `
        <div class="custom-poi-marker">
          <div class="poi-pulse"></div>
          <div class="poi-dot">D${currentDay}</div>
        </div>
      `

      const marker = new AMapObj.Marker({
        position: [lng, lat],
        content: customHtml,
        offset: new AMapObj.Pixel(-14, -14),
        title: `Day ${currentDay} - ${activity.name || '景点'}`,
      })

      marker.on('click', () => {
        const infoWindow = new AMapObj.InfoWindow({
          content: `
            <div style="padding: 10px; max-width: 250px;">
              <div style="font-weight:bold; margin-bottom:4px;">Day ${currentDay} - ${activity.name || '景点'}</div>
              <div style="font-size:12px; color:#666;">${activity.location?.address || ''}</div>
            </div>
          `,
          offset: new AMapObj.Pixel(0, -20),
        })
        infoWindow.open(mapInstance.value, marker.getPosition())
      })

      nextMarkerList.push(marker)
    }
  }

  if (currentRender !== renderVersion) {
    return
  }

  if (coords.length > 0) {
    markerList = nextMarkerList

    if (markerList.length > 0) {
      mapInstance.value.add(markerList)
    }

    // 绘制虚线轨迹
    polylineLayer = new AMapObj.Polyline({
      path: coords,
      isOutline: true,
      outlineColor: '#fff',
      borderWeight: 2,
      strokeColor: '#1360ff',
      strokeOpacity: 0.85,
      strokeWeight: 4,
      strokeStyle: 'dashed',
      strokeDasharray: [10, 5],
      lineJoin: 'round',
      lineCap: 'round',
    })
    mapInstance.value.add(polylineLayer)

    // 视口自适应
    const overlays = [...markerList, polylineLayer]
    mapInstance.value.setFitView(overlays, false, [40, 40, 40, 40])
  } else {
    showNoDataHint.value = true
  }
}

// ========================
// WGS-84 to GCJ-02 convertor
// ========================
const PI = 3.1415926535897932384626
const a = 6378245.0
const ee = 0.00669342162296594323

function transformLat(x: number, y: number) {
  let ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x))
  ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0
  ret += (20.0 * Math.sin(y * PI) + 40.0 * Math.sin(y / 3.0 * PI)) * 2.0 / 3.0
  ret += (160.0 * Math.sin(y / 12.0 * PI) + 320 * Math.sin(y * PI / 30.0)) * 2.0 / 3.0
  return ret
}

function transformLon(x: number, y: number) {
  let ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x))
  ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0
  ret += (20.0 * Math.sin(x * PI) + 40.0 * Math.sin(x / 3.0 * PI)) * 2.0 / 3.0
  ret += (150.0 * Math.sin(x / 12.0 * PI) + 300.0 * Math.sin(x / 30.0 * PI)) * 2.0 / 3.0
  return ret
}

function outOfChina(lat: number, lon: number) {
  if (lon < 72.004 || lon > 137.8347) return true
  if (lat < 0.8293 || lat > 55.8271) return true
  return false
}

function wgs84togcj02(lon: number, lat: number) {
  if (outOfChina(lat, lon)) return [lon, lat]
  let dLat = transformLat(lon - 105.0, lat - 35.0)
  let dLon = transformLon(lon - 105.0, lat - 35.0)
  const radLat = lat / 180.0 * PI
  let magic = Math.sin(radLat)
  magic = 1 - ee * magic * magic
  const sqrtMagic = Math.sqrt(magic)
  dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * PI)
  dLon = (dLon * 180.0) / (a / sqrtMagic * Math.cos(radLat) * PI)
  const mgLat = lat + dLat
  const mgLon = lon + dLon
  return [mgLon, mgLat]
}
</script>

<style scoped>
.dynamic-map-shell {
  position: relative;
  width: 100%;
  height: 100%;
}

.dynamic-map-container {
  width: 100%;
  height: 100%;
  min-height: 420px;
  flex: 1;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  border: 1px solid var(--color-border, #eaeaea);
  z-index: 1;
}

.map-empty-hint {
  position: absolute;
  left: 12px;
  right: 12px;
  bottom: 12px;
  z-index: 700;
  background: rgba(10, 19, 40, 0.72);
  color: #fff;
  border-radius: 10px;
  padding: 8px 10px;
  font-size: 12px;
  line-height: 1.45;
}

/* Custom Marker Styling & Animations - 兼容高德直接注入的 HTML */
:deep(.custom-poi-marker) {
  position: relative;
  width: 28px;
  height: 28px;
}

:deep(.custom-poi-marker .poi-dot) {
  width: 28px;
  height: 28px;
  background: #1360ff;
  border: 2px solid #fff;
  border-radius: 50%;
  box-shadow: 0 4px 10px rgba(19, 96, 255, 0.4);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 2;
}

:deep(.custom-poi-marker .poi-pulse) {
  position: absolute;
  top: -6px;
  left: -6px;
  width: 40px;
  height: 40px;
  background: rgba(19, 96, 255, 0.4);
  border-radius: 50%;
  animation: poi-pulse 2s ease-out infinite;
  z-index: 1;
}

@keyframes poi-pulse {
  0% { transform: scale(0.6); opacity: 1; }
  100% { transform: scale(1.6); opacity: 0; }
}
</style>
