import type { Product } from '@/types/product'

type TripLike = Pick<API.TripVO, 'destination' | 'structuredData'>

type Coordinate = {
  latitude: number
  longitude: number
}

function normalizeCity(value?: string) {
  return value?.trim().toLowerCase() ?? ''
}

function toFiniteNumber(value: unknown) {
  if (typeof value === 'number' && Number.isFinite(value)) {
    return value
  }

  if (typeof value === 'string' && value.trim()) {
    const parsed = Number(value)
    return Number.isFinite(parsed) ? parsed : null
  }

  return null
}

function extractReferenceCoordinate(trip: TripLike): Coordinate | null {
  if (!trip.structuredData) {
    return null
  }

  try {
    const parsed = JSON.parse(trip.structuredData) as {
      dailyPlans?: Array<{
        activities?: Array<{
          location?: {
            latitude?: number | string
            longitude?: number | string
            coordinates?: [number, number]
          }
        }>
      }>
    }

    for (const dailyPlan of parsed.dailyPlans ?? []) {
      for (const activity of dailyPlan.activities ?? []) {
        const latitude =
          toFiniteNumber(activity.location?.latitude) ??
          toFiniteNumber(activity.location?.coordinates?.[1])
        const longitude =
          toFiniteNumber(activity.location?.longitude) ??
          toFiniteNumber(activity.location?.coordinates?.[0])

        if (latitude !== null && longitude !== null) {
          return { latitude, longitude }
        }
      }
    }
  } catch (error) {
    if (import.meta.env.DEV) {
      console.warn('[productRecommendation] 解析行程坐标失败', error)
    }
  }

  return null
}

function haversineDistanceMeters(source: Coordinate, target: Coordinate) {
  const earthRadiusMeters = 6371000
  const toRadians = (degrees: number) => (degrees * Math.PI) / 180
  const dLat = toRadians(target.latitude - source.latitude)
  const dLon = toRadians(target.longitude - source.longitude)
  const lat1 = toRadians(source.latitude)
  const lat2 = toRadians(target.latitude)

  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.sin(dLon / 2) ** 2 * Math.cos(lat1) * Math.cos(lat2)

  return 2 * earthRadiusMeters * Math.asin(Math.sqrt(a))
}

function formatDistance(distanceMeters: number) {
  if (distanceMeters < 1000) {
    return `距行程约 ${Math.round(distanceMeters)}m`
  }

  return `距行程约 ${(distanceMeters / 1000).toFixed(1)}km`
}

export function getMatchedProductsByTrip(products: Product[], trip: TripLike): Product[] {
  const MAX_DISTANCE_METERS = 30_000
  const destination = normalizeCity(trip.destination)
  const matched = products.filter((product) => {
    return product.isRecommendable && normalizeCity(product.city) === destination
  })

  const referenceCoordinate = extractReferenceCoordinate(trip)
  if (!referenceCoordinate) {
    return matched.map((product) => ({
      ...product,
      distanceText: undefined,
    }))
  }

  return matched
    .map((product, index) => {
      if (typeof product.latitude !== 'number' || typeof product.longitude !== 'number') {
        return {
          product: {
            ...product,
            distanceText: undefined,
          },
          distance: Number.POSITIVE_INFINITY,
          index,
        }
      }

      const distance = haversineDistanceMeters(referenceCoordinate, {
        latitude: product.latitude,
        longitude: product.longitude,
      })

      return {
        product: {
          ...product,
          distanceText: formatDistance(distance),
        },
        distance,
        index,
      }
    })
    .filter((item) => item.distance <= MAX_DISTANCE_METERS)
    .sort((left, right) => {
      if (left.distance === right.distance) {
        return left.index - right.index
      }
      return left.distance - right.distance
    })
    .map((item) => item.product)
}
