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
      // Find latitude and longitude
      const lon = activity.location?.longitude
      const lat = activity.location?.latitude

      if (lon && lat && !isNaN(lon) && !isNaN(lat)) {
        const pt: [number, number] = [lat, lon]
        coords.push(pt)
        
        // Add Marker
        const marker = L.marker(pt)
        marker.bindPopup(`
          <div style="font-weight:bold; margin-bottom:4px;">Day ${dayIndex} - ${activity.name || '景点'}</div>
          <div style="font-size:12px; color:#666;">${activity.location?.address || ''}</div>
        `)
        markerGroup.addLayer(marker)
      }
    }
  }

  if (coords.length > 0) {
    // Draw route line connecting the points
    polylineLayer = L.polyline(coords, { color: '#1360ff', weight: 4, opacity: 0.7, dashArray: '8, 8' }).addTo(mapInstance)
    
    // Auto fit bounds
    mapInstance.fitBounds(L.latLngBounds(coords), { padding: [30, 30] })
  }
}
</script>

<style scoped>
.dynamic-map-container {
  width: 100%;
  height: 100%;
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
</style>
