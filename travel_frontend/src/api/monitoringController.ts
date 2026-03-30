// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 DELETE /ai/monitor/cache/all */
export async function clearAllCache(options?: { [key: string]: any }) {
  return request<API.ResponseDTOVoid>('/ai/monitor/cache/all', {
    method: 'DELETE',
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 DELETE /ai/monitor/cache/conversation/${param0} */
export async function clearConversationCache(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.clearConversationCacheParams,
  options?: { [key: string]: any }
) {
  const { conversationId: param0, ...queryParams } = params
  return request<API.ResponseDTOVoid>(`/ai/monitor/cache/conversation/${param0}`, {
    method: 'DELETE',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /ai/monitor/cache/stats */
export async function cacheStats(options?: { [key: string]: any }) {
  return request<API.ResponseDTOMapStringObject>('/ai/monitor/cache/stats', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /ai/monitor/executor/stats */
export async function executorStats(options?: { [key: string]: any }) {
  return request<API.ResponseDTOMapStringObject>('/ai/monitor/executor/stats', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /ai/monitor/health */
export async function health(options?: { [key: string]: any }) {
  return request<API.ResponseDTOMapStringObject>('/ai/monitor/health', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /ai/monitor/quota */
export async function getQuota(options?: { [key: string]: any }) {
  return request<API.ResponseDTOMapStringObject>('/ai/monitor/quota', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /ai/monitor/rag/milvus/query-count */
export async function queryMilvusCount(
  params?: {
    limit?: number
  },
  options?: { [key: string]: any }
) {
  return request<API.ResponseDTOMapStringObject>('/ai/monitor/rag/milvus/query-count', {
    method: 'GET',
    params: {
      ...(params || {}),
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /ai/monitor/rag/milvus/sync */
export async function syncMilvusKnowledge(
  params?: {
    recreateCollection?: boolean
  },
  options?: { [key: string]: any }
) {
  return request<API.ResponseDTOMapStringObject>('/ai/monitor/rag/milvus/sync', {
    method: 'POST',
    params: {
      ...(params || {}),
    },
    ...(options || {}),
  })
}

/** 创建数据补齐任务 POST /ai/tasks/knowledge/ingest */
export async function createKnowledgeIngestionTask(
  data: {
    query: string
    dataSource?: 'AUTO' | 'TAVILY' | 'DASHSCOPE'
    effectPreset?: 'FAST' | 'BALANCED' | 'DEEP'
    maxItems?: number
    mustContainStoreName?: boolean
    maxRetry?: number
  },
  options?: { [key: string]: any }
) {
  return request<API.ResponseDTOMapStringObject>('/ai/tasks/knowledge/ingest', {
    method: 'POST',
    data,
    ...(options || {}),
  })
}

/** 查询任务状态 GET /ai/tasks/${taskId} */
export async function getAiTaskStatus(taskId: string, options?: { [key: string]: any }) {
  return request<API.ResponseDTOMapStringObject>(`/ai/tasks/${taskId}`, {
    method: 'GET',
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /ai/monitor/quota/recharge */
export async function rechargeQuota(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.rechargeQuotaParams,
  options?: { [key: string]: any }
) {
  return request<API.ResponseDTOVoid>('/ai/monitor/quota/recharge', {
    method: 'POST',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 PUT /ai/monitor/quota/reset */
export async function resetUserQuota(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.resetUserQuotaParams,
  options?: { [key: string]: any }
) {
  return request<API.ResponseDTOVoid>('/ai/monitor/quota/reset', {
    method: 'PUT',
    params: {
      ...params,
    },
    ...(options || {}),
  })
}
