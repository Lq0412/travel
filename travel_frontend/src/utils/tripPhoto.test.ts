import { describe, expect, it } from 'vitest'
import { canAddMorePhotos, normalizeTripPhotos, sanitizePhotoUrl } from './tripPhoto'

describe('tripPhoto utils', () => {
  it('sanitizes and accepts valid http photo urls', () => {
    expect(sanitizePhotoUrl('  https://example.com/photo.jpg  ')).toBe('https://example.com/photo.jpg')
  })

  it('rejects non-http photo urls', () => {
    expect(sanitizePhotoUrl('ftp://example.com/photo.jpg')).toBe('')
    expect(sanitizePhotoUrl('javascript:alert(1)')).toBe('')
  })

  it('sorts photos by sort order and creation time', () => {
    const photos = normalizeTripPhotos([
      { id: 3, sortOrder: 3, createTime: '2026-04-01T12:00:00' },
      { id: 2, sortOrder: 1, createTime: '2026-04-02T12:00:00' },
      { id: 1, sortOrder: 1, createTime: '2026-04-01T12:00:00' },
    ])

    expect(photos.map((item) => item.id)).toEqual([1, 2, 3])
  })

  it('limits photo count to six items', () => {
    expect(canAddMorePhotos(new Array(5).fill({}))).toBe(true)
    expect(canAddMorePhotos(new Array(6).fill({}))).toBe(false)
  })
})
