import { describe, expect, it } from 'vitest'
import { resolveLayout } from './layoutResolver'

describe('layoutResolver', () => {
  it('maps route meta layout strings to supported app layouts', () => {
    expect(resolveLayout('landing')).toBe('landing')
    expect(resolveLayout(' none ')).toBe('router-view')
    expect(resolveLayout(undefined)).toBe('topnav')
  })

  it('falls back to topnav for unsupported layout values', () => {
    expect(resolveLayout('sidebar')).toBe('topnav')
  })

  it('normalizes layout values before resolving', () => {
    expect(resolveLayout('LANDING')).toBe('landing')
  })

  it('falls back to topnav for non-string layout metadata', () => {
    expect(resolveLayout(123)).toBe('topnav')
    expect(resolveLayout({ layout: 'landing' })).toBe('topnav')
  })
})
