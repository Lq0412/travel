import { describe, expect, it } from 'vitest'
import {
  buildChatMessageDisplayText,
  buildConversationTitle,
  formatConversationTime,
  restoreStructuredItineraryFromHistory,
  summarizeConversation,
} from './workspaceSession'

describe('workspaceSession utils', () => {
  it('builds a compact conversation title from user input', () => {
    expect(buildConversationTitle('帮我做一个杭州三天两夜轻松游，老人友好一点')).toBe('帮我做一个杭州三天两夜轻松游...')
  })

  it('uses fallback title when input is blank', () => {
    expect(buildConversationTitle('   ')).toBe('新会话')
  })

  it('formats empty conversation time as waiting state', () => {
    expect(formatConversationTime()).toBe('等待生成')
  })

  it('uses a stable loading placeholder for the latest ai message', () => {
    expect(
      buildChatMessageDisplayText(
        { role: 'ai', text: '   ', time: new Date() },
        { isLatest: true, isLoading: true, loadingStage: 'analysis' },
      ),
    ).toBe('正在分析你的出行需求...')
  })

  it('prefers actual message content over the loading placeholder', () => {
    expect(
      buildChatMessageDisplayText(
        { role: 'ai', text: '已为你整理好成都 3 天路线。', time: new Date() },
        { isLatest: true, isLoading: true },
      ),
    ).toBe('已为你整理好成都 3 天路线。')
  })

  it('switches loading placeholder by stage', () => {
    expect(
      buildChatMessageDisplayText(
        { role: 'ai', text: '', time: new Date() },
        { isLatest: true, isLoading: true, loadingStage: 'generation' },
      ),
    ).toBe('正在生成路线与推荐...')

    expect(
      buildChatMessageDisplayText(
        { role: 'ai', text: '', time: new Date() },
        { isLatest: true, isLoading: true, loadingStage: 'structuring' },
      ),
    ).toBe('正在整理地图和行程信息...')
  })

  it('summarizes conversation title and recent time', () => {
    expect(
      summarizeConversation({
        title: '杭州轻松游',
        updateTime: '2026-04-02T09:30:00',
      }),
    ).toMatchObject({
      title: '杭州轻松游',
      subtitle: '最近更新',
    })
  })

  it('restores structured itinerary from the latest assistant message markers', () => {
    const structured = restoreStructuredItineraryFromHistory([
      { role: 'user', content: '帮我规划杭州三日游' },
      {
        role: 'assistant',
        content:
          '好的\n__STRUCTURED_DATA_START__\n{"destination":"杭州","days":3,"dailyPlans":[{"day":1,"activities":[]}]}\n__STRUCTURED_DATA_END__',
      },
    ])

    expect(structured).toMatchObject({
      destination: '杭州',
      days: 3,
    })
  })

  it('falls back to cached structured itinerary when history does not contain recoverable data', () => {
    const cached = {
      destination: '成都',
      days: 2,
      dailyPlans: [{ day: 1, activities: [] }],
    }

    const structured = restoreStructuredItineraryFromHistory(
      [
        { role: 'user', content: '成都周末游' },
        { role: 'assistant', content: '给你安排了一个轻松周末方案。' },
      ],
      cached,
    )

    expect(structured).toEqual(cached)
  })

  it('restores structured itinerary from direct json assistant content', () => {
    const structured = restoreStructuredItineraryFromHistory([
      { role: 'assistant', content: '{"destination":"苏州","days":2,"dailyPlans":[{"day":1,"activities":[]}]}' },
    ])

    expect(structured).toMatchObject({
      destination: '苏州',
      days: 2,
    })
  })
})
