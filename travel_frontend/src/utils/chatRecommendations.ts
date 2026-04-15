import type { ChatRecommendationCards, ChatItem } from '@/types/chat'

export function attachRecommendationsToLatestAIMessage(
  messages: ChatItem[],
  recommendationCards: ChatRecommendationCards,
): ChatItem[] {
  const latestAIIndex = [...messages]
    .map((message, index) => ({ message, index }))
    .reverse()
    .find((entry) => entry.message.role === 'ai')?.index

  if (latestAIIndex === undefined) {
    return messages
  }

  return messages.map((message, index) => {
    if (index !== latestAIIndex) {
      return message
    }

    return {
      ...message,
      recommendationCards,
    }
  })
}
