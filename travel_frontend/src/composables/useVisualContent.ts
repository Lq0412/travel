import { searchContentImages } from '@/api/contentImageController'

export const undrawIllustrations = {
  travelMode: 'https://cdn.undraw.co/illustrations/travel-mode_ydxo.svg',
  aircraft: 'https://cdn.undraw.co/illustrations/aircraft_usu4.svg',
  journey: 'https://cdn.undraw.co/illustrations/journey_brk8.svg',
} as const

export function useVisualContent() {
  async function fetchCollection(query: string, perPage = 6) {
    try {
      const response = await searchContentImages({ query, perPage })
      return response?.data?.data || []
    } catch {
      return []
    }
  }

  async function fetchFirst(query: string) {
    const images = await fetchCollection(query, 1)
    return images[0] || null
  }

  return {
    illustrations: undrawIllustrations,
    fetchCollection,
    fetchFirst,
  }
}
