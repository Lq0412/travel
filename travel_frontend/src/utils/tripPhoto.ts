const HTTP_URL_PATTERN = /^https?:\/\//i

export function sanitizePhotoUrl(input?: string | null) {
  const value = input?.trim() ?? ''
  if (!value || !HTTP_URL_PATTERN.test(value)) {
    return ''
  }

  try {
    const url = new URL(value)
    return url.toString()
  } catch {
    return ''
  }
}

export function normalizeTripPhotos(photos?: API.TripPhotoVO[] | null) {
  return [...(photos ?? [])].sort((left, right) => {
    const sortDiff = Number(left.sortOrder ?? Number.MAX_SAFE_INTEGER) - Number(right.sortOrder ?? Number.MAX_SAFE_INTEGER)
    if (sortDiff !== 0) {
      return sortDiff
    }

    const leftTime = new Date(left.createTime ?? 0).getTime()
    const rightTime = new Date(right.createTime ?? 0).getTime()
    return leftTime - rightTime
  })
}

export function canAddMorePhotos(photos: ArrayLike<unknown>, max = 6) {
  return photos.length < max
}
