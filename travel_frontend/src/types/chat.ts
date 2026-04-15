import type { Product } from '@/types/product'

export interface ChatRecommendationCards {
  title: string
  trip?: Pick<API.TripVO, 'destination' | 'days' | 'theme'> | null
  products: Product[]
}

export interface ChatItem {
  role: 'user' | 'ai'
  text: string
  time: Date
  recommendationCards?: ChatRecommendationCards
}

export interface Conversation {
  id: string
  title: string
  updateTime?: string
}


