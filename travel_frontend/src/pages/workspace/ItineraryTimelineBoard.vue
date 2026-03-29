<template>
  <div class="timeline-board">
    <!-- 固定在屏幕上的主轴线，像齿轮轨道一样 -->
    <div v-if="timelineNodes.length" class="main-axis fixed-track"></div>

    <div v-if="timelineNodes.length" class="timeline-wrapper" @wheel.prevent="onWheel">
      <div class="timeline-scroll-area">
        <div class="timeline-items">
          <template v-for="(spot, index) in timelineNodes" :key="`${spot.day}-${spot.period}-${index}`">
            
            <!-- 天数分隔标记，直接坐在主轴上 -->
            <div v-if="isFirstOfDay(index)" class="day-divider">
              <div class="day-badge">第 {{ spot.day }} 天</div>
            </div>

            <!-- 时间轴重点节点 -->
            <div class="timeline-item" :class="index % 2 === 0 ? 'is-top' : 'is-bottom'">
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
                  </div>
                  <p class="spot-desc">{{ spot.description || '暂无游玩明细，可继续在底部对话框补充需求。' }}</p>
                </div>
              </div>
            </div>

          </template>

          <!-- 用这个空白块来把容器右侧物理撑开，真正起到延伸数轴的作用 -->
          <div class="timeline-end-spacer"></div>
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
import type { Activity, DailyPlan, StructuredItinerary } from '@/types/itinerary'

type DayPeriod = 'morning' | 'noon' | 'evening'

interface TimelineNode {
  day: number
  period: DayPeriod
  name: string
  description: string
  imageUrl?: string
}

const props = defineProps<{
  itinerary?: StructuredItinerary | null
}>()

const failedImages = ref<Set<string>>(new Set())

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

const timelineNodes = computed<TimelineNode[]>(() => {
  const plans = ensureArray<DailyPlan>(props.itinerary?.dailyPlans)
  const nodes: TimelineNode[] = []

  plans.forEach((plan) => {
    const activities = ensureArray<Activity>((plan as { activities?: unknown }).activities)
    if (activities.length === 0) {
      nodes.push({
        day: plan.day,
        period: 'noon',
        name: '自由活动 / 待定',
        description: '该天行程已暂时留空，你可以继续通过底部的对话以补充你的景点要求。',  
      })
      return
    }
    activities.forEach((activity) => {
      nodes.push({
        day: plan.day,
        period: normalizePeriod(activity.time),
        name: activity.name || '未命名活动',
        description: activity.description || '',
        imageUrl: activity.imageUrl,
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
</script>

<style scoped>
.timeline-board {
  width: 100%;
  height: 100%;
  min-height: 540px;
  display: flex;
  flex-direction: column;
  background-color: transparent;
  box-sizing: border-box;
  position: relative;
  overflow: hidden; /* 保证两侧内容滑出范围时被安全裁切，不再越界 */
}

.timeline-wrapper {
  flex: 1;
  height: 100%;
  overflow-x: auto;
  overflow-y: hidden;
  display: flex;
  align-items: center;
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
  width: max-content; /* 关键：允许无限自适应撑开宽度 */
  min-width: 100%;
  height: 560px;
  padding-left: 80px; /* 只保留左侧缩进 */
}

/* 用一个极宽的实际 DOM 节点去撑开 flex 容器，使得后面永远有空余去产生滑动的旷野感 */
.timeline-end-spacer {
  width: 120px; /* 限制最后可以滑出的宽度，不能无限滑了 */
  flex-shrink: 0;
  height: 1px;
}

/* 简洁版固定主轴 */
.main-axis.fixed-track {
  position: absolute;
  top: 50%;
  left: 0;
  width: 100%;
  height: 2px; /* 极简的细线 */
  background-color: #cbd5e1; /* 淡雅的灰线 */
  transform: translateY(-50%);
  z-index: 1; /* 在 wrapper 的底层 */
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

/* 节点容器分配固定水平宽度 */
.timeline-item {
  position: relative;
  width: 250px; 
  margin: 0 20px;
  height: 2px;
  display: flex;
  justify-content: center;
  align-items: center;
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
  height: 50px;
  background-color: #94a3b8;    /* 深色一点 */
  transform: translateX(-50%);
  z-index: 2;
}

/* 详情卡片 */
.spot-card {
  width: 260px;
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

.is-top .spot-card { bottom: 60px; }
.is-bottom .spot-card { top: 60px; }
.is-bottom .spot-card:hover {
  transform: translateX(-50%) translateY(4px) scale(1.02);
}

/* 卡片图片区 */
.card-image {
  width: 100%;
  height: 120px;
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
  height: 120px;
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
  padding: 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
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
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.spot-desc {
  margin: 0;
  font-size: 13px;
  color: #475569;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
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
</style>
