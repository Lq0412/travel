import { describe, expect, it } from 'vitest'

import { shouldRestoreRecentTripItinerary } from './tripWorkflow'

describe('tripWorkflow utils', () => {
  it('does not restore structured itinerary when recent trip is used for product recommendations', () => {
    expect(shouldRestoreRecentTripItinerary('recommend-products')).toBe(false)
  })

  it('restores structured itinerary when recent trip is used to continue trip planning', () => {
    expect(shouldRestoreRecentTripItinerary('resume-trip')).toBe(true)
  })
})
