import { describe, expect, it } from 'vitest'

import {
  HOME_DESTINATIONS,
  type HomeDestination,
  defaultHomeDestination,
  getHomeDestinationById,
  useHomeDestinations,
} from './useHomeDestinations'

describe('useHomeDestinations', () => {
  it('provides curated homepage destinations with tokyo as the stable default', () => {
    expect(HOME_DESTINATIONS).toHaveLength(4)
    expect(HOME_DESTINATIONS.map((destination) => destination.id)).toEqual([
      'tokyo',
      'paris',
      'iceland',
      'island',
    ])
    expect(defaultHomeDestination.id).toBe('tokyo')
  })

  it('looks up destinations by id and falls back to the default destination', () => {
    expect(getHomeDestinationById('paris')?.id).toBe('paris')
    expect(getHomeDestinationById('paris')?.prompt).toContain('巴黎')
    expect(getHomeDestinationById('missing')).toBeUndefined()
    expect(getHomeDestinationById(defaultHomeDestination.id)).toBe(defaultHomeDestination)
  })

  it('tracks the active destination and supports switching by id', () => {
    const { destinations, activeDestination, setActiveDestination, resetActiveDestination } =
      useHomeDestinations()

    expect(destinations.value).toBe(HOME_DESTINATIONS)
    expect(activeDestination.value).toBe(defaultHomeDestination)

    setActiveDestination('iceland')
    expect(activeDestination.value.id).toBe('iceland')

    setActiveDestination('missing')
    expect(activeDestination.value.id).toBe('iceland')

    resetActiveDestination()
    expect(activeDestination.value).toBe(defaultHomeDestination)
  })

  it('resets to the initialized destination when a non-default id is provided', () => {
    const { activeDestination, setActiveDestination, resetActiveDestination } =
      useHomeDestinations('paris')

    expect(activeDestination.value.id).toBe('paris')

    setActiveDestination('iceland')
    expect(activeDestination.value.id).toBe('iceland')

    resetActiveDestination()
    expect(activeDestination.value.id).toBe('paris')
  })

  it('prevents shared destination configuration from being mutated at runtime', () => {
    expect(() => {
      ;(HOME_DESTINATIONS as HomeDestination[]).push(defaultHomeDestination)
    }).toThrow()

    expect(() => {
      const tokyo = getHomeDestinationById('tokyo')
      if (!tokyo) {
        throw new Error('tokyo destination should exist')
      }
      ;(tokyo.theme as HomeDestination['theme']).accent = '#000000'
    }).toThrow()

    const { activeDestination } = useHomeDestinations()
    expect(activeDestination.value.theme.accent).toBe(defaultHomeDestination.theme.accent)
  })
})
