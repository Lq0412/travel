<script setup lang="ts">
defineProps<{
  trips: API.TripVO[]
  loading: boolean
  disabled: boolean
}>()

const emit = defineEmits<{
  select: [trip: API.TripVO]
}>()

function formatTripSummary(trip: API.TripVO) {
  const parts = []
  if (trip.days) {
    parts.push(`${trip.days} 天`)
  }
  if (trip.theme?.trim()) {
    parts.push(trip.theme.trim())
  }
  return parts.join(' · ') || '已保存行程'
}

function handleClick(trip: API.TripVO) {
  emit('select', trip)
}
</script>

<template>
  <div class="recent-trip-cards">
    <div class="recent-trip-header">
      <div>
        <div class="recent-trip-title">最近行程</div>
        <div class="recent-trip-subtitle">选择一个行程后，将自动在 AI 对话里返回该城市的美食推荐卡片。</div>
      </div>
    </div>

    <a-spin :spinning="loading">
      <a-empty v-if="!loading && trips.length === 0" description="暂无最近行程，先保存一个行程再试。" />
      <div v-else class="recent-trip-list">
        <button
          v-for="trip in trips"
          :key="trip.id"
          type="button"
          class="recent-trip-card"
          :disabled="disabled"
          @click="handleClick(trip)"
        >
          <div class="recent-trip-card__top">
            <span class="recent-trip-card__title">{{ trip.destination || '未命名行程' }}</span>
            <a-tag :bordered="false" color="blue">{{ trip.status || 'planned' }}</a-tag>
          </div>
          <div class="recent-trip-card__summary">{{ formatTripSummary(trip) }}</div>
          <div class="recent-trip-card__time">最近更新：{{ trip.updateTime || trip.createTime || '刚刚保存' }}</div>
        </button>
      </div>
    </a-spin>
  </div>
</template>

<style scoped lang="scss">
.recent-trip-cards {
  width: 100%;
  max-width: 960px;
  margin: 0 auto 10px;
}

.recent-trip-header {
  margin-bottom: 10px;
}

.recent-trip-title {
  font-size: 14px;
  font-weight: 600;
  color: #0f1c2e;
}

.recent-trip-subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: #6b7a8c;
}

.recent-trip-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.recent-trip-card {
  padding: 12px 14px;
  border: 1px solid rgba(59, 110, 220, 0.18);
  border-radius: 12px;
  background: #ffffff;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.recent-trip-card:hover:not(:disabled) {
  border-color: #3b6edc;
  box-shadow: 0 8px 20px rgba(59, 110, 220, 0.08);
  transform: translateY(-1px);
}

.recent-trip-card:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.recent-trip-card__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.recent-trip-card__title {
  font-size: 14px;
  font-weight: 600;
  color: #0f1c2e;
}

.recent-trip-card__summary {
  margin-top: 8px;
  font-size: 13px;
  color: #35506b;
}

.recent-trip-card__time {
  margin-top: 6px;
  font-size: 12px;
  color: #7a8899;
}

@media (max-width: 900px) {
  .recent-trip-list {
    grid-template-columns: 1fr;
  }
}
</style>
