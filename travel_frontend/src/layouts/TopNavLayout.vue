<template>
  <div class="top-nav-layout">
    <header class="top-navbar" :class="{ scrolled: isScrolled }">
      <div class="navbar-left">
        <router-link to="/" class="brand-link">
          <img src="https://unpkg.com/lucide-static@latest/icons/compass.svg" alt="logo" class="brand-icon" />
          <span class="brand-text">AI 旅游</span>
        </router-link>
      </div>

      <nav class="navbar-menu" v-if="!isMobile">
        <router-link
          v-for="item in menuItems"
          :key="item.key"
          :to="item.path"
          class="menu-item"
          :class="{ active: isActiveRoute(item.path) }"
          @click="closeUserMenu"
        >
          <span>{{ item.label }}</span>
        </router-link>
      </nav>

      <div class="navbar-right">
        <template v-if="isLoggedIn">
          <div class="user-menu">
            <button class="user-trigger" @click="toggleUserMenu">
              <img
                :src="loginUserStore.loginUser.userAvatar || 'https://unpkg.com/lucide-static@latest/icons/user-circle.svg'"
                alt="avatar"
                class="user-avatar"
              />
              <span class="user-name">{{ loginUserStore.loginUser.userName || '用户' }}</span>
            </button>
            <transition name="dropdown-fade">
              <div v-if="showUserMenu" class="user-dropdown" @click.stop>
                <div class="dropdown-header">
                  <div class="dropdown-name">{{ loginUserStore.loginUser.userName || '用户' }}</div>
                  <div class="dropdown-email">{{ userEmail }}</div>
                </div>
                <div class="dropdown-divider"></div>
                <router-link to="/dashboard" class="dropdown-item" @click="closeUserMenu">仪表盘</router-link>
                <router-link to="/profile" class="dropdown-item" @click="closeUserMenu">个人资料</router-link>
                <router-link to="/settings" class="dropdown-item" @click="closeUserMenu">设置</router-link>
                <div class="dropdown-divider"></div>
                <button class="dropdown-item danger" @click="handleLogout">退出登录</button>
              </div>
            </transition>
          </div>
        </template>
        <template v-else>
          <button class="auth-btn ghost" @click="goLogin">登录</button>
          <button class="auth-btn primary" @click="goRegister">注册</button>
        </template>

        <button class="mobile-menu-btn" @click="toggleMobileMenu">
          <img src="https://unpkg.com/lucide-static@latest/icons/menu.svg" alt="menu" class="menu-icon" />
        </button>
      </div>
    </header>

    <transition name="slide-right">
      <div v-if="showMobileMenu" class="mobile-menu">
        <div class="mobile-header">
          <router-link to="/" class="brand-link">
            <img src="https://unpkg.com/lucide-static@latest/icons/compass.svg" alt="logo" class="brand-icon" />
            <span class="brand-text">AI 旅游</span>
          </router-link>
          <button class="close-mobile-menu" @click="toggleMobileMenu">
            <img src="https://unpkg.com/lucide-static@latest/icons/x.svg" alt="close" class="close-icon" />
          </button>
        </div>

        <nav class="mobile-nav">
          <router-link
            v-for="item in menuItems"
            :key="item.key"
            :to="item.path"
            class="mobile-nav-item"
            :class="{ active: isActiveRoute(item.path) }"
            @click="closeMobileMenu"
          >
            <span>{{ item.label }}</span>
          </router-link>
        </nav>

        <div class="mobile-actions">
          <template v-if="isLoggedIn">
            <div class="mobile-user">
              <img
                :src="loginUserStore.loginUser.userAvatar || 'https://unpkg.com/lucide-static@latest/icons/user-circle.svg'"
                alt="avatar"
                class="mobile-avatar"
              />
              <div>
                <div class="mobile-name">{{ loginUserStore.loginUser.userName || '用户' }}</div>
                <div class="mobile-email">{{ userEmail }}</div>
              </div>
            </div>
            <button class="mobile-btn danger" @click="handleLogout">退出登录</button>
          </template>
          <template v-else>
            <button class="mobile-btn ghost" @click="goLogin">登录</button>
            <button class="mobile-btn primary" @click="goRegister">注册</button>
          </template>
        </div>
      </div>
    </transition>

    <main class="main-content">
      <div class="content-container">
        <router-view />
      </div>
    </main>

    <footer class="layout-footer">
      <div class="footer-content">
        <p>&copy; 2025 AI 旅游推荐平台. All rights reserved.</p>
        <div class="footer-links">
          <a href="#">隐私政策</a>
          <a href="#">服务条款</a>
          <a href="#">帮助中心</a>
        </div>
      </div>
    </footer>

    <transition name="fade">
      <div
        v-if="showUserMenu || showMobileMenu"
        class="overlay"
        @click="closeAllMenus"
      ></div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { userLogout } from '@/api/userController'
import { message } from 'ant-design-vue'

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()

const isScrolled = ref(false)
const showUserMenu = ref(false)
const showMobileMenu = ref(false)
const isMobile = ref(false)
const isLoggedIn = computed(() => Boolean(loginUserStore.loginUser.id))

const userEmail = computed(
  () =>
    loginUserStore.loginUser.userAzccount ||
    (loginUserStore.loginUser as any).userAccount ||
    'user@example.com'
)

const checkMobile = () => {
  isMobile.value = window.innerWidth < 992
}

const handleScroll = () => {
  isScrolled.value = window.scrollY > 12
}

const isActiveRoute = (path: string) => {
  return route.path === path || route.path.startsWith(`${path}/`)
}

const menuItems = computed(() => {
  const userRole = loginUserStore.loginUser.userRole

  const commonItems = [
    { key: 'dashboard', label: '仪表盘', path: '/dashboard' },
    { key: 'ai-planner', label: 'AI行程规划', path: '/ai/planner' },
    { key: 'trips', label: '我的行程', path: '/trips' },
    { key: 'forum', label: '论坛', path: '/user/forum' },
    { key: 'ai', label: 'AI助手', path: '/user/helper' },
    { key: 'message', label: '留言墙', path: '/message-wall' },
  ]

  if (userRole === 'admin') {
    return [
      ...commonItems,
      { key: 'users', label: '用户管理', path: '/admin/userManage' },
      { key: 'merchants', label: '商家管理', path: '/admin/merchantManage' },
      { key: 'products', label: '商品管理', path: '/admin/productManage' },
    ]
  }

  if (userRole === 'merchant') {
    return [
      ...commonItems,
      { key: 'my-products', label: '我的商品', path: '/merchant/products' },
      { key: 'orders', label: '订单管理', path: '/merchant/orders' },
      { key: 'settings', label: '商家设置', path: '/merchant/settings' },
    ]
  }

  return [
    ...commonItems,
    { key: 'shops', label: '商家店铺', path: '/user/shop' },
    { key: 'cart', label: '购物车', path: '/user/cart' },
    { key: 'my-orders', label: '我的订单', path: '/user/orders' },
  ]
})

const goLogin = () => {
  router.push('/user/login')
}

const goRegister = () => {
  router.push('/user/register')
}

const toggleUserMenu = () => {
  showUserMenu.value = !showUserMenu.value
  if (showUserMenu.value) {
    showMobileMenu.value = false
  }
}

const closeUserMenu = () => {
  showUserMenu.value = false
}

const toggleMobileMenu = () => {
  showMobileMenu.value = !showMobileMenu.value
  if (showMobileMenu.value) {
    showUserMenu.value = false
  }
}

const closeMobileMenu = () => {
  showMobileMenu.value = false
}

const closeAllMenus = () => {
  showUserMenu.value = false
  showMobileMenu.value = false
}

const handleLogout = async () => {
  try {
    const res = await userLogout()
    if (res.data.code === 0) {
      loginUserStore.setLoginUser({ userName: '未登录' })
      message.success('退出登录成功')
      await router.push('/user/login')
    } else {
      message.error('退出登录失败，' + res.data.message)
    }
  } catch (error) {
    message.error('退出登录失败')
  }
}

const handleResize = () => {
  checkMobile()
  if (!isMobile.value) {
    showMobileMenu.value = false
  }
}

onMounted(() => {
  checkMobile()
  window.addEventListener('scroll', handleScroll)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('resize', handleResize)
})

watch(
  () => route.path,
  () => {
    closeAllMenus()
  }
)
</script>

<style scoped lang="scss">
.top-nav-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.top-navbar {
  position: sticky;
  top: 0;
  z-index: 1000;
  display: grid;
  grid-template-columns: 240px 1fr auto;
  gap: 16px;
  align-items: center;
  background: #fff;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  padding: 12px 24px;
  transition: box-shadow 0.2s ease, background 0.2s ease;

  &.scrolled {
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.06);
  }

  @media (max-width: 992px) {
    grid-template-columns: 1fr auto;
    grid-template-areas:
      'brand actions'
      'menu menu';
    row-gap: 8px;
  }
}

.navbar-left {
  display: flex;
  align-items: center;
}

.brand-link {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  text-decoration: none;
  color: #111;
  font-weight: 700;
  font-size: 18px;
}

.brand-icon {
  width: 32px;
  height: 32px;
}

.brand-text {
  white-space: nowrap;
}

.navbar-menu {
  display: flex;
  align-items: center;
  gap: 6px;

  @media (max-width: 992px) {
    grid-area: menu;
    flex-wrap: wrap;
  }
}

.menu-item {
  padding: 10px 14px;
  border-radius: 10px;
  color: #4a5568;
  text-decoration: none;
  font-weight: 500;
  transition: background 0.2s ease, color 0.2s ease;
  border: 1px solid transparent;
  background: transparent;
  cursor: pointer;

  &:hover {
    color: #2f54eb;
    background: rgba(47, 84, 235, 0.08);
  }

  &.active {
    color: #2f54eb;
    background: rgba(47, 84, 235, 0.12);
    border-color: rgba(47, 84, 235, 0.2);
  }
}

.navbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
  justify-content: flex-end;
}

.user-menu {
  position: relative;
}

.user-trigger {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  background: #fff;
  cursor: pointer;
  transition: box-shadow 0.2s ease, transform 0.2s ease;

  &:hover {
    box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
    transform: translateY(-1px);
  }
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.user-name {
  font-weight: 600;
  color: #2d3748;
}

.user-dropdown {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  min-width: 220px;
  background: #fff;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 12px;
  box-shadow: 0 12px 30px rgba(0, 0, 0, 0.12);
  padding: 10px;
  z-index: 1001;
}

.dropdown-header {
  margin-bottom: 8px;
}

.dropdown-name {
  font-weight: 700;
  color: #1a202c;
}

.dropdown-email {
  font-size: 13px;
  color: #718096;
}

.dropdown-divider {
  height: 1px;
  background: rgba(0, 0, 0, 0.06);
  margin: 8px 0;
}

.dropdown-item {
  width: 100%;
  text-align: left;
  padding: 10px 8px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: #4a5568;
  font-weight: 500;
  cursor: pointer;
  text-decoration: none;
  display: block;
  transition: background 0.2s ease, color 0.2s ease;

  &:hover {
    background: rgba(47, 84, 235, 0.08);
    color: #2f54eb;
  }

  &.danger {
    color: #e53e3e;

    &:hover {
      background: rgba(229, 62, 62, 0.08);
    }
  }
}

.auth-btn {
  padding: 8px 14px;
  border-radius: 10px;
  border: 1px solid #2f54eb;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.auth-btn.ghost {
  background: #fff;
  color: #2f54eb;
}

.auth-btn.primary {
  background: #2f54eb;
  color: #fff;
  border-color: transparent;
}

.auth-btn:hover {
  box-shadow: 0 8px 18px rgba(47, 84, 235, 0.18);
  transform: translateY(-1px);
}

.mobile-menu-btn {
  display: none;
  width: 38px;
  height: 38px;
  align-items: center;
  justify-content: center;
  border: 1px solid rgba(0, 0, 0, 0.08);
  background: #fff;
  border-radius: 10px;
  cursor: pointer;

  .menu-icon {
    width: 22px;
    height: 22px;
  }

  @media (max-width: 992px) {
    display: inline-flex;
  }
}

.mobile-menu {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  width: 320px;
  max-width: 85%;
  background: #fff;
  box-shadow: -6px 0 24px rgba(0, 0, 0, 0.15);
  z-index: 1002;
  display: flex;
  flex-direction: column;
}

.mobile-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.close-mobile-menu {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: rgba(0, 0, 0, 0.05);
  border-radius: 10px;
  cursor: pointer;

  .close-icon {
    width: 18px;
    height: 18px;
  }
}

.mobile-nav {
  flex: 1;
  padding: 12px 16px;
  overflow-y: auto;
}

.mobile-nav-item {
  display: block;
  padding: 12px 10px;
  border-radius: 10px;
  color: #4a5568;
  text-decoration: none;
  margin-bottom: 6px;
  transition: background 0.2s ease, color 0.2s ease;

  &:hover,
  &.active {
    background: rgba(47, 84, 235, 0.1);
    color: #2f54eb;
  }
}

.mobile-actions {
  padding: 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  display: grid;
  gap: 10px;
}

.mobile-user {
  display: flex;
  align-items: center;
  gap: 10px;
}

.mobile-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
}

.mobile-name {
  font-weight: 700;
  color: #1a202c;
}

.mobile-email {
  font-size: 13px;
  color: #718096;
}

.mobile-btn {
  width: 100%;
  padding: 10px;
  border-radius: 10px;
  border: 1px solid #2f54eb;
  font-weight: 600;
  cursor: pointer;
  background: #fff;
  color: #2f54eb;
  transition: all 0.2s ease;

  &.primary {
    background: #2f54eb;
    color: #fff;
    border-color: transparent;
  }

  &.danger {
    border-color: #e53e3e;
    color: #e53e3e;
  }

  &:hover {
    box-shadow: 0 8px 18px rgba(0, 0, 0, 0.08);
    transform: translateY(-1px);
  }
}

.main-content {
  flex: 1;
  padding: 24px 0;
}

.content-container {
  max-width: 1600px;
  margin: 0 auto;
  padding: 0 24px;
}

.layout-footer {
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  background: #fff;
  padding: 18px 0;
}

.footer-content {
  max-width: 1600px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  color: #718096;
  font-size: 14px;

  a {
    color: #718096;
    text-decoration: none;

    &:hover {
      color: #2f54eb;
    }
  }
}

.overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  z-index: 998;
}

.dropdown-fade-enter-active,
.dropdown-fade-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.dropdown-fade-enter-from,
.dropdown-fade-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

.slide-right-enter-active,
.slide-right-leave-active {
  transition: all 0.25s ease;
}

.slide-right-enter-from,
.slide-right-leave-to {
  transform: translateX(100%);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
