import { describe, expect, it } from 'vitest'

import { validateProductCoverFile } from './productCoverUpload'

describe('productCoverUpload', () => {
  it('accepts supported image files within 2MB', () => {
    const file = new File(['cover'], 'cover.png', { type: 'image/png' })

    expect(validateProductCoverFile(file)).toBeNull()
  })

  it('rejects non-image files', () => {
    const file = new File(['text'], 'cover.txt', { type: 'text/plain' })

    expect(validateProductCoverFile(file)).toBe('只能上传 JPG、PNG 或 WebP 图片')
  })

  it('rejects files larger than 2MB', () => {
    const largeFile = {
      name: 'large-cover.jpg',
      type: 'image/jpeg',
      size: 2 * 1024 * 1024 + 1,
    } as File

    expect(validateProductCoverFile(largeFile)).toBe('图片大小不能超过 2MB')
  })
})
