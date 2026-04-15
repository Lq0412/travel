import { beforeEach, describe, expect, it, vi } from 'vitest'

import type { Product } from '@/types/product'

const getMyProductsMock = vi.fn()
const getProductByIdMock = vi.fn()
const saveProductMock = vi.fn()
const deleteProductByIdMock = vi.fn()

vi.mock('@/api/productController', () => ({
  getMyProducts: getMyProductsMock,
  getProductById: getProductByIdMock,
  saveProduct: saveProductMock,
  deleteProductById: deleteProductByIdMock,
}))

describe('productCatalog', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.clearAllMocks()

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

  it('omits id when saving a newly created product', async () => {
    const { saveProductToServer } = await import('./productCatalog')
    const product: Product = {
      id: '',
      name: '揭阳豆干',
      city: '揭阳',
      address: '揭阳市榕城区进贤门大道 18 号',
      latitude: 23.54321,
      longitude: 116.35555,
      tags: ['伴手礼'],
      description: '测试商品',
      isRecommendable: true,
      isPurchasable: true,
      cover: 'https://example.com/cover.jpg',
    }

    saveProductMock.mockResolvedValue({
      data: {
        code: 0,
        message: 'ok',
        data: 101,
      },
    })
    getMyProductsMock.mockResolvedValue({
      data: {
        code: 0,
        message: 'ok',
        data: [],
      },
    })

    await saveProductToServer(product)

    expect(saveProductMock).toHaveBeenCalledTimes(1)
    expect(Object.prototype.hasOwnProperty.call(saveProductMock.mock.calls[0][0], 'id')).toBe(false)
  })

  it('includes numeric id when saving an existing product', async () => {
    const { saveProductToServer } = await import('./productCatalog')
    const product: Product = {
      id: '15',
      name: '揭阳乒乓粿组合',
      city: '揭阳',
      address: '揭阳市榕城区中山路 66 号',
      latitude: 23.53555,
      longitude: 116.36001,
      tags: ['小吃'],
      description: '测试商品',
      isRecommendable: true,
      isPurchasable: true,
      cover: 'https://example.com/cover.jpg',
    }

    saveProductMock.mockResolvedValue({
      data: {
        code: 0,
        message: 'ok',
        data: 15,
      },
    })
    getMyProductsMock.mockResolvedValue({
      data: {
        code: 0,
        message: 'ok',
        data: [],
      },
    })

    await saveProductToServer(product)

    expect(saveProductMock).toHaveBeenCalledWith(
      expect.objectContaining({
        id: 15,
        address: '揭阳市榕城区中山路 66 号',
        latitude: 23.53555,
        longitude: 116.36001,
      }),
    )
  })
})
