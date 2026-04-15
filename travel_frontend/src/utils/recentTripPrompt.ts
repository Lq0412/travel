import { fetchMyProducts } from '@/utils/productCatalog'
import { getMatchedProductsByTrip } from '@/utils/productRecommendation'
import type { Product } from '@/types/product'

function buildTripSummary(trip: Pick<API.TripVO, 'destination' | 'days' | 'theme'>) {
  const summaryParts = []
  const headlineParts = []

  if (trip.destination?.trim()) {
    headlineParts.push(trip.destination.trim())
  }

  if (trip.days) {
    headlineParts.push(`${trip.days} 天`)
  }

  if (headlineParts.length > 0) {
    summaryParts.push(headlineParts.join(' '))
  }

  if (trip.theme?.trim()) {
    summaryParts.push(trip.theme.trim())
  }

  return summaryParts.join('，') || '当前城市行程'
}

export function buildRecentTripConversationLabel(
  trip: Pick<API.TripVO, 'destination'>,
) {
  const destination = trip.destination?.trim()
  return destination ? `请推荐 ${destination} 当地美食` : '请推荐当前城市当地美食'
}

export async function buildRecentTripRecommendationPrompt(
  trip: Pick<API.TripVO, 'destination' | 'days' | 'theme' | 'structuredData'>,
  products?: Product[],
) {
  const destination = trip.destination?.trim() || '当前城市'
  const summary = buildTripSummary(trip)
  const sourceProducts = products ?? await fetchMyProducts()
  const matchedProducts = getMatchedProductsByTrip(sourceProducts, trip).slice(0, 6)
  const basePrompt = `你是一名旅行美食助手。请基于当前行程所在城市，为用户推荐当地特色美食，并优先结合已有商品库内容输出答案。请先用 2 到 3 句话说明为什么推荐这些美食，再给出 3 到 6 个推荐项，覆盖美食特色、适合场景、是否适合买作伴手礼。当前行程：${summary}。当前城市：${destination}。`

  if (matchedProducts.length > 0) {
    const productListText = matchedProducts
      .map(
        (product) =>
          `- ${product.name} (城市: ${product.city}；地址: ${product.address || '未填写'}；标签: ${product.tags?.join('、') || '无'}${product.distanceText ? `；${product.distanceText}` : ''})：${product.description}`,
      )
      .join('\n')

    return `${basePrompt}\n\n请优先参考以下“自定义美食库”商品：\n${productListText}\n\n输出内容时，避免编造成具体门店，重点推荐用户在这个城市值得吃什么、可以买什么。`
  }

  return `${basePrompt}\n\n如果商品库里没有完全匹配的商品，也请基于城市特色继续完成推荐。`
}
