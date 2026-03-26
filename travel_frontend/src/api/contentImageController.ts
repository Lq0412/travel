// @ts-ignore
/* eslint-disable */
import request from '@/request'

/** 搜索页面内容图片 GET /content/images/search */
export async function searchContentImages(
  params: API.searchContentImagesParams,
  options?: { [key: string]: any }
) {
  return request<API.ResponseDTOListContentImageVO>('/content/images/search', {
    method: 'GET',
    params,
    ...(options || {}),
  })
}
