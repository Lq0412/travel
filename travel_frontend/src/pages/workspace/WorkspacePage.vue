<template>
  <div class="workspace-page">
    <header class="page-header">
      <div class="header-actions">
        <a-dropdown
          v-if="isLoggedIn && conversations.length > 0"
          placement="bottomRight"
          :trigger="['click']"
          :overlayStyle="{ maxHeight: '400px', overflowY: 'auto' }"
        >
          <a-button type="link" class="conv-trigger-btn">
            {{ currentConversationTitle }}
            <DownOutlined style="font-size: 10px; margin-left: 4px" />
          </a-button>
          <template #overlay>
            <a-menu
              class="workspace-conv-menu"
              :selectedKeys="conversationId ? [conversationId] : []"
            >
              <a-menu-item
                key="new-conversation"
                @click="resetConversation"
                style="padding: 10px 16px"
              >
                <strong style="color: #3b6edc"
                  ><PlusOutlined style="margin-right: 4px" /> 新会话</strong
                >
              </a-menu-item>
              <a-menu-divider />
              <a-menu-item
                v-for="conv in conversations"
                :key="String(conv.id)"
                @click="selectConversation(conv)"
                style="padding: 10px 16px; min-width: 220px"
              >
                <div class="conv-menu-item" style="display: flex; flex-direction: column; gap: 4px">
                  <div
                    class="conv-title"
                    :style="{
                      fontSize: '14px',
                      color: String(conv.id) === conversationId ? '#3b6edc' : '#333',
                      fontWeight: String(conv.id) === conversationId ? 600 : 400,
                    }"
                  >
                    {{ summarizeConversation(conv).title }}
                  </div>
                  <div class="conv-time" style="font-size: 12px; color: #999">
                    {{ summarizeConversation(conv).timeLabel }}
                  </div>
                </div>
              </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
        <a-button v-else-if="isLoggedIn" type="link" @click="resetConversation">新会话</a-button>

        <a-button
          v-if="isLoggedIn && currentItinerary"
          type="primary"
          :loading="isSavingTrip"
          :disabled="isLoading"
          @click="saveCurrentItinerary"
        >
          保存行程
        </a-button>
        <a-button v-if="isLoggedIn" type="default" @click="toggleMapVisible">
          {{ mapVisible ? '隐藏地图' : '显示地图' }}
        </a-button>
        <a-button v-if="isLoggedIn" type="link" @click="goTrips">我的行程</a-button>
        <a-button v-else type="primary" @click="goLogin">登录</a-button>
      </div>
    </header>

    <section v-if="isLoggedIn" class="planner-stage">
      <div class="stage-content" :class="{ 'map-hidden': !mapVisible }" v-show="currentItinerary">
        <div class="timeline-panel" :class="{ 'timeline-panel-full': !mapVisible }">
          <ItineraryTimelineBoard
            :itinerary="currentItinerary"
            :meta="responseMeta"
            :diff="itineraryDiff"
          />
        </div>
        <div v-if="mapVisible" class="map-panel">
          <DynamicMap :itinerary="currentItinerary" />
        </div>
      </div>

      <div
        class="chat-history-panel"
        :class="{ 'is-collapsed': chatPanelCollapsed, 'is-center-mode': !currentItinerary }"
        v-if="chatMessages.length > 0 || !currentItinerary"
      >
        <div class="chat-history-header" v-if="currentItinerary">
          <span class="chat-history-title">对话记录</span>
          <button type="button" class="chat-history-toggle" @click="chatPanelCollapsed = !chatPanelCollapsed">
            {{ chatPanelCollapsed ? '展开' : '收起' }}
          </button>
        </div>
        <div class="chat-history-list" v-show="!chatPanelCollapsed || !currentItinerary">
          <div v-if="chatMessages.length === 0 && !currentItinerary" class="chat-welcome-message text-center py-6" style="margin-top: 40px; padding: 40px 0;">
            <h3 style="font-size: 24px; font-weight: 600; color: #1e293b; margin-bottom: 12px; text-align: center;">你好！我是你的智能旅行规划师 👋</h3>
            <p style="color: #64748b; font-size: 15px; margin: 0; text-align: center; line-height: 1.6;">告诉我你的出行想法，比如去哪里、和谁一起、行程大约几天？<br/>我将为你量身定制专属行程。</p>
          </div>
          <div
            v-for="(item, index) in chatMessages"
            :key="`${item.role}-${index}-${item.time.getTime()}`"
            class="chat-message"
            :class="[
              item.role === 'user' ? 'is-user' : 'is-ai',
              { 'has-recommendation-cards': Boolean(item.recommendationCards) },
            ]"
          >
            <div class="chat-message-meta">
              <span>{{ item.role === 'user' ? '我' : '旅行助手' }}</span>
              <span>{{ formatChatTime(item.time) }}</span>
            </div>
            <div v-if="item.text" class="chat-message-text">
              {{ getChatMessageDisplayText(item, index) }}
            </div>
            <ChatRecommendationCards
              v-if="item.recommendationCards"
              :title="item.recommendationCards.title"
              :products="item.recommendationCards.products"
              @purchase="handleProductPurchase"
            />
          </div>
        </div>
      </div>

      <div class="bottom-chat">
        <RecentTripPromptCards
          :trips="recentTrips"
          :loading="recentTripsLoading"
          :disabled="isLoading"
          @select="handleRecentTripSelect"
        />
        <ChatInput
          :disabled="isLoading"
          placeholder="告诉我你的旅行计划，比如：成都 3 天美食游，预算 2000 元"
          @send-message="handleSendMessage"
        />
      </div>
    </section>

    <section v-else class="empty-stage">
      <div class="login-prompt">
        <img :src="illustrations.travelMode" alt="travel illustration" />
        <div class="prompt-content">
          <h2>登录后开始规划行程</h2>
          <p>告诉 AI 你想去哪、玩几天、预算多少，3 分钟生成专属旅行方案。</p>
          <a-button type="primary" size="large" @click="goLogin">立即登录</a-button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { DownOutlined, PlusOutlined } from '@ant-design/icons-vue'
import ChatInput from '@/pages/user/ChatInput.vue'
import ItineraryTimelineBoard from '@/pages/workspace/ItineraryTimelineBoard.vue'
import DynamicMap from '@/pages/workspace/DynamicMap.vue'
import RecentTripPromptCards from '@/pages/workspace/components/RecentTripPromptCards.vue'
import ChatRecommendationCards from '@/pages/workspace/components/ChatRecommendationCards.vue'
import { getMyTrips, saveTrip } from '@/api/tripController'
import type {
  Activity,
  DailyPlan,
  ItineraryDayDiffStat,
  ItineraryDiffSummary,
  StructuredItinerary,
} from '@/types/itinerary'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { useWorkspaceConversations } from '@/composables/useWorkspaceConversations'
import { useVisualContent } from '@/composables/useVisualContent'
import { useChatStream } from '@/composables/useChatStream'
import { getConversationMessagesById } from '@/api/chatConversationClient'
import {
  filterAIResponse,
  removeStructuredDataMarkers,
} from '@/utils/chatStreamParser'
import {
  buildChatMessageDisplayText,
  buildConversationTitle,
  type ChatLoadingStage,
  restoreStructuredItineraryFromHistory,
  summarizeConversation,
} from '@/utils/workspaceSession'
import { useRecentTripRecommendations } from '@/composables/useRecentTripRecommendations'
import type { Product } from '@/types/product'
import type { ChatRecommendationCards as ChatRecommendationCardsPayload } from '@/types/chat'
import { attachRecommendationsToLatestAIMessage } from '@/utils/chatRecommendations'
import {
  buildRecentTripConversationLabel,
  buildRecentTripRecommendationPrompt,
} from '@/utils/recentTripPrompt'
import { shouldRestoreRecentTripItinerary } from '@/utils/tripWorkflow'

type DayPeriod = 'morning' | 'noon' | 'evening'
type ActivityType = 'attraction' | 'transport' | 'rest' | 'meal'

const router = useRouter()
const route = useRoute()
const loginUserStore = useLoginUserStore()
const { illustrations, fetchFirst } = useVisualContent()

const currentItinerary = ref<StructuredItinerary | null>(null)
const conversationId = ref<string | undefined>(undefined)
const currentConversationTitle = ref('新会话')
const lastGeneratedAt = ref('')
const isSavingTrip = ref(false)
const enrichmentVersion = ref(0)
const lastItinerarySignature = ref('')
const itineraryDiff = ref<ItineraryDiffSummary | null>(null)
const itineraryDiffRound = ref(0)
const imageQueryCache = new Map<string, string>()
const structuredConversationCache = new Map<string, StructuredItinerary>()
const mapVisible = ref(false)
const chatPanelCollapsed = ref(false)
const recentTrips = ref<API.TripVO[]>([])
const recentTripsLoading = ref(false)
const pendingRecommendationCards = ref<ChatRecommendationCardsPayload | null>(null)
const ALLOWED_ACTIVITY_TYPES = new Set<ActivityType>(['attraction', 'transport', 'rest', 'meal'])

const isLoggedIn = computed(() => Boolean(loginUserStore.loginUser.id))
const {
  messages: chatMessages,
  isLoading,
  loadingStage,
  startStream,
  closeStream,
  structuredData,
  responseMeta,
} = useChatStream()
const {
  conversations,
  loading: conversationsLoading,
  refreshConversations,
  upsertConversation,
} = useWorkspaceConversations(() => loginUserStore.loginUser.id)
const {
  recommendationTitle,
  selectTrip: selectRecentTripRecommendation,
  clearRecommendations,
} = useRecentTripRecommendations()

function getChatMessageDisplayText(item: { role: 'user' | 'ai'; text: string }, index: number) {
  return buildChatMessageDisplayText(item, {
    isLatest: index === chatMessages.value.length - 1,
    isLoading: isLoading.value,
    loadingStage: loadingStage.value as ChatLoadingStage,
  })
}

function finalizePendingRecommendationCards() {
  if (!pendingRecommendationCards.value) {
    return
  }

  chatMessages.value = attachRecommendationsToLatestAIMessage(
    chatMessages.value,
    pendingRecommendationCards.value,
  )
  pendingRecommendationCards.value = null
}

watch(structuredData, (data) => {
  if (!data) {
    return
  }

  applyStructuredItinerary(data)
})

function applyStructuredItinerary(data: StructuredItinerary) {
  const signature = JSON.stringify(data)
  if (signature === lastItinerarySignature.value) {
    return
  }
  lastItinerarySignature.value = signature

  if (import.meta.env.DEV) {
    const activityCount = ensureArray<DailyPlan>(data.dailyPlans).reduce((total, plan) => {
      const value = plan as { activities?: unknown }
      return total + ensureArray<Activity>(value.activities).length
    }, 0)
    console.log('[WorkspacePage] 接收到结构化行程', {
      destination: data.destination,
      days: data.days,
      dailyPlans: data.dailyPlans?.length ?? 0,
      activities: activityCount,
    })
  }

  const normalized = normalizeIncomingItinerary(data)
  const previous = currentItinerary.value
  if (!previous) {
    itineraryDiff.value = null
    itineraryDiffRound.value = 0
  } else {
    itineraryDiffRound.value += 1
    itineraryDiff.value = buildItineraryDiff(previous, normalized, itineraryDiffRound.value)
  }

  currentItinerary.value = normalized
  if (conversationId.value) {
    structuredConversationCache.set(conversationId.value, normalized)
  }
  lastGeneratedAt.value = new Date().toISOString()

  const currentVersion = ++enrichmentVersion.value
  void enrichItineraryImages(normalized).then((enriched) => {
    if (enrichmentVersion.value === currentVersion) {
      currentItinerary.value = enriched
    }
  })
}

function goLogin() {
  router.push('/user/login')
}

function goTrips() {
  router.push('/trips')
}

async function loadRecentTrips() {
  if (!isLoggedIn.value) {
    recentTrips.value = []
    return
  }

  recentTripsLoading.value = true
  try {
    const response = await getMyTrips()
    recentTrips.value = [...(response.data?.data || [])]
      .sort((left, right) => {
        const leftTime = new Date(left.updateTime || left.createTime || 0).getTime()
        const rightTime = new Date(right.updateTime || right.createTime || 0).getTime()
        return rightTime - leftTime
      })
      .slice(0, 3)
  } catch (error) {
    if (import.meta.env.DEV) {
      console.warn('[WorkspacePage] 加载最近行程失败', error)
    }
  } finally {
    recentTripsLoading.value = false
  }
}

function toggleMapVisible() {
  mapVisible.value = !mapVisible.value
}

async function handleRecentTripSelect(trip: API.TripVO) {
  resetConversation()

  if (!shouldRestoreRecentTripItinerary('recommend-products')) {
    currentItinerary.value = null
  }

  const products = await selectRecentTripRecommendation(trip)
  pendingRecommendationCards.value = {
    title: recommendationTitle.value,
    trip,
    products: [...products],
  }

  await handleSendMessage(await buildRecentTripRecommendationPrompt(trip, products), {
    displayText: buildRecentTripConversationLabel(trip),
  })
}

function resetConversation() {
  enrichmentVersion.value += 1
  lastItinerarySignature.value = ''
  conversationId.value = undefined
  currentConversationTitle.value = '新会话'
  lastGeneratedAt.value = ''
  currentItinerary.value = null
  itineraryDiff.value = null
  itineraryDiffRound.value = 0
  responseMeta.value = null
  chatMessages.value = []
  pendingRecommendationCards.value = null
  clearRecommendations()
}

async function selectConversation(conversation: API.AIConversationVO) {
  if (!conversation.id) {
    return
  }

  conversationId.value = String(conversation.id)
  currentConversationTitle.value = conversation.title?.trim() || '新会话'
  lastGeneratedAt.value = conversation.updateTime || conversation.createTime || ''

  // 清理当前状态，避免展示旧的行程卡片
  enrichmentVersion.value += 1
  lastItinerarySignature.value = ''
  currentItinerary.value = null
  itineraryDiff.value = null
  itineraryDiffRound.value = 0
  responseMeta.value = null
  chatMessages.value = []
  pendingRecommendationCards.value = null
  clearRecommendations()

  try {
    const userId = loginUserStore.loginUser.id
    if (!userId) return

    const resp = await getConversationMessagesById(conversation.id, userId)
    if (resp?.data?.code === 0 && resp.data.data) {
      const historyMessages = resp.data.data
      chatMessages.value = historyMessages
        .filter((msg: any) => msg?.role === 'user' || msg?.role === 'assistant' || msg?.role === 'ai')
        .map((msg: any) => {
          const role = msg.role === 'user' ? 'user' : 'ai'
          const rawContent = typeof msg.content === 'string' ? msg.content : ''
          const cleanedContent =
            role === 'ai'
              ? filterAIResponse(rawContent) || removeStructuredDataMarkers(rawContent) || rawContent
              : rawContent

          return {
            role,
            text: cleanedContent,
            time: msg.createTime ? new Date(msg.createTime) : new Date(),
          }
        })

      const restoredItinerary = restoreStructuredItineraryFromHistory(
        historyMessages,
        structuredConversationCache.get(conversationId.value) ?? null,
      )
      if (restoredItinerary) {
        applyStructuredItinerary(restoredItinerary)
        message.success('已恢复会话行程上下文')
        return
      }
    }
  } catch (error) {
    console.error('加载会话历史内容失败', error)
  }

  message.info('已切换会话上下文，继续输入即可沿用这条会话')
}

function isUsableImageUrl(rawUrl: string): boolean {
  const value = (rawUrl || '').trim()
  if (!value) {
    return false
  }

  if (!/^https?:\/\//i.test(value)) {
    return false
  }

  try {
    const url = new URL(value)
    const host = url.hostname.toLowerCase()
    if (!host) {
      return false
    }

    if (host === 'example.com' || host.endsWith('.example.com')) {
      return false
    }

    return true
  } catch {
    return false
  }
}

function normalizePeriod(raw: string): DayPeriod {
  const value = (raw || '').trim().toLowerCase()

  if (!value) {
    return 'morning'
  }
  if (value.includes('morning') || value.includes('早') || value.includes('上午')) {
    return 'morning'
  }
  if (
    value.includes('noon') ||
    value.includes('afternoon') ||
    value.includes('中') ||
    value.includes('午')
  ) {
    return 'noon'
  }
  if (value.includes('evening') || value.includes('night') || value.includes('晚')) {
    return 'evening'
  }

  const match = value.match(/(\d{1,2}):(\d{2})/)
  if (match) {
    const hour = Number(match[1])
    if (hour < 11) return 'morning'
    if (hour < 17) return 'noon'
    return 'evening'
  }

  return 'morning'
}

function normalizeActivityType(raw: string): ActivityType {
  const value = (raw || '').trim().toLowerCase() as ActivityType
  if (ALLOWED_ACTIVITY_TYPES.has(value)) {
    return value
  }
  return 'attraction'
}

function periodWeight(period: DayPeriod): number {
  if (period === 'morning') return 1
  if (period === 'noon') return 2
  return 3
}

function ensureArray<T>(value: unknown): T[] {
  return Array.isArray(value) ? (value as T[]) : []
}

interface DiffNodeSnapshot {
  nodeKey: string
  day: number
  signature: string
}

function activityNodeBaseKey(day: number, activity: Activity): string {
  const time = (activity.time || '').trim().toLowerCase()
  const name = (activity.name || '').trim().toLowerCase()
  return `${day}|${time}|${name}`
}

function activitySignature(activity: Activity): string {
  const type = (activity.type || '').trim().toLowerCase()
  const description = (activity.description || '').trim()
  const address = (activity.location?.address || '').trim()
  const estimatedCost = Number(activity.estimatedCost || 0)
  return `${type}@@${description}@@${address}@@${estimatedCost}`
}

function flattenDiffNodes(itinerary: StructuredItinerary): DiffNodeSnapshot[] {
  const nodes: DiffNodeSnapshot[] = []
  const occurrenceMap = new Map<string, number>()

  for (const plan of ensureArray<DailyPlan>(itinerary.dailyPlans)) {
    const activities = ensureArray<Activity>((plan as { activities?: unknown }).activities)
    for (const activity of activities) {
      const baseKey = activityNodeBaseKey(plan.day, activity)
      const count = (occurrenceMap.get(baseKey) || 0) + 1
      occurrenceMap.set(baseKey, count)

      nodes.push({
        nodeKey: `${baseKey}#${count}`,
        day: plan.day,
        signature: activitySignature(activity),
      })
    }
  }

  return nodes
}

function increaseDayStat(
  dayStats: Map<number, ItineraryDayDiffStat>,
  day: number,
  key: 'added' | 'removed' | 'updated',
) {
  const current = dayStats.get(day) || { day, added: 0, removed: 0, updated: 0 }
  current[key] += 1
  dayStats.set(day, current)
}

function buildItineraryDiff(
  previous: StructuredItinerary,
  current: StructuredItinerary,
  round: number,
): ItineraryDiffSummary {
  const prevNodes = flattenDiffNodes(previous)
  const currNodes = flattenDiffNodes(current)

  const prevMap = new Map(prevNodes.map((node) => [node.nodeKey, node]))
  const currMap = new Map(currNodes.map((node) => [node.nodeKey, node]))

  let addedCount = 0
  let removedCount = 0
  let updatedCount = 0
  const changedNodeKeys: string[] = []
  const dayStats = new Map<number, ItineraryDayDiffStat>()

  for (const [key, currNode] of currMap.entries()) {
    const prevNode = prevMap.get(key)
    if (!prevNode) {
      addedCount += 1
      changedNodeKeys.push(key)
      increaseDayStat(dayStats, currNode.day, 'added')
      continue
    }

    if (prevNode.signature !== currNode.signature) {
      updatedCount += 1
      changedNodeKeys.push(key)
      increaseDayStat(dayStats, currNode.day, 'updated')
    }
  }

  for (const [key, prevNode] of prevMap.entries()) {
    if (!currMap.has(key)) {
      removedCount += 1
      increaseDayStat(dayStats, prevNode.day, 'removed')
    }
  }

  const changedDays = Array.from(dayStats.keys()).sort((a, b) => a - b)
  const perDay = Array.from(dayStats.values()).sort((a, b) => a.day - b.day)

  return {
    round,
    changedDays,
    changedNodeKeys: Array.from(new Set(changedNodeKeys)),
    addedCount,
    removedCount,
    updatedCount,
    perDay,
  }
}

function normalizeIncomingItinerary(source: StructuredItinerary): StructuredItinerary {
  const cloned = JSON.parse(JSON.stringify(source)) as StructuredItinerary

  const sourcePlans = ensureArray<DailyPlan>(cloned.dailyPlans)

  cloned.dailyPlans = sourcePlans.map((plan, index) => {
    type ActivityWithSourceIndex = Activity & { __sourceIndex: number }
    const rawPlan = plan as { activities?: unknown }
    const sourceActivities = ensureArray<Activity>(rawPlan.activities)

    plan.day = index + 1
    const normalizedActivities: ActivityWithSourceIndex[] = sourceActivities.map(
      (activity, activityIndex) => {
        const raw = activity as unknown as {
          imageUrl?: string
          image?: string
          picture?: string
          address?: string
        }
        type RawLocation = {
          address?: string
          latitude?: number | string
          longitude?: number | string
          lat?: number | string
          lng?: number | string
          coordinates?: [number | string, number | string]
        }

        const rawObj = activity as unknown as {
          location?: RawLocation
          latitude?: number | string
          longitude?: number | string
          lat?: number | string
          lng?: number | string
          'location.latitude'?: number | string
          'location.longitude'?: number | string
        }

        const fallbackImage = [raw.imageUrl, raw.image, raw.picture].find(
          (value): value is string => typeof value === 'string' && value.trim().length > 0,
        )
        const candidateImage = activity.imageUrl || fallbackImage || ''
        const fallbackAddress = typeof raw.address === 'string' ? raw.address.trim() : ''
        const locationAddress =
          activity.location?.address?.trim() ||
          rawObj.location?.address?.trim() ||
          fallbackAddress ||
          activity.name?.trim() ||
          ''

        let lat =
          rawObj.location?.latitude ??
          rawObj.location?.lat ??
          rawObj.latitude ??
          rawObj['location.latitude'] ??
          rawObj.lat
        let lon =
          rawObj.location?.longitude ??
          rawObj.location?.lng ??
          rawObj.longitude ??
          rawObj['location.longitude'] ??
          rawObj.lng

        if (
          (lat === undefined || lon === undefined) &&
          Array.isArray(rawObj.location?.coordinates)
        ) {
          lon = rawObj.location.coordinates[0]
          lat = rawObj.location.coordinates[1]
        }

        const normalizedLocation: Activity['location'] = {
          address: locationAddress,
        }

        const latNum = Number(lat)
        const lonNum = Number(lon)
        if (Number.isFinite(latNum) && Number.isFinite(lonNum)) {
          normalizedLocation.latitude = latNum
          normalizedLocation.longitude = lonNum
          normalizedLocation.coordinates = [lonNum, latNum]
        }

        const period = normalizePeriod(activity.time)
        return {
          ...activity,
          __sourceIndex: activityIndex,
          time: period,
          name: activity.name || '',
          description: activity.description || '',
          type: normalizeActivityType(activity.type),
          imageUrl: isUsableImageUrl(candidateImage) ? candidateImage : '',
          location: normalizedLocation,
          estimatedCost: Number(activity.estimatedCost || 0),
        }
      },
    )
    plan.activities = normalizedActivities
      .sort((a, b) => {
        const periodDiff = periodWeight(a.time as DayPeriod) - periodWeight(b.time as DayPeriod)
        if (periodDiff !== 0) {
          return periodDiff
        }
        return a.__sourceIndex - b.__sourceIndex
      })
      .map(({ __sourceIndex, ...activity }) => {
        void __sourceIndex
        return activity
      })
    return plan
  })

  cloned.days = cloned.dailyPlans.length
  return cloned
}

async function resolveImageByQuery(query: string): Promise<string> {
  const normalizedQuery = query.trim().toLowerCase()
  if (!normalizedQuery) {
    return ''
  }

  const cached = imageQueryCache.get(normalizedQuery)
  if (cached !== undefined) {
    return cached
  }

  const image = await fetchFirst(query)
  const imageUrlCandidate =
    image?.landscapeUrl || image?.large2xUrl || image?.largeUrl || image?.mediumUrl || ''
  const imageUrl = isUsableImageUrl(imageUrlCandidate) ? imageUrlCandidate : ''

  imageQueryCache.set(normalizedQuery, imageUrl)
  return imageUrl
}

async function enrichItineraryImages(source: StructuredItinerary): Promise<StructuredItinerary> {
  const enriched = JSON.parse(JSON.stringify(source)) as StructuredItinerary
  const tasks: Array<Promise<void>> = []
  let filledCount = 0
  const maxFillCount = 9

  for (const plan of ensureArray<DailyPlan>(enriched.dailyPlans)) {
    const activities = ensureArray<Activity>((plan as { activities?: unknown }).activities)

    for (const activity of activities) {
      if (filledCount >= maxFillCount) {
        break
      }

      if (isUsableImageUrl(activity.imageUrl || '')) {
        continue
      }

      activity.imageUrl = ''

      filledCount += 1
      const attractionName = activity.name?.trim() || '旅行景点'
      const destination = enriched.destination?.trim() || ''
      const primaryQuery = destination ? `${destination} ${attractionName}` : attractionName
      const fallbackQuery = destination || attractionName

      tasks.push(
        (async () => {
          const primaryImage = await resolveImageByQuery(primaryQuery)
          activity.imageUrl = primaryImage || (await resolveImageByQuery(fallbackQuery))
        })(),
      )
    }
  }

  if (tasks.length > 0) {
    await Promise.all(tasks)
  }

  return enriched
}

function toDailyHighlights(itinerary: StructuredItinerary): Record<number, string[]> {
  const highlights: Record<number, string[]> = {}

  for (const plan of ensureArray<DailyPlan>(itinerary.dailyPlans)) {
    const activities = ensureArray<Activity>((plan as { activities?: unknown }).activities)

    const dayItems = activities
      .map((activity) => {
        const time = activity.time?.trim() || ''
        const name = activity.name?.trim() || ''
        const description = activity.description?.trim() || ''

        if (!name && !description) {
          return ''
        }

        if (time && name && description) {
          return `${time} ${name}：${description}`
        }

        if (time && name) {
          return `${time} ${name}`
        }

        return [time, name, description].filter(Boolean).join(' ')
      })
      .filter(Boolean)

    if (dayItems.length > 0) {
      highlights[plan.day] = dayItems
    }
  }

  return highlights
}

async function saveCurrentItinerary() {
  if (!currentItinerary.value || isSavingTrip.value) {
    return
  }

  const itinerary = currentItinerary.value
  if (!itinerary.destination?.trim()) {
    message.warning('当前行程缺少目的地，暂时无法保存')
    return
  }

  isSavingTrip.value = true
  try {
    const request: API.TripSaveRequest = {
      destination: itinerary.destination,
      days: itinerary.days,
      budget: itinerary.budget,
      theme: itinerary.theme,
      dailyHighlights: toDailyHighlights(itinerary),
      structuredData: JSON.stringify(itinerary),
    }

    const response = await saveTrip(request)
    if (response.data.code !== 0 || !response.data.data) {
      throw new Error(response.data.message || '保存失败')
    }

    void loadRecentTrips()
    message.success('行程已保存，正在跳转详情')
    router.push(`/trips/${response.data.data}`)
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : '保存失败，请稍后重试'
    message.error(errorMessage)
  } finally {
    isSavingTrip.value = false
  }
}

async function handleSendMessage(text: string, options?: { displayText?: string }) {
  const prompt = text.trim()
  if (!prompt) {
    return
  }

  const displayText = options?.displayText?.trim() || prompt
  const optimisticTitle = buildConversationTitle(displayText)
  await startStream(
    prompt,
    conversationId.value,
    () => {},
    (newConversationId: string) => {
      conversationId.value = newConversationId
      currentConversationTitle.value = optimisticTitle
      const now = new Date().toISOString()
      lastGeneratedAt.value = now
      upsertConversation({
        id: Number(newConversationId),
        title: optimisticTitle,
        createTime: now,
        updateTime: now,
      })
    },
    (data: StructuredItinerary) => {
      applyStructuredItinerary(data)
      void refreshConversations()
    },
    displayText,
  )
}

function handleProductPurchase(product: Product) {
  router.push(`/products/${product.id}`)
}

function formatChatTime(time: Date): string {
  const hour = String(time.getHours()).padStart(2, '0')
  const minute = String(time.getMinutes()).padStart(2, '0')
  return `${hour}:${minute}`
}

onUnmounted(() => {
  closeStream()
})

watch(
  isLoading,
  (loading, wasLoading) => {
    if (!loading && wasLoading) {
      finalizePendingRecommendationCards()
    }
  },
)

watch(
  isLoggedIn,
  (loggedIn) => {
    if (loggedIn) {
      void refreshConversations()
      void loadRecentTrips()
      return
    }

    conversationId.value = undefined
    currentConversationTitle.value = '新会话'
    lastGeneratedAt.value = ''
    chatMessages.value = []
    recentTrips.value = []
    pendingRecommendationCards.value = null
    clearRecommendations()
  },
  { immediate: true },
)

watch(
  conversations,
  (items) => {
    if (!conversationId.value) {
      return
    }

    const currentConversation = items.find((item) => String(item.id) === conversationId.value)
    if (!currentConversation) {
      return
    }

    currentConversationTitle.value =
      currentConversation.title?.trim() || currentConversationTitle.value
    lastGeneratedAt.value =
      currentConversation.updateTime || currentConversation.createTime || lastGeneratedAt.value
  },
  { deep: true },
)

onMounted(() => {
  if (isLoggedIn.value) {
    void refreshConversations()
    void loadRecentTrips()
    
    const initialPrompt = route.query.prompt as string
    if (initialPrompt && !isLoading.value) {
      void handleSendMessage(initialPrompt)
      router.replace({ ...route, query: {} })
    }
  }
})
</script>

<style scoped lang="scss">
.workspace-page {
  width: min(100%, 1280px);
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  margin: 0 auto;
  padding: 0 16px 8px;
  box-sizing: border-box;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 8px 0 8px;
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}

.planner-stage {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
}

.stage-content {
  flex: 1;
  display: flex;
  min-height: 0;
  overflow: hidden;
  gap: 12px;
  align-items: stretch;
}

.timeline-panel {
  flex: 2.2; /* 时间轴占更大比例 */
  min-width: 0;
  min-height: 0;
  display: flex;
  height: 100%;
  overflow: hidden;
}

.map-panel {
  flex: 0.9;
  min-width: 0;
  display: flex;
  min-height: 0;
  height: 100%;
  overflow: hidden;
}

.stage-content.map-hidden {
  gap: 0;
}

.timeline-panel-full {
  flex: 1 1 100%;
  width: 100%;
}

.timeline-panel :deep(.timeline-board) {
  width: 100%;
  height: 100%;
  box-shadow: none; /* 移除外层阴影 */
}

.chat-history-panel {
  position: absolute;
  top: 10px;
  right: 10px;
  width: min(600px, calc(100% - 20px));
  border: 1px solid rgba(15, 28, 46, 0.08);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(8px);
  overflow: hidden;
  z-index: 12;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.12);
}

.chat-history-panel.is-center-mode {
  position: relative;
  top: auto;
  left: auto;
  right: auto;
  transform: none;
  width: 100%;
  max-width: 960px;
  margin: 0 auto;
  flex: 1;
  display: flex;
  flex-direction: column;
  box-shadow: none;
  border: none;
  background: transparent;
  backdrop-filter: none;
}

.chat-history-panel.is-center-mode .chat-history-list {
  flex: 1;
  max-height: none;
  padding: 20px 4px 60px;
}
.chat-history-panel.is-collapsed {
  width: auto;
}

.chat-history-panel.is-center-mode.is-collapsed {
  top: 10px;
  right: 10px;
  left: auto;
  transform: none;
}

.chat-history-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.2);
}

.chat-history-title {
  font-size: 12px;
  font-weight: 600;
  color: #334155;
}

.chat-history-toggle {
  border: 1px solid rgba(30, 64, 175, 0.25);
  background: rgba(30, 64, 175, 0.08);
  color: #1e40af;
  border-radius: 999px;
  font-size: 12px;
  line-height: 1;
  padding: 4px 10px;
  cursor: pointer;
}

.chat-history-list {
  max-height: min(52vh, 420px);
  overflow-y: auto;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.chat-message {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-width: 90%;
  padding: 8px 10px;
  border-radius: 10px;
}

.chat-message.is-user {
  align-self: flex-end;
  background: rgba(19, 96, 255, 0.12);
  border: 1px solid rgba(19, 96, 255, 0.2);
}

.chat-message.is-ai {
  align-self: flex-start;
  background: rgba(15, 23, 42, 0.06);
  border: 1px solid rgba(15, 23, 42, 0.08);
}

.chat-message.has-recommendation-cards {
  max-width: 100%;
  width: 100%;
}

.chat-message-meta {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #64748b;
}

.chat-message-text {
  white-space: pre-wrap;
  line-height: 1.5;
  font-size: 14px;
  color: #0f172a;
}

.bottom-chat {
  position: relative;
  z-index: 8;
  flex-shrink: 0;
  padding: 6px 0 6px;
  border-top: 1px solid rgba(15, 28, 46, 0.08);
  background: linear-gradient(180deg, rgba(244, 247, 251, 0.3), rgba(244, 247, 251, 0.98));
  backdrop-filter: blur(8px);
}

.bottom-chat :deep(.chat-input) {
  padding: 0;
  width: 100%;
  max-width: 960px;
  margin: 0 auto;
}

.bottom-chat :deep(.input-container) {
  max-width: 960px;
  margin: 0 auto;
}

@media (max-width: 960px) {
  .stage-content {
    flex-direction: row;
    min-height: 0;
  }

  .timeline-panel {
    flex: 1.8;
  }

  .map-panel {
    flex: 1;
    min-height: 0;
  }
}

@media (max-width: 760px) {
  .chat-history-panel {
    top: 8px;
    right: 8px;
    width: calc(100% - 16px);
  }

  .chat-history-list {
    max-height: 42vh;
  }

  .stage-content {
    flex-direction: column;
  }

  .timeline-panel {
    flex: 1;
  }

  .map-panel {
    flex: 0 0 180px;
    min-height: 180px;
  }
}

.empty-stage {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-prompt {
  display: flex;
  align-items: center;
  gap: 24px;
  border: 1px solid rgba(15, 28, 46, 0.08);
  border-radius: 22px;
  background: #fff;
  padding: 28px;
}

.login-prompt img {
  width: 140px;
  height: auto;
}

.prompt-content h2 {
  margin: 0 0 8px;
  color: var(--color-text);
}

.prompt-content p {
  margin: 0 0 16px;
  color: var(--color-text-secondary);
}

@media (max-width: 900px) {
  .page-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .login-prompt {
    flex-direction: column;
    text-align: center;
  }
}
</style>

