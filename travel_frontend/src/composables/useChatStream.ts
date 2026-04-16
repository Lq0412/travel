import { ref, onBeforeUnmount } from 'vue'
import type { ChatItem } from '@/types/chat'
import type { Activity, DailyPlan, ItineraryResponseMeta, StructuredItinerary } from '@/types/itinerary'
import { createConversationByUserId } from '@/api/chatConversationClient'
import { chat1 } from '@/api/aiController'
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

function ensureArray<T>(value: unknown): T[] {
  return Array.isArray(value) ? (value as T[]) : []
}

function countActivities(itinerary: StructuredItinerary | null): number {
  if (!itinerary) {
    return 0
  }

  const plans = ensureArray<DailyPlan>(itinerary.dailyPlans)
  return plans.reduce((sum, plan) => {
    const activities = ensureArray<Activity>((plan as { activities?: unknown }).activities)
    return sum + activities.length
  }, 0)
}

function positiveNumber(value: unknown): number | undefined {
  const num = Number(value)
  return Number.isFinite(num) && num > 0 ? num : undefined
}

function sanitizeVisibleMessage(text: string): string {
  return removeStructuredDataMarkers(filterAIResponse(text) || text)
}

function parseStructuredFromText(raw: string): StructuredItinerary | null {
  const value = (raw || '').trim()
  if (!value) {
    return null
  }
  try {
    const parsed = JSON.parse(value) as StructuredItinerary
    if (!parsed || typeof parsed !== 'object' || !Array.isArray(parsed.dailyPlans)) {
      return null
    }
    return parsed
  } catch {
    return null
  }
}

function buildMeta(
  source: ItineraryResponseMeta['structuredSource'],
  itinerary: StructuredItinerary | null,
  base?: Partial<ItineraryResponseMeta>,
): ItineraryResponseMeta {
  return {
    intentType: base?.intentType,
    intentDescription: base?.intentDescription,
    structuredAvailable: Boolean(itinerary),
    structuredSource: itinerary ? source : 'none',
    totalEstimatedCost: positiveNumber(itinerary?.totalEstimatedCost),
    activityCount: itinerary ? countActivities(itinerary) : undefined,
  }
}

export function useChatStream() {
  const messages = ref<ChatItem[]>([])
  const isLoading = ref(false)
  const loadingStage = ref<'analysis' | 'generation' | 'structuring'>('analysis')
  const structuredData = ref<StructuredItinerary | null>(null)
  const abortController = ref<AbortController | null>(null)
  const lastStructuredSignature = ref('')
  const responseMeta = ref<ItineraryResponseMeta | null>(null)

  const trySetStructuredData = (
    fullResponseBuffer: string,
    onStructuredData?: (data: StructuredItinerary) => void,
  ) => {
    const structured = extractStructuredData(fullResponseBuffer)
    if (!structured) {
      return false
    }

    const signature = JSON.stringify(structured)
    if (signature === lastStructuredSignature.value) {
      return true
    }
    lastStructuredSignature.value = signature

    loadingStage.value = 'structuring'
    responseMeta.value = buildMeta('stream-marker', structured, {
      intentType: responseMeta.value?.intentType,
      intentDescription: responseMeta.value?.intentDescription,
    })
    structuredData.value = structured
    onStructuredData?.(structured)
    if (import.meta.env.DEV) {
      console.log('[useChatStream] ✅ 已提取结构化行程', {
        destination: structured.destination,
        days: structured.days,
        dailyPlans: structured.dailyPlans?.length ?? 0,
      })
    }
    return true
  }

  const tryFallbackStructuredBySync = async (
    task: string,
    index: number,
    onStructuredData?: (data: StructuredItinerary) => void,
  ): Promise<boolean> => {
    try {
      const response = await chat1({
        message: task,
        stream: false,
      })

      if (response.data.code !== 0 || !response.data.data) {
        return false
      }

      const aiResponse = response.data.data as API.AIResponse
      const metadata = (aiResponse.metadata ?? {}) as Record<string, unknown>
      const intentType = typeof metadata.intentType === 'string' ? metadata.intentType : undefined
      const intentDescription =
        typeof metadata.intentDescription === 'string' ? metadata.intentDescription : undefined

      const structuredAvailable =
        typeof metadata.structuredItineraryAvailable === 'boolean'
          ? metadata.structuredItineraryAvailable
          : false

      const rawStructured =
        typeof metadata.structuredItineraryJson === 'string' ? metadata.structuredItineraryJson : ''
      const parsed = parseStructuredFromText(rawStructured)

      if (!parsed) {
        responseMeta.value = {
          intentType,
          intentDescription,
          structuredAvailable,
          structuredSource: 'none',
        }

        if (!messages.value[index].text.trim() && aiResponse.content) {
          messages.value[index].text = sanitizeVisibleMessage(aiResponse.content)
        }

        return false
      }

      responseMeta.value = buildMeta('sync-metadata', parsed, {
        intentType,
        intentDescription,
      })

      const signature = JSON.stringify(parsed)
      if (signature !== lastStructuredSignature.value) {
        lastStructuredSignature.value = signature
        structuredData.value = parsed
        onStructuredData?.(parsed)
      }

      if (!messages.value[index].text.trim() && aiResponse.content) {
        messages.value[index].text = sanitizeVisibleMessage(aiResponse.content)
      }

      if (import.meta.env.DEV) {
        console.log('[useChatStream] ✅ 已通过同步接口 metadata 回退提取结构化行程', {
          intentType,
          intentDescription,
          destination: parsed.destination,
          days: parsed.days,
        })
      }

      return true
    } catch (error) {
      if (import.meta.env.DEV) {
        console.warn('[useChatStream] 同步 metadata 回退失败', error)
      }
      return false
    }
  }

  /**
   * 处理流结束时的最终渲染
   */
  const handleStreamEnd = async (
    index: number,
    fullResponseBuffer: string,
    task: string,
    onScroll: (smooth?: boolean) => void,
    onStructuredData?: (data: StructuredItinerary) => void
  ) => {
    const hasStructured = trySetStructuredData(fullResponseBuffer, onStructuredData)
    if (import.meta.env.DEV && !hasStructured) {
      console.warn('[useChatStream] ⚠️ 流结束但未提取到结构化行程', {
        length: fullResponseBuffer.length,
        hasStartMarker: fullResponseBuffer.includes('__STRUCTURED_DATA_START__'),
        hasEndMarker: fullResponseBuffer.includes('__STRUCTURED_DATA_END__'),
      })
    }
    if (hasStructured) {
      const cleaned = removeStructuredDataMarkers(messages.value[index].text)
      if (cleaned) messages.value[index].text = cleaned
      onScroll(false)
    } else {
      const resolvedByFallback = await tryFallbackStructuredBySync(task, index, onStructuredData)
      if (!resolvedByFallback && !responseMeta.value) {
        responseMeta.value = {
          structuredAvailable: false,
          structuredSource: 'none',
        }
      }
    }
    if (!messages.value[index].text.trim()) {
      messages.value[index].text = sanitizeVisibleMessage(fullResponseBuffer)
    }
    loadingStage.value = 'analysis'
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
    responseMeta.value = null
    lastStructuredSignature.value = ''
    loadingStage.value = 'analysis'
    isLoading.value = true

    const userText = (conversationTitle || task || '').trim()
    if (userText) {
      messages.value.push({ role: 'user', text: userText, time: new Date() })
    }

    const index = messages.value.length
    messages.value.push({ role: 'ai', text: '', time: new Date() })
    
    let fullResponseBuffer = ''

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
        let currentEvent = 'message'
        let eventDataLines: string[] = []
        let streamFinished = false

        const finishStream = () => {
          if (streamFinished) {
            return
          }
          streamFinished = true
          void handleStreamEnd(index, fullResponseBuffer, task, onScroll, onStructuredData)
        }

        const appendAndRender = (text: string, replaceBuffer = false) => {
          if (!text) {
            return
          }

          if (replaceBuffer) {
            fullResponseBuffer = text
          } else {
            fullResponseBuffer += text
          }

          trySetStructuredData(fullResponseBuffer, onStructuredData)

          const filtered = sanitizeVisibleMessage(fullResponseBuffer)
          const hasDebugText = hasChatDebugMarkers(fullResponseBuffer)
          if (filtered && !lastStructuredSignature.value) {
            loadingStage.value = 'generation'
          }
          messages.value[index].text =
            filtered || (!hasDebugText ? removeStructuredDataMarkers(fullResponseBuffer) : messages.value[index].text)
          onScroll(false)
        }

        const dispatchEvent = (): boolean => {
          const eventType = currentEvent.trim().toLowerCase()
          const payload = eventDataLines.join('\n')
          currentEvent = 'message'
          eventDataLines = []

          if (!eventType && !payload) {
            return false
          }

          if (eventType === 'complete' || eventType === 'end' || eventType === 'done') {
            finishStream()
            return true
          }

          if (eventType === 'error') {
            if (payload) {
              messages.value[index].text = payload
            }
            loadingStage.value = 'analysis'
            isLoading.value = false
            return true
          }

          if (eventType === 'start') {
            return false
          }

          const text = parsePayload(payload)
          if (!text) {
            return false
          }

          if (text === '[DONE]' || text === '流式响应完成' || text === '通用旅行代理执行完成') {
            finishStream()
            return true
          }

          if (eventType === 'result') {
            appendAndRender(text, true)
            return false
          }

          appendAndRender(text)
          return false
        }
        
        const processStream = async () => {
          try {
            while (true) {
              const { done, value } = await reader.read()
              
              if (done) {
                if (eventDataLines.length > 0) {
                  dispatchEvent()
                }
                finishStream()
                break
              }
              
              buffer += decoder.decode(value, { stream: true })
              const lines = buffer.split('\n')
              buffer = lines.pop() || ''
              
              for (const line of lines) {
                const normalizedLine = line.replace(/\r$/, '')

                if (!normalizedLine) {
                  if (dispatchEvent()) {
                    return
                  }
                  continue
                }

                if (normalizedLine.startsWith('event:')) {
                  currentEvent = normalizedLine.substring(6).trim()
                  continue
                }

                if (normalizedLine.startsWith('data:')) {
                  const rawData = normalizedLine.substring(5)
                  eventDataLines.push(rawData.startsWith(' ') ? rawData.slice(1) : rawData)
                }
              }
            }
          } catch (error: unknown) {
            if (!isAbortError(error)) {
              console.error('Stream processing error:', error)
              loadingStage.value = 'analysis'
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
        loadingStage.value = 'analysis'
        isLoading.value = false
      })
  }

  const closeStream = () => {
    abortController.value?.abort()
    abortController.value = null
    loadingStage.value = 'analysis'
    isLoading.value = false
  }

  onBeforeUnmount(() => closeStream())

  return { messages, isLoading, loadingStage, structuredData, responseMeta, startStream, closeStream }
}
