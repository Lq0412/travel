import { computed, shallowRef } from 'vue'

import { fetchMyProducts } from '@/utils/productCatalog'
import { getMatchedProductsByTrip } from '@/utils/productRecommendation'
import type { Product } from '@/types/product'

export function useRecentTripRecommendations() {
  const selectedTrip = shallowRef<API.TripVO | null>(null)
  const recommendedProducts = shallowRef<Product[]>([])

  const showRecommendations = computed(() => selectedTrip.value !== null)
  const hasMatches = computed(() => recommendedProducts.value.length > 0)
  const recommendationTitle = computed(() => {
    const destination = selectedTrip.value?.destination?.trim()
    if (!destination) {
      return '行程美食推荐'
    }

    return `${destination} 行程美食推荐`
  })

  async function selectTrip(trip: API.TripVO) {
    selectedTrip.value = trip
    const products = await fetchMyProducts()
    const nextProducts = getMatchedProductsByTrip(products, trip)
    recommendedProducts.value = nextProducts
    return nextProducts
  }

  function clearRecommendations() {
    selectedTrip.value = null
    recommendedProducts.value = []
  }

  return {
    selectedTrip,
    recommendedProducts,
    showRecommendations,
    hasMatches,
    recommendationTitle,
    selectTrip,
    clearRecommendations,
  }
}
