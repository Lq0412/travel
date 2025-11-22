<template>
  <div class="chat-page-with-digital-human">
    <!-- 侧边栏 -->
    <ChatSidebar
      v-if="showSidebar"
      :conversations="conversations"
      :activeId="currentConversationId"
      @close="showSidebar = false"
      @switch="handleSwitch"
      @delete="handleDelete"
      @new="handleNew"
      class="sidebar"
    />

    <!-- 主内容区域 -->
    <div class="main-container" :class="{ 'sidebar-open': showSidebar }">
      <!-- 左侧：聊天窗口 -->
      <div class="chat-section">
        <div class="chat-header">
          <button @click="showSidebar = !showSidebar" class="menu-btn">☰</button>
          <h2>旅行助手</h2>
          <button @click="handleNew" class="new-chat-btn">新对话</button>
        </div>

        <ChatWindow ref="windowRef" @update:loading="(v) => (isLoading = v)" />

        <ChatInput :disabled="isLoading" placeholder="随便问我什么..." @send-message="onSend" />
      </div>

      <!-- 右侧：数字人 -->
      <div class="digital-human-section">
        <div class="digital-human-header">
          <h3>数字人助手</h3>
          <button @click="toggleDigitalHumanMode" class="toggle-btn">
            {{ useIframe ? '切换完整模式' : '切换简化模式' }}
          </button>
        </div>
        
        <!-- 根据模式选择不同的组件 -->
        <DigitalHumanIframe 
          v-if="useIframe"
          :src="digitalHumanUrl"
          @loaded="onDigitalHumanLoaded"
        />
        <DigitalHuman
          v-else
          :baseUrl="digitalHumanBaseUrl"
          :showDialog="false"
          :showControls="true"
          :initialCharacter="selectedCharacter"
          :initialVoice="selectedVoice"
          @loaded="onDigitalHumanLoaded"
          @error="onDigitalHumanError"
        />
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue'
import ChatSidebar from './ChatSidebar.vue'
import ChatWindow from './ChatWindow.vue'
import ChatInput from './ChatInput.vue'
import DigitalHuman from '@/components/DigitalHuman.vue'
import DigitalHumanIframe from '@/components/DigitalHumanIframe.vue'
import type { Conversation } from '@/types/chat'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { getUserConversations, deleteConversation } from '@/api/conversationController'

const showSidebar = ref(false)
const isLoading = ref(false)
const conversations = ref<Conversation[]>([])
const currentConversationId = ref<string | null>(null)
const windowRef = ref<InstanceType<typeof ChatWindow> | null>(null)
const loginUserStore = useLoginUserStore()

// 数字人相关配置
const useIframe = ref(true) // 默认使用 iframe 模式（更简单）
const digitalHumanUrl = ref(`http://127.0.0.1:8888/static/MiniLive_RealTime.html?v=${Date.now()}`)
const digitalHumanBaseUrl = ref('http://127.0.0.1:8888/static')
const selectedCharacter = ref<'assets' | 'assets2' | 'assets3'>('assets')
const selectedVoice = ref('0')

// 切换数字人模式
function toggleDigitalHumanMode() {
  useIframe.value = !useIframe.value
  console.log('切换数字人模式:', useIframe.value ? 'iframe模式' : '完整模式')
}

// 数字人加载完成
function onDigitalHumanLoaded() {
  console.log('数字人加载完成')
}

// 数字人加载错误
function onDigitalHumanError(error: Error) {
  console.error('数字人加载失败:', error)
  // 可以显示错误提示给用户
}

// 加载用户会话列表
async function loadConversations() {
  console.log('📞 开始调用 loadConversations 函数')
  try {
    if (!loginUserStore.loginUser.id) {
      console.warn('⚠️ 用户未登录，无法加载会话列表')
      return
    }

    const response = await getUserConversations({
      userId: loginUserStore.loginUser.id,
      pageNum: 1,
      pageSize: 50
    })

    if ((response.data.code === 0 || response.data.code === 200) && response.data.data) {
      conversations.value = response.data.data.map((conv: any) => ({
        id: String(conv.id),
        title: conv.title || '新对话',
        updateTime: conv.updateTime
      }))
      console.log('✅ 加载会话列表成功，共', conversations.value.length, '个对话')
    } else {
      console.error('❌ 加载会话列表失败:', response.data.message)
    }
  } catch (error) {
    console.error('❌ 加载会话列表时发生错误:', error)
  }
}

// 组件挂载时加载会话列表
onMounted(() => {
  loadConversations()
})

async function onSend(payload: string | { message: string }) {
  const message = typeof payload === 'string' ? payload : payload.message
  if (!message?.trim()) return
  windowRef.value?.addUserMessage(message)
  await windowRef.value?.start(
    message,
    currentConversationId.value ?? undefined,
    (newConversationId: string) => {
      console.log('🔄 收到新创建的对话ID，更新当前会话:', newConversationId)
      currentConversationId.value = newConversationId
      loadConversations()
    }
  )
}

async function handleSwitch(id: string) {
  console.log('切换会话:', id)
  currentConversationId.value = id
  showSidebar.value = false
  if (windowRef.value) {
    await windowRef.value.loadConversationHistory(id)
  }
}

async function handleDelete(id: number) {
  try {
    if (!loginUserStore.loginUser.id) {
      console.warn('用户未登录，无法删除会话')
      return
    }

    const response = await deleteConversation({
      conversationId: id,
      userId: loginUserStore.loginUser.id
    })

    if (response.data.code === 0 || response.data.code === 200) {
      await loadConversations()
      if (String(id) === currentConversationId.value) {
        currentConversationId.value = null
        if (windowRef.value) {
          windowRef.value.clearMessages()
        }
      }
      console.log('✅ 删除会话成功')
    } else {
      console.error('❌ 删除会话失败:', response.data.message)
    }
  } catch (error) {
    console.error('❌ 删除会话时发生错误:', error)
  }
}

async function handleNew() {
  console.log('创建新对话')
  currentConversationId.value = null
  if (windowRef.value) {
    windowRef.value.clearMessages()
  }
}
</script>

<style scoped>
.chat-page-with-digital-human {
  display: flex;
  flex-direction: row;
  height: 100vh;
  min-height: 0;
  overflow: hidden;
  position: relative;
}

.main-container {
  flex: 1;
  display: flex;
  flex-direction: row;
  min-height: 0;
  transition: transform 0.3s ease;
}

.main-container.sidebar-open {
  transform: translateX(0);
}

.chat-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-right: 1px solid var(--color-border);
}

.digital-human-section {
  width: 400px;
  min-width: 400px;
  display: flex;
  flex-direction: column;
  background-color: var(--color-bg-secondary);
  border-left: 1px solid var(--color-border);
}

.digital-human-header {
  padding: 16px;
  border-bottom: 1px solid var(--color-border);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: var(--color-bg-secondary);
}

.digital-human-header h3 {
  margin: 0;
  font-size: 16px;
  color: var(--color-text);
}

.toggle-btn {
  background: var(--primary-500);
  color: #fff;
  border: none;
  padding: 6px 12px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
  transition: background-color 0.2s;
}

.toggle-btn:hover {
  background: var(--primary-600);
}

.digital-human-section > *:last-child {
  flex: 1;
  min-height: 0;
}

.chat-header {
  padding: 16px;
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  gap: 16px;
  background-color: var(--color-bg-secondary);
  z-index: 5;
}

.menu-btn,
.new-chat-btn {
  background: none;
  border: 1px solid var(--color-border);
  padding: 8px 12px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s;
}

.menu-btn:hover,
.new-chat-btn:hover {
  background-color: var(--color-bg-secondary);
}

.chat-header h2 {
  margin: 0;
  flex: 1;
  font-size: 18px;
  color: var(--color-text);
}

.sidebar {
  position: relative;
  z-index: 10;
}

/* 移动端响应式 */
@media (max-width: 1024px) {
  .main-container {
    flex-direction: column;
  }

  .digital-human-section {
    width: 100%;
    min-width: 0;
    height: 40vh;
    border-left: none;
    border-top: 1px solid var(--color-border);
  }

  .chat-section {
    flex: 1;
  }
}

@media (max-width: 768px) {
  .sidebar {
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 100;
    box-shadow: 2px 0 10px rgba(0, 0, 0, 0.1);
  }

  .main-container.sidebar-open {
    transform: translateX(300px);
  }

  .digital-human-section {
    height: 30vh;
  }
}
</style>

