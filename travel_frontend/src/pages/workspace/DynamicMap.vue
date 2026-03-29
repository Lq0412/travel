<template>
  <div class="dynamic-map-container" ref="mapContainer"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import type { StructuredItinerary } from '@/types/itinerary'

// Fix leaflet icon issue in Vue / Webpack / Vite
import iconRetinaUrl from 'leaflet/dist/images/marker-icon-2x.png'
import iconUrl from 'leaflet/dist/images/marker-icon.png'
import shadowUrl from 'leaflet/dist/images/marker-shadow.png'

L.Icon.Default.mergeOptions({
  iconRetinaUrl: iconRetinaUrl,
  iconUrl: iconUrl,
  shadowUrl: shadowUrl,
})

const props = defineProps<{
  itinerary: StructuredItinerary | null
}>()

const mapContainer = ref<HTMLElement | null>(null)
let mapInstance: L.Map | null = null
let markerGroup: L.LayerGroup | null = null
let polylineLayer: L.Polyline | null = null

onMounted(() => {
  if (!mapContainer.value) return

  // Initialize Map
  mapInstance = L.map(mapContainer.value).setView([35.86166, 104.195397], 4) // Center of China

  // Use AutoNavi (GaoDe) Tile Layer, very fast in China
  L.tileLayer('http://webrd04.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=7&x={x}&y={y}&z={z}', {
    maxZoom: 18,
    attribution: '© GaoDe Map'
  }).addTo(mapInstance)

  markerGroup = L.layerGroup().addTo(mapInstance)

  // Draw if we already have itinerary
  if (props.itinerary) {
    drawItinerary(props.itinerary)
  }

  // Hack for Leaflet inside Vue Flexbox rendering issue
  setTimeout(() => {
    mapInstance?.invalidateSize()
  }, 300)
})

onUnmounted(() => {
  if (mapInstance) {
    mapInstance.remove()
    mapInstance = null
  }
})

watch(() => props.itinerary, (newItinerary) => {
  drawItinerary(newItinerary)
}, { deep: true })

function drawItinerary(itinerary: StructuredItinerary | null) {
  if (!mapInstance || !markerGroup) return
  
  markerGroup.clearLayers()
  if (polylineLayer) {
    mapInstance.removeLayer(polylineLayer)
    polylineLayer = null
  }

  if (!itinerary || !itinerary.dailyPlans) return

  const coords: [number, number][] = []

  let dayIndex = 0;
  for (const plan of itinerary.dailyPlans) {
    dayIndex++
    if (!plan.activities) continue

    for (const activity of plan.activities) {
        // Find latitude and longitude with string-to-number fallback support
        const rawLon = activity.location?.longitude
        const rawLat = activity.location?.latitude

        const lon = typeof rawLon === 'string' ? parseFloat(rawLon) : rawLon
        const lat = typeof rawLat === 'string' ? parseFloat(rawLat) : rawLat

        if (lon !== undefined && lat !== undefined && !isNaN(lon) && !isNaN(lat)) {
        coords.push(pt)
        
        // Add Marker with Custom CSS animation
        const customIcon = L.divIcon({
          className: 'custom-poi-marker',
          html: `
            <div class="poi-pulse"></div>
            <div class="poi-dot">D${dayIndex}</div>
          `,
          iconSize: [28, 28],
          iconAnchor: [14, 14],
          popupAnchor: [0, -14]
        })

        const marker = L.marker(pt, { icon: customIcon })
        marker.bindPopup(`
          <div style="font-weight:bold; margin-bottom:4px;">Day ${dayIndex} - ${activity.name || '景点'}</div>
          <div style="font-size:12px; color:#666;">${activity.location?.address || ''}</div>
        `)
        markerGroup.addLayer(marker)
      }
    }
  }

  if (coords.length > 0) {
    // Draw route line connecting the points with animation
    polylineLayer = L.polyline(coords, { 
      color: '#1360ff', 
      weight: 4, 
      opacity: 0.85, 
      className: 'animated-route'
    }).addTo(mapInstance)
    
    // Auto fit bounds with a cinematic flyTo animation instead of instant jump
    mapInstance.flyToBounds(L.latLngBounds(coords), { 
      padding: [40, 40],
      duration: 1.5,
      easeLinearity: 0.25
    })
  }
}
</script>

<style scoped>
.dynamic-map-container {
  width: 100%;
  height: 100%;
  min-height: 420px; /* 确保最小高度，防止在 Flex 布局中塌陷 */
  flex: 1;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08); /* slight depth */
  border: 1px solid var(--color-border, #eaeaea);
  z-index: 1; /* Keep leaflet popups below other absolute popups if any */
}

/* Fix z-index issue where Leaflet controls overlap higher-level fixed elements */
:deep(.leaflet-top),
:deep(.leaflet-bottom) {
  z-index: 500 !important;
}

/* Custom Marker Styling & Animations */
:deep(.custom-poi-marker) {
  position: relative;
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

/* Animated Route Line */
:deep(.animated-route) {
  stroke-dasharray: 12, 12;
  animation: flow-dash 30s linear infinite;
}

@keyframes flow-dash {
  to {
    stroke-dashoffset: -1000;
  }
}
</style>
