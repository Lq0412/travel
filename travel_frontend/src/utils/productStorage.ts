import type { Product } from '@/types/product'

const PRODUCT_STORAGE_KEY = 'travel_mall_products'

function canUseLocalStorage() {
  return typeof localStorage !== 'undefined'
}

export function getStoredProducts(): Product[] {
  if (!canUseLocalStorage()) {
    return []
  }

  const stored = localStorage.getItem(PRODUCT_STORAGE_KEY)
  if (!stored) {
    return []
  }

  try {
    const parsed = JSON.parse(stored)
    return Array.isArray(parsed) ? (parsed as Product[]) : []
  } catch (error) {
    console.error('解析商品缓存失败', error)
    return []
  }
}

export function saveStoredProducts(products: Product[]) {
  if (!canUseLocalStorage()) {
    return
  }

  localStorage.setItem(PRODUCT_STORAGE_KEY, JSON.stringify(products))
}

export function mergeStoredProduct(product: Product) {
  const products = getStoredProducts()
  const existingIndex = products.findIndex((item) => item.id === product.id)
  if (existingIndex >= 0) {
    products.splice(existingIndex, 1, product)
  } else {
    products.push(product)
  }
  saveStoredProducts(products)
}

export function getStoredProductById(productId: string): Product | undefined {
  return getStoredProducts().find((product) => product.id === productId)
}
