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
let polylineLayers: any[] = [] // 改为数组，存储多条路线
let renderVersion = 0
const positionCache = new Map<string, [number, number] | null>()
const MAX_GEOCODE_CONCURRENCY = 4

// 定义一组高级感的颜色，用于区分不同天数的路线和标记
const DAY_COLORS = ['#1360ff', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#06b6d4', '#ec4899', '#f97316'];

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
      plugins: ['AMap.Marker', 'AMap.Polyline', 'AMap.InfoWindow', 'AMap.Geocoder', 'AMap.PlaceSearch', 'AMap.Driving'],
    })

    mapInstance.value = new AMapObj.Map(mapContainer.value, {
      center: [104.195397, 35.86166], // 中国中心点坐标
      zoom: 4, // 初始地图级别
      viewMode: '3D', // 开启3D视图
      pitch: 45, // 俯仰角，增加科技感
      rotation: 15,
      mapStyle: 'amap://styles/normal' // 或换成更炫酷的底图 amap://styles/macaron
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
  if (polylineLayers.length > 0) {
    mapInstance.value.remove(polylineLayers)
    polylineLayers = []
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

  if (Math.abs(lon) > 180 || Math.abs(lat) > 90) {
    return null
  }

  return [lon, lat]
}

function periodRank(rawPeriod: unknown): number {
  const value = String(rawPeriod || '').toLowerCase().trim()
  if (value.includes('morning') || value.includes('早') || value.includes('上午')) return 1
  if (value.includes('noon') || value.includes('afternoon') || value.includes('中') || value.includes('午')) return 2
  if (value.includes('evening') || value.includes('night') || value.includes('晚')) return 3
  return 4
}

async function mapWithConcurrency<T, R>(
  items: T[],
  concurrency: number,
  worker: (item: T, index: number) => Promise<R>,
): Promise<R[]> {
  if (items.length === 0) {
    return []
  }

  const safeConcurrency = Math.max(1, Math.min(concurrency, items.length))
  const results = new Array<R>(items.length)
  let nextIndex = 0

  async function runWorker() {
    while (nextIndex < items.length) {
      const current = nextIndex++
      results[current] = await worker(items[current], current)
    }
  }

  await Promise.all(Array.from({ length: safeConcurrency }, () => runWorker()))
  return results
}

function buildQueryCandidates(itinerary: StructuredItinerary, activity: any): string[] {
  const destination = (itinerary.destination || '').trim()
  const address = (activity?.location?.address || '').trim()
  const name = (activity?.name || '').trim()
  const candidates = [
    [destination, name].filter(Boolean).join(' '),
    [destination, address].filter(Boolean).join(' '),
    address,
    [address, name].filter(Boolean).join(' '),
    name,
  ]

  return Array.from(new Set(candidates.filter((item) => item.length > 0)))
}

function buildPoiCandidates(itinerary: StructuredItinerary, activity: any): string[] {
  const destination = (itinerary.destination || '').trim()
  const name = (activity?.name || '').trim()
  const address = (activity?.location?.address || '').trim()
  const keywords = [
    [destination, name].filter(Boolean).join(' '),
    name,
    [destination, address].filter(Boolean).join(' '),
    address,
  ]
  return Array.from(new Set(keywords.filter((item) => item.length > 0)))
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
  const cacheKey = JSON.stringify({
    destination: itinerary.destination || '',
    address: activity?.location?.address || '',
    name: activity?.name || '',
    lon: activity?.location?.longitude ?? activity?.longitude ?? activity?.lng,
    lat: activity?.location?.latitude ?? activity?.latitude ?? activity?.lat,
  })
  if (positionCache.has(cacheKey)) {
    return positionCache.get(cacheKey) || null
  }

  const city = (itinerary.destination || '').trim()
  const poiKeywords = buildPoiCandidates(itinerary, activity)
  const candidates = buildQueryCandidates(itinerary, activity)

  // 1) 先用地点名 / POI 检索，尽量拿到高德原生点位
  for (const keyword of poiKeywords) {
    const poiPoint = await searchByPoi(keyword, city)
    if (poiPoint) {
      positionCache.set(cacheKey, poiPoint)
      return poiPoint
    }
  }

  // 2) 再做地理编码兜底
  for (const query of candidates) {
    const point = await geocodeByAddress(query)
    if (point) {
      positionCache.set(cacheKey, point)
      return point
    }
  }

  // 3) 最后再使用经纬度兜底
  const rawCoordinate = parseRawCoordinate(activity)
  if (rawCoordinate) {
    const [gcjLon, gcjLat] = wgs84togcj02(rawCoordinate[0], rawCoordinate[1])
    const point: [number, number] = [gcjLon, gcjLat]
    positionCache.set(cacheKey, point)
    return point
  }

  // 4) 终极兜底到目的地中心点，避免地图完全无点位
  const destinationCenter = await resolveDestinationCenter(itinerary.destination || '')
  if (destinationCenter) {
    positionCache.set(cacheKey, destinationCenter)
    return destinationCenter
  }

  positionCache.set(cacheKey, null)
  return null
}

async function resolveDestinationCenter(destination: string): Promise<[number, number] | null> {
  const trimmed = (destination || '').trim()
  if (!trimmed) {
    return null
  }

  const cacheKey = `destination:${trimmed}`
  if (positionCache.has(cacheKey)) {
    return positionCache.get(cacheKey) || null
  }

  const geoPoint = await geocodeByAddress(trimmed)
  if (geoPoint) {
    positionCache.set(cacheKey, geoPoint)
    return geoPoint
  }

  const poiPoint = await searchByPoi(trimmed, trimmed)
  if (poiPoint) {
    positionCache.set(cacheKey, poiPoint)
    return poiPoint
  }

  positionCache.set(cacheKey, null)
  return null
}

async function drawItinerary(itinerary: StructuredItinerary | null) {
  const currentRender = ++renderVersion
  clearMap()
  showNoDataHint.value = false

  if (!itinerary || !itinerary.dailyPlans) return

  const dayCoordsMap: Record<number, [number, number][]> = {}
  const nextMarkerList: any[] = []
  const overlapCounter = new Map<string, number>()
  let dayIndex = 0
  let totalPointsCount = 0

  for (const plan of itinerary.dailyPlans) {
    dayIndex++
    const parsedDay = Number(plan.day)
    const currentDay = Number.isFinite(parsedDay) && parsedDay > 0 ? parsedDay : dayIndex
    const planActivities = Array.isArray(plan.activities) ? plan.activities : []
    if (planActivities.length === 0) continue

    const orderedActivities = planActivities
      .map((activity, index) => ({ activity, index, rank: periodRank(activity?.time) }))
      .sort((a, b) => a.rank - b.rank || a.index - b.index)

    const resolvedActivities = await mapWithConcurrency(
      orderedActivities,
      MAX_GEOCODE_CONCURRENCY,
      async ({ activity }) => ({
        activity,
        point: await resolveActivityPosition(itinerary, activity),
      }),
    )

    if (currentRender !== renderVersion) {
      return
    }

    for (const resolved of resolvedActivities) {
      if (!resolved.point) {
        continue
      }

      const activity = resolved.activity
      const [lng, lat] = resolved.point
      const overlapKey = `${lng.toFixed(6)},${lat.toFixed(6)}`
      const overlapCount = overlapCounter.get(overlapKey) || 0
      overlapCounter.set(overlapKey, overlapCount + 1)
      const displayLng = overlapCount === 0 ? lng : lng + overlapCount * 0.00018
      const displayLat = overlapCount === 0 ? lat : lat + overlapCount * 0.00012

      if (!dayCoordsMap[currentDay]) {
        dayCoordsMap[currentDay] = []
      }
      dayCoordsMap[currentDay].push([displayLng, displayLat])
      totalPointsCount++

      const dayColor = DAY_COLORS[(currentDay - 1) % DAY_COLORS.length]

      const marker = new AMapObj.Marker({
        position: [displayLng, displayLat],
        title: `第${currentDay}天 - ${activity.name || '景点'}`,
      })
      if (typeof marker.setLabel === 'function') {
        const timeRank = periodRank(activity?.time)
        let emoji = timeRank === 1 ? '🌅' : timeRank === 2 ? '☀️' : timeRank === 3 ? '🌙' : '📌'
        const nameOrDesc = (activity?.name || '') + (activity?.description || '')
        if (activity?.type === 'rest' || nameOrDesc.includes('酒店') || nameOrDesc.includes('民宿') || nameOrDesc.includes('住')) {
          emoji = '🏨'
        }
        marker.setLabel({
          direction: 'top',
          offset: new AMapObj.Pixel(0, -8),
          content: `<span style="display:inline-block;background:#fff;border:1px solid ${dayColor};color:${dayColor};padding:2px 8px;border-radius:999px;font-size:12px;font-weight:bold;box-shadow:0 2px 6px rgba(0,0,0,0.1);"><span style="margin-right:4px">${emoji}</span>${activity.name || '景点'}</span>`,
        })
      }

      marker.on('click', () => {
        const infoWindow = new AMapObj.InfoWindow({
          content: `
            <div style="padding: 10px; max-width: 250px;">
              <div style="font-weight:bold; margin-bottom:4px; color:${dayColor};">第${currentDay}天 - ${activity.name || '景点'}</div>
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

  if (totalPointsCount > 0) {
    markerList = nextMarkerList

    if (markerList.length > 0) {
      mapInstance.value.add(markerList)
    }

    polylineLayers = []

    // 按天绘制并带有方向提示的真实路线
    for (const dayKey of Object.keys(dayCoordsMap)) {
      const dIndex = parseInt(dayKey)
      const dayPts = dayCoordsMap[dIndex]
      const color = DAY_COLORS[(dIndex - 1) % DAY_COLORS.length]

      // 仅当该天有超过1个点时才进行路线规划
      if (dayPts.length > 1) {
        const driving = new AMapObj.Driving({
          map: mapInstance.value,
          hideMarkers: true,
          showTraffic: false,
          autoFitView: false
        });
        
        const origin = dayPts[0];
        const destination = dayPts[dayPts.length - 1];
        const waypoints = dayPts.slice(1, dayPts.length - 1);
        
        driving.search(new AMapObj.LngLat(origin[0], origin[1]), new AMapObj.LngLat(destination[0], destination[1]), {
          waypoints: waypoints.map(pt => new AMapObj.LngLat(pt[0], pt[1]))
        }, (status: string, result: any) => {
          if (status === 'complete' && result.routes && result.routes.length) {
            const path: any[] = [];
            result.routes[0].steps.forEach((step: any) => {
              step.path.forEach((pt: any) => {
                path.push([pt.lng, pt.lat]);
              });
            });
            const polyline = new AMapObj.Polyline({
              path: path,
              isOutline: true,
              outlineColor: '#ffffff',
              borderWeight: 2,
              strokeColor: color,
              strokeOpacity: 0.9,
              strokeWeight: 6,
              showDir: true,
              lineJoin: 'round',
              lineCap: 'round',
            })
            mapInstance.value.add(polyline);
            polylineLayers.push(polyline);
          } else {
             // 如果路线规划失败，降级为直接连线
             const fallbackPolyline = new AMapObj.Polyline({
              path: dayPts,
              isOutline: true,
              outlineColor: '#ffffff',
              borderWeight: 2,
              strokeColor: color,
              strokeOpacity: 0.9,
              strokeWeight: 6,
              showDir: true,
              lineJoin: 'round',
              lineCap: 'round',
              strokeStyle: 'dashed',
            })
            mapInstance.value.add(fallbackPolyline);
            polylineLayers.push(fallbackPolyline);
          }
        });
      }
    }

    // 如果每天都有且只有一个点，回退为“全程连线”
    if (totalPointsCount === Object.keys(dayCoordsMap).length && totalPointsCount > 1) {
      const allPoints = Object.keys(dayCoordsMap)
        .map((key) => Number(key))
        .sort((a, b) => a - b)
        .flatMap((day) => dayCoordsMap[day] || [])
      if (allPoints.length > 1) {
        const driving = new AMapObj.Driving({
          map: mapInstance.value,
          hideMarkers: true,
          showTraffic: false,
          autoFitView: false
        });
        
        const origin = allPoints[0];
        const destination = allPoints[allPoints.length - 1];
        const waypoints = allPoints.slice(1, allPoints.length - 1);
        
        driving.search(new AMapObj.LngLat(origin[0], origin[1]), new AMapObj.LngLat(destination[0], destination[1]), {
          waypoints: waypoints.map(pt => new AMapObj.LngLat(pt[0], pt[1]))
        }, (status: string, result: any) => {
          if (status === 'complete' && result.routes && result.routes.length) {
            const path: any[] = [];
            result.routes[0].steps.forEach((step: any) => {
              step.path.forEach((pt: any) => {
                path.push([pt.lng, pt.lat]);
              });
            });
            const fallbackPolyline = new AMapObj.Polyline({
              path: path,
              isOutline: true,
              outlineColor: '#ffffff',
              borderWeight: 2,
              strokeColor: '#1360ff',
              strokeOpacity: 0.85,
              strokeWeight: 5,
              showDir: true,
              lineJoin: 'round',
              lineCap: 'round',
              strokeStyle: 'solid',
            })
            mapInstance.value.add(fallbackPolyline);
            polylineLayers.push(fallbackPolyline);
          } else {
             // 失败则直线兜底
             const fallbackPolyline = new AMapObj.Polyline({
              path: allPoints,
              isOutline: true,
              outlineColor: '#ffffff',
              borderWeight: 2,
              strokeColor: '#1360ff',
              strokeOpacity: 0.85,
              strokeWeight: 5,
              showDir: true,
              lineJoin: 'round',
              lineCap: 'round',
              strokeStyle: 'dashed',
            })
            mapInstance.value.add(fallbackPolyline);
            polylineLayers.push(fallbackPolyline);
          }
        });
      }
    }

    // 视口自适应由于异步搜索的原因，这里可能取不到全线的边界，但能自适应所有marker点
    // 视口自适应延时让路线先画上
    setTimeout(() => {
      const overlays = [...markerList, ...polylineLayers]
      mapInstance.value.setFitView(overlays, false, [60, 60, 60, 60])
    }, 800)
  } else {
    showNoDataHint.value = true

    const fallbackCenter = await resolveDestinationCenter(itinerary.destination || '')
    if (fallbackCenter && mapInstance.value) {
      const [lng, lat] = fallbackCenter
      mapInstance.value.setCenter([lng, lat])
      mapInstance.value.setZoom(11)

      const destinationMarker = new AMapObj.Marker({
        position: [lng, lat],
        title: itinerary.destination || '目的地',
      })
      markerList = [destinationMarker]
      mapInstance.value.add(markerList)
    }
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
  min-height: 0;
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
  border: 2px solid #fff;
  border-radius: 50%;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 2;
  transition: transform 0.2s ease;
}
:deep(.custom-poi-marker:hover .poi-dot) {
  transform: scale(1.15);
}

:deep(.custom-poi-marker .poi-pulse) {
  position: absolute;
  top: -6px;
  left: -6px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  animation: poi-pulse 1.5s ease-out infinite;
  z-index: 1;
}

@keyframes poi-pulse {
  0% { transform: scale(0.6); opacity: 1; }
  100% { transform: scale(1.6); opacity: 0; }
}
</style>
