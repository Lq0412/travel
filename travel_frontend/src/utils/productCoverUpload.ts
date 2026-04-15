import { uploadProductCover } from '@/api/fileController'

const ALLOWED_FILE_TYPES = ['image/jpeg', 'image/jpg', 'image/png', 'image/webp']
const MAX_FILE_SIZE = 2 * 1024 * 1024

export function validateProductCoverFile(file: File): string | null {
  if (!ALLOWED_FILE_TYPES.includes(file.type)) {
    return '只能上传 JPG、PNG 或 WebP 图片'
  }

  if (file.size > MAX_FILE_SIZE) {
    return '图片大小不能超过 2MB'
  }

  return null
}

export async function uploadProductCoverFile(file: File): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)

  const response = await uploadProductCover(formData)
  if (response.data.code !== 0 || !response.data.data) {
    throw new Error(response.data.message || '图片上传失败')
  }

  return response.data.data
}
