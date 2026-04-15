import { getProductById as getProductByIdRequest, getMyProducts, saveProduct as saveProductRequest, deleteProductById } from '@/api/productController'
import type { Product } from '@/types/product'
import { getStoredProducts, mergeStoredProduct, saveStoredProducts } from '@/utils/productStorage'

type ProductRecord = {
  id: string | number
  name: string
  city: string
  address: string
  latitude?: number
  longitude?: number
  tags?: string[]
  description: string
  isRecommendable: boolean
  isPurchasable: boolean
  cover: string
}

function normalizeProduct(record: ProductRecord): Product {
  return {
    id: String(record.id),
    name: record.name,
    city: record.city,
    address: record.address || '',
    latitude: typeof record.latitude === 'number' ? record.latitude : undefined,
    longitude: typeof record.longitude === 'number' ? record.longitude : undefined,
    tags: Array.isArray(record.tags) ? [...record.tags] : [],
    description: record.description,
    isRecommendable: Boolean(record.isRecommendable),
    isPurchasable: Boolean(record.isPurchasable),
    cover: record.cover,
  }
}

export async function fetchMyProducts(): Promise<Product[]> {
  const response = await getMyProducts()
  if (response.data.code !== 0 || !Array.isArray(response.data.data)) {
    throw new Error(response.data.message || '加载商品列表失败')
  }

  const normalized = response.data.data.map((item) => normalizeProduct(item))
  saveStoredProducts(normalized)
  return normalized
}

export async function fetchProductById(productId: string): Promise<Product | null> {
  const response = await getProductByIdRequest(productId)
  if (response.data.code !== 0 || !response.data.data) {
    return null
  }

  const normalized = normalizeProduct(response.data.data)
  mergeStoredProduct(normalized)
  return normalized
}

export function getCachedProducts(): Product[] {
  return getStoredProducts()
}

export async function saveProductToServer(product: Product): Promise<Product[]> {
  const payload = {
    name: product.name,
    city: product.city,
    address: product.address,
    latitude: product.latitude,
    longitude: product.longitude,
    tags: product.tags,
    description: product.description,
    isRecommendable: product.isRecommendable,
    isPurchasable: product.isPurchasable,
    cover: product.cover,
    ...(product.id ? { id: Number(product.id) } : {}),
  }

  const response = await saveProductRequest(payload)

  if (response.data.code !== 0) {
    throw new Error(response.data.message || '商品保存失败')
  }

  return fetchMyProducts()
}

export async function deleteProductFromServer(productId: string): Promise<Product[]> {
  const response = await deleteProductById(productId)
  if (response.data.code !== 0) {
    throw new Error(response.data.message || '商品删除失败')
  }

  return fetchMyProducts()
}
