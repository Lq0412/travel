<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeftOutlined, CheckCircleFilled, EnvironmentOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'

import {
  getProductPriceLabel,
  saveFakeOrder,
} from '@/utils/productOrder'
import { fetchProductById } from '@/utils/productCatalog'
import type { Product } from '@/types/product'

const route = useRoute()
const router = useRouter()
const isSubmitting = ref(false)
const product = ref<Product | null>(null)

const productId = computed(() => String(route.params.id || ''))
const priceLabel = computed(() => {
  if (!product.value) {
    return '￥0.00'
  }
  return getProductPriceLabel(product.value)
})

const featureItems = computed(() => {
  if (!product.value) {
    return []
  }

  return [
    `${product.value.city}目的地精选`,
    product.value.address ? `地址：${product.value.address}` : '待补充详细地址',
    product.value.isPurchasable ? '支持即时下单' : '当前仅支持浏览',
    '下单后展示假支付成功结果',
  ]
})

async function loadProduct() {
  product.value = await fetchProductById(productId.value)
}

async function handleBuyNow() {
  if (!product.value || !product.value.isPurchasable || isSubmitting.value) {
    return
  }

  isSubmitting.value = true
  try {
    saveFakeOrder({
      productId: product.value.id,
      amountLabel: priceLabel.value,
    })

    message.success('模拟支付成功')
    await router.push(`/products/${product.value.id}/payment-success`)
  } finally {
    isSubmitting.value = false
  }
}

function goBack() {
  router.back()
}

function goMall() {
  router.push('/mall')
}

watch(productId, () => {
  void loadProduct()
}, { immediate: true })

onMounted(() => {
  void loadProduct()
})
</script>

<template>
  <div class="product-detail-page">
    <div class="product-detail-toolbar">
      <a-button type="link" class="toolbar-back" @click="goBack">
        <template #icon>
          <ArrowLeftOutlined />
        </template>
        返回
      </a-button>
    </div>

    <a-result
      v-if="!product"
      status="404"
      title="商品不存在"
      sub-title="当前商品可能已被移除，您可以返回商城重新选择。"
    >
      <template #extra>
        <a-button type="primary" @click="goMall">返回商城</a-button>
      </template>
    </a-result>

    <div v-else class="product-detail-layout">
      <section class="detail-main">
        <div class="hero-card">
          <div class="hero-cover">
            <img :src="product.cover" :alt="product.name" />
          </div>
          <div class="hero-content">
            <a-tag color="blue" :bordered="false">{{ product.city }}</a-tag>
            <h1 class="hero-title">{{ product.name }}</h1>
            <p class="hero-description">{{ product.description }}</p>

            <div class="hero-tags">
              <a-tag v-for="tag in product.tags" :key="tag">{{ tag }}</a-tag>
            </div>

            <div class="hero-highlights">
              <div v-for="item in featureItems" :key="item" class="highlight-item">
                <CheckCircleFilled />
                <span>{{ item }}</span>
              </div>
            </div>
          </div>
        </div>

        <a-card title="商品说明" :bordered="false" class="detail-section">
          <div class="detail-copy">
            <p>这是一条面向旅行场景的商品详情占位流程，当前用于打通推荐卡片到详情浏览和下单路径。</p>
            <p>商品信息来自当前账号下的真实商品库，地址与坐标会用于后续推荐距离控制。</p>
            <p>后续可以继续接入库存、规格选择、真实支付和订单中心。</p>
          </div>
        </a-card>
      </section>

      <aside class="detail-side">
        <a-card :bordered="false" class="purchase-card">
          <div class="purchase-price">{{ priceLabel }}</div>
          <div class="purchase-meta">
            <span class="meta-line">
              <EnvironmentOutlined />
              {{ product.city }} 推荐商品
            </span>
            <span class="meta-line">{{ product.isPurchasable ? '当前可立即下单' : '当前仅支持浏览' }}</span>
          </div>

          <a-button
            type="primary"
            size="large"
            block
            :disabled="!product.isPurchasable"
            :loading="isSubmitting"
            @click="handleBuyNow"
          >
            立即购买
          </a-button>
          <a-button size="large" block class="secondary-btn" @click="goMall">继续逛商城</a-button>
        </a-card>
      </aside>
    </div>
  </div>
</template>

<style scoped lang="scss">
.product-detail-page {
  padding: 24px 0 40px;
}

.product-detail-toolbar {
  margin-bottom: 16px;
}

.toolbar-back {
  padding-left: 0;
}

.product-detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) 340px;
  gap: 24px;
  align-items: start;
}

.detail-main {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.hero-card,
.purchase-card,
.detail-section {
  border-radius: 16px;
  border: 1px solid #f0f0f0;
  background: #fff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.05);
}

.hero-card {
  display: grid;
  grid-template-columns: minmax(280px, 420px) minmax(0, 1fr);
  overflow: hidden;
}

.hero-cover img {
  display: block;
  width: 100%;
  height: 100%;
  min-height: 320px;
  object-fit: cover;
}

.hero-content {
  padding: 28px;
}

.hero-title {
  margin: 16px 0 12px;
  font-size: 30px;
  line-height: 1.25;
  color: #1f1f1f;
}

.hero-description {
  margin: 0;
  font-size: 15px;
  line-height: 1.8;
  color: #595959;
}

.hero-tags {
  margin-top: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hero-highlights {
  margin-top: 24px;
  display: grid;
  gap: 12px;
}

.highlight-item {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #1677ff;
}

.detail-copy {
  display: grid;
  gap: 12px;
  font-size: 14px;
  line-height: 1.8;
  color: #595959;
}

.detail-copy p {
  margin: 0;
}

.detail-side {
  position: sticky;
  top: 24px;
}

.purchase-card {
  padding: 24px;
}

.purchase-price {
  font-size: 34px;
  font-weight: 700;
  color: #1677ff;
}

.purchase-meta {
  margin: 16px 0 24px;
  display: grid;
  gap: 10px;
  color: #595959;
  font-size: 14px;
}

.meta-line {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.secondary-btn {
  margin-top: 12px;
}

@media (max-width: 960px) {
  .product-detail-layout {
    grid-template-columns: 1fr;
  }

  .hero-card {
    grid-template-columns: 1fr;
  }

  .detail-side {
    position: static;
  }
}
</style>
