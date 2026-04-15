export interface Product {
  id: string
  name: string
  city: string
  address: string
  latitude?: number
  longitude?: number
  distanceText?: string
  tags: string[]
  description: string
  isRecommendable: boolean
  isPurchasable: boolean
  cover: string
}
