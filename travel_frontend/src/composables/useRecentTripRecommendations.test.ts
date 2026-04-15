import { beforeEach, describe, expect, it, vi } from 'vitest'

const { fetchMyProductsMock } = vi.hoisted(() => ({
  fetchMyProductsMock: vi.fn(),
}))

vi.mock('@/utils/productCatalog', () => ({
  fetchMyProducts: fetchMyProductsMock,
}))

import { useRecentTripRecommendations } from './useRecentTripRecommendations'

describe('useRecentTripRecommendations', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    fetchMyProductsMock.mockResolvedValue([
      {
        id: '1',
        name: '成都火锅双人餐',
        city: '成都',
        address: '成都市锦江区红星路一段 1 号',
        latitude: 30.659,
        longitude: 104.083,
        tags: ['美食', '火锅'],
        description: '适合行程内安排的一顿经典成都火锅。',
        isRecommendable: true,
        isPurchasable: true,
        cover: 'https://example.org/chengdu-hotpot.jpg',
      },
      {
        id: '2',
        name: '成都兔头礼盒',
        city: '成都',
        address: '成都市青羊区少城路 8 号',
        latitude: 30.664,
        longitude: 104.05,
        tags: ['伴手礼'],
        description: '城市匹配但不可推荐，不应展示。',
        isRecommendable: false,
        isPurchasable: true,
        cover: 'https://example.org/chengdu-gift.jpg',
      },
      {
        id: '3',
        name: '重庆小面体验券',
        city: '重庆',
        address: '重庆市渝中区解放碑 1 号',
        latitude: 29.56,
        longitude: 106.57,
        tags: ['美食'],
        description: '可推荐但城市不匹配，不应展示。',
        isRecommendable: true,
        isPurchasable: true,
        cover: 'https://example.org/chongqing-noodle.jpg',
      },
      {
        id: '4',
        name: '春熙路串串双人餐',
        city: '成都',
        address: '成都市锦江区春熙路 88 号',
        latitude: 30.6585,
        longitude: 104.0845,
        tags: ['美食', '串串'],
        description: '离春熙路商圈更近。',
        isRecommendable: true,
        isPurchasable: true,
        cover: 'https://example.org/chengdu-chuanchuan.jpg',
      },
    ])
  })

  it('keeps recommendation panel hidden before any recent trip is selected', () => {
    const { selectedTrip, recommendedProducts, showRecommendations, recommendationTitle } =
      useRecentTripRecommendations()

    expect(selectedTrip.value).toBeNull()
    expect(recommendedProducts.value).toEqual([])
    expect(showRecommendations.value).toBe(false)
    expect(recommendationTitle.value).toBe('行程美食推荐')
  })

  it('shows only recommendable products whose city matches the selected trip destination', async () => {
    const { selectTrip, selectedTrip, recommendedProducts, showRecommendations, hasMatches } =
      useRecentTripRecommendations()

    await selectTrip({
      id: 101,
      destination: '成都',
      days: 3,
      theme: '美食游',
    })

    expect(selectedTrip.value?.destination).toBe('成都')
    expect(showRecommendations.value).toBe(true)
    expect(hasMatches.value).toBe(true)
    expect(recommendedProducts.value.map((product) => product.name)).toEqual([
      '成都火锅双人餐',
      '春熙路串串双人餐',
    ])
  })

  it('sorts matched products by distance when trip structured data contains reference coordinates', async () => {
    const { selectTrip, recommendedProducts } = useRecentTripRecommendations()

    await selectTrip({
      id: 105,
      destination: '成都',
      structuredData: JSON.stringify({
        dailyPlans: [
          {
            day: 1,
            activities: [
              {
                name: '春熙路',
                location: {
                  address: '成都市锦江区春熙路',
                  longitude: 104.0846,
                  latitude: 30.6586,
                },
              },
            ],
          },
        ],
      }),
    })

    expect(recommendedProducts.value.map((product) => product.name)).toEqual([
      '春熙路串串双人餐',
      '成都火锅双人餐',
    ])
    expect(recommendedProducts.value[0].distanceText).toContain('距行程约')
  })

  it('keeps the panel visible with an empty state when no city-matched recommendable products exist', async () => {
    const { selectTrip, recommendedProducts, showRecommendations, hasMatches, recommendationTitle } =
      useRecentTripRecommendations()

    await selectTrip({
      id: 102,
      destination: '杭州',
      days: 2,
    })

    expect(showRecommendations.value).toBe(true)
    expect(hasMatches.value).toBe(false)
    expect(recommendedProducts.value).toEqual([])
    expect(recommendationTitle.value).toContain('杭州')
  })

  it('clears the selected trip and recommendation results together', async () => {
    const { clearRecommendations, selectTrip, selectedTrip, recommendedProducts, showRecommendations } =
      useRecentTripRecommendations()

    await selectTrip({
      id: 103,
      destination: '成都',
    })
    clearRecommendations()

    expect(selectedTrip.value).toBeNull()
    expect(recommendedProducts.value).toEqual([])
    expect(showRecommendations.value).toBe(false)
  })

  it('keeps recommendations empty when the real product library returns no products', async () => {
    fetchMyProductsMock.mockResolvedValueOnce([])
    const { selectTrip, recommendedProducts, hasMatches } = useRecentTripRecommendations()

    await selectTrip({
      id: 104,
      destination: '揭阳',
      days: 2,
    })

    expect(recommendedProducts.value).toEqual([])
    expect(hasMatches.value).toBe(false)
  })
})
