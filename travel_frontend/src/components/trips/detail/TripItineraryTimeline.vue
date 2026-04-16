<script setup lang="ts">
import { computed } from 'vue'
import type { Activity, DailyPlan, StructuredItinerary } from '@/types/itinerary'
import { buildStructuredActivityKey } from '@/utils/tripDetail'

interface TimelineNode {
  key: string
  day: number
  time: string
  name: string
  description: string
  type: string
  address: string
  estimatedCost?: number
  imageUrl?: string
  tips: string[]
}

const props = defineProps<{
  itinerary: StructuredItinerary | null
  selectedActivityKey: string | null
}>()

const emit = defineEmits<{
  selectActivity: [key: string]
}>()

function ensureArray<T>(value: unknown): T[] {
  return Array.isArray(value) ? (value as T[]) : []
}

function typeLabel(type: string) {
  const labelMap: Record<string, string> = {
    attraction: '景点',
    transport: '交通',
    rest: '休息',
    meal: '餐饮',
  }
  return labelMap[type] || '安排'
}

const dayGroups = computed(() =>
  ensureArray<DailyPlan>(props.itinerary?.dailyPlans).map((plan) => ({
    day: plan.day,
    transportAdvice: plan.transportAdvice || '',
    meals: ensureArray<{ time?: string; recommendation?: string }>(plan.meals),
    nodes: ensureArray<Activity>(plan.activities).map((activity, index) => ({
      key: buildStructuredActivityKey(plan.day, activity, index),
      day: plan.day,
      time: activity.time || '待定',
      name: activity.name || '未命名活动',
      description: activity.description || '当前活动暂无进一步说明。',
      type: typeLabel(activity.type),
      address: activity.location?.address || '',
      estimatedCost: typeof activity.estimatedCost === 'number' ? activity.estimatedCost : undefined,
      imageUrl: activity.imageUrl,
      tips: ensureArray<string>(activity.tips).filter(Boolean),
    })),
  })),
)
</script>

<template>
  <section v-if="dayGroups.length" class="timeline-panel">
    <section v-for="group in dayGroups" :key="group.day" class="day-section">
      <header class="day-header">
        <div>
          <h3 class="day-title">Day {{ group.day }}</h3>
          <p v-if="group.transportAdvice" class="day-subtitle">{{ group.transportAdvice }}</p>
        </div>
      </header>

      <div v-if="group.nodes.length" class="timeline-list">
        <article
          v-for="(node, index) in group.nodes"
          :key="node.key"
          class="timeline-row"
          :class="{ active: selectedActivityKey === node.key }"
          @click="emit('selectActivity', node.key)"
        >
          <!-- 左侧时间戳和节点轴 -->
          <div class="timeline-time">
            <span class="time-text">{{ node.time }}</span>
            <div class="time-axis">
              <span class="time-dot" :class="{'is-active': selectedActivityKey === node.key}"></span>
              <div v-if="index !== group.nodes.length - 1" class="time-line"></div>
            </div>
          </div>

          <!-- 右侧卡片内容 -->
          <div class="timeline-card">
            <!-- 占满上半部分的封面图 -->
            <div class="timeline-cover">
              <img v-if="node.imageUrl" :src="node.imageUrl" :alt="node.name" />
              <!-- 如果没有图片，可以用预设的渐变色或文字兜底，但撑满上半部 -->
              <div v-else class="cover-fallback">
                <span class="fallback-text">{{ node.name.slice(0, 2) }}</span>
              </div>
              <div class="timeline-time-badge">{{ node.time }}</div>
              <button class="more-btn"><span class="icon-down"></span></button>
            </div>

            <!-- 下半部文本信息 -->
            <div class="timeline-body">
              <div class="timeline-headline">
                <div class="title-with-icon">
                  <span class="location-icon">
                    <svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor">
                      <path d="M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5a2.5 2.5 0 010-5 2.5 2.5 0 010 5z"/>
                    </svg>
                  </span>
                  <div class="title-group">
                    <h4 class="timeline-title">{{ node.name }}</h4>
                    <!-- 截断或隐藏过长描述，保留清爽外观。鼠标Hover可展开或点开抽屉，这里做单行截断 -->
                    <p class="timeline-description-short">{{ node.description || '探索周边环境，开启新的一段旅程。' }}</p>
                  </div>
                </div>
                <button class="action-dots">...</button>
              </div>
            </div>
          </div>
        </article>
      </div>

      <a-empty v-else description="当天还没有活动安排" />

      <div v-if="group.meals.length" class="meal-block">
        <span class="meal-label">餐饮建议</span>
        <ul class="meal-list">
          <li v-for="meal in group.meals" :key="`${meal.time}-${meal.recommendation}`">
            {{ meal.time || '用餐' }} · {{ meal.recommendation || '待补充' }}
          </li>
        </ul>
      </div>
    </section>
  </section>

  <a-empty v-else description="当前没有可展示的结构化行程" />
</template>

<style scoped lang="scss">
.timeline-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.day-section {
  /* 移除原本的边框和内边距，让卡片看起来像是漂浮的设计图样式 */
  background: transparent;
  border: none;
}

.day-header {
  margin-bottom: 24px;
}

.day-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text);
  padding: 8px 16px;
}

.day-subtitle {
  margin: 6px 0 0;
  color: var(--color-muted);
  padding: 0 16px;
}

.timeline-list {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

.timeline-row {
  display: grid;
  /* 左侧时间部分给 80px，右侧自适应卡片 */
  grid-template-columns: 80px minmax(0, 1fr);
  gap: 20px;
  cursor: pointer;
  align-items: stretch;
}

.timeline-time {
  position: relative;
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  justify-content: flex-end;
  gap: 16px;
  padding-top: 16px;
}

.time-text {
  font-weight: 600;
  font-size: 14px;
  color: #475569;
  background: #fbe6d5; /* 模拟左侧那个橘色/蓝色的时间胶囊 */
  padding: 6px 12px;
  border-radius: 20px;
  white-space: nowrap;
  
  /* 奇偶项可以交替颜色或者根据活动类型，这里设置统一橙红色圆角背景以匹配设计图1 */
}

/* 连续打点和连线 */
.time-axis {
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  padding-top: 8px;
}

.time-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 3px solid #f97316; /* 图中上方为橘点，下方可能为蓝点，这里默认橘，可以动态绑颜色 */
  background: white;
  z-index: 2;
  box-sizing: border-box;

  &.is-active {
    border-color: #3b82f6;
  }
}

.time-line {
  flex: 1;
  width: 1px;
  background: #e2e8f0;
  border-left: 2px dashed #cbd5e1; /* 虚线连接 */
  margin-top: 4px;
}

/* === 新版右侧垂直卡片 === */
.timeline-card {
  display: flex;
  flex-direction: column;
  border-radius: 24px;
  background: #ffffff;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.08); /* 增加悬浮投影感 */
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  overflow: hidden;
  height: 240px; /* 固定高度模拟大卡片 */
  position: relative;
}

.timeline-row.active .timeline-card,
.timeline-row:hover .timeline-card {
  box-shadow: 0 20px 48px rgba(59, 130, 246, 0.15);
  transform: translateY(-2px);
}

.timeline-cover {
  width: 100%;
  height: 160px; /* 图占据主要高度 */
  position: relative;
  overflow: hidden;
  background: #f2f4f7;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
    transition: transform 0.3s ease;
  }
}

.timeline-row:hover .timeline-cover img {
  transform: scale(1.05); /* hover轻微放大图片 */
}

.cover-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #a7c8ff 0%, #d1e3ff 100%);
  color: #1e3a8a;
  font-size: 28px;
  font-weight: 700;
}

.timeline-time-badge {
  position: absolute;
  top: 16px;
  left: 16px;
  background: transparent;
  color: white;
  font-weight: 600;
  font-size: 16px;
  text-shadow: 0 2px 4px rgba(0,0,0,0.5); /* 白色粗体带黑色描边发光 */
}

/* 封面图悬浮右上角箭头 */
.more-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(8px);
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;

  .icon-down {
    display: block;
    width: 8px;
    height: 8px;
    border-right: 2px solid #333;
    border-bottom: 2px solid #333;
    transform: translateY(-2px) rotate(45deg);
  }
}

.timeline-body {
  flex: 1;
  display: flex;
  align-items: center;
  padding: 16px 20px;
  background: white;
}

.timeline-headline {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.title-with-icon {
  display: flex;
  align-items: center;
  gap: 12px;
}

.location-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #f0f7ff;
  color: #3b82f6;
}

.title-group {
  display: flex;
  flex-direction: column;
}

.timeline-title {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.timeline-description-short {
  margin: 4px 0 0;
  font-size: 13px;
  color: #94a3b8;
  max-width: 140px; /* 防止过长 */
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.action-dots {
  background: none;
  border: none;
  font-weight: bold;
  font-size: 20px;
  color: #94a3b8;
  cursor: pointer;
  padding-bottom: 8px; /* 对齐圆点 */
}

/* 隐藏其余列表辅助信息 */
.timeline-meta,
.timeline-tips,
.timeline-type,
.timeline-description {
  display: none;
}

.meal-block {
  margin-top: 18px;
  padding-top: 16px;
  padding-left: 16px;
  border-top: 1px dashed rgba(15, 28, 46, 0.12);
}

.meal-label {
  display: block;
  margin-bottom: 8px;
  color: var(--color-muted);
  font-size: 13px;
}

.meal-list {
  margin: 12px 0 0;
  padding-left: 18px;
  color: var(--color-text-secondary);
}

@media (max-width: 768px) {
  .timeline-row {
    grid-template-columns: 1fr;
  }
}
</style>
