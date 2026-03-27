<template>
  <div class="helper-page" :class="{ embedded }">
    <!-- 聊天卡片 -->
    <div class="chat-main">
      <div class="chat-content">
        <!-- 头部 -->
        <div class="chat-header">
          <button @click="showHistory = !showHistory" class="history-btn" :class="{ active: showHistory }">
            <img src="https://unpkg.com/lucide-static@latest/icons/history.svg" alt="历史" class="icon" />
          </button>
          
          <div class="header-title">
            <h2>AI 旅行助手</h2>
          </div>

          <button @click="handleNew" class="new-btn" title="新对话">
            <img src="https://unpkg.com/lucide-static@latest/icons/plus.svg" alt="新对话" class="icon" />
          </button>

          <!-- 历史下拉面板 -->
          <transition name="dropdown">
            <div v-if="showHistory" class="history-dropdown">
              <div class="history-header">
                <span>对话历史</span>
                <button @click="showHistory = false" class="close-btn">
                  <img src="https://unpkg.com/lucide-static@latest/icons/x.svg" alt="关闭" class="icon" />
                </button>
              </div>
              <div class="history-list">
                <div v-if="conversations.length === 0" class="empty-hint">暂无对话历史</div>
                <div
                  v-for="c in conversations"
                  :key="c.id"
                  class="history-item"
                  :class="{ active: String(c.id) === currentConversationId }"
                  @click="handleSwitch(String(c.id))"
                >
                  <span class="item-title">{{ c.title }}</span>
                  <button class="delete-btn" @click.stop="handleDelete(String(c.id))">
                    <img src="https://unpkg.com/lucide-static@latest/icons/trash-2.svg" alt="删除" class="icon" />
                  </button>
                </div>
              </div>
            </div>
          </transition>
        </div>

        <ChatWindow ref="windowRef" @update:loading="(v) => (isLoading = v)" />

        <ChatInput :disabled="isLoading" placeholder="输入你的问题..." @send-message="onSend" />
      </div>
      
      <!-- 数字人悬浮按钮 -->
      <transition name="scale-fade">
        <div v-if="!embedded && !showDigitalHuman" class="digital-human-fab">
          <button 
            @click="toggleDigitalHuman" 
            class="fab-button"
            title="数字人助手"
          >
            <img src="https://unpkg.com/lucide-static@latest/icons/bot.svg" alt="数字人" class="icon" />
            <span class="fab-ripple"></span>
          </button>
        </div>
      </transition>
    </div>

    <!-- 数字人全屏面板 -->
    <transition name="modal-fade">
      <div v-if="!embedded && showDigitalHuman" class="digital-human-overlay" @click.self="toggleDigitalHuman">
        <div class="digital-human-panel" @click.stop>
          <div class="digital-human-header">
            <div class="header-left">
              <img src="https://unpkg.com/lucide-static@latest/icons/bot.svg" alt="" class="header-icon" />
              <h3>数字人助手</h3>
            </div>
            <button @click="toggleDigitalHuman" class="close-btn" title="关闭">
              <img src="https://unpkg.com/lucide-static@latest/icons/x.svg" alt="关闭" class="icon" />
            </button>
          </div>
          <div class="digital-human-content">
            <DigitalHumanIframe
              src="http://127.0.0.1:8888/static/MiniLive_RealTime.html"
              @loaded="onDigitalHumanLoaded"
            />
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue'
import ChatWindow from './ChatWindow.vue'
import ChatInput from './ChatInput.vue'
import DigitalHumanIframe from '@/components/DigitalHumanIframe.vue'
import type { Conversation } from '@/types/chat'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { getUserConversations, deleteConversation } from '@/api/conversationController'

withDefaults(defineProps<{ embedded?: boolean }>(), {
  embedded: false
})

const showHistory = ref(false)
const isLoading = ref(false)
const conversations = ref<Conversation[]>([])
const currentConversationId = ref<string | null>(null)
const windowRef = ref<InstanceType<typeof ChatWindow> | null>(null)
const loginUserStore = useLoginUserStore()
const showDigitalHuman = ref(false)

// 加载用户会话列表
async function loadConversations() {
  console.log('📞 开始调用 loadConversations 函数')
  try {
    console.log('检查用户登录状态...')
    console.log('loginUserStore.loginUser:', loginUserStore.loginUser)
    console.log('loginUserStore.loginUser.id:', loginUserStore.loginUser.id)

    if (!loginUserStore.loginUser.id) {
      console.warn('⚠️ 用户未登录，无法加载会话列表')
      return
    }

    console.log('✅ 用户已登录，开始加载会话列表，用户ID:', loginUserStore.loginUser.id)
    console.log('📡 调用 getUserConversations API...')

    const response = await getUserConversations({
      userId: loginUserStore.loginUser.id,
      pageNum: 1,
      pageSize: 50
    })

    console.log('📥 会话列表响应:', response.data)
    if ((response.data.code === 0 || response.data.code === 200) && response.data.data) {
      conversations.value = response.data.data.map((conv: any) => ({
        id: String(conv.id),
        title: conv.title || '新对话',
        updateTime: conv.updateTime
      }))
      console.log('✅ 加载会话列表成功，共', conversations.value.length, '个对话')
      console.log('会话列表数据:', conversations.value)
    } else {
      console.error('❌ 加载会话列表失败:', response.data.message)
    }
  } catch (error) {
    console.error('❌ 加载会话列表时发生错误:', error)
  }
}

// 组件挂载时加载会话列表
onMounted(() => {
  console.log('🚀 页面加载，开始获取会话列表...')
  console.log('用户登录状态:', loginUserStore.loginUser)
  console.log('用户ID:', loginUserStore.loginUser.id)
  loadConversations()
})

async function onSend(payload: string | { message: string }) {
  const message = typeof payload === 'string' ? payload : payload.message
  if (!message?.trim()) return
  // 先把用户消息渲染出来
  windowRef.value?.addUserMessage(message)
  // 然后启动流式，如果创建了新对话，更新 currentConversationId
  await windowRef.value?.start(
    message, 
    currentConversationId.value ?? undefined,
    (newConversationId: string) => {
      // 当创建新对话时，保存 conversationId 以便后续消息使用同一个会话
      console.log('🔄 收到新创建的对话ID，更新当前会话:', newConversationId)
      currentConversationId.value = newConversationId
      // 刷新会话列表以显示新对话
      loadConversations()
    }
  )
}

async function handleSwitch(id: string) {
  console.log('切换会话:', id)
  currentConversationId.value = id
  showHistory.value = false

  // 加载选中会话的历史消息
  if (windowRef.value) {
    await windowRef.value.loadConversationHistory(id)
  }
}

async function handleDelete(id: string | number) {
  try {
    if (!loginUserStore.loginUser.id) {
      console.warn('用户未登录，无法删除会话')
      return
    }

    // 保持 conversationId 为字符串格式，避免大整数精度丢失
    const conversationIdStr = String(id)
    const userIdStr = String(loginUserStore.loginUser.id)

    console.log('删除会话:', conversationIdStr)
    console.log('用户ID:', userIdStr)
    console.log('传递的参数:', {
      conversationId: conversationIdStr,
      userId: userIdStr
    })

    const response = await deleteConversation({
      conversationId: conversationIdStr as any, // 保持为字符串，避免精度丢失
      userId: userIdStr as any // 保持为字符串，避免精度丢失
    } as any)

    console.log('删除会话响应:', response.data)

    if (response.data.code === 0 || response.data.code === 200) {
      // 删除成功，刷新会话列表
      await loadConversations()

      // 如果删除的是当前会话，清空当前会话
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

// 切换数字人显示
function toggleDigitalHuman() {
  showDigitalHuman.value = !showDigitalHuman.value
  // 不再强制重新创建组件，保持角色切换状态
  // 缓存问题由 DigitalHumanIframe 组件内部的 URL 参数解决
  console.log('数字人面板状态:', showDigitalHuman.value ? '显示' : '隐藏')
}

// 数字人加载完成
function onDigitalHumanLoaded() {
  console.log('数字人加载完成')
}
</script>


<style scoped>
/* 页面容器 */
.helper-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 128px);
  min-height: calc(100vh - 128px);
  overflow: hidden;
  position: relative;
  background: #ffffff;
}

.helper-page.embedded {
  height: 100%;
  min-height: 720px;
  border-radius: 24px;
  border: 1px solid var(--color-border);
}

@supports (height: 100dvh) {
  .helper-page {
    height: calc(100dvh - 128px);
    min-height: calc(100dvh - 128px);
  }

  .helper-page.embedded {
    height: 100%;
    min-height: 720px;
  }
}

/* 聊天卡片 */
.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  background: white;
  overflow: hidden;
  position: relative;
}

/* 卡片内容区 */
.chat-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

/* 头部 */
.chat-header {
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  background: white;
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
  position: relative;
}

/* 按钮 */
.history-btn,
.new-btn {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 8px;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.history-btn:hover,
.new-btn:hover {
  background: var(--color-bg-muted);
}

.history-btn.active {
  background: var(--color-bg-muted);
}

.history-btn .icon,
.new-btn .icon {
  width: 20px;
  height: 20px;
  filter: brightness(0) saturate(100%) invert(39%) sepia(57%) saturate(2878%) hue-rotate(211deg) brightness(95%) contrast(101%);
}

/* 头部标题区 */
.header-title {
  flex: 1;
  display: flex;
  align-items: center;
}

.header-title h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
}

/* 历史下拉面板 */
.history-dropdown {
  position: absolute;
  top: 100%;
  left: 12px;
  width: 280px;
  max-height: 400px;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  z-index: 100;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.history-header {
  padding: 12px 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--color-border);
  font-weight: 600;
  font-size: 14px;
}

.history-header .close-btn {
  width: 24px;
  height: 24px;
  border: none;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
}

.history-header .close-btn:hover {
  background: var(--color-bg-muted);
}

.history-header .close-btn .icon {
  width: 16px;
  height: 16px;
}

.history-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

.history-list .empty-hint {
  padding: 24px;
  text-align: center;
  color: var(--color-muted);
  font-size: 13px;
}

.history-item {
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.history-item:hover {
  background: var(--color-bg-muted);
}

.history-item.active {
  background: var(--color-bg-muted);
}

.item-title {
  flex: 1;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-item .delete-btn {
  width: 24px;
  height: 24px;
  border: none;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  opacity: 0;
}

.history-item:hover .delete-btn {
  opacity: 1;
}

.history-item .delete-btn:hover {
  background: rgba(239, 68, 68, 0.1);
}

.history-item .delete-btn .icon {
  width: 14px;
  height: 14px;
}

/* 下拉动画 */
.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.2s ease;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* 数字人悬浮按钮 */
.digital-human-fab {
  position: absolute;
  bottom: 100px;
  right: 32px;
  z-index: 1000;
}

.fab-button {
  position: relative;
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: #ffffff;
  border: 1px solid var(--color-border);
  cursor: pointer;
  box-shadow: none;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.15s ease, border-color 0.15s ease, transform 0.15s ease;
  overflow: hidden;
}

.fab-button:hover {
  transform: translateY(-2px);
  background: var(--color-bg-muted);
  border-color: var(--color-border-strong);
}

.fab-button:active {
  transform: translateY(-2px) scale(1);
}

.fab-button .icon {
  width: 28px;
  height: 28px;
  filter: none;
  z-index: 2;
  position: relative;
}

.fab-ripple {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.3);
  animation: ripple 2s ease-out infinite;
}

@keyframes ripple {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  100% {
    transform: scale(1.5);
    opacity: 0;
  }
}

/* 数字人全屏遮罩层 */
.digital-human-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.2);
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
}

/* 数字人面板 */
.digital-human-panel {
  width: 100%;
  max-width: 1400px;
  height: 100%;
  max-height: 90vh;
  background: #ffffff;
  backdrop-filter: none;
  -webkit-backdrop-filter: none;
  border-radius: 12px;
  border: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  box-shadow: none;
  overflow: hidden;
}

.digital-human-header {
  padding: 24px 32px;
  border-bottom: 1px solid var(--color-border);
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #ffffff;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  width: 28px;
  height: 28px;
  filter: none;
}

.digital-human-header h3 {
  margin: 0;
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text);
}

.close-btn {
  background: rgba(239, 68, 68, 0.1);
  border: none;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.close-btn:hover {
  background: rgba(239, 68, 68, 0.2);
  transform: rotate(90deg);
}

.close-btn .icon {
  width: 24px;
  height: 24px;
  filter: brightness(0) saturate(100%) invert(25%) sepia(93%) saturate(4661%) hue-rotate(344deg) brightness(91%) contrast(91%);
}

.digital-human-content {
  flex: 1;
  min-height: 0;
  position: relative;
  background: var(--color-bg-secondary);
}

.digital-human-content > * {
  width: 100%;
  height: 100%;
}

/* 过渡动画 */
.scale-fade-enter-active,
.scale-fade-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.scale-fade-enter-from,
.scale-fade-leave-to {
  opacity: 0;
  transform: scale(0.8);
}

.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.modal-fade-enter-active .digital-human-panel,
.modal-fade-leave-active .digital-human-panel {
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.modal-fade-enter-from {
  opacity: 0;
}

.modal-fade-enter-from .digital-human-panel {
  transform: scale(0.9) translateY(20px);
  opacity: 0;
}

.modal-fade-leave-to {
  opacity: 0;
}

.modal-fade-leave-to .digital-human-panel {
  transform: scale(0.95) translateY(10px);
  opacity: 0;
}

/* 移动端响应式 */
@media (max-width: 768px) {
  .helper-page {
    position: relative;
  }

  .sidebar {
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    z-index: 100;
    box-shadow: 4px 0 24px rgba(0, 0, 0, 0.1);
  }

  .chat-main {
    margin: 0 auto;
    width: calc(100% - 32px);
    border-radius: 12px;
  }

  .chat-main.sidebar-open {
    transform: translateX(280px);
  }

  .chat-header {
    padding: 16px;
  }

  .header-title h2 {
    font-size: 16px;
  }

  .digital-human-fab {
    bottom: 90px;
    right: 20px;
  }

  .fab-button {
    width: 56px;
    height: 56px;
  }

  .digital-human-overlay {
    padding: 0;
  }

  .digital-human-panel {
    max-height: 100vh;
    border-radius: 0;
  }

  .digital-human-header {
    padding: 16px 20px;
  }

  .digital-human-header h3 {
    font-size: 18px;
  }
}
</style>
