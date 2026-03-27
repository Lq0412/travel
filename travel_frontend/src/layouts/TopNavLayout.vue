<template>
  <div class="top-layout">
    <header class="top-header" :class="{ scrolled: isScrolled }">
      <div class="top-inner">
        <router-link to="/" class="brand">
          <svg class="brand-logo" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
            <!-- 背景圆 -->
            <circle cx="24" cy="24" r="22" fill="url(#logoGrad)"/>
            <!-- 山峰 -->
            <path d="M12 32L20 20L26 26L36 14" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round" fill="none"/>
            <path d="M8 36H40" stroke="white" stroke-width="2" stroke-linecap="round" opacity="0.6"/>
            <!-- 飞机 -->
            <path d="M32 10L35 13L28 20L26 18L32 10Z" fill="white"/>
            <path d="M26 18L22 22L24 24L28 20L26 18Z" fill="white" opacity="0.8"/>
            <!-- 渐变定义 -->
            <defs>
              <linearGradient id="logoGrad" x1="0" y1="0" x2="48" y2="48">
                <stop offset="0%" stop-color="#38bdf8"/>
                <stop offset="100%" stop-color="#0284c7"/>
              </linearGradient>
            </defs>
          </svg>
          <div>
            <strong>旅刻</strong>
            <small>AI 旅行助手</small>
          </div>
        </router-link>

        <nav class="nav" v-if="!isMobile">
          <router-link
            v-for="item in navItems"
            :key="item.path"
            :to="item.path"
            class="nav-link"
          >
            {{ item.label }}
          </router-link>
        </nav>

        <div class="actions" v-if="!isMobile">
          <template v-if="isLoggedIn">
            <button class="user-chip" type="button" @click="goProfile">
              <img
                :src="loginUserStore.loginUser.userAvatar || 'https://unpkg.com/lucide-static@latest/icons/user-circle.svg'"
                alt="avatar"
              />
              <span>{{ loginUserStore.loginUser.userName || '用户' }}</span>
            </button>
            <a-button @click="handleLogout">退出</a-button>
          </template>
          <template v-else>
            <a-button @click="goLogin">登录</a-button>
            <a-button type="primary" @click="goRegister">注册</a-button>
          </template>
        </div>

        <button class="mobile-toggle" type="button" @click="showMobileMenu = !showMobileMenu">
          {{ showMobileMenu ? '关闭' : '菜单' }}
        </button>
      </div>

      <div v-if="showMobileMenu" class="mobile-menu">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="mobile-link"
          @click="showMobileMenu = false"
        >
          {{ item.label }}
        </router-link>

        <div class="mobile-actions">
          <template v-if="isLoggedIn">
            <a-button block @click="goProfile">我的</a-button>
            <a-button block @click="handleLogout">退出</a-button>
          </template>
          <template v-else>
            <a-button block @click="goLogin">登录</a-button>
            <a-button type="primary" block @click="goRegister">注册</a-button>
          </template>
        </div>
      </div>
    </header>

    <main class="main-shell">
      <div class="main-inner">
        <router-view />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { userLogout } from '@/api/userController'
import { useLoginUserStore } from '@/stores/useLoginUserStore'

const router = useRouter()
const loginUserStore = useLoginUserStore()

const navItems = [
  { label: '首页', path: '/' },
  { label: '规划行程', path: '/workspace' },
  { label: '我的行程', path: '/trips' },
  { label: '灵感广场', path: '/inspiration' },
  { label: '我的', path: '/profile' },
]

const isScrolled = ref(false)
const isMobile = ref(false)
const showMobileMenu = ref(false)
const isLoggedIn = computed(() => Boolean(loginUserStore.loginUser.id))

function syncViewport() {
  isMobile.value = window.innerWidth < 1100
  if (!isMobile.value) {
    showMobileMenu.value = false
  }
}

function handleScroll() {
  isScrolled.value = window.scrollY > 6
}

function goLogin() {
  router.push('/user/login')
  showMobileMenu.value = false
}

function goRegister() {
  router.push('/user/register')
  showMobileMenu.value = false
}

function goProfile() {
  router.push('/profile')
  showMobileMenu.value = false
}

async function handleLogout() {
  try {
    await userLogout()
  } catch {
    // ignore
  }

  loginUserStore.setLoginUser({
    id: undefined,
    userName: '未登录',
    userAvatar: '',
    userAccount: '',
    userRole: undefined as any,
  })
  message.success('已退出登录')
  showMobileMenu.value = false
  router.push('/')
}

onMounted(() => {
  loginUserStore.fetchLoginUser()
  syncViewport()
  handleScroll()
  window.addEventListener('resize', syncViewport)
  window.addEventListener('scroll', handleScroll)
})

onUnmounted(() => {
  window.removeEventListener('resize', syncViewport)
  window.removeEventListener('scroll', handleScroll)
})
</script>

<style scoped lang="scss">
.top-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background:
    radial-gradient(circle at top left, rgba(59, 110, 220, 0.08), transparent 25%),
    linear-gradient(180deg, #f7faff 0%, #f4f7fb 220px, #f4f7fb 100%);
}

.top-header {
  position: sticky;
  top: 0;
  z-index: 30;
  padding: 14px 18px;
  transition: background-color 0.2s ease, box-shadow 0.2s ease, backdrop-filter 0.2s ease;

  &.scrolled {
    background: rgba(255, 255, 255, 0.86);
    backdrop-filter: blur(18px);
    box-shadow: 0 12px 34px rgba(18, 52, 97, 0.08);
  }
}

.top-inner {
  max-width: 1480px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: auto 1fr auto auto;
  gap: 18px;
  align-items: center;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--color-text);
  text-decoration: none;

  strong,
  small {
    display: block;
  }

  small {
    color: var(--color-muted);
  }
}

.brand-logo {
  width: 42px;
  height: 42px;
  flex-shrink: 0;
}

.nav {
  display: flex;
  justify-content: center;
  gap: 10px;
}

.nav-link {
  padding: 10px 14px;
  border-radius: 999px;
  color: var(--color-text-secondary);
  text-decoration: none;
  transition: background-color 0.2s ease, color 0.2s ease;

  &:hover,
  &.router-link-active {
    background: rgba(59, 110, 220, 0.1);
    color: var(--primary-700);
  }
}

.actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  border: 1px solid rgba(15, 28, 46, 0.08);
  background: #ffffff;
  border-radius: 999px;
  padding: 8px 10px 8px 8px;
  cursor: pointer;

  img {
    width: 32px;
    height: 32px;
    border-radius: 50%;
    object-fit: cover;
  }

  span {
    color: var(--color-text);
    font-weight: 600;
  }
}

.mobile-toggle {
  display: none;
  border: 1px solid var(--color-border);
  background: #ffffff;
  border-radius: 999px;
  padding: 10px 14px;
  cursor: pointer;
}

.mobile-menu {
  max-width: 1480px;
  margin: 12px auto 0;
  padding: 16px;
  border-radius: 20px;
  background: #ffffff;
  border: 1px solid var(--color-border);
  box-shadow: 0 18px 44px rgba(18, 52, 97, 0.08);
}

.mobile-link {
  display: block;
  padding: 12px 0;
  color: var(--color-text);
  text-decoration: none;
  border-bottom: 1px solid rgba(15, 28, 46, 0.06);
}

.mobile-actions {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.main-shell {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 14px 18px 28px;
}

.main-inner {
  max-width: 1480px;
  margin: 0 auto;
  width: 100%;
  flex: 1;
  display: flex;
  flex-direction: column;
}

@media (max-width: 1100px) {
  .top-inner {
    grid-template-columns: 1fr auto;
  }

  .nav,
  .actions {
    display: none;
  }

  .mobile-toggle {
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }
}

@media (max-width: 768px) {
  .top-header,
  .main-shell {
    padding-left: 12px;
    padding-right: 12px;
  }
}
</style>
