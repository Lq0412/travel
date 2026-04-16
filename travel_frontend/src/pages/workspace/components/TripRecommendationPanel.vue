<script setup lang="ts">
import { ShoppingCartOutlined } from '@ant-design/icons-vue'

import type { Product } from '@/types/product'

defineProps<{
  title: string
  trip: API.TripVO | null
  products: Product[]
}>()

const emit = defineEmits<{
  purchase: [product: Product]
}>()

function formatTripSummary(trip: API.TripVO | null) {
  if (!trip) {
    return ''
  }

  const parts = []
  if (trip.days) {
    parts.push(`${trip.days} 天`)
  }
  if (trip.theme?.trim()) {
    parts.push(trip.theme.trim())
  }
  return parts.join(' · ') || '已选中最近行程'
}
</script>

<template>
  <section class="trip-recommendation-panel">
    <div class="trip-recommendation-panel__header">
      <div>
        <div class="trip-recommendation-panel__title">{{ title }}</div>
        <div class="trip-recommendation-panel__subtitle">
          {{ formatTripSummary(trip) }}
        </div>
      </div>
    </div>

    <a-empty
      v-if="products.length === 0"
      class="trip-recommendation-panel__empty"
      description="当前行程所在城市暂无可推荐美食"
    />

    <div v-else class="trip-recommendation-panel__grid">
      <a-card
        v-for="product in products"
        :key="product.id"
        class="trip-recommendation-card"
        :bordered="false"
        :body-style="{ padding: '14px' }"
      >
        <template #cover>
          <img class="trip-recommendation-card__cover" :src="product.cover" :alt="product.name" />
        </template>

        <div class="trip-recommendation-card__content">
          <div class="trip-recommendation-card__top">
            <div class="trip-recommendation-card__name">{{ product.name }}</div>
            <a-tag :bordered="false" color="blue">{{ product.city }}</a-tag>
          </div>
          <div class="trip-recommendation-card__tags">
            <a-tag v-for="tag in product.tags" :key="tag">{{ tag }}</a-tag>
          </div>
          <div class="trip-recommendation-card__description">{{ product.description }}</div>
          <div class="trip-recommendation-card__footer">
            <span
              class="trip-recommendation-card__status"
              :class="{ 'is-purchasable': product.isPurchasable }"
            >
              {{ product.isPurchasable ? '支持购买' : '暂不可购买' }}
            </span>
            <a-button
              v-if="product.isPurchasable"
              type="primary"
              size="small"
              @click="emit('purchase', product)"
            >
              <template #icon>
                <ShoppingCartOutlined />
              </template>
              立即购买
            </a-button>
          </div>
        </div>
      </a-card>
    </div>
  </section>
</template>

<style scoped lang="scss">
.trip-recommendation-panel {
  width: 100%;
  max-width: 960px;
  margin: 0 auto 10px;
  padding: 16px;
  border: 1px solid rgba(15, 28, 46, 0.08);
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.9);
}

.trip-recommendation-panel__header {
  margin-bottom: 12px;
}

.trip-recommendation-panel__title {
  font-size: 15px;
  font-weight: 600;
  color: #0f1c2e;
}

.trip-recommendation-panel__subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: #6b7a8c;
}

.trip-recommendation-panel__empty {
  padding: 16px 0 4px;
}

.trip-recommendation-panel__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.trip-recommendation-card {
  overflow: hidden;
  border: 1px solid rgba(15, 28, 46, 0.06);
  border-radius: 14px;
  box-shadow: 0 10px 28px rgba(15, 28, 46, 0.06);
}

.trip-recommendation-card__cover {
  display: block;
  width: 100%;
  height: 164px;
  object-fit: cover;
  background: #f3f5f7;
}

.trip-recommendation-card__content {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.trip-recommendation-card__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.trip-recommendation-card__name {
  font-size: 15px;
  line-height: 1.4;
  font-weight: 600;
  color: #0f1c2e;
}

.trip-recommendation-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.trip-recommendation-card__description {
  min-height: 44px;
  font-size: 13px;
  line-height: 1.6;
  color: #4e5969;
}

.trip-recommendation-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.trip-recommendation-card__status {
  font-size: 12px;
  color: #86909c;
}

.trip-recommendation-card__status.is-purchasable {
  color: #1677ff;
}

@media (max-width: 900px) {
  .trip-recommendation-panel__grid {
    grid-template-columns: 1fr;
  }
}
</style>
