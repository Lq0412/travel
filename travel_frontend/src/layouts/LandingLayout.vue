<template>
  <div class="landing-layout">
    <header class="landing-header" :class="{ scrolled: isScrolled }">
      <div class="header-inner">
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

        <nav class="nav-links" v-if="!isMobile">
          <router-link v-for="item in navItems" :key="item.path" :to="item.path">
            {{ item.label }}
          </router-link>
        </nav>

        <div class="header-actions" v-if="!isMobile">
          <template v-if="isLoggedIn">
            <a-button type="primary" @click="goWorkspace">进入工作台</a-button>
            <a-button @click="goProfile">我的</a-button>
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

      <div v-if="showMobileMenu" class="mobile-panel">
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
            <a-button type="primary" block @click="goWorkspace">进入工作台</a-button>
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

    <main class="landing-main">
      <router-view />
    </main>

    <footer class="landing-footer">
      <div class="footer-inner">
        <div>
          <strong>旅刻</strong>
          <p>从灵感到回忆，让每段旅程都值得刻下。</p>
        </div>
        <div class="footer-links">
          <router-link to="/workspace">规划行程</router-link>
          <router-link to="/trips">我的行程</router-link>
          <router-link to="/inspiration">灵感广场</router-link>
        </div>
      </div>
    </footer>
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
]

const isScrolled = ref(false)
const showMobileMenu = ref(false)
const isMobile = ref(false)
const isLoggedIn = computed(() => Boolean(loginUserStore.loginUser.id))

function syncViewport() {
  isMobile.value = window.innerWidth < 980
  if (!isMobile.value) {
    showMobileMenu.value = false
  }
}

function handleScroll() {
  isScrolled.value = window.scrollY > 8
}

function goLogin() {
  router.push('/user/login')
  showMobileMenu.value = false
}

function goRegister() {
  router.push('/user/register')
  showMobileMenu.value = false
}

function goWorkspace() {
  router.push('/workspace')
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
    // ignore network error and clear local state below
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
  window.addEventListener('scroll', handleScroll)
  window.addEventListener('resize', syncViewport)
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
  window.removeEventListener('resize', syncViewport)
})
</script>

<style scoped lang="scss">
.landing-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background:
    linear-gradient(180deg, rgba(59, 110, 220, 0.08) 0%, rgba(244, 247, 251, 0) 20%),
    #f4f7fb;
}

.landing-header {
  position: sticky;
  top: 0;
  z-index: 30;
  padding: 14px 20px;
  transition: background-color 0.2s ease, box-shadow 0.2s ease, backdrop-filter 0.2s ease;

  &.scrolled {
    background: rgba(255, 255, 255, 0.86);
    backdrop-filter: blur(18px);
    box-shadow: 0 12px 30px rgba(18, 52, 97, 0.08);
  }
}

.header-inner {
  max-width: 1320px;
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

.nav-links {
  display: flex;
  justify-content: center;
  gap: 10px;

  a {
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
}

.header-actions {
  display: flex;
  gap: 10px;
}

.mobile-toggle {
  display: none;
  border: 1px solid var(--color-border);
  background: #ffffff;
  border-radius: 999px;
  padding: 10px 14px;
  cursor: pointer;
}

.mobile-panel {
  max-width: 1320px;
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

.landing-main {
  flex: 1;
}

.landing-footer {
  border-top: 1px solid rgba(15, 28, 46, 0.08);
  background: #ffffff;
}

.footer-inner {
  max-width: 1320px;
  margin: 0 auto;
  padding: 24px 20px 36px;
  display: flex;
  justify-content: space-between;
  gap: 20px;

  p {
    margin: 8px 0 0;
    max-width: 560px;
    color: var(--color-muted);
  }
}

.footer-links {
  display: flex;
  gap: 18px;
  flex-wrap: wrap;

  a {
    color: var(--color-text-secondary);
    text-decoration: none;
  }
}

@media (max-width: 980px) {
  .header-inner {
    grid-template-columns: 1fr auto;
  }

  .nav-links,
  .header-actions {
    display: none;
  }

  .mobile-toggle {
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }
}

@media (max-width: 768px) {
  .landing-header {
    padding: 12px;
  }

  .footer-inner {
    flex-direction: column;
  }
}
</style>
