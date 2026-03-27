<template>
  <div class="chat-window">
    <div class="messages" ref="listRef">
      <div v-if="!messages.length && !isLoading" class="empty-hint">
        你好，我是你的旅行助手。问我任何与旅行相关的问题吧～
      </div>
      <ChatMessage
        v-for="(item, idx) in messages"
        :key="idx"
        :message="item.text"
        :isUser="item.role === 'user'"
        :timestamp="item.time"
      />
      
      <!-- 结构化行程展示 -->
      <ItineraryCard 
        v-if="structuredData" 
        :itinerary="structuredData"
        @save="handleSaveItinerary"
      />
      
      <div ref="endRef" style="height:1px; width:1px; overflow:hidden; visibility:hidden;" aria-hidden="true"></div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { ref, shallowRef, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import ChatMessage from './ChatMessage.vue'
import ItineraryCard from './ItineraryCard.vue'
import type { StructuredItinerary } from '@/types/itinerary'
import { useChatStream } from '@/composables/useChatStream'
import { useAutoScroll } from '@/composables/useAutoScroll'
import { getConversationMessages } from '@/api/conversationController'
import { useLoginUserStore } from '@/stores/useLoginUserStore'

// no props for now
const emit = defineEmits<{
  (e: 'update:loading', value: boolean): void
  (e: 'itinerary-generated', data: StructuredItinerary): void
}>()

const { messages, isLoading, structuredData, startStream, closeStream } = useChatStream()
const listRef = ref<HTMLElement | null>(null)
const endRef = ref<HTMLElement | null>(null)
const { scrollToBottom } = useAutoScroll(listRef, endRef, { getLength: () => messages.value.length })
const loginUserStore = useLoginUserStore()
const router = useRouter()

// 暴露控制函数给父组件
async function start(
  task: string,
  conversationId?: string,
  onConversationCreated?: (conversationId: string) => void,
  conversationTitle?: string
) {
  // 将回调传递给 startStream，以便在创建新对话时通知父组件
  await startStream(
    task, 
    conversationId, 
    (smooth?: boolean) => scrollToBottom(!!smooth), 
    onConversationCreated,
    (data: StructuredItinerary) => {
      console.log('📋 接收到行程数据:', data)
      emit('itinerary-generated', data)
    },
    conversationTitle
  )
}
function close() {
  closeStream()
}
function addUserMessage(text: string) {
  if (!text?.trim()) return
  messages.value.push({ role: 'user', text, time: new Date() })
  scrollToBottom(true)
}

// 加载会话历史消息
async function loadConversationHistory(conversationId: string) {
  try {
    console.log('📜 开始加载会话历史消息')
    console.log('conversationId (原始):', conversationId, '类型:', typeof conversationId)
    console.log('用户ID (原始):', loginUserStore.loginUser.id, '类型:', typeof loginUserStore.loginUser.id)

    // 重置结构化数据，避免旧行程卡片残留
    structuredData.value = null
    
    const userId = loginUserStore.loginUser.id
    if (!userId) {
      console.error('❌ 用户未登录，无法加载历史消息')
      return
    }

    // 重要：保持 conversationId 和 userId 为字符串格式，避免 JavaScript 数字精度丢失
    // Spring Boot 的 PathVariable 和 RequestParam 会自动将字符串转换为 Long
    // 对于大整数（雪花算法生成的 ID），必须保持字符串格式传递
    const conversationIdStr = String(conversationId)
    const userIdStr = String(userId)
    
    console.log('conversationId (字符串):', conversationIdStr)
    console.log('userId (字符串):', userIdStr)
    
    // 验证字符串不为空
    if (!conversationIdStr || conversationIdStr === 'undefined' || conversationIdStr === 'null') {
      console.error('❌ conversationId 无效:', conversationIdStr)
      return
    }
    if (!userIdStr || userIdStr === 'undefined' || userIdStr === 'null') {
      console.error('❌ userId 无效:', userIdStr)
      return
    }

    // 注意：虽然类型定义是 number，但我们需要传递字符串以避免大整数精度丢失
    // Spring Boot 会自动将 URL 路径和查询参数中的字符串转换为 Long
    const response = await getConversationMessages({
      conversationId: conversationIdStr as any, // 保持为字符串，避免精度丢失
      userId: userIdStr as any // 保持为字符串，避免精度丢失
    } as any)

    console.log('📥 历史消息响应:', response.data)
    console.log('响应码:', response.data.code)
    console.log('响应数据:', response.data.data)
    console.log('响应消息:', response.data.message)

    if ((response.data.code === 0 || response.data.code === 200) && response.data.data) {
      // 清空当前消息
      messages.value = []

      // 加载历史消息
      const messageList = Array.isArray(response.data.data) ? response.data.data : []
      console.log('📝 准备加载', messageList.length, '条历史消息')
      
      messageList.forEach((msg: any) => {
        const content = msg.content || ''
        messages.value.push({
          role: msg.role === 'user' ? 'user' : 'ai',
          text: content,
          time: new Date(msg.createTime || Date.now())
        })
        
        // 检查AI消息中是否包含结构化数据
        if (msg.role === 'assistant' && content.includes('__STRUCTURED_DATA_START__')) {
          console.log('🔍 发现历史消息中的结构化数据')
          extractAndSetStructuredData(content)
        }
      })

      // 滚动到底部
      scrollToBottom(true)
      console.log('✅ 加载历史消息成功，共', messages.value.length, '条消息')
      console.log('消息列表:', messages.value)
    } else {
      console.error('❌ 加载历史消息失败，响应码:', response.data.code, '错误信息:', response.data.message)
      // 即使失败也显示提示
      messages.value = []
    }
  } catch (error: any) {
    console.error('❌ 加载历史消息时发生错误:', error)
    console.error('错误详情:', error.response?.data || error.message)
    // 错误时也清空消息，避免显示错误状态
    messages.value = []
  }
}

// 从文本中提取并设置结构化数据
function extractAndSetStructuredData(text: string) {
  try {
    const startMarker = '__STRUCTURED_DATA_START__'
    const endMarker = '__STRUCTURED_DATA_END__'
    
    const startIndex = text.indexOf(startMarker)
    const endIndex = text.indexOf(endMarker)
    
    if (startIndex !== -1 && endIndex !== -1) {
      const jsonStr = text.substring(startIndex + startMarker.length, endIndex).trim()
      const data = JSON.parse(jsonStr) as StructuredItinerary
      structuredData.value = data
      console.log('✅ 成功从历史消息恢复结构化数据:', data)
      
      // 通知父组件
      emit('itinerary-generated', data)
    }
  } catch (error) {
    console.error('❌ 从历史消息提取结构化数据失败:', error)
  }
}

// 清空消息
function clearMessages() {
  messages.value = []
  structuredData.value = null
}

// 从结构化行程中提取每日亮点
function extractDailyHighlights(itinerary: StructuredItinerary): Record<number, string[]> {
  const highlights: Record<number, string[]> = {}
  
  itinerary.dailyPlans?.forEach(plan => {
    const dayHighlights: string[] = []
    
    // 提取活动亮点
    plan.activities?.forEach(activity => {
      let highlight = ''
      
      // 添加时间
      if (activity.time) {
        highlight += `${activity.time} `
      }
      
      // 添加活动名称和描述
      if (activity.name && activity.description) {
        highlight += `${activity.name}：${activity.description}`
      } else if (activity.name) {
        highlight += activity.name
      } else if (activity.description) {
        highlight += activity.description
      }
      
      if (highlight.trim()) {
        dayHighlights.push(highlight.trim())
      }
    })
    
    // 提取餐饮亮点
    plan.meals?.forEach(meal => {
      let highlight = ''
      
      // 添加时间
      if (meal.time) {
        highlight += `${meal.time} `
      }
      
      // 添加餐饮类型和推荐
      const mealType = meal.type === 'breakfast' ? '早餐' : meal.type === 'lunch' ? '午餐' : '晚餐'
      highlight += `${mealType}：${meal.recommendation}`
      
      if (highlight.trim()) {
        dayHighlights.push(highlight.trim())
      }
    })
    
    // 保存这一天的亮点
    if (dayHighlights.length > 0) {
      highlights[plan.day] = dayHighlights
    }
  })
  
  return highlights
}

// 保存行程
async function handleSaveItinerary(itinerary: StructuredItinerary) {
  try {
    console.log('💾 开始保存行程:', itinerary)
    
    // 动态导入API
    const { saveTrip } = await import('@/api/tripController')
    
    // 提取每日亮点
    const dailyHighlights = extractDailyHighlights(itinerary)
    console.log('📋 提取的每日亮点:', dailyHighlights)
    
    // 构建保存请求
    const saveRequest = {
      destination: itinerary.destination,
      days: itinerary.days,
      budget: itinerary.budget,
      theme: itinerary.theme,
      // 每日亮点（用于自定义调整）
      dailyHighlights: dailyHighlights,
      // 将完整的结构化数据序列化为JSON字符串（用于完整恢复）
      structuredData: JSON.stringify(itinerary)
    }
    
    console.log('📤 发送保存请求:', saveRequest)
    
    const response = await saveTrip(saveRequest as any)
    
    if (response.data.code === 0 && response.data.data) {
      console.log('✅ 保存成功，行程ID:', response.data.data)
      message.success('行程已保存，正在跳转到详情页')
      router.push(`/trips/${response.data.data}`)
    } else {
      console.error('❌ 保存失败:', response.data.message)
      message.error(response.data.message || '保存失败')
    }
  } catch (error: any) {
    console.error('❌ 保存行程时发生错误:', error)
    message.error(error.message || '保存失败')
  }
}

defineExpose({ start, close, addUserMessage, loadConversationHistory, clearMessages, messages, isLoading })

// 同步内部加载态到父组件，保证输入不被错误禁用
watch(isLoading, (v) => emit('update:loading', !!v), { immediate: true })
</script>

<style scoped>
.chat-window {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.messages {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  padding: 16px;
}

.empty-hint {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-muted);
  font-size: 14px;
  text-align: center;
}
</style>
