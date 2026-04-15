<script setup lang="ts">
import { ShoppingCartOutlined } from '@ant-design/icons-vue'

import type { Product } from '@/types/product'

defineProps<{
  title: string
  products: Product[]
}>()

const emit = defineEmits<{
  purchase: [product: Product]
}>()
</script>

<template>
  <section class="chat-recommendation-cards">
    <div class="chat-recommendation-cards__header">
      <div class="chat-recommendation-cards__title">{{ title }}</div>
      <div class="chat-recommendation-cards__subtitle">推荐结果来自当前城市的美食商品库</div>
    </div>

    <a-empty
      v-if="products.length === 0"
      class="chat-recommendation-cards__empty"
      description="当前城市暂无可展示的美食推荐"
    />

    <div v-else class="chat-recommendation-cards__grid">
      <a-card
        v-for="product in products"
        :key="product.id"
        size="small"
        class="chat-recommendation-card"
        :bordered="false"
        :body-style="{ padding: '12px' }"
      >
        <template #cover>
          <img class="chat-recommendation-card__cover" :src="product.cover" :alt="product.name" />
        </template>

        <div class="chat-recommendation-card__top">
          <div class="chat-recommendation-card__name">{{ product.name }}</div>
          <a-tag :bordered="false" color="blue">{{ product.city }}</a-tag>
        </div>
        <div class="chat-recommendation-card__tags">
          <a-tag v-for="tag in product.tags" :key="tag">{{ tag }}</a-tag>
        </div>
        <div class="chat-recommendation-card__description">{{ product.description }}</div>
        <div v-if="product.address" class="chat-recommendation-card__address">{{ product.address }}</div>
        <div class="chat-recommendation-card__footer">
          <div class="chat-recommendation-card__meta">
            <span
              class="chat-recommendation-card__status"
              :class="{ 'is-purchasable': product.isPurchasable }"
            >
              {{ product.isPurchasable ? '支持购买' : '暂不可购买' }}
            </span>
            <span v-if="product.distanceText" class="chat-recommendation-card__distance">
              {{ product.distanceText }}
            </span>
          </div>
          <a-button
            v-if="product.isPurchasable"
            type="primary"
            size="small"
            @click="emit('purchase', product)"
          >
            <template #icon>
              <ShoppingCartOutlined />
            </template>
            去看看
          </a-button>
        </div>
      </a-card>
    </div>
  </section>
</template>

<style scoped lang="scss">
.chat-recommendation-cards {
  margin-top: 10px;
  padding: 12px;
  border: 1px solid rgba(59, 110, 220, 0.12);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.82);
}

.chat-recommendation-cards__header {
  margin-bottom: 10px;
}

.chat-recommendation-cards__title {
  font-size: 14px;
  font-weight: 600;
  color: #0f1c2e;
}

.chat-recommendation-cards__subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: #6b7a8c;
}

.chat-recommendation-cards__empty {
  padding: 12px 0 0;
}

.chat-recommendation-cards__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.chat-recommendation-card {
  overflow: hidden;
  border: 1px solid rgba(15, 28, 46, 0.06);
  border-radius: 12px;
  box-shadow: 0 8px 18px rgba(15, 23, 42, 0.05);
}

.chat-recommendation-card__cover {
  display: block;
  width: 100%;
  height: 132px;
  object-fit: cover;
  background: #f3f5f7;
}

.chat-recommendation-card__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.chat-recommendation-card__name {
  font-size: 14px;
  line-height: 1.4;
  font-weight: 600;
  color: #0f1c2e;
}

.chat-recommendation-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.chat-recommendation-card__description {
  margin-top: 8px;
  min-height: 40px;
  font-size: 13px;
  line-height: 1.6;
  color: #4e5969;
}

.chat-recommendation-card__address {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.5;
  color: #8c8c8c;
}

.chat-recommendation-card__footer {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.chat-recommendation-card__meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.chat-recommendation-card__status {
  font-size: 12px;
  color: #86909c;
}

.chat-recommendation-card__status.is-purchasable {
  color: #1677ff;
}

.chat-recommendation-card__distance {
  font-size: 12px;
  color: #389e0d;
}

@media (max-width: 1080px) {
  .chat-recommendation-cards__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .chat-recommendation-cards__grid {
    grid-template-columns: 1fr;
  }
}
</style>
