import type { ChatItem, Conversation } from '@/types/chat'

export function mapConversationVOToConversation(source: API.AIConversationVO): Conversation {
  return {
    id: source.id != null ? String(source.id) : '',
    title: source.title?.trim() || '新对话',
    updateTime: source.updateTime,
  }
}

export function mapAIMessageToChatItem(source: API.AIMessage): ChatItem {
  return {
    role: source.role === 'user' ? 'user' : 'ai',
    text: source.content || '',
    time: new Date(source.createTime || Date.now()),
  }
}
