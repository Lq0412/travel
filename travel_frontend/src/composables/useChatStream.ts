import { ref, onBeforeUnmount } from 'vue'
import type { ChatItem } from '@/types/chat'
import type { StructuredItinerary } from '@/types/itinerary'
import { createConversationByUserId } from '@/api/chatConversationClient'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import {
  parsePayload,
  filterAIResponse,
  extractStructuredData,
  removeStructuredDataMarkers,
  hasDebugMarkers as hasChatDebugMarkers,
} from '@/utils/chatStreamParser'

function isAbortError(error: unknown): boolean {
  return Boolean(error && typeof error === 'object' && 'name' in error && (error as { name?: string }).name === 'AbortError')
}

export function useChatStream() {
  const messages = ref<ChatItem[]>([])
  const isLoading = ref(false)
  const structuredData = ref<StructuredItinerary | null>(null)
  const abortController = ref<AbortController | null>(null)

  /**
   * 处理流结束时的最终渲染
   */
  const handleStreamEnd = (
    index: number,
    fullResponseBuffer: string,
    onScroll: (smooth?: boolean) => void,
    onStructuredData?: (data: StructuredItinerary) => void
  ) => {
    const structured = extractStructuredData(fullResponseBuffer)
    if (structured) {
      structuredData.value = structured
      onStructuredData?.(structured)
      const cleaned = removeStructuredDataMarkers(messages.value[index].text)
      if (cleaned) messages.value[index].text = cleaned
      onScroll(false)
    }
    if (!messages.value[index].text.trim()) {
      messages.value[index].text = filterAIResponse(fullResponseBuffer) || removeStructuredDataMarkers(fullResponseBuffer) || fullResponseBuffer
    }
    isLoading.value = false
  }

  const startStream = async (
    task: string, 
    conversationId: string | undefined, 
    onScroll: (smooth?: boolean) => void,
    onConversationCreated?: (conversationId: string) => void,
    onStructuredData?: (data: StructuredItinerary) => void,
    conversationTitle?: string
  ) => {
    console.log('📍 startStream 被调用, task:', task.substring(0, 50), 'conversationId:', conversationId)
    abortController.value?.abort()
    structuredData.value = null
    isLoading.value = true

    const index = messages.value.length
    messages.value.push({ role: 'ai', text: '', time: new Date() })
    
    let fullResponseBuffer = ''
    let waitingResult = false

    // 如果没有对话ID，先创建新对话
    let finalConversationId = conversationId
    if (!finalConversationId) {
      try {
        const loginUserStore = useLoginUserStore()
        const userId = loginUserStore.loginUser.id
        if (!userId) {
          isLoading.value = false
          messages.value.splice(index, 1)
          return
        }
        
        const titleSource = conversationTitle?.trim() || task
        const response = await createConversationByUserId({
          userId: String(userId),
          title: titleSource.length > 20 ? titleSource.substring(0, 20) + '...' : titleSource,
          provider: 'dashscope',
          model: 'qwen-turbo'
        })
        
        if ((response.data.code === 0 || response.data.code === 200) && response.data.data) {
          finalConversationId = String(response.data.data.id)
          console.log('✅ 创建对话成功, ID:', finalConversationId)
          onConversationCreated?.(finalConversationId)
        } else {
          isLoading.value = false
          messages.value[index].text = response.data.message || '创建对话失败，请稍后重试'
          return
        }
      } catch (error) {
        console.error('创建对话时发生错误:', error)
        isLoading.value = false
        messages.value[index].text = '创建对话失败，请检查登录状态后重试'
        return
      }
    }

    const url = new URL('/api/ai/tourism/stream', window.location.origin)
    url.searchParams.set('task', task)
    if (finalConversationId) url.searchParams.set('conversationId', String(finalConversationId))
    
    console.log('🚀 准备发起流式请求:', url.toString())

    abortController.value = new AbortController()
    
    fetch(url.toString(), {
      method: 'GET',
      credentials: 'include',
      headers: { 'Accept': 'text/event-stream', 'Cache-Control': 'no-cache' },
      signal: abortController.value.signal
    })
      .then(response => {
        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`)
        if (!response.body) throw new Error('Response body is null')
        
        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''
        
        const processStream = async () => {
          try {
            while (true) {
              const { done, value } = await reader.read()
              
              if (done) {
                handleStreamEnd(index, fullResponseBuffer, onScroll, onStructuredData)
                break
              }
              
              buffer += decoder.decode(value, { stream: true })
              const lines = buffer.split('\n')
              buffer = lines.pop() || ''
              
              for (const line of lines) {
                if (!line.trim()) continue
                
                if (line.startsWith('event:')) {
                  const eventType = line.substring(6).trim()
                  if (eventType === 'result') waitingResult = true
                  if (eventType === 'complete' || eventType === 'end' || eventType === 'done') {
                    handleStreamEnd(index, fullResponseBuffer, onScroll, onStructuredData)
                    return
                  }
                } else if (line.startsWith('data:')) {
                  const data = line.substring(5).trim()
                  const text = parsePayload(data)
                  
                  if (waitingResult) {
                    waitingResult = false
                    fullResponseBuffer = text
                  }
                  
                  if (text === '[DONE]' || text === '流式响应完成') {
                    handleStreamEnd(index, fullResponseBuffer, onScroll, onStructuredData)
                    return
                  }
                  
                  if (text) {
                    fullResponseBuffer += text
                    const filtered = filterAIResponse(fullResponseBuffer)
                    const hasDebugText = hasChatDebugMarkers(fullResponseBuffer)
                    messages.value[index].text = filtered || (!hasDebugText ? removeStructuredDataMarkers(fullResponseBuffer) : messages.value[index].text)
                    onScroll(false)
                  }
                }
              }
            }
          } catch (error: unknown) {
            if (!isAbortError(error)) {
              console.error('Stream processing error:', error)
              isLoading.value = false
            }
          }
        }
        processStream()
      })
      .catch((error: unknown) => {
        if (!isAbortError(error)) {
          console.error('Fetch error:', error)
          messages.value[index].text += '\n\n❌ 连接失败，请检查网络或重新登录'
        }
        isLoading.value = false
      })
  }

  const closeStream = () => {
    abortController.value?.abort()
    abortController.value = null
    isLoading.value = false
  }

  onBeforeUnmount(() => closeStream())

  return { messages, isLoading, structuredData, startStream, closeStream }
}
