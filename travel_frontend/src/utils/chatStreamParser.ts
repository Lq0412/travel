import type { StructuredItinerary } from '@/types/itinerary'

const STRUCTURED_DATA_START = '__STRUCTURED_DATA_START__'
const STRUCTURED_DATA_END = '__STRUCTURED_DATA_END__'

export function parsePayload(raw: string): string {
  const text = typeof raw === 'string' ? raw : String(raw ?? '')
  if (!text) return ''

  try {
    const parsed = JSON.parse(text)
    if (parsed?.content) return String(parsed.content)
    if (parsed?.delta?.content) return String(parsed.delta.content)
    if (Array.isArray(parsed?.choices) && parsed.choices[0]?.delta?.content) {
      return String(parsed.choices[0].delta.content)
    }
    if (typeof parsed?.data === 'string') return parsed.data
    return text
  } catch {
    return text
  }
}

export function filterAIResponse(fullText: string): string {
  if (!fullText) return ''

  const observationMatch = fullText.match(/观察[:：]\s*(.+)/s)
  if (observationMatch) {
    return observationMatch[1].trim().replace(/^🏞️\s*/g, '')
  }

  if (
    fullText.includes('思考:') ||
    fullText.includes('思考：') ||
    fullText.includes('行动:') ||
    fullText.includes('行动：')
  ) {
    return ''
  }

  return fullText
}

export function extractStructuredData(text: string): StructuredItinerary | null {
  const startIndex = text.indexOf(STRUCTURED_DATA_START)
  const endIndex = text.indexOf(STRUCTURED_DATA_END)

  if (startIndex === -1 || endIndex === -1) return null

  const jsonStr = text.substring(startIndex + STRUCTURED_DATA_START.length, endIndex).trim()
  try {
    return JSON.parse(jsonStr) as StructuredItinerary
  } catch {
    return null
  }
}

export function removeStructuredDataMarkers(text: string): string {
  return text
    .replace(/__STRUCTURED_DATA_START__[\s\S]*?__STRUCTURED_DATA_END__/g, '')
    .replace(/```json[\s\S]*?```/g, '')
    .trim()
}

export function hasDebugMarkers(text: string): boolean {
  return text.includes('思考') || text.includes('行动')
}
