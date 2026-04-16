<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeftOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { completeTrip, getTripById } from '@/api/tripController'
import TripDaySwitcher from '@/components/trips/detail/TripDaySwitcher.vue'
import TripDetailSummaryCards from '@/components/trips/detail/TripDetailSummaryCards.vue'
import TripItineraryTimeline from '@/components/trips/detail/TripItineraryTimeline.vue'
import DynamicMap from '@/pages/workspace/DynamicMap.vue'
import type { StructuredItinerary } from '@/types/itinerary'
import {
  formatWorkflowDate,
  formatWorkflowDateTime,
  getTripStatusLabel,
  getTripStatusTone,
  normalizeTripStatus,
} from '@/utils/tripWorkflow'
import {
  buildItinerarySummaryItems,
  filterItineraryByDay,
  parseStructuredTripData,
} from '@/utils/tripDetail'

type SelectedDay = number | 'all'
type TripDetailRecord = API.TripVO & { structuredData?: string }

const route = useRoute()
const router = useRouter()

const trip = ref<TripDetailRecord | null>(null)
const structuredItinerary = ref<StructuredItinerary | null>(null)
const selectedDay = ref<SelectedDay>('all')
const selectedActivityKey = ref<string | null>(null)
const completing = ref(false)

const hasStructuredData = computed(() => Boolean(trip.value?.structuredData?.trim()))

const availableDays = computed(() => {
  if (!structuredItinerary.value) {
    return []
  }

  return structuredItinerary.value.dailyPlans
    .map((plan) => Number(plan.day))
    .filter((day) => Number.isFinite(day))
    .sort((left, right) => left - right)
})

const visibleItinerary = computed(() =>
  filterItineraryByDay(structuredItinerary.value, selectedDay.value),
)

const summaryItems = computed(() => buildItinerarySummaryItems(structuredItinerary.value))

const isSummaryExpanded = ref(true)

const highlightEntries = computed(() => {
  const highlights = trip.value?.dailyHighlights
  if (!highlights) {
    return []
  }

  return Object.entries(highlights)
    .sort((a, b) => Number(a[0]) - Number(b[0]))
    .map(([day, items]) => ({
      day: Number(day),
      items: Array.isArray(items) ? items.filter(Boolean) : [],
    }))
    .filter((item) => item.items.length > 0)
})

const totalHighlightCount = computed(() =>
  highlightEntries.value.reduce((sum, item) => sum + item.items.length, 0),
)

function setOverviewState(nextStructuredItinerary: StructuredItinerary | null) {
  structuredItinerary.value = nextStructuredItinerary
  selectedActivityKey.value = null

  if (!nextStructuredItinerary || availableDays.value.length === 0) {
    selectedDay.value = 'all'
    return
  }

  selectedDay.value = availableDays.value[0]
}

function handleSelectDay(value: SelectedDay) {
  selectedDay.value = value
  selectedActivityKey.value = null
}

function handleSelectActivity(key: string) {
  selectedActivityKey.value = key
}

function handleMapSelectActivity(key: string) {
  selectedActivityKey.value = key
}

function goBack() {
  router.push('/trips')
}

async function refreshTrip() {
  const id = Number(route.params.id)
  if (!id) {
    return
  }

  try {
    const resp = await getTripById({ id })
    const fetchedTrip = (resp?.data?.data || null) as TripDetailRecord | null
    trip.value = fetchedTrip

    const parsed = parseStructuredTripData(fetchedTrip?.structuredData)
    setOverviewState(parsed)

    if (fetchedTrip?.structuredData && !parsed) {
      message.warning('结构化数据解析失败，当前已回退到基础摘要视图')
    }
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

async function markCompleted() {
  if (!trip.value?.id) {
    return
  }

  completing.value = true
  try {
    await completeTrip({ id: Number(trip.value.id) })
    message.success('已标记为已完成')
    await refreshTrip()
  } catch (error: unknown) {
    const responseMessage =
      typeof error === 'object' && error !== null && 'response' in error
        ? (error as { response?: { data?: { message?: string } } }).response?.data?.message
        : undefined
    const runtimeMessage = error instanceof Error ? error.message : undefined
    const errorMsg = responseMessage || runtimeMessage || '操作失败'
    message.error(errorMsg)
  } finally {
    completing.value = false
  }
}

function handlePhotosChanged(photos: API.TripPhotoVO[]) {
  if (!trip.value) {
    return
  }

  trip.value = {
    ...trip.value,
    photos,
  }
}

watch(
  () => route.params.id,
  async () => {
    await refreshTrip()
  },
)

onMounted(async () => {
  await refreshTrip()
})
</script>

<template>
  <div v-if="trip" class="trip-layout">
    <!-- 左侧侧边栏 -->
    <aside class="sidebar custom-scroll">
      <!-- 固定的侧边栏头部区域 -->
      <div class="sidebar-head">
        <div class="sidebar-top-actions">
          <a-button type="text" class="back-btn" @click="goBack">
            <ArrowLeftOutlined />
          </a-button>
          <span class="head-title">itinerary Details</span>
        </div>
      </div>

      <!-- 侧边栏可滚动的核心主内容区 -->
      <div class="sidebar-scrollable custom-scroll">
        <template v-if="structuredItinerary">
          <!-- 日期切换器，跟随滚动 -->
          <div class="timeline-day-switcher">
            <TripDaySwitcher
              :days="availableDays"
              :selected-day="selectedDay"
              @select="handleSelectDay"
            />
          </div>
            
            <!-- 时间线行程列表 -->
            <div class="timeline-list-wrap">
              <TripItineraryTimeline
                :itinerary="visibleItinerary"
                :selected-activity-key="selectedActivityKey"
                @select-activity="handleSelectActivity"
              />
            </div>
        </template>

        <template v-else>
          <div class="fallback-summary">
             <h3 class="fallback-title">非结构化概览</h3>
             <p class="fallback-desc">{{ hasStructuredData ? '无法解析结构数据。' : '当前行程无详细时间线。' }}</p>
          </div>
        </template>
      </div>
    </aside>

    <!-- 右侧主视觉（地图与悬浮面板） -->
    <main class="main-view">
      <!-- 沉浸式全屏地图 -->
      <div class="map-bg">
        <DynamicMap
          :itinerary="visibleItinerary"
          :selected-activity-key="selectedActivityKey"
          @select-activity="handleMapSelectActivity"
        />
      </div>

      <!-- 悬浮层 (Glassmorphism overlays) -->
      <div class="overlays-layer pointer-events-none">
        <!-- 顶部信息浮层 -->
        <div class="top-overlay glass-card pointer-events-auto">
          <div class="top-stats">
            <div class="stat-block">
              <span class="stat-val">{{ trip.days || structuredItinerary?.days || 0 }}</span>
              <span class="stat-label">DAYS</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-block">
              <span class="stat-val">00</span>
              <span class="stat-label">HRS</span>
            </div>
          </div>
          <div class="trip-summary-info">
            <div class="user-avatar-placeholder">A</div>
            <div class="trip-meta-text">
              <h4>{{ trip.destination || '未命名行程' }}</h4>
              <p>{{ trip.theme || '通用旅行' }} · {{ formatWorkflowDate(trip.startDate) || '时间待定' }}</p>
            </div>
          </div>
        </div>

        <!-- 右下角信息浮层 -->
        <div v-if="structuredItinerary" class="right-overlay glass-card pointer-events-auto" :class="{ 'collapsed': !isSummaryExpanded }">
          <div class="summary-card-header">
            <h3>行程统计</h3>
            <button class="collapse-btn" @click="isSummaryExpanded = !isSummaryExpanded" title="折叠/展开">
              <svg v-if="isSummaryExpanded" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="18 15 12 9 6 15"></polyline></svg>
              <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="6 9 12 15 18 9"></polyline></svg>
            </button>
          </div>
          <!-- 收缩控制的内容层 -->
          <div class="summary-content-wrap" v-show="isSummaryExpanded">
            <TripDetailSummaryCards :items="summaryItems" />
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped lang="scss">
.trip-layout {
  display: flex;
  height: 100%;
  width: 100%;
  overflow: hidden;
  background: #F4F7FB;
  border-radius: 20px; /* 圆角呈现类卡片/APP的效果 */
  box-shadow: 0 12px 48px rgba(15, 23, 42, 0.06); /* 增加悬浮投影感 */
  /* 去除了 fixed 全屏定位，回归正常的文档流，受限于 TopNavLayout 的 1320px max-width */
}

/* === 左侧栏 === */
.sidebar {
  width: 480px;
  background: white;
  display: flex;
  flex-direction: column;
  z-index: 10;
  box-shadow: 4px 0 24px rgba(0, 0, 0, 0.04);
}

.sidebar-head {
  padding: 32px 40px 24px;
}

.sidebar-top-actions {
  display: flex;
  align-items: center;
  gap: 16px;

  .back-btn {
    padding: 0;
    font-size: 20px;
  }

  .head-title {
    font-size: 28px;
    font-weight: 700;
    color: #1a1a1a;
  }
}

.sidebar-scrollable {
  flex: 1;
  overflow-y: auto;
  padding: 0 40px 40px;
}

.timeline-day-switcher {
  margin-bottom: 20px;
}

.timeline-list-wrap {
  /* 自定义时间线内容的样式可以放这里，覆盖子组件的背景 */
  :deep(.timeline-card) {
    background: #fff;
    border: 1px solid #f0f0f0;
    box-shadow: 0 4px 16px rgba(0,0,0,0.03);
    border-radius: 16px;
  }
}

.fallback-summary,
.photos-tab-wrap {
  margin-top: 20px;
}

.ai-input-dock {
  padding: 16px 24px 24px;
  background: white;
  border-top: 1px solid #f0f4f9;
}

.ai-input-bar {
  display: flex;
  align-items: center;
  background: white;
  border-radius: 30px;
  padding: 8px 8px 8px 20px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.ai-icon-left {
  color: #fbbf24;
  font-size: 18px;
}

.ai-input-field {
  flex: 1;
  border: none;
  outline: none;
  padding: 8px 12px;
  font-size: 14px;
  color: #333;
  &::placeholder {
    color: #a0aec0;
  }
}

.ai-send-btn {
  background: #3b82f6;
  border: none;
  border-radius: 50%;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: white;
  transition: transform 0.1s;
  &:hover {
    transform: scale(1.05);
  }
}

/* === 右侧主视觉 === */
.main-view {
  flex: 1;
  position: relative;
}

.map-bg {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  /* 取消现有DynamicMap内的padding等限制，让地图直接撑满 */
  :deep(.map-container) {
    height: 100% !important;
    width: 100% !important;
    border-radius: 0 !important;
    margin: 0 !important;
  }
}

/* === 悬浮层通用类 === */
.overlays-layer {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  z-index: 20;
  padding: 24px;
}

.pointer-events-none { pointer-events: none; }
.pointer-events-auto { pointer-events: auto; }

.glass-card {
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.4);
  border-radius: 20px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.05);
}

/* 顶部信息悬浮 */
.top-overlay {
  display: inline-flex;
  align-items: center;
  gap: 32px;
  padding: 16px 24px;
  position: absolute;
  top: 32px;
  left: 50%;
  transform: translateX(-50%);
}

.top-stats {
  display: flex;
  align-items: center;
  gap: 20px;
}

.stat-block {
  display: flex;
  flex-direction: column;
  align-items: center;

  .stat-val {
    font-size: 18px;
    font-weight: 700;
    line-height: 1.2;
    color: #1e293b;
  }
  .stat-label {
    font-size: 10px;
    color: #64748b;
    letter-spacing: 1px;
  }
}

.stat-divider {
  height: 24px;
  width: 1px;
  background: rgba(0, 0, 0, 0.1);
}

.trip-summary-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-left: 12px;
  border-left: 1px solid rgba(0, 0, 0, 0.1);

  .user-avatar-placeholder {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    background: #e2e8f0;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: bold;
    color: #475569;
  }

  .trip-meta-text {
    h4 {
      margin: 0;
      font-size: 14px;
      font-weight: 600;
      color: #1e293b;
    }
    p {
      margin: 2px 0 0;
      font-size: 12px;
      color: #64748b;
    }
  }
}

/* 右下侧图表悬浮 */
.right-overlay {
  position: absolute;
  bottom: 24px;
  right: 24px;
  width: 280px;
  padding: 16px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  transform-origin: bottom right;

  &.collapsed {
    width: 140px;
    padding: 12px 16px;

    .summary-card-header {
      margin-bottom: 0;
      h3 { font-size: 14px; }
    }
  }

  .summary-card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    h3 {
      margin: 0;
      font-size: 15px;
      font-weight: 600;
    }

    .collapse-btn {
      background: rgba(0, 0, 0, 0.04);
      border: 1px solid rgba(0, 0, 0, 0.05);
      border-radius: 50%;
      width: 26px;
      height: 26px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #64748b;
      cursor: pointer;
      transition: all 0.2s;

      &:hover {
        background: rgba(0, 0, 0, 0.08);
        color: #334155;
      }
    }
  }
}

/* 滚动条优化 */
.custom-scroll {
  &::-webkit-scrollbar {
    width: 6px;
  }
  &::-webkit-scrollbar-track {
    background: transparent;
  }
  &::-webkit-scrollbar-thumb {
    background-color: rgba(0, 0, 0, 0.1);
    border-radius: 10px;
  }
}
</style>
