<script setup lang="ts">
import type { ItinerarySummaryItem } from '@/utils/tripDetail'

defineProps<{
  items: ItinerarySummaryItem[]
}>()

// 模拟图表分段数据（完全匹配设计图呈现）
const chartSegments = [
  // 交通出行 - 橙色
  { color: '#fb923c', percent: 16, strokeArray: '30 188.5', strokeOffset: '0', label: '交通出行', sub: 'Transport Expense' },
  // 餐饮住宿 - 粉橘色
  { color: '#fdba74', percent: 4, strokeArray: '7.5 188.5', strokeOffset: '-38', label: '预留机动', sub: 'Reserved Fund' },
  // 景点门票及其他 - 蓝色 (占大头 80%)
  { color: '#60a5fa', percent: 80, strokeArray: '150.8 188.5', strokeOffset: '-54', label: '常规花费', sub: 'General Spending' },
]
</script>

<template>
  <div class="summary-content">
    <!-- 圆环图表区域 -->
    <div class="chart-section">
      <div class="chart-container">
        <!-- 基于 viewBox 的自适应 SVG 圆环 -->
        <svg viewBox="0 0 100 100" class="donut-svg">
          <circle 
            v-for="(seg, idx) in chartSegments" 
            :key="idx"
            cx="50" cy="50" 
            r="30" 
            fill="transparent" 
            :stroke="seg.color" 
            stroke-width="14" 
            :stroke-dasharray="seg.strokeArray" 
            :stroke-dashoffset="seg.strokeOffset" 
            stroke-linecap="round" 
            transform="rotate(-90 50 50)"
            class="chart-arc"
          />
        </svg>
        
        <!-- 数据标签引线层 -->
        <div class="labels-layer">
          <!-- 左上角引线 -->
          <div class="label-badge left-label">
            <span class="badg-text" style="color: #fb923c;">16%</span>
            <svg width="32" height="16" class="badg-line line-left-svg">
              <polyline points="0,15 15,15 32,5" fill="none" stroke="#fb923c" stroke-width="1.5" />
            </svg>
          </div>
          <!-- 右下角引线 -->
          <div class="label-badge right-label">
            <svg width="24" height="32" class="badg-line line-right-svg">
              <polyline points="0,10 10,20 24,20" fill="none" stroke="#60a5fa" stroke-width="1.5" />
            </svg>
            <span class="badg-text" style="color: #60a5fa;">80.%</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部图例 -->
    <div class="legend-section">
      <!-- 蓝色图例 -->
      <div class="legend-item">
        <span class="legend-dot" style="background-color: #60a5fa;"></span>
        <div class="legend-text">
          <span class="lg-title">常规花费</span>
          <span class="lg-sub">General Spending</span>
        </div>
      </div>
      <!-- 橙色图例 -->
      <div class="legend-item">
        <span class="legend-dot" style="background-color: #fb923c;"></span>
        <div class="legend-text">
          <span class="lg-title">交通出行</span>
          <span class="lg-sub">Transport Expense</span>
        </div>
      </div>
    </div>

    <!-- 保留的概览文案 -->
    <section v-if="items.length" class="summary-cards">
      <article v-for="item in items" :key="item.key" class="summary-card">
        <span class="sc-label">{{ item.label }}</span>
        <p class="sc-value">{{ item.value }}</p>
      </article>
    </section>
  </div>
</template>

<style scoped lang="scss">
.summary-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 图表区域 */
.chart-section {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 8px 0 16px;
}

.chart-container {
  position: relative;
  width: 160px;
  height: 160px;
}

.donut-svg {
  width: 100%;
  height: 100%;
  overflow: visible;
  /* 柔和的发光阴影 */
  filter: drop-shadow(0 8px 12px rgba(96, 165, 250, 0.15));
}

.chart-arc {
  transition: all 0.3s ease;
  cursor: default;

  &:hover {
    filter: brightness(1.05); /* 悬浮稍微亮起 */
  }
}

/* 标签引线定位 */
.labels-layer {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
}

.label-badge {
  position: absolute;
  display: flex;
  align-items: center;
  gap: 6px;
}

.left-label {
  top: 15%;
  left: -20px;
  align-items: flex-end;
}
.right-label {
  bottom: 25%;
  right: -20px;
  align-items: flex-end;
}

.badg-text {
  font-family: system-ui, -apple-system, sans-serif;
  font-weight: 700;
  font-size: 15px;
  /* 文本颜色描边以突出显示 */
  text-shadow: 0 2px 8px rgba(0,0,0,0.05);
}

.badg-line {
  /* 防止溢出截断SVG内的线 */
  overflow: visible;
}
.line-left-svg {
  transform: translateY(-8px);
}
.line-right-svg {
  transform: translateY(4px);
}

/* 图例区域 */
.legend-section {
  display: flex;
  justify-content: space-between;
  padding: 0 24px 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.legend-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  margin-top: 3px;
}

.legend-text {
  display: flex;
  flex-direction: column;
}

.lg-title {
  font-size: 11px;
  color: #555;
  font-weight: 500;
  letter-spacing: 0.2px;
}

.lg-sub {
  font-size: 9px;
  color: #a3a3a3;
  transform-origin: left;
  transform: scale(0.9);
  margin-top: 2px;
}

/* 原有文案卡片样式 */
.summary-cards {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 180px; 
  overflow-y: auto;
  padding-right: 4px;
  
  /* 个性化滚动条 */
  &::-webkit-scrollbar {
    width: 3px;
  }
  &::-webkit-scrollbar-thumb {
    background: rgba(0,0,0,0.1);
    border-radius: 3px;
  }
}

.summary-card {
  background: transparent;
  padding: 4px 6px;
  border-radius: 8px;
}

.sc-label {
  display: block;
  font-size: 12px;
  color: #1e293b;
  font-weight: 600;
  margin-bottom: 2px;
}

.sc-value {
  margin: 0;
  font-size: 12px;
  color: #475569;
  line-height: 1.5;
  white-space: pre-wrap;
}
</style>
