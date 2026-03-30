<template>
  <div class="dynamic-map-shell">
    <div class="dynamic-map-container" ref="mapContainer"></div>
    <div v-if="showNoDataHint" class="map-empty-hint">
      当前行程没有可用坐标，地图先展示底图。继续发送一次行程请求后会自动打点。
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
let markerList: any[] = []
let polylineLayer: any = null

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
      plugins: ['AMap.Marker', 'AMap.Polyline', 'AMap.InfoWindow'], // 需要使用的的插件列表
    })

    mapInstance.value = new AMapObj.Map(mapContainer.value, {
      center: [104.195397, 35.86166], // 中国中心点坐标
      zoom: 4, // 初始地图级别
    })

    if (props.itinerary) {
      drawItinerary(props.itinerary)
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
})

watch(() => props.itinerary, (newItinerary) => {
  if (mapInstance.value && AMapObj) {
    drawItinerary(newItinerary)
  }
}, { deep: true })

function clearMap() {
  if (markerList.length > 0) {
    mapInstance.value.remove(markerList)
    markerList = []
  }
  if (polylineLayer) {
    mapInstance.value.remove(polylineLayer)
    polylineLayer = null
  }
}

function drawItinerary(itinerary: StructuredItinerary | null) {
  clearMap()
  showNoDataHint.value = false

  if (!itinerary || !itinerary.dailyPlans) return

  const coords: [number, number][] = []
  let dayIndex = 0;

  for (const plan of itinerary.dailyPlans) {
    dayIndex++
    if (!plan.activities) continue

    for (const activity of plan.activities) {
      const rawLon = activity.location?.longitude
      const rawLat = activity.location?.latitude

      let lon = typeof rawLon === 'string' ? parseFloat(rawLon) : rawLon
      let lat = typeof rawLat === 'string' ? parseFloat(rawLat) : rawLat

      if (Number.isFinite(lon) && Number.isFinite(lat)) {
        // Fallback constraint in case the AI swaps lon/lat
        if (Math.abs(lat as number) > 90 && Math.abs(lon as number) <= 90) {
          const temp = lon
          lon = lat
          lat = temp
        }

        // 高德原生要求使用 GCJ-02，理论上如果是 WGS-84 可以调用 AMap.convertFrom() 进行转换。
        // 为了稳定、离线的高效转化，我们仍保留之前的转火星坐标公式：
        const [gcjLon, gcjLat] = wgs84togcj02(lon as number, lat as number)
        coords.push([gcjLon, gcjLat])

        const customHtml = `
          <div class="custom-poi-marker">
            <div class="poi-pulse"></div>
            <div class="poi-dot">D${dayIndex}</div>
          </div>
        `

        const marker = new AMapObj.Marker({
          position: [gcjLon, gcjLat],
          content: customHtml,
          offset: new AMapObj.Pixel(-14, -14),
          title: `Day ${dayIndex} - ${activity.name || '景点'}`
        })

        // 添加点击信息窗
        marker.on('click', () => {
          const infoWindow = new AMapObj.InfoWindow({
            content: `
              <div style="padding: 10px; max-width: 250px;">
                <div style="font-weight:bold; margin-bottom:4px;">Day ${dayIndex} - ${activity.name || '景点'}</div>
                <div style="font-size:12px; color:#666;">${activity.location?.address || ''}</div>
              </div>
            `,
            offset: new AMapObj.Pixel(0, -20)
          })
          infoWindow.open(mapInstance.value, marker.getPosition())
        })

        markerList.push(marker)
      }
    }
  }

  if (coords.length > 0) {
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
    mapInstance.value.setFitView(undefined, false, [40, 40, 40, 40])
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

/* 隐藏高德 logo 和 调整 copyright (可选) */
:deep(.amap-logo) {
  display: none !important;
}
:deep(.amap-copyright) {
  opacity: 0.5;
}
</style>
