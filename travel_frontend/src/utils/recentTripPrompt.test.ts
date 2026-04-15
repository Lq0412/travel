import { beforeEach, describe, expect, it, vi } from 'vitest'

const { fetchMyProductsMock } = vi.hoisted(() => ({
  fetchMyProductsMock: vi.fn(),
}))

vi.mock('@/utils/productCatalog', () => ({
  fetchMyProducts: fetchMyProductsMock,
}))

import { buildRecentTripRecommendationPrompt } from './recentTripPrompt'

describe('recentTripPrompt utils', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    fetchMyProductsMock.mockResolvedValue([
      {
        id: 'chengdu-1',
        name: '成都火锅双人餐',
        city: '成都',
        address: '成都市锦江区红星路一段 1 号',
        latitude: 30.659,
        longitude: 104.083,
        tags: ['美食', '火锅'],
        description: '适合第一次到成都的游客安排一顿重口味热辣晚餐。',
        isRecommendable: true,
        isPurchasable: true,
        cover: 'https://example.org/chengdu-hotpot.jpg',
      },
      {
        id: 'hangzhou-1',
        name: '杭州龙井茶酥礼盒',
        city: '杭州',
        address: '杭州市西湖区龙井路 18 号',
        latitude: 30.229,
        longitude: 120.112,
        tags: ['伴手礼'],
        description: '适合返程带走的杭州本地茶点。',
        isRecommendable: true,
        isPurchasable: true,
        cover: 'https://example.org/hangzhou-gift.jpg',
      },
      {
        id: 'chengdu-2',
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

  it('builds a city-aware recommendation prompt from trip summary', async () => {
    const prompt = await buildRecentTripRecommendationPrompt({
      destination: '成都',
      days: 3,
      theme: '美食游',
    })

    expect(prompt).toContain('成都 3 天')
    expect(prompt).toContain('美食游')
    expect(prompt).toContain('当地特色美食')
    expect(prompt).toContain('成都火锅双人餐')
  })

  it('falls back gracefully when trip metadata is incomplete', async () => {
    const prompt = await buildRecentTripRecommendationPrompt({
      destination: '杭州',
    })

    expect(prompt).toContain('杭州')
    expect(prompt).not.toContain('潮汕旅行语境')
  })

  it('does not inject any default product copy when the real product library is empty', async () => {
    fetchMyProductsMock.mockResolvedValueOnce([])

    const prompt = await buildRecentTripRecommendationPrompt({
      destination: '揭阳',
      days: 2,
    })

    expect(prompt).toContain('揭阳')
    expect(prompt).not.toContain('揭阳普宁豆干礼盒')
    expect(prompt).not.toContain('请优先参考以下“自定义美食库”商品')
  })

  it('lists nearer matched products first when trip structured data includes coordinates', async () => {
    const prompt = await buildRecentTripRecommendationPrompt({
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

    expect(prompt.indexOf('春熙路串串双人餐')).toBeLessThan(prompt.indexOf('成都火锅双人餐'))
  })
})
