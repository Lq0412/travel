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
        <a-button v-if="normalizeTripStatus(trip.status) !== 'completed'" @click="markCompleted" :loading="completing">
          标记已完成
        </a-button>
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
            <p>上传照片、管理行程，围绕同一个行程继续推进。</p>
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
        <h2>照片管理</h2>
        <p>在这一页完成照片关联和管理。</p>
      </div>
      <p style="color: var(--color-muted);">照片上传功能开发中...</p>
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
} from '@/api/tripController'
import PexelsCredit from '@/components/content/PexelsCredit.vue'
import { useVisualContent } from '@/composables/useVisualContent'
import {
  formatWorkflowDate,
  formatWorkflowDateTime,
  getTripStatusLabel,
  getTripStatusTone,
  normalizeTripStatus,
} from '@/utils/tripWorkflow'

type TripTab = 'overview' | 'photos'

const route = useRoute()
const router = useRouter()
const { fetchFirst, illustrations } = useVisualContent()

const tabs: Array<{ key: TripTab; label: string; desc: string }> = [
  { key: 'overview', label: '概览', desc: '统一看进度与亮点' },
  { key: 'photos', label: '照片', desc: '上传并管理素材' },
]

const activeTab = ref<TripTab>('overview')
const trip = ref<API.TripVO | null>(null)
const completing = ref(false)
const destinationPhoto = ref<API.ContentImageVO | null>(null)
const lastDestinationQuery = ref('')

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

watch(
  () => route.query.tab,
  async () => {
    syncTabFromRoute()
  }
)

watch(
  () => route.params.id,
  async () => {
    await refreshTrip()
  }
)

onMounted(async () => {
  syncTabFromRoute()
  await refreshTrip()
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

.highlight-list {
  display: grid;
  gap: 10px;
  padding-left: 18px;
  margin: 0;
  color: var(--color-text-secondary);
}

@media (max-width: 1200px) {
  .trip-hero,
  .hero-visuals,
  .workflow-tabs,
  .content-grid,
  .overview-grid {
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
}
</style>
