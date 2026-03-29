import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '@/pages/HomePage.vue'
import WorkspacePage from '@/pages/workspace/WorkspacePage.vue'
import TripsPage from '@/pages/trips/TripsPage.vue'
import TripDetailPage from '@/pages/trips/TripDetailPage.vue'
import ProfilePage from '@/pages/user/ProfilePage.vue'
import UserLoginPage from '@/pages/user/UserLoginPage.vue'
import UserRegisterPage from '@/pages/user/UserRegisterPage.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: '首页',
      component: HomePage,
      meta: { layout: 'topnav' },
    },
    {
      path: '/workspace',
      name: '旅行工作台',
      component: WorkspacePage,
      meta: { layout: 'topnav' },
    },
    {
      path: '/trips',
      name: '我的行程',
      component: TripsPage,
      meta: { layout: 'topnav' },
    },
    {
      path: '/trips/:id',
      name: '行程详情',
      component: TripDetailPage,
      meta: { layout: 'topnav' },
    },
    {
      path: '/profile',
      name: '我的',
      component: ProfilePage,
      meta: { layout: 'topnav' },
    },
    {
      path: '/user/login',
      name: '用户登录',
      component: UserLoginPage,
      meta: { layout: 'none' },
    },
    {
      path: '/user/register',
      name: '用户注册',
      component: UserRegisterPage,
      meta: { layout: 'none' },
    },
    {
      path: '/dashboard',
      redirect: '/workspace',
    },
    {
      path: '/user/helper',
      redirect: '/workspace',
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/',
    },
  ],
})

export default router
