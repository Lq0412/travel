import { describe, expect, it } from 'vitest'

import type { ChatItem } from '@/types/chat'

import { attachRecommendationsToLatestAIMessage } from './chatRecommendations'

describe('chatRecommendations', () => {
  it('attaches recommendation cards to the latest ai message only', () => {
    const baseTime = new Date('2026-04-15T09:00:00.000Z')
    const messages: ChatItem[] = [
      { role: 'user', text: '帮我推荐成都美食', time: baseTime },
      { role: 'ai', text: '先去试试火锅。', time: new Date('2026-04-15T09:01:00.000Z') },
      { role: 'ai', text: '再给你几张商品卡片。', time: new Date('2026-04-15T09:02:00.000Z') },
    ]

    const nextMessages = attachRecommendationsToLatestAIMessage(messages, {
      title: '成都行程美食推荐',
      products: [
        {
          id: 'food-1',
          name: '成都火锅双人餐',
          city: '成都',
          address: '成都市锦江区红星路一段 1 号',
          tags: ['美食', '火锅'],
          description: '麻辣鲜香，适合夜间聚餐。',
          isRecommendable: true,
          isPurchasable: true,
          cover: 'https://example.org/chengdu-hotpot.jpg',
        },
      ],
    })

    expect(nextMessages).toHaveLength(3)
    expect(nextMessages[1].recommendationCards).toBeUndefined()
    expect(nextMessages[2].recommendationCards?.title).toBe('成都行程美食推荐')
    expect(nextMessages[2].recommendationCards?.products.map((product) => product.name)).toEqual([
      '成都火锅双人餐',
    ])
  })

  it('returns the original messages when no ai message exists', () => {
    const messages: ChatItem[] = [
      { role: 'user', text: '只保留用户消息', time: new Date('2026-04-15T09:00:00.000Z') },
    ]

    expect(
      attachRecommendationsToLatestAIMessage(messages, {
        title: '空推荐',
        products: [],
      }),
    ).toEqual(messages)
  })
})
