import request from '@/request'

export async function uploadProductCover(body: FormData, options?: { [key: string]: any }) {
  return request<API.ResponseDTOString>('/file/upload/product-cover', {
    method: 'POST',
    headers: {
      'Content-Type': 'multipart/form-data',
    },
    data: body,
    ...(options || {}),
  })
}
