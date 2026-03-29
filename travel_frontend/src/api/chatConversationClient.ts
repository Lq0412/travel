import {
  createConversation,
  deleteConversation,
  getConversationMessages,
  getUserConversations,
} from '@/api/conversationController'

type IdLike = string | number

function toGeneratedNumericParam(value: IdLike): number {
  // OpenAPI currently types these IDs as number, but runtime uses string safely for snowflake IDs.
  return value as unknown as number
}

export function getUserConversationsByUserId(userId: IdLike, pageNum = 1, pageSize = 50) {
  return getUserConversations({
    userId: toGeneratedNumericParam(userId),
    pageNum,
    pageSize,
  })
}

export function createConversationByUserId(params: {
  userId: IdLike
  title?: string
  provider?: string
  model?: string
}) {
  return createConversation({
    userId: toGeneratedNumericParam(params.userId),
    title: params.title,
    provider: params.provider,
    model: params.model,
  })
}

export function deleteConversationById(conversationId: IdLike, userId: IdLike) {
  return deleteConversation({
    conversationId: toGeneratedNumericParam(conversationId),
    userId: toGeneratedNumericParam(userId),
  })
}

export function getConversationMessagesById(conversationId: IdLike, userId: IdLike) {
  return getConversationMessages({
    conversationId: toGeneratedNumericParam(conversationId),
    userId: toGeneratedNumericParam(userId),
  })
}
