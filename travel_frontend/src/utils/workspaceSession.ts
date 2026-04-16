import { extractStructuredData } from './chatStreamParser'
import type { StructuredItinerary } from '@/types/itinerary'

type ConversationMessageLike = {
  role?: string | null
  content?: string | null
}

type ChatMessageLike = {
  role?: string | null
  text?: string | null
  time?: Date | null
}

export type ChatLoadingStage = 'analysis' | 'generation' | 'structuring'

function truncate(input: string, maxLength: number) {
  return input.length > maxLength ? `${input.slice(0, maxLength)}...` : input
}

export function buildConversationTitle(input?: string | null) {
  const value = input?.trim() ?? ''
  if (!value) {
    return '新会话'
  }

  return truncate(value, 14)
}

export function formatConversationTime(input?: string | null) {
  if (!input) {
    return '等待生成'
  }

  const date = new Date(input)
  if (Number.isNaN(date.getTime())) {
    return '等待生成'
  }

  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function summarizeConversation(conversation?: API.AIConversationVO | null) {
  return {
    title: buildConversationTitle(conversation?.title),
    subtitle: conversation?.updateTime ? '最近更新' : '等待继续提问',
    timeLabel: formatConversationTime(conversation?.updateTime ?? conversation?.createTime),
  }
}

export function buildChatMessageDisplayText(
  message: ChatMessageLike,
  options?: {
    isLatest?: boolean
    isLoading?: boolean
    loadingStage?: ChatLoadingStage
  },
) {
  const text = message?.text ?? ''
  if (text.trim()) {
    return text
  }

  if (message?.role === 'ai' && options?.isLatest && options?.isLoading) {
    if (options.loadingStage === 'generation') {
      return '正在生成路线与推荐...'
    }

    if (options.loadingStage === 'structuring') {
      return '正在整理地图和行程信息...'
    }

    return '正在分析你的出行需求...'
  }

  return ''
}

export function restoreStructuredItineraryFromHistory(
  messages: ConversationMessageLike[],
  fallback?: StructuredItinerary | null,
) {
  const aiMessages = messages.filter((msg) => msg?.role === 'assistant' || msg?.role === 'ai')

  for (let i = aiMessages.length - 1; i >= 0; i -= 1) {
    const currentMessage = aiMessages[i]
    const rawContent = typeof currentMessage?.content === 'string' ? currentMessage.content.trim() : ''
    if (!rawContent) {
      continue
    }

    const structuredByMarkers = extractStructuredData(rawContent)
    if (structuredByMarkers) {
      return structuredByMarkers
    }

    try {
      const directJson = JSON.parse(rawContent) as StructuredItinerary
      if (directJson && typeof directJson === 'object' && Array.isArray(directJson.dailyPlans)) {
        return directJson
      }
    } catch {
      // ignore invalid JSON payloads and continue scanning older assistant messages
    }
  }

  return fallback ?? null
}
