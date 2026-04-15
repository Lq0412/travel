import { beforeAll, describe, expect, it, vi } from 'vitest'

const HomePageStub = { name: 'HomePageStub' }
const WorkspacePageStub = { name: 'WorkspacePageStub' }
vi.mock('vue-router', async () => {
  const actual = await vi.importActual<typeof import('vue-router')>('vue-router')

  return {
    ...actual,
    createWebHistory: () => actual.createMemoryHistory(),
  }
})

vi.mock('@/pages/HomePage.vue', () => ({
  default: HomePageStub,
}))

vi.mock('@/pages/workspace/WorkspacePage.vue', () => ({
  default: WorkspacePageStub,
}))

vi.mock('@/pages/trips/TripsPage.vue', () => ({
  default: { name: 'TripsPageStub' },
}))

vi.mock('@/pages/trips/TripDetailPage.vue', () => ({
  default: { name: 'TripDetailPageStub' },
}))

vi.mock('@/pages/ai/AIAdminMonitorPage.vue', () => ({
  default: { name: 'AIAdminMonitorPageStub' },
}))

vi.mock('@/pages/user/ForumPage.vue', () => ({
  default: { name: 'ForumPageStub' },
}))

vi.mock('@/pages/user/ProfilePage.vue', () => ({
  default: { name: 'ProfilePageStub' },
}))

vi.mock('@/pages/user/UserLoginPage.vue', () => ({
  default: { name: 'UserLoginPageStub' },
}))

vi.mock('@/pages/user/UserRegisterPage.vue', () => ({
  default: { name: 'UserRegisterPageStub' },
}))

vi.mock('@/pages/mall/MallPage.vue', () => ({
  default: { name: 'MallPageStub' },
}))

vi.mock('@/pages/mall/ProductDetailPage.vue', () => ({
  default: { name: 'ProductDetailPageStub' },
}))

vi.mock('@/pages/mall/ProductPaymentSuccessPage.vue', () => ({
  default: { name: 'ProductPaymentSuccessPageStub' },
}))

let router: (typeof import('./index'))['default']

beforeAll(async () => {
  router = (await import('./index')).default
})

describe('router', () => {
  it('maps the homepage route to the top navigation layout and HomePage component', () => {
    const homeRoute = router.getRoutes().find((route) => route.path === '/')

    expect(homeRoute).toBeDefined()
    expect(homeRoute?.name).toBe('首页')
    expect(homeRoute?.meta.layout).toBe('topnav')
    expect(homeRoute?.components?.default).toBe(HomePageStub)
  })

  it('registers the mall route inside the app shell', () => {
    const mallRoute = router.getRoutes().find((route) => route.path === '/mall')

    expect(mallRoute).toBeDefined()
    expect(mallRoute?.name).toBe('商城')
    expect(mallRoute?.meta.layout).toBe('topnav')
  })

  it('keeps the workspace route pointed at WorkspacePage within the app shell', () => {
    const workspaceRoute = router.getRoutes().find((route) => route.path === '/workspace')

    expect(workspaceRoute).toBeDefined()
    expect(workspaceRoute?.name).toBe('旅行工作台')
    expect(workspaceRoute?.meta.layout).toBe('topnav')
    expect(workspaceRoute?.components?.default).toBe(WorkspacePageStub)
  })

  it('registers product detail and payment success routes inside the app shell', () => {
    const productDetailRoute = router.getRoutes().find((route) => route.path === '/products/:id')
    const productPaymentRoute = router
      .getRoutes()
      .find((route) => route.path === '/products/:id/payment-success')

    expect(productDetailRoute).toBeDefined()
    expect(productDetailRoute?.name).toBe('商品详情')
    expect(productDetailRoute?.meta.layout).toBe('topnav')
    expect(productPaymentRoute).toBeDefined()
    expect(productPaymentRoute?.name).toBe('支付成功')
    expect(productPaymentRoute?.meta.layout).toBe('topnav')
  })
})
