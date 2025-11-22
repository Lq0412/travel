// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 POST /ai/images/memory-card */
export async function generateMemoryCard(
  body: API.MemoryCardGenerateRequest,
  options?: { [key: string]: any }
) {
  return request<API.ResponseDTOMemoryCardGenerateResponse>('/ai/images/memory-card', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /ai/images/memory-card/status/${param0} */
export async function getMemoryCardStatus(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getMemoryCardStatusParams,
  options?: { [key: string]: any }
) {
  const { taskId: param0, ...queryParams } = params
  return request<API.ResponseDTOMemoryCardVO>(`/ai/images/memory-card/status/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /ai/images/memory-card/trip/${param0} */
export async function getMemoryCardByTripId(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getMemoryCardByTripIdParams,
  options?: { [key: string]: any }
) {
  const { tripId: param0, ...queryParams } = params
  return request<API.ResponseDTOMemoryCardVO>(`/ai/images/memory-card/trip/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 重新生成回忆图 POST /ai/images/memory-card/${tripId}/regenerate */
export async function regenerateMemoryCard(
  params: { tripId: number },
  options?: { [key: string]: any }
) {
  const { tripId } = params
  return request<API.ResponseDTOMemoryCardGenerateResponse>(`/ai/images/memory-card/${tripId}/regenerate`, {
    method: 'POST',
    ...(options || {}),
  })
}

/** 历史列表 GET /ai/images/memory-card/history/${tripId} */
export async function getMemoryCardHistory(
  params: { tripId: number },
  options?: { [key: string]: any }
) {
  const { tripId } = params
  return request<API.ResponseDTOListMemoryCardHistory>(`/ai/images/memory-card/history/${tripId}`, {
    method: 'GET',
    ...(options || {}),
  })
}

/** 设为当前 POST /ai/images/memory-card/history/${historyId}/set-current */
export async function setMemoryCardCurrentFromHistory(
  params: { historyId: number },
  options?: { [key: string]: any }
) {
  const { historyId } = params
  return request<API.ResponseDTOBoolean>(`/ai/images/memory-card/history/${historyId}/set-current`, {
    method: 'POST',
    ...(options || {}),
  })
}
