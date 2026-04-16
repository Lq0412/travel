<template>
  <div class="trips-page">
    <section class="page-header">
      <div>
        <h1>My Trips</h1>
      </div>
      <div class="search-wrap">
        <a-input
          v-model:value="keyword"
          allow-clear
          class="search-input custom-search-input"
          placeholder="Search trips..."
        >
          <template #suffix>
            <search-outlined style="color: rgba(0,0,0,.45)" />
          </template>
        </a-input>
      </div>
    </section>

    <section v-if="filteredTrips.length" class="trip-grid">
      <button
        v-for="trip in filteredTrips"
        :key="trip.id"
        type="button"
        class="trip-card"
        @click="openTrip(trip.id)"
      >
        <div class="card-cover">
          <img v-if="trip.id && coverMap[trip.id]" :src="coverMap[trip.id]" alt="cover" />
          <div v-else class="cover-placeholder"></div>
          
          <div class="status-badge" :class="normalizeTripStatus(trip.status)">
            <span class="status-icon"></span>
            {{ normalizeTripStatus(trip.status) === 'planned' ? 'Ongoing' : 'Draft' }}
          </div>
        </div>

        <div class="card-body">
          <div class="trip-top">
            <h2 class="trip-title">{{ trip.destination || '未命名行程' }}</h2>
            <div class="action-btn" :class="normalizeTripStatus(trip.status)">
              {{ normalizeTripStatus(trip.status) === 'planned' ? 'Ongoing' : 'Draft' }}
            </div>
          </div>

          <div class="trip-meta">
            <span class="meta-date">Date: {{ trip.startDate ? formatWorkflowDate(trip.startDate) : 'TBD' }}</span>
            <span class="meta-separator"></span>
            <span class="meta-location">
              <environment-outlined /> 
              {{ trip.days || 0 }} Days
            </span>
          </div>
        </div>
      </button>
    </section>

    <a-empty v-else description="还没有行程，点击右上角'规划新行程'开始吧" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { SearchOutlined, EnvironmentOutlined } from '@ant-design/icons-vue'
import { getMyTrips } from '@/api/tripController'
import { useVisualContent } from '@/composables/useVisualContent'
import {
  formatWorkflowDate,
  getTripStatusLabel,
  isTripPublished,
  normalizeTripStatus,
} from '@/utils/tripWorkflow'

const router = useRouter()
const { fetchFirst } = useVisualContent()

const trips = ref<API.TripVO[]>([])
const keyword = ref('')
const coverMap = ref<Record<number, string>>({})

const filteredTrips = computed(() => {
  const text = keyword.value.trim().toLowerCase()

  return [...trips.value]
    .filter((trip) => {
      if (!text) {
        return true
      }

      const destination = trip.destination?.toLowerCase() || ''
      const theme = trip.theme?.toLowerCase() || ''
      return destination.includes(text) || theme.includes(text)
    })
    .sort((a, b) => {
      const left = new Date(b.updateTime || b.createTime || 0).getTime()
      const right = new Date(a.updateTime || a.createTime || 0).getTime()
      return left - right
    })
})

async function loadTrips() {
  try {
    const resp = await getMyTrips()
    const fetchedTrips = resp?.data?.data || []
    trips.value = fetchedTrips
    
    fetchedTrips.forEach(async (trip) => {
      if (!trip.id) return
      
      if (trip.photos && trip.photos[0]?.photoUrl) {
        coverMap.value[trip.id] = trip.photos[0].photoUrl
      } else if (trip.destination) {
        try {
          const img = await fetchFirst(trip.destination)
          if (img) {
            coverMap.value[trip.id] = img.mediumUrl || img.largeUrl || ''
          }
        } catch (err) {
          // ignore
        }
      }
    })
  } catch (error: unknown) {
    const responseMessage =
      typeof error === 'object' && error !== null && 'response' in error
        ? (error as { response?: { data?: { message?: string } } }).response?.data?.message
        : undefined
    const runtimeMessage = error instanceof Error ? error.message : undefined
    const errorMsg = responseMessage || runtimeMessage || '加载行程失败'
    message.error(errorMsg)
  }
}

function openTrip(id?: number) {
  if (!id) {
    return
  }
  router.push(`/trips/${id}`)
}

onMounted(loadTrips)
</script>

<style scoped lang="scss">
.trips-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;

  h1 {
    margin: 0;
    font-size: 32px;
    font-weight: 700;
    color: #111827;
  }
}

.search-wrap {
  width: 260px;
}

:deep(.custom-search-input) {
  border-radius: 20px;
  padding: 6px 16px;
  border: 1px solid #e5e7eb;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);

  .ant-input {
    background: transparent;
  }
  .ant-input:focus {
    box-shadow: none;
  }
}

.trip-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.trip-card {
  display: flex;
  flex-direction: column;
  padding: 16px;
  border-radius: 24px;
  border: 1px solid transparent;
  background: #ffffff;
  text-align: left;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
  transition: transform 0.2s ease, box-shadow 0.2s ease;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 12px 32px rgba(0, 0, 0, 0.08);
  }
}

.card-cover {
  position: relative;
  width: 100%;
  height: 200px;
  border-radius: 16px;
  overflow: hidden;
  background-color: #e2e8f0;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #94a3b8 0%, #cbd5e1 100%);
}

.status-badge {
  position: absolute;
  top: 12px;
  right: 12px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  backdrop-filter: blur(4px);
  background: rgba(255, 255, 255, 0.9);

  &.planned {
    color: #3b82f6;
  }
  &.completed {
    color: #10b981;
  }
  
  .status-icon {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    border: 1.5px solid currentColor;
  }
}

.card-body {
  padding: 16px 4px 4px;
}

.trip-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.trip-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #111827;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.action-btn {
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  
  &.planned {
    background: #fca5a5;
    color: #fff;
  }
  
  &.completed {
    background: #60a5fa;
    color: #fff;
  }
}

.trip-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: #6b7280;
}

.meta-separator {
  width: 1px;
  height: 10px;
  background-color: #cbd5e1;
}

.meta-location {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

@media (max-width: 1024px) {
  .trip-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 640px) {
  .trip-grid {
    grid-template-columns: 1fr;
  }
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
}
</style>
