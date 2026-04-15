import type { Product } from '@/types/product'

const PRODUCT_ORDER_STORAGE_KEY = 'travel_mall_latest_fake_order'

export interface FakeProductOrder {
  orderNo: string
  productId: string
  amountLabel: string
  status: 'paid'
  paidAt: string
}

export function getProductPriceLabel(product: Pick<Product, 'id'>): string {
  const numericId = Number.parseInt(product.id, 10)
  const amount = Number.isFinite(numericId) ? 39 + numericId * 10 : 99
  return `￥${amount}.00`
}

export function saveFakeOrder(input: Pick<FakeProductOrder, 'productId' | 'amountLabel'>): FakeProductOrder {
  const order: FakeProductOrder = {
    orderNo: `MOCK-${Date.now()}`,
    productId: input.productId,
    amountLabel: input.amountLabel,
    status: 'paid',
    paidAt: new Date().toISOString(),
  }

  localStorage.setItem(PRODUCT_ORDER_STORAGE_KEY, JSON.stringify(order))
  return order
}

export function getLatestFakeOrder(): FakeProductOrder | null {
  const rawOrder = localStorage.getItem(PRODUCT_ORDER_STORAGE_KEY)
  if (!rawOrder) {
    return null
  }

  try {
    return JSON.parse(rawOrder) as FakeProductOrder
  } catch (error) {
    console.error('读取假支付订单失败', error)
    return null
  }
}
