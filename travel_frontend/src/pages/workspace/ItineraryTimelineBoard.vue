<template>
  <div class="timeline-board">
    <!-- 移除了外层的 main-axis fixed-track -->

    <div v-if="timelineNodes.length" class="timeline-shell">
      <div class="main-axis fixed-track"></div>

      <div class="timeline-wrapper" @wheel.prevent="onWheel">
        <div class="timeline-scroll-area">
          <div class="timeline-items">
          <template v-for="(spot, index) in timelineNodes" :key="spot.nodeKey">
            
            <!-- 天数分隔标记，直接坐在主轴上 -->
            <div v-if="isFirstOfDay(index)" class="day-divider">
              <div class="day-badge" :class="{ 'is-changed': dayHasChanged(spot.day) }">第 {{ spot.day }} 天</div>
            </div>

            <!-- 时间轴重点节点 -->
            <div
              class="timeline-item"
              :class="[index % 2 === 0 ? 'is-top' : 'is-bottom', { 'is-changed': spot.isChanged }]"
            >
              <div class="axis-dot"></div>
              <div class="connector"></div>
              
              <!-- 详情卡片 -->
              <div class="spot-card">
                <div class="card-image" v-if="spot.imageUrl && !isImageFailed(spot.imageUrl)">
                  <img :src="spot.imageUrl" :alt="spot.name" @error="onImageError(spot.imageUrl)" />
                </div>
                <div class="card-image-placeholder" v-else>
                  <span class="placeholder-text">{{ spot.name.substring(0, 2) }}</span>
                </div>
                
                <div class="card-body">
                  <div class="card-header">
                    <span class="period-badge" :class="spot.period">{{ periodLabel(spot.period) }}</span>
                    <h4 class="spot-title" :title="spot.name">{{ spot.name }}</h4>
                    <span v-if="spot.isChanged" class="spot-change-tag">更新</span>
                  </div>
                  <p class="spot-desc">{{ spot.description || '暂无游玩明细，可继续在底部对话框补充需求。' }}</p>
                  <div
                    v-if="spot.address || (spot.estimatedCost && spot.estimatedCost > 0)"
                    class="spot-meta"
                  >
                    <span v-if="spot.address" class="meta-address">{{ spot.address }}</span>
                    <span v-if="spot.estimatedCost && spot.estimatedCost > 0" class="meta-cost">
                      约 ¥{{ spot.estimatedCost }}
                    </span>
                  </div>
                </div>
              </div>
            </div>

          </template>

          <!-- 末尾留小缓冲，避免最后卡片贴边 -->
            <div class="timeline-end-spacer"></div>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="empty-state">
      <h3>等待生成行程</h3>
      <p>在底部输入需求后，这里会展示结构化行程结果。</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type {
  Activity,
  DailyPlan,
  ItineraryDiffSummary,
  ItineraryResponseMeta,
  StructuredItinerary,
} from '@/types/itinerary'

type DayPeriod = 'morning' | 'noon' | 'evening'

interface TimelineNode {
  nodeKey: string
  day: number
  period: DayPeriod
  name: string
  description: string
  imageUrl?: string
  address?: string
  estimatedCost?: number
  isChanged?: boolean
}

const props = defineProps<{
  itinerary?: StructuredItinerary | null
  meta?: ItineraryResponseMeta | null
  diff?: ItineraryDiffSummary | null
}>()

const failedImages = ref<Set<string>>(new Set())
const metaDropdownOpen = ref(false)

function ensureArray<T>(value: unknown): T[] {
  return Array.isArray(value) ? (value as T[]) : []
}

function isImageFailed(imageUrl?: string): boolean {
  if (!imageUrl) return false
  return failedImages.value.has(imageUrl)
}

function onImageError(imageUrl?: string) {
  if (!imageUrl) return
  failedImages.value.add(imageUrl)
}

function normalizePeriod(raw: string): DayPeriod {
  const value = (raw || '').trim().toLowerCase()
  if (value.includes('morning') || value.includes('早') || value.includes('上午')) return 'morning'
  if (value.includes('noon') || value.includes('afternoon') || value.includes('午')) return 'noon'
  if (value.includes('evening') || value.includes('night') || value.includes('晚')) return 'evening'
  return 'morning'
}

function periodLabel(period: DayPeriod): string {
  if (period === 'morning') return '上午'
  if (period === 'noon') return '下午'
  return '晚上'
}

function positiveNumber(value: unknown): number | undefined {
  const num = Number(value)
  return Number.isFinite(num) && num > 0 ? num : undefined
}

const summaryMeta = computed(() => {
  if (!timelineNodes.value.length) {
    return null
  }

  const intentType = props.meta?.intentType || 'ITINERARY_GENERATION'
  const intentLabelMap: Record<string, string> = {
    ITINERARY_GENERATION: '意图：行程规划',
    ATTRACTION_QUERY: '意图：景点查询',
    GENERAL_CHAT: '意图：通用聊天',
  }

  const sourceLabelMap: Record<string, string> = {
    'stream-marker': '流式结构化标记',
    'sync-metadata': '同步 metadata 回退',
    none: '未识别结构化结果',
  }

  return {
    intentLabel: intentLabelMap[intentType] || `意图：${intentType}`,
    sourceLabel: sourceLabelMap[props.meta?.structuredSource || 'none'] || '未识别结构化结果',
    activityCount: props.meta?.activityCount || timelineNodes.value.length,
    totalEstimatedCost: props.meta?.totalEstimatedCost || positiveNumber(props.itinerary?.totalEstimatedCost),
  }
})

const diffMeta = computed(() => {
  const diff = props.diff
  if (!diff) {
    return null
  }

  const changedDaysLabel = diff.changedDays.length
    ? diff.changedDays.map((day) => `第${day}天`).join('、')
    : ''

  return {
    round: diff.round,
    addedCount: diff.addedCount,
    removedCount: diff.removedCount,
    updatedCount: diff.updatedCount,
    changedDaysLabel,
  }
})

function dayHasChanged(day: number): boolean {
  return Boolean(props.diff?.changedDays.includes(day))
}

const timelineNodes = computed<TimelineNode[]>(() => {
  const plans = ensureArray<DailyPlan>(props.itinerary?.dailyPlans)
  const nodes: TimelineNode[] = []
  const occurrenceMap = new Map<string, number>()
  const changedKeys = new Set(props.diff?.changedNodeKeys || [])

  plans.forEach((plan) => {
    const activities = ensureArray<Activity>((plan as { activities?: unknown }).activities)
    if (activities.length === 0) {
      const baseKey = `${plan.day}|noon|自由活动 / 待定`
      const count = (occurrenceMap.get(baseKey) || 0) + 1
      occurrenceMap.set(baseKey, count)
      const nodeKey = `${baseKey}#${count}`
      nodes.push({
        nodeKey,
        day: plan.day,
        period: 'noon',
        name: '自由活动 / 待定',
        description: '该天行程已暂时留空，你可以继续通过底部的对话以补充你的景点要求。',  
        isChanged: changedKeys.has(nodeKey),
      })
      return
    }
    activities.forEach((activity) => {
      const period = normalizePeriod(activity.time)
      const baseKey = `${plan.day}|${period}|${(activity.name || '').trim().toLowerCase()}`
      const count = (occurrenceMap.get(baseKey) || 0) + 1
      occurrenceMap.set(baseKey, count)
      const nodeKey = `${baseKey}#${count}`

      nodes.push({
        nodeKey,
        day: plan.day,
        period,
        name: activity.name || '未命名活动',
        description: activity.description || '',
        imageUrl: activity.imageUrl,
        address: activity.location?.address || '',
        estimatedCost: positiveNumber(activity.estimatedCost),
        isChanged: changedKeys.has(nodeKey),
      })
    })
  })
  return nodes
})

function isFirstOfDay(index: number) {
  if (index === 0) return true
  return timelineNodes.value[index].day !== timelineNodes.value[index - 1].day
}

function onWheel(e: WheelEvent) {
  const el = e.currentTarget as HTMLElement
  if (e.deltaY !== 0) {
    el.scrollLeft += e.deltaY
  }
}

function toggleMetaDropdown() {
  metaDropdownOpen.value = !metaDropdownOpen.value
}
</script>

<style scoped>
.timeline-board {
  width: 100%;
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background-color: transparent;
  box-sizing: border-box;
  position: relative;
  overflow: hidden; /* 保证两侧内容滑出范围时被安全裁切，不再越界 */
}

.meta-dropdown {
  position: absolute;
  top: 8px;
  right: 8px;
  z-index: 12;
}

.meta-trigger {
  border: 1px solid rgba(15, 23, 42, 0.18);
  background: rgba(255, 255, 255, 0.94);
  color: #0f172a;
  border-radius: 999px;
  padding: 5px 11px;
  font-size: 12px;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.12);
}

.meta-panel {
  margin-top: 6px;
  min-width: 220px;
  max-width: 300px;
  border-radius: 10px;
  border: 1px solid rgba(15, 23, 42, 0.12);
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.16);
  padding: 8px;
}

.meta-row {
  font-size: 12px;
  color: #334155;
  line-height: 1.4;
  padding: 2px 4px;
}

.meta-row-strong {
  color: #1d4ed8;
  font-weight: 600;
}

.timeline-shell {
  flex: 1;
  min-height: 0;
  display: flex;
  align-items: center;
  position: relative;
  z-index: 2;
  margin-top: 0;
  overflow: hidden;
}

.timeline-wrapper {
  flex: 1;
  width: 100%;
  min-width: 0;
  min-height: 0;
  overflow-x: auto;
  overflow-y: hidden;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  position: relative;
  z-index: 2;
  /* 隐藏滚动条，增强无边界感 */
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE/Edge */
}

/* 隐藏滚动条（Chrome/Safari）*/
.timeline-wrapper::-webkit-scrollbar {
  display: none;
}

.timeline-scroll-area {
  position: relative;
  display: flex;
  align-items: center;
  width: max-content;
  min-width: 100%;
  height: 560px;
  box-sizing: border-box;
  padding-left: 80px; /* 只保留左侧缩进 */
  padding-right: 80px;
  padding-top: 12px;
  padding-bottom: 12px;
}

.timeline-end-spacer {
  width: 48px;
  flex-shrink: 0;
  height: 1px;
}

/* 简洁版固定主轴 */
.main-axis.fixed-track {
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 2px; /* 极简的细线 */
  background-color: #cbd5e1; /* 淡雅的灰线 */
  transform: translateY(-50%);
  z-index: 1;
  pointer-events: none;
}

/* 极简右侧箭头 */
.main-axis.fixed-track::after {
  content: '';
  position: absolute;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  border-width: 6px 0 6px 8px;
  border-style: solid;
  border-color: transparent transparent transparent #cbd5e1;
}

.timeline-items {
  display: flex;
  align-items: center;
  height: 100%;
  z-index: 2;
}

/* 天数提示 */
.day-divider {
  width: 140px;
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 5;
}

.day-badge {
  background: #2563eb;
  color: #fff;
  padding: 8px 20px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: bold;
  box-shadow: 0 4px 10px rgba(37, 99, 235, 0.3);
  position: relative;
  white-space: nowrap;
  letter-spacing: 1px;
}

.day-badge.is-changed {
  background: linear-gradient(135deg, #f97316, #f59e0b);
  box-shadow: 0 4px 12px rgba(249, 115, 22, 0.4);
}

/* 节点容器分配固定水平宽度 */
.timeline-item {
  position: relative;
  width: 200px; 
  margin: 0 10px;
  height: 2px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.timeline-item.is-changed .axis-dot {
  border-color: #f97316;
  box-shadow: 0 0 0 6px rgba(249, 115, 22, 0.22);
}

.timeline-item.is-changed .spot-card {
  border-color: rgba(249, 115, 22, 0.65);
}

/* 轴上的原点 */
.axis-dot {
  position: absolute;
  top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  width: 18px; height: 18px;
  background-color: #fff;
  border: 4px solid #2563eb;    /* 明显的蓝色圆点 */
  border-radius: 50%;
  z-index: 3;
}

/* 连接主轴与卡片的线 */
.connector {
  position: absolute;
  left: 50%;
  width: 3px;
  height: 18px;
  background-color: #94a3b8;    /* 深色一点 */
  transform: translateX(-50%);
  z-index: 2;
}

/* 详情卡片 */
.spot-card {
  width: 220px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  overflow: hidden;
  border: 1px solid #e2e8f0;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  z-index: 4;
  display: flex;
  flex-direction: column;
}

.spot-card:hover {
  transform: translateX(-50%) translateY(-4px) scale(1.02);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.12);
  z-index: 10;
}

/* 上下交错布局 */
.is-top .connector { bottom: 0; }
.is-bottom .connector { top: 0; }

.is-top .spot-card { bottom: 24px; }
.is-bottom .spot-card { top: 24px; }
.is-bottom .spot-card:hover {
  transform: translateX(-50%) translateY(4px) scale(1.02);
}

/* 卡片图片区 */
.card-image {
  width: 100%;
  height: 104px;
  background: #f8fafc;
  overflow: hidden;
}
.card-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.card-image-placeholder {
  width: 100%;
  height: 104px;
  background: linear-gradient(135deg, #e0e7ff 0%, #bfdbfe 100%);
  display: flex;
  align-items: center;
  justify-content: center;
}

.placeholder-text {
  font-size: 28px;
  font-weight: bold;
  color: #60a5fa;
  letter-spacing: 2px;
}

/* 卡片文本区 */
.card-body {
  padding: 12px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.period-badge {
  font-size: 11px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 12px;
  flex-shrink: 0;
}
.period-badge.morning { background: #dbeafe; color: #1d4ed8; }
.period-badge.noon { background: #fef3c7; color: #b45309; }
.period-badge.evening { background: #f3e8ff; color: #7e22ce; }

.spot-title {
  margin: 0;
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.spot-change-tag {
  margin-left: auto;
  font-size: 11px;
  color: #9a3412;
  background: rgba(251, 146, 60, 0.2);
  border: 1px solid rgba(249, 115, 22, 0.5);
  border-radius: 999px;
  padding: 2px 6px;
}

.spot-desc {
  margin: 0;
  font-size: 13px;
  color: #475569;
  line-height: 1.45;
  line-clamp: 2;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.spot-meta {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.meta-address {
  font-size: 12px;
  color: #64748b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.meta-cost {
  font-size: 12px;
  color: #0f766e;
  font-weight: 600;
  white-space: nowrap;
}

.empty-state {
  height: 100%;
  min-height: 480px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #64748b;
}

.empty-state h3 {
  color: #1e293b;
  margin-bottom: 12px;
  font-size: 20px;
}

.empty-state p {
  font-size: 15px;
}

@media (max-width: 960px) {
  .timeline-board {
    min-height: 0;
  }

  .meta-trigger {
    font-size: 11px;
    padding: 4px 9px;
  }

  .meta-panel {
    min-width: 200px;
    max-width: 220px;
  }

  .timeline-scroll-area {
    height: 520px;
    padding-left: 56px;
    padding-top: 10px;
    padding-bottom: 10px;
  }

  .is-top .spot-card {
    bottom: 26px;
  }

  .is-bottom .spot-card {
    top: 26px;
  }
}
</style>
