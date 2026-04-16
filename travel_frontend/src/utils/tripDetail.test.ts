import { describe, expect, it } from 'vitest'
import {
  buildItinerarySummaryItems,
  buildStructuredActivityKey,
  filterItineraryByDay,
  parseStructuredTripData,
} from './tripDetail'

describe('tripDetail utils', () => {
  const rawStructuredData = JSON.stringify({
    destination: '东京',
    days: 3,
    weatherOverview: '早晚偏凉，建议带薄外套',
    transportationOverview: '优先使用地铁与步行',
    accommodationOverview: '浅草或上野区域更方便',
    totalEstimatedCost: 3200,
    dailyPlans: [
      {
        day: 1,
        transportAdvice: '地铁为主',
        activities: [
          {
            time: '09:00',
            name: '浅草寺',
            type: 'attraction',
            description: '参观寺庙与周边街区',
            location: { address: '东京台东区浅草寺' },
          },
        ],
      },
      {
        day: 2,
        activities: [
          {
            time: '13:00',
            name: '晴空塔',
            type: 'attraction',
            description: '登塔看城市景观',
          },
        ],
      },
    ],
  })

  it('parses valid structured trip json and normalizes day count from plans', () => {
    const itinerary = parseStructuredTripData(rawStructuredData)

    expect(itinerary).toMatchObject({
      destination: '东京',
      days: 3,
    })
    expect(itinerary?.dailyPlans).toHaveLength(2)
  })

  it('returns null when structured data is invalid or missing daily plans', () => {
    expect(parseStructuredTripData('{"destination":"东京"}')).toBeNull()
    expect(parseStructuredTripData('not-json')).toBeNull()
    expect(parseStructuredTripData(undefined)).toBeNull()
  })

  it('filters itinerary into a single day view while preserving shared metadata', () => {
    const itinerary = parseStructuredTripData(rawStructuredData)
    const dayTwo = filterItineraryByDay(itinerary, 2)

    expect(dayTwo).toMatchObject({
      destination: '东京',
      days: 1,
    })
    expect(dayTwo?.dailyPlans).toHaveLength(1)
    expect(dayTwo?.dailyPlans[0]).toMatchObject({
      day: 2,
    })
  })

  it('builds summary items only for populated overview fields', () => {
    const itinerary = parseStructuredTripData(rawStructuredData)
    const summaryItems = buildItinerarySummaryItems(itinerary)

    expect(summaryItems).toEqual([
      { key: 'weather', label: '天气提示', value: '早晚偏凉，建议带薄外套' },
      { key: 'transport', label: '交通建议', value: '优先使用地铁与步行' },
      { key: 'accommodation', label: '住宿建议', value: '浅草或上野区域更方便' },
      { key: 'budget', label: '预计花费', value: '¥3,200' },
    ])
  })

  it('builds a stable activity key for timeline and map linkage', () => {
    const key = buildStructuredActivityKey(
      2,
      {
        time: '13:00',
        name: '晴空塔',
      },
      1,
    )

    expect(key).toBe('2-13:00-晴空塔-1')
  })

  it('normalizes invalid day count to the number of parsed plans', () => {
    const itinerary = parseStructuredTripData(
      JSON.stringify({
        destination: '深圳',
        days: 0,
        dailyPlans: [
          { day: 1, activities: [] },
          { day: 2, activities: [] },
        ],
      }),
    )

    expect(itinerary?.days).toBe(2)
  })
})
