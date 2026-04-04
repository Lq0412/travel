import { describe, expect, it } from 'vitest'
import {
  buildConversationTitle,
  formatConversationTime,
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
})
