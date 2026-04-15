import request from '@/request'

export type ProductPayload = {
  id: string | number
  name: string
  city: string
  address: string
  latitude?: number
  longitude?: number
  tags: string[]
  description: string
  isRecommendable: boolean
  isPurchasable: boolean
  cover: string
  createTime?: string
  updateTime?: string
}

export type SaveProductPayload = Omit<ProductPayload, 'id' | 'createTime' | 'updateTime'> & {
  id?: string | number
}

type ResponseDTO<T> = {
  code: number
  message: string
  data: T
}

export async function getMyProducts(options?: { [key: string]: any }) {
  return request<ResponseDTO<ProductPayload[]>>('/products/my', {
    method: 'GET',
    ...(options || {}),
  })
}

export async function getProductById(id: string | number, options?: { [key: string]: any }) {
  return request<ResponseDTO<ProductPayload>>(`/products/${id}`, {
    method: 'GET',
    ...(options || {}),
  })
}

export async function saveProduct(
  body: SaveProductPayload,
  options?: { [key: string]: any },
) {
  return request<ResponseDTO<number>>('/products/save', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

export async function deleteProductById(id: string | number, options?: { [key: string]: any }) {
  return request<ResponseDTO<boolean>>(`/products/${id}`, {
    method: 'DELETE',
    ...(options || {}),
  })
}
