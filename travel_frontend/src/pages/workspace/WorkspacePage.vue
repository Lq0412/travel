<template>
  <div class="workspace-page">
    <header class="page-header">
      <div class="header-actions">
        <a-button v-if="isLoggedIn" type="link" @click="resetConversation">新会话</a-button>
        <a-button
          v-if="isLoggedIn && currentItinerary"
          type="primary"
          :loading="isSavingTrip"
          :disabled="isLoading"
          @click="saveCurrentItinerary"
        >
          保存行程
        </a-button>
        <a-button v-if="isLoggedIn" type="link" @click="goTrips">我的行程</a-button>
        <a-button v-else type="primary" @click="goLogin">登录</a-button>
      </div>
    </header>

    <section v-if="isLoggedIn" class="planner-stage">
      <div class="stage-content">
        <div class="timeline-panel">
          <ItineraryTimelineBoard :itinerary="currentItinerary" />
        </div>
        <div class="map-panel">
          <DynamicMap :itinerary="currentItinerary" />
        </div>
      </div>

      <div class="bottom-chat">
        <div class="theme-selector-container">
          <span class="theme-label">选择定制偏好：</span>
          <a-radio-group v-model:value="selectedTheme" size="small" button-style="solid">
            <a-radio-button value="none">常规路线</a-radio-button>
            <a-radio-button value="elderly">👴 银发族适老游</a-radio-button>
            <a-radio-button value="rural">🌾 乡村振兴非遗游</a-radio-button>
            <a-radio-button value="student">🎒 大学生特种兵游</a-radio-button>
          </a-radio-group>
        </div>
        <div class="quick-suggestions">
          <button
            v-for="suggestion in suggestionInputs"
            :key="suggestion"
            type="button"
            class="suggestion-chip"
            :disabled="isLoading"
            @click="applySuggestion(suggestion)"
          >
            {{ suggestion }}
          </button>
        </div>
        <ChatInput
          :disabled="isLoading"
          placeholder="告诉我下一步想怎么改，例如：把第二天晚上的景点改成夜景路线"
          @send-message="handleSendMessage"
        />
      </div>
    </section>

    <section v-else class="empty-stage">
      <div class="login-prompt">
        <img :src="illustrations.travelMode" alt="travel illustration" />
        <div class="prompt-content">
          <h2>登录后开始动态规划</h2>
          <p>你可以在底部持续输入需求，AI 会把你的想法实时同步到中间时间轴。</p>
          <a-button type="primary" size="large" @click="goLogin">前往登录</a-button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import ChatInput from '@/pages/user/ChatInput.vue'
import ItineraryTimelineBoard from '@/pages/workspace/ItineraryTimelineBoard.vue'
import DynamicMap from '@/pages/workspace/DynamicMap.vue'
import type { Activity, DailyPlan, StructuredItinerary } from '@/types/itinerary'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { useVisualContent } from '@/composables/useVisualContent'
import { useChatStream } from '@/composables/useChatStream'
import { saveTrip } from '@/api/tripController'

type DayPeriod = 'morning' | 'noon' | 'evening'

const router = useRouter()
const loginUserStore = useLoginUserStore()
const { illustrations, fetchFirst } = useVisualContent()

const currentItinerary = ref<StructuredItinerary | null>(null)
const conversationId = ref<string | undefined>(undefined)
const isSavingTrip = ref(false)
const selectedTheme = ref<string>('none')
const enrichmentVersion = ref(0)
const lastItinerarySignature = ref('')
const imageQueryCache = new Map<string, string>()
const baseSuggestions = [
  '给我一版 3 天游轻松路线，早中晚各 1 个景点',
  '把第二天晚上改成夜景和夜市路线',
  '整体预算控制到 2000 元以内',
  '每一天午间安排室内景点，避开暴晒',
]

const isLoggedIn = computed(() => Boolean(loginUserStore.loginUser.id))
const { isLoading, startStream, closeStream, structuredData } = useChatStream()
const suggestionInputs = computed(() => {
  const destination = currentItinerary.value?.destination?.trim()
  if (!destination) {
    return baseSuggestions
  }

  return [
    `把${destination}行程改成亲子友好版`,
    `优化${destination}第二天，减少交通折返`,
    `给${destination}补充更多拍照出片景点`,
    `在${destination}每晚安排本地美食和夜游`,
  ]
})

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
  currentItinerary.value = normalized

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

function resetConversation() {
  enrichmentVersion.value += 1
  lastItinerarySignature.value = ''
  conversationId.value = undefined
  currentItinerary.value = null
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
  if (value.includes('noon') || value.includes('afternoon') || value.includes('中') || value.includes('午')) {
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

function ensureArray<T>(value: unknown): T[] {
  return Array.isArray(value) ? (value as T[]) : []
}

function normalizeIncomingItinerary(source: StructuredItinerary): StructuredItinerary {
  const cloned = JSON.parse(JSON.stringify(source)) as StructuredItinerary

  const sourcePlans = ensureArray<DailyPlan>(cloned.dailyPlans)

  cloned.dailyPlans = sourcePlans.map((plan, index) => {
    const rawPlan = plan as { activities?: unknown }
    const sourceActivities = ensureArray<Activity>(rawPlan.activities)

    plan.day = index + 1
    plan.activities = sourceActivities.map((activity) => {
      const raw = activity as unknown as {
        imageUrl?: string
        image?: string
        picture?: string
        address?: string
      }
      const fallbackImage = [raw.imageUrl, raw.image, raw.picture].find(
        (value): value is string => typeof value === 'string' && value.trim().length > 0,
      )
      const candidateImage = activity.imageUrl || fallbackImage || ''
      const fallbackAddress = typeof raw.address === 'string' ? raw.address : ''
        const rawObj = activity as any
        const lat = rawObj.location?.latitude || rawObj.latitude || rawObj['location.latitude'] || rawObj.lat
        const lon = rawObj.location?.longitude || rawObj.longitude || rawObj['location.longitude'] || rawObj.lng
        
        const normalizedLocation = activity.location || { address: fallbackAddress }
        if (lat !== undefined && lon !== undefined) {
           normalizedLocation.latitude = Number(lat)
           normalizedLocation.longitude = Number(lon)
        }
        return {
          ...activity,
          time: normalizePeriod(activity.time),
          name: activity.name || '',
          description: activity.description || '',
          type: activity.type || 'attraction',
          imageUrl: isUsableImageUrl(candidateImage) ? candidateImage : '',
          location: normalizedLocation,
          estimatedCost: Number(activity.estimatedCost || 0),
        }
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

    message.success('行程已保存，正在跳转详情')
    router.push(`/trips/${response.data.data}`)
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : '保存失败，请稍后重试'
    message.error(errorMessage)
  } finally {
    isSavingTrip.value = false
  }
}

function buildPlannerPrompt(userInput: string): string {
  let themeInstruction = ''
  if (selectedTheme.value === 'elderly') {
    themeInstruction = '【偏好要求：银发族适老游】必须安排慢节奏的行程，减少步行和高负荷运动，优先安排自然风光和人文历史景点，且关注周边餐饮和休息区的便利性。'
  } else if (selectedTheme.value === 'rural') {
    themeInstruction = '【偏好要求：乡村振兴非遗游】必须带有社会价值导向，重点安排当地乡村生态旅游、非物质文化遗产体验、助农项目和小众冷门特色乡村点位。'
  } else if (selectedTheme.value === 'student') {
    themeInstruction = '【偏好要求：大学生特种兵打卡游】必须是高性价比、高密度的行程。包含网红打卡点、高性价比夜市和小吃街，不强制高品质酒店，主打一个高效低预算游玩。'
  }

  const baseConstraint = '【输出要求】请输出可保存的结构化行程 JSON，字段必须包含 destination、days、budget、theme、dailyPlans、totalEstimatedCost、tips。dailyPlans.activities 中每个景点必须包含：time（morning/noon/evening）、name、description、type、location（必须是包含 address、longitude小数经度、latitude小数纬度 这三个字段的嵌套对象）、estimatedCost、imageUrl。'

  return `${userInput}\n\n${themeInstruction ? themeInstruction + '\n\n' : ''}${baseConstraint}`
}

async function handleSendMessage(text: string) {
  const prompt = text.trim()
  if (!prompt) {
    return
  }

  const enhancedTask = buildPlannerPrompt(prompt)
  await startStream(
    enhancedTask,
    conversationId.value,
    () => {},
    (newConversationId: string) => {
      conversationId.value = newConversationId
    },
    (data: StructuredItinerary) => {
      applyStructuredItinerary(data)
    },
    prompt
  )
}

function applySuggestion(suggestion: string) {
  void handleSendMessage(suggestion)
}

onUnmounted(() => {
  closeStream()
})
</script>

<style scoped lang="scss">
.workspace-page {
  width: min(100%, 1280px);
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
  margin: 0 auto;
  padding: 0 16px 12px;
  box-sizing: border-box;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding: 10px 0 12px;
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}

.planner-stage {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.stage-content {
  flex: 1;
  display: flex;
  min-height: 420px;
  gap: 16px;
  padding: 12px 0;
}

.timeline-panel {
  flex: 1.5; /* 时间轴占更大比例 */
  min-width: 0;
  display: flex;
}

.map-panel {
  flex: 1;
  min-width: 0;
  display: flex;
}

.timeline-panel :deep(.timeline-board) {
  width: 100%;
  height: 100%;
  box-shadow: none; /* 移除外层阴影 */
}

.bottom-chat {
  position: sticky;
  bottom: 0;
  z-index: 8;
  padding: 8px 0 10px;
  border-top: 1px solid rgba(15, 28, 46, 0.08);
  background: linear-gradient(180deg, rgba(244, 247, 251, 0.3), rgba(244, 247, 251, 0.98));
  backdrop-filter: blur(8px);
}

.theme-selector-container {
  display: flex;
  margin: 0 auto 12px;
  max-width: 960px;
  align-items: center;
}

.theme-label {
  font-size: 13px;
  color: var(--color-text-secondary, #666);
  margin-right: 8px;
  font-weight: 500;
}

.quick-suggestions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin: 0 auto 8px;
  max-width: 960px;
}

.suggestion-chip {
  min-height: 30px;
  border-radius: 999px;
  border: 1px solid rgba(19, 96, 255, 0.26);
  background: rgba(19, 96, 255, 0.08);
  color: #1b4bc4;
  padding: 0 12px;
  font-size: 12px;
  cursor: pointer;
}

.suggestion-chip:hover:not(:disabled) {
  background: rgba(19, 96, 255, 0.14);
  border-color: rgba(19, 96, 255, 0.42);
}

.suggestion-chip:disabled {
  cursor: not-allowed;
  opacity: 0.55;
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
    flex-direction: column;
    min-height: 320px;
  }
  .map-panel {
    display: none; /* 在小屏幕上隐藏地图，避免过分拥挤 */
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

