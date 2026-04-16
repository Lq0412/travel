import { describe, expect, it } from 'vitest'
import { removeStructuredDataMarkers } from './chatStreamParser'

describe('chatStreamParser', () => {
  it('removes incomplete json code fences during streaming', () => {
    const text = `为你规划成都 3 天美食之旅。\n\n\`\`\`json\n{\n  "destination":"成都"`

    expect(removeStructuredDataMarkers(text)).toBe('为你规划成都 3 天美食之旅。')
  })

  it('removes structured data markers even when the end marker has not arrived yet', () => {
    const text =
      '先给你一个总览\n__STRUCTURED_DATA_START__\n{"destination":"成都","days":3,"dailyPlans":[]}'

    expect(removeStructuredDataMarkers(text)).toBe('先给你一个总览')
  })

  it('keeps normal markdown content when there is no structured payload', () => {
    const text = '推荐先去宽窄巷子，再去锦里。\n\n- 地铁出行更稳妥'

    expect(removeStructuredDataMarkers(text)).toBe(text)
  })
})
