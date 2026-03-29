<template>
  <div class="workspace-page">
    <header class="page-header">
      <div class="header-actions">
        <a-button v-if="isLoggedIn" type="link" @click="resetConversation">新会话</a-button>
        <a-button v-if="isLoggedIn" type="link" @click="goTrips">我的行程</a-button>
        <a-button v-else type="primary" @click="goLogin">登录</a-button>
      </div>
    </header>

    <section v-if="isLoggedIn" class="planner-stage">
      <div class="timeline-main">
        <ItineraryTimelineBoard :itinerary="currentItinerary" />
      </div>

      <div class="bottom-chat">
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
import ChatInput from '@/pages/user/ChatInput.vue'
import ItineraryTimelineBoard from '@/pages/workspace/ItineraryTimelineBoard.vue'
import type { StructuredItinerary } from '@/types/itinerary'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { useVisualContent } from '@/composables/useVisualContent'
import { useChatStream } from '@/composables/useChatStream'

type DayPeriod = 'morning' | 'noon' | 'evening'

const router = useRouter()
const loginUserStore = useLoginUserStore()
const { illustrations } = useVisualContent()

const currentItinerary = ref<StructuredItinerary | null>(null)
const conversationId = ref<string | undefined>(undefined)
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
  currentItinerary.value = normalizeIncomingItinerary(data)
})

function goLogin() {
  router.push('/user/login')
}

function goTrips() {
  router.push('/trips')
}

function resetConversation() {
  conversationId.value = undefined
  currentItinerary.value = null
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

function normalizeIncomingItinerary(source: StructuredItinerary): StructuredItinerary {
  const cloned = JSON.parse(JSON.stringify(source)) as StructuredItinerary

  cloned.dailyPlans = (cloned.dailyPlans || []).map((plan, index) => {
    plan.day = index + 1
    plan.activities = (plan.activities || []).map((activity) => {
      const raw = activity as any
      return {
        ...activity,
        time: normalizePeriod(activity.time),
        name: activity.name || '',
        description: activity.description || '',
        type: activity.type || 'attraction',
        imageUrl: activity.imageUrl || raw.imageUrl || raw.image || raw.picture || '',
        location: activity.location || { address: raw.address || '' },
        estimatedCost: Number(activity.estimatedCost || 0),
      }
    })
    return plan
  })

  cloned.days = cloned.dailyPlans.length
  return cloned
}

function buildPlannerPrompt(userInput: string): string {
  return `${userInput}\n\n【输出要求】请输出可保存的结构化行程 JSON，字段必须包含 destination、days、budget、theme、dailyPlans、totalEstimatedCost、tips。dailyPlans.activities 中每个景点必须包含：time（只允许 morning/noon/evening 三种）、name、description、type、location.address、estimatedCost、imageUrl。`
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
      currentItinerary.value = normalizeIncomingItinerary(data)
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

.timeline-main {
  flex: 1;
  min-height: 0;
  padding: 12px 0;
  display: flex;
}

.timeline-main :deep(.timeline-board) {
  width: 100%;
  height: 100%;
  box-shadow: 0 16px 36px rgba(18, 52, 97, 0.08);
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
