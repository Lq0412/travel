/**
 * 结构化行程数据类型定义
 * 用于前端展示AI返回的行程规划
 */

export interface StructuredItinerary {
  destination: string
  days: number
  budget?: number
  theme?: string
  weatherOverview?: string
  accommodationOverview?: string
  transportationOverview?: string
  dailyPlans: DailyPlan[]
  totalEstimatedCost?: number
  tips?: string[]
}

export interface DailyPlan {
  day: number
  date?: string
  transportAdvice?: string
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
    longitude?: number
    latitude?: number
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

export type StructuredSource = 'stream-marker' | 'sync-metadata' | 'none'

export interface ItineraryResponseMeta {
  intentType?: string
  intentDescription?: string
  structuredAvailable: boolean
  structuredSource: StructuredSource
  totalEstimatedCost?: number
  activityCount?: number
}

export interface ItineraryDayDiffStat {
  day: number
  added: number
  removed: number
  updated: number
}

export interface ItineraryDiffSummary {
  round: number
  changedDays: number[]
  changedNodeKeys: string[]
  addedCount: number
  removedCount: number
  updatedCount: number
  perDay: ItineraryDayDiffStat[]
}
