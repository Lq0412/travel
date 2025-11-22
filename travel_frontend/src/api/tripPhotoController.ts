// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 此处后端没有提供注释 GET /trips/${param0}/photos */
export async function getTripPhotos(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.getTripPhotosParams,
  options?: { [key: string]: any }
) {
  const { tripId: param0, ...queryParams } = params
  return request<API.ResponseDTOListTripPhotoVO>(`/trips/${param0}/photos`, {
    method: 'GET',
    params: { ...queryParams },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /trips/${param0}/photos */
export async function uploadPhoto(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.uploadPhotoParams,
  options?: { [key: string]: any }
) {
  const { tripId: param0, ...queryParams } = params
  return request<API.ResponseDTOTripPhotoVO>(`/trips/${param0}/photos`, {
    method: 'POST',
    params: {
      ...queryParams,
    },
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 POST /trips/${param0}/photos/batch */
export async function uploadPhotos(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.uploadPhotosParams,
  body: string[],
  options?: { [key: string]: any }
) {
  const { tripId: param0, ...queryParams } = params
  return request<API.ResponseDTOListTripPhotoVO>(`/trips/${param0}/photos/batch`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    params: { ...queryParams },
    data: body,
    ...(options || {}),
  })
}

/** 此处后端没有提供注释 DELETE /trips/photos/${param0} */
export async function deletePhoto(
  // 叠加生成的Param类型 (非body参数swagger默认没有生成对象)
  params: API.deletePhotoParams,
  options?: { [key: string]: any }
) {
  const { photoId: param0, ...queryParams } = params
  return request<API.ResponseDTOBoolean>(`/trips/photos/${param0}`, {
    method: 'DELETE',
    params: { ...queryParams },
    ...(options || {}),
  })
}
