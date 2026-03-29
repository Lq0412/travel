/**
 * 结构化行程数据类型定义
 * 用于前端展示AI返回的行程规划
 */

export interface StructuredItinerary {
  destination: string
  days: number
  budget?: number
  theme?: string
  dailyPlans: DailyPlan[]
  totalEstimatedCost?: number
  tips?: string[]
}

export interface DailyPlan {
  day: number
  date?: string
  activities: Activity[]
  meals?: Meal[]
}

export interface Activity {
  time: string
  name: string
  type: 'attraction' | 'transport' | 'rest' | 'meal'
  description: string
  imageUrl?: string
  location?: {
    address: string
    coordinates?: [number, number]
  }
  estimatedCost?: number
  tips?: string[]
}

export interface Meal {
  time: string
  type: 'breakfast' | 'lunch' | 'dinner'
  recommendation: string
  estimatedCost?: number
}
