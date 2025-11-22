// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 GET /ai/trips/${param0} */
export async function getTripById(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getTripByIdParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.ResponseDTOTripVO>(`/ai/trips/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 DELETE /ai/trips/${param0} */
export async function deleteTrip(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deleteTripParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.ResponseDTOBoolean>(`/ai/trips/${param0}`, {
    method: 'DELETE',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /ai/trips/${param0}/complete */
export async function completeTrip(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.completeTripParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.ResponseDTOBoolean>(`/ai/trips/${param0}/complete`, {
    method: 'POST',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /ai/trips/${param0}/publish-forum */
export async function publishToForum(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.publishToForumParams,
  body: API.ForumPublishRequest,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.ResponseDTOLong>(`/ai/trips/${param0}/publish-forum`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    params: { ...queryParams },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /ai/trips/generate */
export async function generateTrip(
  body: API.TripGenerateRequest,
  options?: { [key: string]: any }
) {
  return request<API.ResponseDTOTripGenerateResponse>('/ai/trips/generate', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /ai/trips/my */
export async function getMyTrips(options?: { [key: string]: any }) {
  return request<API.ResponseDTOListTripVO>('/ai/trips/my', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /ai/trips/save */
export async function saveTrip(body: API.TripSaveRequest, options?: { [key: string]: any }) {
  return request<API.ResponseDTOLong>('/ai/trips/save', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
