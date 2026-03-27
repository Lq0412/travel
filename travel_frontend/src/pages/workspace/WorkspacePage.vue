<template>
  <div class="workspace-page">
    <!-- 简洁头部 -->
    <header class="page-header">
      <div class="header-content">
        <h1>AI 旅行助手</h1>
        <p>
          <span class="step-badge">1. 输入需求</span>
          <span class="step-arrow">→</span>
          <span class="step-badge">2. 自动出卡</span>
          <span class="step-arrow">→</span>
          <span class="step-badge">3. 保存行程</span>
        </p>
      </div>
      <div class="header-actions">
        <a-button v-if="isLoggedIn" type="link" @click="goPlanner">精修草稿</a-button>
        <a-button v-if="isLoggedIn" type="link" @click="goTrips">我的行程</a-button>
        <a-button v-else type="primary" @click="goLogin">登录</a-button>
      </div>
    </header>

    <!-- 全屏聊天区域 -->
    <section class="chat-section">
      <ChatPage v-if="isLoggedIn" embedded />
      <div v-else class="login-prompt">
        <img :src="illustrations.travelMode" alt="travel illustration">
        <div class="prompt-content">
          <h2>登录后直接出行程草稿</h2>
          <p>登录后即可与 AI 对话，自动生成可保存、可编辑的结构化行程</p>
          <a-button type="primary" size="large" @click="goLogin">前往登录</a-button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getMyTrips } from '@/api/tripController'
import ChatPage from '@/pages/user/ChatPage.vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { useVisualContent } from '@/composables/useVisualContent'

const router = useRouter()
const loginUserStore = useLoginUserStore()
const { illustrations } = useVisualContent()

const trips = ref<API.TripVO[]>([])

const isLoggedIn = computed(() => Boolean(loginUserStore.loginUser.id))

function goLogin() {
  router.push('/user/login')
}

function goPlanner() {
  router.push('/ai/planner')
}

function goTrips() {
  router.push('/trips')
}

async function loadTrips() {
  if (!isLoggedIn.value) {
    trips.value = []
    return
  }
  try {
    const response = await getMyTrips()
    trips.value = response?.data?.data || []
  } catch {
    trips.value = []
  }
}

onMounted(loadTrips)
</script>

<style scoped lang="scss">
.workspace-page {
  width: 100%;
  display: flex;
  flex-direction: column;
  flex: 1;
  min-height: 0;
}

@supports (height: 100dvh) {
  .workspace-page {
    /* Optional since flex now handles height, but can be kept cleanly or removed */
  }
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 0;
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}

.header-content h1 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
}

.header-content p {
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--color-muted);
  display: flex;
  align-items: center;
  gap: 8px;
}

.step-badge {
  display: inline-flex;
  align-items: center;
  background: var(--color-bg-muted);
  color: var(--color-text-secondary);
  padding: 4px 10px;
  border-radius: 999px;
  font-weight: 500;
}

.step-arrow {
  color: var(--color-border-strong);
}

.chat-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 0 0 0;
  overflow: hidden;
  min-height: 0;
}

.chat-section :deep(.helper-page) {
  width: 100%;
  max-width: 768px;
  flex: 1;
  overflow: hidden;
  height: 100%;
  min-height: 0;
}

.login-prompt {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 20px;
  padding: 48px;
  text-align: center;
  width: 100%;
  max-width: 768px;
}

.login-prompt img {
  width: 160px;
  max-width: 100%;
  height: auto;
}

.prompt-content h2 {
  margin: 0 0 6px;
  font-size: 16px;
  color: var(--color-text);
}

.prompt-content p {
  margin: 0 0 16px;
  font-size: 13px;
  color: var(--color-muted);
}
</style>
