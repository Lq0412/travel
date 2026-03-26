<template>
  <div v-if="trip" class="trip-detail-page">
    <section class="trip-hero">
      <div class="hero-copy">
        <p class="eyebrow">旅程工作区</p>
        <h1>{{ trip.destination || '未命名行程' }} · {{ trip.days || 0 }} 天</h1>
        <div class="hero-meta">
          <span>主题：{{ trip.theme || '通用主题' }}</span>
          <span>{{ trip.startDate ? `出发：${formatWorkflowDate(trip.startDate)}` : '出发时间待定' }}</span>
          <span>最近更新：{{ formatWorkflowDateTime(trip.updateTime || trip.createTime) }}</span>
        </div>
      </div>

      <div class="hero-actions">
        <a-tag :color="getTripStatusTone(trip.status)">{{ getTripStatusLabel(trip.status) }}</a-tag>
        <a-tag v-if="hasTripMemoryCard(trip)" color="cyan">已有回忆图</a-tag>
        <a-tag v-if="isTripPublished(trip)" color="success">已发布</a-tag>
        <a-button v-if="normalizeTripStatus(trip.status) !== 'completed'" @click="markCompleted" :loading="completing">
          标记已完成
        </a-button>
        <a-button type="primary" @click="setTab('publish')">去发布</a-button>
      </div>

      <div class="hero-visuals" :class="{ 'single-card': !destinationPhoto }">
        <article v-if="destinationPhoto" class="destination-photo">
          <img :src="photoSrc(destinationPhoto)" :alt="`${trip.destination || '旅行'} 实景图`" />
          <div class="destination-overlay">
            <strong>{{ trip.destination || '真实旅途氛围' }}</strong>
            <p>按目的地实时拉取实景图，让行程详情页直接连接真实旅行场景。</p>
            <PexelsCredit :image="destinationPhoto" />
          </div>
        </article>

        <article class="trip-illustration">
          <img :src="illustrations.journey" alt="journey illustration" />
          <div>
            <strong>执行与沉淀继续在这里完成</strong>
            <p>上传照片、生成回忆图、发布到灵感广场，都围绕同一个行程继续推进。</p>
          </div>
        </article>
      </div>
    </section>

    <section class="workflow-tabs">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        type="button"
        class="tab-trigger"
        :class="{ active: activeTab === tab.key }"
        @click="setTab(tab.key)"
      >
        <strong>{{ tab.label }}</strong>
        <span>{{ tab.desc }}</span>
      </button>
    </section>

    <section v-if="activeTab === 'overview'" class="content-stack">
      <div class="content-grid">
        <div class="content-card">
          <div class="card-head">
            <h2>旅程概览</h2>
            <p>用统一状态和真实资产展示当前进度。</p>
          </div>
          <div class="overview-grid">
            <div class="overview-item">
              <strong>{{ getTripStatusLabel(trip.status) }}</strong>
              <span>当前状态</span>
            </div>
            <div class="overview-item">
              <strong>{{ trip.photos?.length || 0 }}</strong>
              <span>已关联照片</span>
            </div>
            <div class="overview-item">
              <strong>{{ hasTripMemoryCard(trip) ? '已生成' : '未生成' }}</strong>
              <span>回忆图</span>
            </div>
            <div class="overview-item">
              <strong>{{ isTripPublished(trip) ? '已沉淀' : '待沉淀' }}</strong>
              <span>灵感内容</span>
            </div>
          </div>
        </div>

        <div class="content-card" v-if="trip.memoryCard?.imageUrl">
          <div class="card-head">
            <h2>当前回忆图</h2>
            <p>发布时将默认作为灵感内容封面。</p>
          </div>
          <div class="memory-preview">
            <img :src="trip.memoryCard.imageUrl" :alt="`${trip.destination} 回忆图`" />
          </div>
        </div>
      </div>

      <div class="content-card">
        <div class="card-head">
          <h2>每日亮点</h2>
          <p>把 AI 方案继续落到可执行的每日安排里。</p>
        </div>

        <a-collapse v-if="highlightEntries.length" ghost>
          <a-collapse-panel
            v-for="day in highlightEntries"
            :key="day.day"
            :header="`第 ${day.day} 天 · ${day.items.length} 个亮点`"
          >
            <ul class="highlight-list">
              <li v-for="item in day.items" :key="item">{{ item }}</li>
            </ul>
          </a-collapse-panel>
        </a-collapse>
        <a-empty v-else description="当前还没有每日亮点" />
      </div>
    </section>

    <section v-else-if="activeTab === 'photos'" class="content-card">
      <div class="card-head">
        <h2>照片管理与生成入口</h2>
        <p>在这一页完成照片关联，并直接发起回忆图生成。</p>
      </div>
      <TripMemoryPage embedded />
    </section>

    <section v-else-if="activeTab === 'memory'" class="content-stack">
      <div class="content-card">
        <div class="card-head">
          <div>
            <h2>回忆图工作区</h2>
            <p>支持查看当前版本、重新生成和切换历史版本。</p>
          </div>
          <div class="card-actions">
            <a-button @click="setTab('photos')">去照片页上传</a-button>
            <a-button @click="refreshTrip">刷新</a-button>
            <a-button @click="loadHistory" :loading="historyLoading">刷新历史</a-button>
            <a-button type="primary" @click="regenerate" :loading="regenerating">重新生成</a-button>
          </div>
        </div>

        <div v-if="trip.memoryCard?.imageUrl" class="memory-showcase">
          <img :src="trip.memoryCard.imageUrl" :alt="`${trip.destination} 回忆图`" />
          <div class="memory-meta">
            <span>模板：{{ trip.memoryCard.templateName || 'default' }}</span>
            <span>状态：{{ trip.memoryCard.status || 'success' }}</span>
            <span>更新时间：{{ formatWorkflowDateTime(trip.memoryCard.updateTime || trip.memoryCard.createTime) }}</span>
          </div>
        </div>
        <a-empty v-else description="还没有回忆图，请先去照片页上传并生成" />
      </div>

      <div class="content-card">
        <div class="card-head">
          <h2>历史版本</h2>
          <p>保留多次生成结果，方便比较和切换更合适的版本。</p>
        </div>

        <div v-if="historyList.length" class="history-grid">
          <article v-for="history in historyList" :key="history.id" class="history-card">
            <img :src="history.imageUrl" alt="历史回忆图" />
            <div class="history-body">
              <strong>{{ history.templateName || 'default' }}</strong>
              <p>{{ formatWorkflowDateTime(history.createTime) }}</p>
              <div class="history-actions">
                <a-button @click="setCurrent(history.id)" :loading="historyActionId === history.id">
                  设为当前
                </a-button>
                <a :href="history.imageUrl" target="_blank" rel="noreferrer">查看原图</a>
              </div>
            </div>
          </article>
        </div>
        <a-empty v-else description="暂无历史版本" />
      </div>
    </section>

    <section v-else class="content-card">
      <div class="card-head">
        <div>
          <h2>发布到灵感广场</h2>
          <p>只有生成成功的回忆图才允许发布，确保主流程结果可信。</p>
        </div>
        <a-tag v-if="isTripPublished(trip)" color="success">该行程已发布过</a-tag>
      </div>

      <a-alert
        v-if="!trip.memoryCard?.imageUrl"
        type="warning"
        show-icon
        message="请先在照片或回忆图标签生成成功的回忆图，再进行发布。"
        class="publish-alert"
      />

      <a-form layout="vertical" class="publish-form">
        <a-form-item label="标题">
          <a-input v-model:value="publishForm.title" placeholder="输入游记标题" />
        </a-form-item>
        <a-form-item label="正文">
          <a-textarea
            v-model:value="publishForm.content"
            :rows="8"
            placeholder="描述此次行程亮点、体验和建议"
          />
        </a-form-item>
        <a-form-item label="标签">
          <a-input v-model:value="tagsInput" placeholder="使用英文逗号分隔，例如：广州,3天,#AI生成" />
        </a-form-item>
      </a-form>

      <div v-if="trip.memoryCard?.imageUrl" class="publish-preview">
        <img :src="trip.memoryCard.imageUrl" alt="回忆图封面预览" />
        <div>
          <strong>封面预览</strong>
          <p>当前回忆图将自动作为灵感内容封面。</p>
        </div>
      </div>

      <div class="publish-actions">
        <a-button @click="refreshTrip">刷新状态</a-button>
        <a-button
          type="primary"
          :disabled="!trip.memoryCard?.imageUrl"
          :loading="publishing"
          @click="publish"
        >
          {{ isTripPublished(trip) ? '再次发布到灵感广场' : '发布到灵感广场' }}
        </a-button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  completeTrip,
  getTripById,
  publishToForum,
} from '@/api/tripController'
import {
  getMemoryCardHistory,
  regenerateMemoryCard,
  setMemoryCardCurrentFromHistory,
} from '@/api/memoryCardController'
import PexelsCredit from '@/components/content/PexelsCredit.vue'
import { useVisualContent } from '@/composables/useVisualContent'
import TripMemoryPage from '@/pages/trips/TripMemoryPage.vue'
import {
  formatWorkflowDate,
  formatWorkflowDateTime,
  getTripStatusLabel,
  getTripStatusTone,
  hasTripMemoryCard,
  isTripPublished,
  normalizeTripStatus,
} from '@/utils/tripWorkflow'

type TripTab = 'overview' | 'photos' | 'memory' | 'publish'

const route = useRoute()
const router = useRouter()
const { fetchFirst, illustrations } = useVisualContent()

const tabs: Array<{ key: TripTab; label: string; desc: string }> = [
  { key: 'overview', label: '概览', desc: '统一看进度与亮点' },
  { key: 'photos', label: '照片', desc: '上传并管理素材' },
  { key: 'memory', label: '回忆图', desc: '查看结果与历史版本' },
  { key: 'publish', label: '发布', desc: '沉淀为灵感内容' },
]

const activeTab = ref<TripTab>('overview')
const trip = ref<API.TripVO | null>(null)
const completing = ref(false)
const regenerating = ref(false)
const publishing = ref(false)
const historyLoading = ref(false)
const historyActionId = ref<number | null>(null)
const historyList = ref<API.MemoryCardHistory[]>([])
const tagsInput = ref('')
const destinationPhoto = ref<API.ContentImageVO | null>(null)
const lastDestinationQuery = ref('')
const publishForm = ref({
  title: '',
  content: '',
})

const highlightEntries = computed(() => {
  const highlights = trip.value?.dailyHighlights
  if (!highlights) {
    return []
  }

  return Object.entries(highlights)
    .sort((a, b) => Number(a[0]) - Number(b[0]))
    .map(([day, items]) => ({
      day: Number(day),
      items: Array.isArray(items) ? items.filter(Boolean) : [],
    }))
    .filter((item) => item.items.length > 0)
})

function syncTabFromRoute() {
  const nextTab = route.query.tab
  if (nextTab && tabs.some((tab) => tab.key === nextTab)) {
    activeTab.value = nextTab as TripTab
    return
  }
  activeTab.value = 'overview'
}

function setTab(tab: TripTab) {
  activeTab.value = tab
  router.replace({
    path: route.path,
    query: tab === 'overview' ? {} : { ...route.query, tab },
  })
}

function prefillPublishForm(currentTrip: API.TripVO) {
  const highlightText = highlightEntries.value
    .slice(0, 3)
    .map((item) => `第 ${item.day} 天：${item.items.slice(0, 3).join('，')}`)
    .join('\n')

  publishForm.value.title = `${currentTrip.destination || '旅行'} ${currentTrip.days || 0}天旅程记录`
  publishForm.value.content = `目的地：${currentTrip.destination || '待补充'}
天数：${currentTrip.days || 0} 天
主题：${currentTrip.theme || '通用主题'}

亮点摘要：
${highlightText || '本次行程正在继续完善中。'}

这次旅行的回忆图已经生成，欢迎继续交流路线、体验和避坑建议。`
  tagsInput.value = [currentTrip.destination, `${currentTrip.days || 0}天`, '#AI生成', currentTrip.theme]
    .filter(Boolean)
    .join(',')
}

function photoSrc(image?: API.ContentImageVO | null) {
  return image?.landscapeUrl || image?.large2xUrl || image?.largeUrl || image?.mediumUrl || ''
}

async function loadDestinationPhoto(destination?: string | null) {
  const query = destination?.trim()
  if (!query) {
    destinationPhoto.value = null
    lastDestinationQuery.value = ''
    return
  }

  if (lastDestinationQuery.value === query && destinationPhoto.value) {
    return
  }

  lastDestinationQuery.value = query
  destinationPhoto.value = await fetchFirst(`${query} travel destination landscape`)
}

async function refreshTrip() {
  const id = Number(route.params.id)
  if (!id) {
    return
  }

  try {
    const resp = await getTripById({ id } as any)
    trip.value = resp?.data?.data || null
    if (trip.value) {
      prefillPublishForm(trip.value)
      await loadDestinationPhoto(trip.value.destination)
    } else {
      destinationPhoto.value = null
      lastDestinationQuery.value = ''
    }
  } catch (error: any) {
    const errorMsg = error?.response?.data?.message || error?.message || '加载行程失败'
    message.error(errorMsg)
  }
}

async function markCompleted() {
  if (!trip.value?.id) {
    return
  }

  completing.value = true
  try {
    await completeTrip({ id: trip.value.id } as any)
    message.success('已标记为已完成')
    await refreshTrip()
  } catch (error: any) {
    const errorMsg = error?.response?.data?.message || error?.message || '操作失败'
    message.error(errorMsg)
  } finally {
    completing.value = false
  }
}

async function regenerate() {
  if (!trip.value?.id) {
    return
  }

  regenerating.value = true
  try {
    await regenerateMemoryCard({ tripId: trip.value.id })
    message.success('已提交重新生成任务，请稍后刷新查看')
    setTab('photos')
    await refreshTrip()
  } catch (error: any) {
    const errorMsg = error?.response?.data?.message || error?.message || '重新生成失败'
    message.error(errorMsg)
  } finally {
    regenerating.value = false
  }
}

async function loadHistory() {
  if (!trip.value?.id) {
    return
  }

  historyLoading.value = true
  try {
    const resp = await getMemoryCardHistory({ tripId: trip.value.id })
    historyList.value = resp?.data?.data || []
  } catch (error: any) {
    const errorMsg = error?.response?.data?.message || error?.message || '加载历史版本失败'
    message.error(errorMsg)
  } finally {
    historyLoading.value = false
  }
}

async function setCurrent(historyId?: number) {
  if (!historyId) {
    return
  }

  historyActionId.value = historyId
  try {
    await setMemoryCardCurrentFromHistory({ historyId })
    message.success('已设为当前回忆图')
    await Promise.all([refreshTrip(), loadHistory()])
  } catch (error: any) {
    const errorMsg = error?.response?.data?.message || error?.message || '设置失败'
    message.error(errorMsg)
  } finally {
    historyActionId.value = null
  }
}

async function publish() {
  if (!trip.value?.id) {
    return
  }
  if (!trip.value.memoryCard?.imageUrl) {
    message.warning('请先生成回忆图')
    return
  }
  if (!publishForm.value.title.trim() || !publishForm.value.content.trim()) {
    message.warning('请补全标题和正文')
    return
  }

  publishing.value = true
  try {
    const tags = tagsInput.value
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean)

    await publishToForum(
      { id: trip.value.id } as any,
      {
        title: publishForm.value.title.trim(),
        content: publishForm.value.content.trim(),
        memoryCardId: trip.value.memoryCard?.id,
        tags: tags.length ? tags : ['#AI生成'],
      } as any
    )

    message.success('已发布到灵感广场')
    await refreshTrip()
    router.push('/inspiration')
  } catch (error: any) {
    const errorMsg = error?.response?.data?.message || error?.message || '发布失败'
    message.error(errorMsg)
  } finally {
    publishing.value = false
  }
}

watch(
  () => route.query.tab,
  async () => {
    syncTabFromRoute()
    if (activeTab.value === 'memory') {
      await loadHistory()
      await refreshTrip()
    }
  }
)

watch(
  () => route.params.id,
  async () => {
    await refreshTrip()
    if (activeTab.value === 'memory') {
      await loadHistory()
    }
  }
)

onMounted(async () => {
  syncTabFromRoute()
  await refreshTrip()
  if (activeTab.value === 'memory') {
    await loadHistory()
  }
})
</script>

<style scoped lang="scss">
.trip-detail-page {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.trip-hero,
.content-card {
  padding: 26px 28px;
  border-radius: 28px;
  border: 1px solid rgba(15, 28, 46, 0.08);
  background: #ffffff;
  box-shadow: 0 18px 48px rgba(18, 52, 97, 0.06);
}

.trip-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.02fr) minmax(320px, 0.98fr);
  gap: 20px;
  align-items: stretch;
  background:
    radial-gradient(circle at top right, rgba(47, 144, 240, 0.16), transparent 28%),
    radial-gradient(circle at bottom left, rgba(212, 139, 31, 0.1), transparent 30%),
    linear-gradient(135deg, #ffffff 0%, #f1f7ff 100%);

  h1 {
    margin: 8px 0 10px;
    font-size: 36px;
    color: var(--color-text);
  }
}

.hero-copy {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 18px;
}

.eyebrow {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.14em;
  color: var(--primary-600);
}

.hero-meta {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  color: var(--color-muted);
}

.hero-actions {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  flex-wrap: wrap;
}

.hero-visuals {
  display: grid;
  grid-template-columns: minmax(0, 1.06fr) 220px;
  gap: 14px;
  min-height: 260px;

  &.single-card {
    grid-template-columns: 1fr;
  }
}

.destination-photo,
.trip-illustration {
  overflow: hidden;
  border-radius: 24px;
  border: 1px solid rgba(15, 28, 46, 0.08);
  background: #ffffff;
}

.destination-photo {
  position: relative;

  img {
    width: 100%;
    height: 100%;
    min-height: 260px;
    object-fit: cover;
    display: block;
  }
}

.destination-overlay {
  position: absolute;
  inset: auto 0 0 0;
  padding: 18px;
  background: linear-gradient(180deg, rgba(15, 28, 46, 0.06) 0%, rgba(15, 28, 46, 0.86) 100%);

  strong {
    display: block;
    margin-bottom: 6px;
    color: #ffffff;
    font-size: 20px;
  }

  p {
    margin: 0 0 10px;
    color: rgba(255, 255, 255, 0.82);
  }
}

.trip-illustration {
  display: grid;
  gap: 10px;
  align-content: center;
  justify-items: center;
  padding: 18px;
  text-align: center;
  background: linear-gradient(180deg, #ffffff 0%, #eef5ff 100%);

  img {
    width: min(100%, 180px);
    height: auto;
  }

  strong {
    color: var(--color-text);
  }

  p {
    margin: 6px 0 0;
    color: var(--color-muted);
  }
}

.workflow-tabs {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.tab-trigger {
  padding: 18px;
  border-radius: 22px;
  border: 1px solid rgba(15, 28, 46, 0.08);
  background: rgba(255, 255, 255, 0.82);
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease, box-shadow 0.2s ease;

  strong,
  span {
    display: block;
  }

  strong {
    margin-bottom: 6px;
    color: var(--color-text);
  }

  span {
    color: var(--color-muted);
    font-size: 13px;
  }

  &.active {
    border-color: rgba(59, 110, 220, 0.32);
    background: rgba(234, 241, 255, 0.86);
    box-shadow: 0 12px 30px rgba(18, 52, 97, 0.06);
  }

  &:hover {
    transform: translateY(-2px);
  }
}

.content-stack {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.content-grid {
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  gap: 20px;
}

.card-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;

  h2 {
    margin: 0;
    font-size: 24px;
    color: var(--color-text);
  }

  p {
    margin: 6px 0 0;
    color: var(--color-muted);
  }
}

.card-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.overview-item {
  padding: 18px;
  border-radius: 20px;
  background: #f8fbff;
  border: 1px solid rgba(15, 28, 46, 0.06);

  strong,
  span {
    display: block;
  }

  strong {
    font-size: 24px;
    color: var(--color-text);
  }

  span {
    margin-top: 6px;
    color: var(--color-muted);
  }
}

.memory-preview,
.memory-showcase {
  img {
    width: 100%;
    border-radius: 22px;
    border: 1px solid rgba(15, 28, 46, 0.08);
    object-fit: cover;
  }
}

.memory-meta {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 12px;
  color: var(--color-muted);
  font-size: 13px;
}

.highlight-list {
  display: grid;
  gap: 10px;
  padding-left: 18px;
  margin: 0;
  color: var(--color-text-secondary);
}

.history-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.history-card {
  display: grid;
  grid-template-columns: 160px 1fr;
  gap: 16px;
  padding: 16px;
  border-radius: 22px;
  background: #f9fbff;
  border: 1px solid rgba(15, 28, 46, 0.06);

  img {
    width: 100%;
    height: 160px;
    border-radius: 18px;
    object-fit: cover;
  }
}

.history-body {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 12px;

  strong {
    color: var(--color-text);
  }

  p {
    margin: 0;
    color: var(--color-muted);
  }
}

.history-actions {
  display: flex;
  align-items: center;
  gap: 12px;

  a {
    color: var(--primary-700);
  }
}

.publish-alert {
  margin-bottom: 18px;
}

.publish-form {
  max-width: 860px;
}

.publish-preview {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 18px;
  margin-top: 10px;
  padding: 18px;
  border-radius: 22px;
  background: #f9fbff;
  border: 1px solid rgba(15, 28, 46, 0.06);

  img {
    width: 100%;
    border-radius: 18px;
    object-fit: cover;
  }

  strong {
    display: block;
    margin-bottom: 8px;
    color: var(--color-text);
  }

  p {
    margin: 0;
    color: var(--color-muted);
  }
}

.publish-actions {
  display: flex;
  gap: 12px;
  margin-top: 18px;
}

@media (max-width: 1200px) {
  .trip-hero,
  .hero-visuals,
  .workflow-tabs,
  .content-grid,
  .overview-grid,
  .history-grid,
  .publish-preview {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .trip-hero,
  .content-card {
    padding: 22px 18px;
  }

  .trip-hero {
    h1 {
      font-size: 28px;
    }
  }

  .history-card {
    grid-template-columns: 1fr;
  }

  .publish-actions {
    flex-direction: column;
  }
}
</style>
