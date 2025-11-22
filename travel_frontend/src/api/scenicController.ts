// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 GET /scenic/${param0} */
export async function getScenicDetail(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getScenicDetailParams,
  options?: { [key: string]: any }
) {
  const { id: param0, ...queryParams } = params
  return request<API.ResponseDTOScenicSpot>(`/scenic/${param0}`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 GET /scenic/list */
export async function listSpots(options?: { [key: string]: any }) {
  return request<API.ResponseDTOListScenicSpot>('/scenic/list', {
    method: 'GET',
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /scenic/add */
export async function addSpot(body: API.ScenicSpot, options?: { [key: string]: any }) {
  return request<API.ResponseDTOLong>('/scenic/add', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /scenic/update */
export async function updateSpot(body: API.ScenicSpot, options?: { [key: string]: any }) {
  return request<API.ResponseDTOBoolean>('/scenic/update', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /scenic/delete */
export async function deleteSpot(body: API.DeleteRequest, options?: { [key: string]: any }) {
  return request<API.ResponseDTOBoolean>('/scenic/delete', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    data: body,
    ...(options || {}),
  })
}
