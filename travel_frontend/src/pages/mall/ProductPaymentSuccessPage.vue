<script setup lang="ts">
import { computed, shallowRef, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { getLatestFakeOrder } from '@/utils/productOrder'
import { fetchProductById } from '@/utils/productCatalog'
import type { Product } from '@/types/product'

const route = useRoute()
const router = useRouter()

const order = computed(() => getLatestFakeOrder())
const product = shallowRef<Product | null>(null)
const routeProductId = computed(() => String(route.params.id || ''))
const isOrderMatched = computed(() => order.value?.productId === routeProductId.value)

async function loadProduct() {
  if (!order.value || !isOrderMatched.value) {
    product.value = null
    return
  }

  product.value = await fetchProductById(order.value.productId)
}

function goMall() {
  router.push('/mall')
}

function goProduct() {
  if (routeProductId.value) {
    router.push(`/products/${routeProductId.value}`)
  } else {
    goMall()
  }
}

watch([order, routeProductId], () => {
  void loadProduct()
}, { immediate: true })
</script>

<template>
  <div class="payment-success-page">
    <a-result
      v-if="!order || !isOrderMatched || !product"
      status="warning"
      title="暂无支付结果"
      sub-title="没有找到对应的假支付订单，请返回商品详情重新发起购买。"
    >
      <template #extra>
        <a-button type="primary" @click="goProduct">返回商品详情</a-button>
        <a-button @click="goMall">返回商城</a-button>
      </template>
    </a-result>

    <div v-else class="payment-success-layout">
      <a-result status="success" title="支付成功" sub-title="当前为假支付流程，已为您生成模拟订单结果。" />

      <a-card :bordered="false" class="summary-card">
        <a-descriptions :column="1" title="订单摘要" bordered size="middle">
          <a-descriptions-item label="订单号">{{ order.orderNo }}</a-descriptions-item>
          <a-descriptions-item label="商品">{{ product.name }}</a-descriptions-item>
          <a-descriptions-item label="目的地">{{ product.city }}</a-descriptions-item>
          <a-descriptions-item label="支付金额">{{ order.amountLabel }}</a-descriptions-item>
          <a-descriptions-item label="支付状态">已支付</a-descriptions-item>
          <a-descriptions-item label="支付时间">{{ new Date(order.paidAt).toLocaleString() }}</a-descriptions-item>
        </a-descriptions>

        <div class="summary-actions">
          <a-button type="primary" size="large" @click="goMall">返回商城</a-button>
          <a-button size="large" @click="goProduct">返回商品详情</a-button>
        </div>
      </a-card>
    </div>
  </div>
</template>

<style scoped lang="scss">
.payment-success-page {
  padding: 32px 0 48px;
}

.payment-success-layout {
  display: grid;
  gap: 24px;
}

.summary-card {
  border-radius: 16px;
  border: 1px solid #f0f0f0;
  background: #fff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.05);
}

.summary-actions {
  margin-top: 24px;
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

@media (max-width: 760px) {
  .summary-actions {
    flex-direction: column;
  }
}
</style>
