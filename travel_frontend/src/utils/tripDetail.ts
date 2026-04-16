import type { Activity, DailyPlan, StructuredItinerary } from '@/types/itinerary'

export interface ItinerarySummaryItem {
  key: 'weather' | 'transport' | 'accommodation' | 'budget'
  label: string
  value: string
}

export function buildStructuredActivityKey(day: number, activity: Pick<Activity, 'time' | 'name'>, index: number) {
  return `${day}-${activity.time || 'time'}-${activity.name || 'activity'}-${index}`
}

function isDailyPlanList(value: unknown): value is DailyPlan[] {
  return Array.isArray(value)
}

function normalizeDays(itinerary: StructuredItinerary): StructuredItinerary {
  const planCount = itinerary.dailyPlans.length
  const days = Number.isFinite(itinerary.days) && itinerary.days > 0 ? itinerary.days : planCount

  return {
    ...itinerary,
    days: Math.max(days, planCount || 0),
  }
}

export function parseStructuredTripData(rawStructuredData?: string | null): StructuredItinerary | null {
  if (!rawStructuredData?.trim()) {
    return null
  }

  try {
    const parsed = JSON.parse(rawStructuredData) as Partial<StructuredItinerary> | null
    if (!parsed || typeof parsed !== 'object' || !isDailyPlanList(parsed.dailyPlans)) {
      return null
    }

    return normalizeDays({
      destination: parsed.destination || '',
      days: Number(parsed.days) || parsed.dailyPlans.length,
      budget: parsed.budget,
      theme: parsed.theme,
      weatherOverview: parsed.weatherOverview,
      accommodationOverview: parsed.accommodationOverview,
      transportationOverview: parsed.transportationOverview,
      dailyPlans: parsed.dailyPlans,
      totalEstimatedCost: parsed.totalEstimatedCost,
      tips: parsed.tips,
    })
  } catch (error) {
    console.error('Failed to parse trip structured data', error)
    return null
  }
}

export function filterItineraryByDay(
  itinerary: StructuredItinerary | null,
  selectedDay: number | 'all',
): StructuredItinerary | null {
  if (!itinerary) {
    return null
  }

  if (selectedDay === 'all') {
    return itinerary
  }

  const dailyPlans = itinerary.dailyPlans.filter((plan) => Number(plan.day) === Number(selectedDay))
  if (dailyPlans.length === 0) {
    return null
  }

  return {
    ...itinerary,
    days: dailyPlans.length,
    dailyPlans,
  }
}

export function buildItinerarySummaryItems(
  itinerary: StructuredItinerary | null,
): ItinerarySummaryItem[] {
  if (!itinerary) {
    return []
  }

  const items: ItinerarySummaryItem[] = []

  if (itinerary.weatherOverview?.trim()) {
    items.push({
      key: 'weather',
      label: '天气提示',
      value: itinerary.weatherOverview.trim(),
    })
  }

  if (itinerary.transportationOverview?.trim()) {
    items.push({
      key: 'transport',
      label: '交通建议',
      value: itinerary.transportationOverview.trim(),
    })
  }

  if (itinerary.accommodationOverview?.trim()) {
    items.push({
      key: 'accommodation',
      label: '住宿建议',
      value: itinerary.accommodationOverview.trim(),
    })
  }

  if (typeof itinerary.totalEstimatedCost === 'number' && itinerary.totalEstimatedCost > 0) {
    items.push({
      key: 'budget',
      label: '预计花费',
      value: `¥${itinerary.totalEstimatedCost.toLocaleString('en-US')}`,
    })
  }

  return items
}
