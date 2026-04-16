export type PublicTripStatus = 'planned' | 'completed'
export type RecentTripAction = 'recommend-products' | 'resume-trip'

export function normalizeTripStatus(status?: string | null): PublicTripStatus {
  return status === 'completed' ? 'completed' : 'planned'
}

export function getTripStatusLabel(status?: string | null) {
  return normalizeTripStatus(status) === 'completed' ? '已完成' : '待出行'
}

export function getTripStatusTone(status?: string | null) {
  return normalizeTripStatus(status) === 'completed' ? 'success' : 'processing'
}

export function shouldRestoreRecentTripItinerary(action: RecentTripAction) {
  return action === 'resume-trip'
}

export function isTripPublished(trip?: API.TripVO | null) {
  return Boolean(trip?.publishedToInspiration)
}

export function formatWorkflowDate(value?: string | Date | null) {
  if (!value) {
    return '待定'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return typeof value === 'string' ? value : '待定'
  }

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(date)
}

export function formatWorkflowDateTime(value?: string | Date | null) {
  if (!value) {
    return '待定'
  }

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return typeof value === 'string' ? value : '待定'
  }

  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date)
}
