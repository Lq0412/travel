<template>
  <div class="timeline-board">
    <div v-if="timelineNodes.length" class="timeline-wrapper">
      <div class="timeline-scroll-area">
        <div class="main-axis"></div>
        <div class="timeline-items">
          <div
            v-for="(spot, index) in timelineNodes"
            :key="`${spot.day}-${spot.period}-${spot.name}-${index}`"
            class="timeline-item"
            :class="index % 2 === 0 ? 'is-top' : 'is-bottom'"
          >
            <div class="axis-dot"></div>
            <div class="connector"></div>
            <div class="spot-card">
              <div class="spot-image">
                <img v-if="spot.imageUrl" :src="spot.imageUrl" :alt="spot.name" />
                <span v-else>图片区域</span>
              </div>
              <div class="spot-desc">
                <div class="spot-name">D{{ spot.day }} · {{ periodLabel(spot.period) }} · {{ spot.name }}</div>
                <div class="spot-detail">{{ spot.description || '可继续在底部对话里补充该景点安排。' }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="empty-state">
      <h3>等待生成行程</h3>
      <p>在底部输入需求后，这里会展示时间轴结果。</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { StructuredItinerary } from '@/types/itinerary'

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

function normalizePeriod(raw: string): DayPeriod {
  const value = (raw || '').trim().toLowerCase()
  if (value.includes('morning') || value.includes('早') || value.includes('上午')) {
    return 'morning'
  }
  if (value.includes('noon') || value.includes('afternoon') || value.includes('中') || value.includes('午')) {
    return 'noon'
  }
  if (value.includes('evening') || value.includes('night') || value.includes('晚')) {
    return 'evening'
  }
  return 'morning'
}

function periodLabel(period: DayPeriod): string {
  if (period === 'morning') {
    return '早'
  }
  if (period === 'noon') {
    return '中'
  }
  return '晚'
}

const timelineNodes = computed<TimelineNode[]>(() => {
  const plans = props.itinerary?.dailyPlans || []
  const nodes: TimelineNode[] = []

  plans.forEach((plan) => {
    ;(plan.activities || []).forEach((activity) => {
      nodes.push({
        day: plan.day,
        period: normalizePeriod(activity.time),
        name: activity.name || '未命名景点',
        description: activity.description || '',
        imageUrl: activity.imageUrl,
      })
    })
  })

  return nodes
})
</script>

<style scoped>
.timeline-board {
  width: 100%;
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background-color: #f7f9fc;
  box-sizing: border-box;
}

.empty-state {
  height: 100%;
  min-height: 280px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #4b5563;
  padding: 24px;
}

.empty-state h3 {
  margin: 0;
  font-size: 20px;
  color: #101828;
}

.empty-state p {
  margin: 12px 0 0;
  font-size: 14px;
}

.timeline-wrapper {
  flex: 1;
  overflow-x: auto;
  overflow-y: hidden;
  position: relative;
  display: flex;
  align-items: center;
  padding: 0 28px;
}

.timeline-wrapper::-webkit-scrollbar {
  height: 8px;
}

.timeline-wrapper::-webkit-scrollbar-thumb {
  background: #ccc;
  border-radius: 4px;
}

.timeline-scroll-area {
  position: relative;
  display: flex;
  align-items: center;
  width: max-content;
  min-width: 100%;
  height: 100%;
}

.main-axis {
  position: absolute;
  top: 50%;
  left: 0;
  right: 0;
  height: 4px;
  background-color: #333;
  transform: translateY(-50%);
  z-index: 1;
}

.main-axis::after {
  content: '';
  position: absolute;
  right: -2px;
  top: 50%;
  transform: translateY(-50%);
  border-width: 8px 0 8px 12px;
  border-style: solid;
  border-color: transparent transparent transparent #333;
}

.timeline-items {
  display: flex;
  align-items: center;
  gap: 140px;
  padding: 0 80px;
  height: 100%;
  position: relative;
  z-index: 2;
}

.timeline-item {
  position: relative;
  width: 220px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.axis-dot {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 14px;
  height: 14px;
  background-color: #fff;
  border: 4px solid #333;
  border-radius: 50%;
  z-index: 3;
}

.spot-card {
  width: 100%;
  background-color: #fff;
  border: 2px solid #ff5a5a;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(255, 90, 90, 0.15);
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  overflow: hidden;
  z-index: 4;
}

.spot-image {
  height: 120px;
  background-color: #ffe8e8;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #ff5a5a;
  font-weight: bold;
}

.spot-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.spot-desc {
  padding: 12px;
  border-top: 2px dashed #ff5a5a;
  text-align: center;
}

.spot-name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 6px;
}

.spot-detail {
  font-size: 13px;
  color: #666;
  line-height: 1.4;
}

.is-top .spot-card {
  bottom: 60px;
}

.is-top .connector {
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 2px;
  height: 48px;
  background-color: #333;
}

.is-top .connector::before {
  content: '';
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  border-width: 0 6px 8px 6px;
  border-style: solid;
  border-color: transparent transparent #333 transparent;
}

.is-bottom .spot-card {
  top: 60px;
}

.is-bottom .connector {
  position: absolute;
  top: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 2px;
  height: 48px;
  background-color: #333;
}

.is-bottom .connector::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  border-width: 8px 6px 0 6px;
  border-style: solid;
  border-color: #333 transparent transparent transparent;
}
</style>
