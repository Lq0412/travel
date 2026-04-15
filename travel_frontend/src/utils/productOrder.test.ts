import { beforeEach, describe, expect, it } from 'vitest'

import { getLatestFakeOrder, saveFakeOrder } from './productOrder'

describe('productOrder', () => {
  beforeEach(() => {
    const storage = new Map<string, string>()
    Object.defineProperty(globalThis, 'localStorage', {
      configurable: true,
      value: {
        clear: () => storage.clear(),
        getItem: (key: string) => storage.get(key) ?? null,
        key: (index: number) => Array.from(storage.keys())[index] ?? null,
        removeItem: (key: string) => {
          storage.delete(key)
        },
        setItem: (key: string, value: string) => {
          storage.set(key, value)
        },
        get length() {
          return storage.size
        },
      } satisfies Storage,
    })
    localStorage.clear()
  })

  it('persists and restores the latest fake paid order', () => {
    saveFakeOrder({
      productId: '13',
      amountLabel: '￥99.00',
    })

    const order = getLatestFakeOrder()

    expect(order).toBeTruthy()
    expect(order?.productId).toBe('13')
    expect(order?.amountLabel).toBe('￥99.00')
    expect(order?.status).toBe('paid')
    expect(order?.orderNo).toMatch(/^MOCK-/)
  })
})
