<template>
  <div class="helper-page" :class="{ embedded }">
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

    <div class="quick-actions">
      <button
        v-for="prompt in quickPrompts"
        :key="prompt"
        class="quick-chip"
        type="button"
        @click="handleQuickPrompt(prompt)"
      >
        {{ prompt }}
      </button>
    </div>

    <!-- 聊天内容区 -->
    <ChatWindow
      ref="windowRef"
      :show-itinerary-card="!embedded"
      @update:loading="(v) => (isLoading = v)"
      @itinerary-generated="handleItineraryGenerated"
    />

    <!-- 底部输入框 -->
    <div class="input-wrapper">
      <ChatInput
        :disabled="isLoading"
        placeholder="输入一句需求，AI 会自动生成结构化行程卡片"
        @send-message="onSend"
      />
    </div>

  </div>
</template>

<script lang="ts" setup>
import { shallowRef, ref, computed, onMounted } from 'vue'
import ChatWindow from './ChatWindow.vue'
import ChatInput from './ChatInput.vue'
import type { Conversation } from '@/types/chat'
import type { StructuredItinerary } from '@/types/itinerary'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { getUserConversations, deleteConversation } from '@/api/conversationController'

withDefaults(defineProps<{ embedded?: boolean }>(), {
  embedded: false
})

const emit = defineEmits<{
  (e: 'itinerary-generated', data: StructuredItinerary): void
  (e: 'itinerary-cleared'): void
}>()

// Use shallowRef for primitive values (Vue best practice)
const showHistory = shallowRef(false)
const isLoading = shallowRef(false)
const currentConversationId = shallowRef<string | null>(null)

// Use ref for objects that need deep reactivity
const conversations = ref<Conversation[]>([])
const windowRef = ref<InstanceType<typeof ChatWindow> | null>(null)
const loginUserStore = useLoginUserStore()

const quickPrompts = [
  '帮我生成一个3天2晚的周末行程',
  '按预算帮我规划一个城市自由行路线',
  '给我一份适合情侣出游的结构化行程'
]

// Computed for derived state
const historyButtonClass = computed(() => ({ active: showHistory.value }))

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
    buildAssistantTask(message), 
    currentConversationId.value ?? undefined,
    (newConversationId: string) => {
      // 当创建新对话时，保存 conversationId 以便后续消息使用同一个会话
      console.log('🔄 收到新创建的对话ID，更新当前会话:', newConversationId)
      currentConversationId.value = newConversationId
      // 刷新会话列表以显示新对话
      loadConversations()
    },
    message
  )
}

async function handleSwitch(id: string) {
  console.log('切换会话:', id)
  currentConversationId.value = id
  showHistory.value = false
  emit('itinerary-cleared')

  // 加载选中会话的历史消息
  if (windowRef.value) {
    await windowRef.value.loadConversationHistory(id)
  }
}

function handleQuickPrompt(prompt: string) {
  onSend(prompt)
}

function handleItineraryGenerated(data: StructuredItinerary) {
  emit('itinerary-generated', data)
}

function isItineraryRequest(text: string) {
  return /((帮我|给我|想要|需要|请帮|麻烦).*(规划|安排|制定|设计|生成|做一份|出一份).*(行程|计划|路线|方案))|((生成|规划|安排|制定|设计).*(行程|计划|路线|方案))|((几天|\d+天|\d+日).*(游|旅游|旅行))|(怎么玩)|(行程|路线|方案).*(推荐|建议)|(帮我做|做一份|出一份).*(行程|路线|方案)/.test(text)
}

function buildAssistantTask(message: string) {
  const trimmed = message.trim()
  if (!isItineraryRequest(trimmed)) {
    return trimmed
  }

  return `${trimmed}\n\n【生成要求】如果这是旅行规划请求，请直接输出可保存的结构化行程草稿，字段必须包含 destination、days、budget、theme、dailyPlans、totalEstimatedCost、tips。`
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
  emit('itinerary-cleared')
}
</script>


<style scoped>
.helper-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background: #fff;
}

.helper-page.embedded .chat-header {
  display: none;
}

.chat-header {
  padding: 12px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
  position: relative;
}

.quick-actions {
  width: 100%;
  max-width: 800px;
  margin: 12px auto 0;
  padding: 0 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.quick-chip {
  border: 1px solid var(--color-border);
  background: linear-gradient(135deg, rgba(18, 96, 255, 0.08), rgba(0, 180, 216, 0.08));
  color: var(--color-text);
  border-radius: 999px;
  padding: 8px 14px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.18s ease;
}

.quick-chip:hover {
  border-color: var(--primary-500);
  color: var(--primary-500);
  transform: translateY(-1px);
  box-shadow: 0 6px 18px rgba(18, 96, 255, 0.12);
}

/* 底部输入框容器 */
.input-wrapper {
  max-width: 800px;
  width: 100%;
  margin: 0 auto;
  padding-bottom: 24px;
  flex-shrink: 0;
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
  transition: background-color 0.15s ease;
  flex-shrink: 0;
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
  left: 0;
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

/* 移动端响应式 */
@media (max-width: 768px) {
  .helper-page {
    position: relative;
  }

  .chat-main {
    margin: 0;
    width: 100%;
    max-width: 100%;
  }

  .chat-header {
    padding: 12px 16px;
  }

  .header-title h2 {
    font-size: 15px;
  }

  .history-btn,
  .new-btn {
    width: 32px;
    height: 32px;
  }

  .history-dropdown {
    width: calc(100vw - 32px);
    max-width: 280px;
    left: 0;
  }
}
</style>
