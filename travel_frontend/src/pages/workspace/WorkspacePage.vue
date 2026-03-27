<template>
  <div class="workspace-page">
    <section class="workspace-hero">
      <div class="hero-copy">
        <p class="eyebrow">规划行程</p>
        <h1>从需求到回忆，把整段旅程放在同一块画布上。</h1>
        <p class="hero-intro">
          左侧负责对话澄清，中间负责生成和 DIY 编辑，右侧持续显示最近行程、配套推荐和下一步动作，
          让整个旅程推进更连贯，而不是多个彼此孤立的页面。
        </p>

        <div class="hero-pills">
          <span>对话澄清需求</span>
          <span>生成候选方案</span>
          <span>保存为真实行程</span>
          <span>回忆图与游记沉淀</span>
        </div>

        <div class="hero-actions">
          <a-button type="primary" size="large" @click="isLoggedIn ? goTrips() : goLogin()">
            {{ isLoggedIn ? '查看我的行程' : '登录后开始规划' }}
          </a-button>
          <a-button size="large" @click="goInspiration">查看灵感广场</a-button>
        </div>
      </div>

      <div class="hero-visuals">
        <article v-if="workspacePhotos[0]" class="hero-photo hero-photo-main">
          <img :src="photoSrc(workspacePhotos[0])" :alt="workspacePhotos[0].alt || '规划行程氛围图'">
          <div class="photo-caption">
            <strong>真实场景承接工作流</strong>
            <p>用旅行照片把 AI 规划、出行执行和内容沉淀放回真实世界。</p>
            <PexelsCredit :image="workspacePhotos[0]" />
          </div>
        </article>

        <div class="hero-stack">
          <article class="illustration-card">
            <img :src="illustrations.journey" alt="journey illustration">
            <div>
              <strong>规划中心</strong>
              <p>不是再造一个首页，而是把旅程节点集中呈现。</p>
            </div>
          </article>

          <article v-if="workspacePhotos[1]" class="hero-photo hero-photo-side">
            <img :src="photoSrc(workspacePhotos[1])" :alt="workspacePhotos[1].alt || '旅途执行图'">
            <div class="side-credit">
              <PexelsCredit :image="workspacePhotos[1]" />
            </div>
          </article>
        </div>
      </div>
    </section>

    <section class="metric-grid">
      <article class="metric-card">
        <strong>{{ isLoggedIn ? trips.length : '登录后可见' }}</strong>
        <span>已保存行程</span>
        <p>规划中心不做伪数据，只展示你自己的真实资产。</p>
      </article>
      <article class="metric-card">
        <strong>{{ isLoggedIn ? plannedCount : '登录后可见' }}</strong>
        <span>待推进旅程</span>
        <p>从候选方案保存后，继续在行程详情推进执行阶段。</p>
      </article>
      <article class="metric-card">
        <strong>{{ isLoggedIn ? memoryReadyCount : '登录后可见' }}</strong>
        <span>已有回忆图</span>
        <p>把素材处理结果直接沉淀成可展示的作品资产。</p>
      </article>
      <article class="metric-card">
        <strong>{{ isLoggedIn ? readyToPublishCount : '登录后可见' }}</strong>
        <span>待发布内容</span>
        <p>有回忆图但未发布的行程会优先提醒继续完成闭环。</p>
      </article>
    </section>

    <section class="workspace-grid">
      <article class="workspace-panel">
        <div class="panel-head">
          <h2>澄清需求</h2>
        </div>

        <div v-if="isLoggedIn" class="panel-body panel-body-embedded">
          <ChatPage embedded />
        </div>
        <div v-else class="login-gate">
          <img :src="illustrations.travelMode" alt="travel mode illustration">
          <div>
            <strong>登录后进入完整工作流</strong>
            <p>登录后即可保存对话、生成候选方案，并把结果沉淀到行程库。</p>
            <a-button type="primary" @click="goLogin">前往登录</a-button>
          </div>
        </div>
      </article>

      <article class="workspace-panel">
        <div class="panel-head">
          <span class="step-badge">Step 02</span>
          <h2>生成行程</h2>
        </div>

        <div class="panel-body panel-body-embedded">
          <PlannerPage embedded />
        </div>
      </article>

      <aside class="workspace-rail">
        <article class="rail-card">
          <div class="rail-head">
            <div>
              <p class="panel-kicker">旅程推进</p>
              <h3>下一步动作</h3>
            </div>
            <a-button type="link" @click="goTrips">去行程库</a-button>
          </div>

          <ul class="progress-list">
            <li
              v-for="item in progressItems"
              :key="item.key"
              class="progress-item"
              :class="item.tone"
            >
              <div class="progress-index">{{ item.index }}</div>
              <div>
                <strong>{{ item.title }}</strong>
                <p>{{ item.desc }}</p>
              </div>
            </li>
          </ul>
        </article>

        <article class="rail-card">
          <div class="rail-head">
            <div>
              <p class="panel-kicker">最近修改</p>
              <h3>我的行程</h3>
            </div>
            <a-button type="link" @click="goTrips">全部行程</a-button>
          </div>

          <a-spin :spinning="tripLoading">
            <div v-if="recentTrips.length" class="trip-list">
              <button
                v-for="trip in recentTrips"
                :key="trip.id"
                type="button"
                class="trip-item"
                @click="openTrip(trip.id)"
              >
                <div class="trip-item-top">
                  <strong>{{ trip.destination || '未命名行程' }}</strong>
                  <a-tag :color="normalizeTripStatus(trip.status) === 'completed' ? 'success' : 'processing'">
                    {{ getTripStatusLabel(trip.status) }}
                  </a-tag>
                </div>
                <p>{{ trip.days || 0 }} 天 · {{ trip.theme || '通用主题' }}</p>
                <div class="trip-item-meta">
                  <span>{{ hasTripMemoryCard(trip) ? '已有回忆图' : '待生成回忆图' }}</span>
                  <span>{{ isTripPublished(trip) ? '已发布' : '未发布' }}</span>
                </div>
                <span class="trip-time">更新于 {{ formatWorkflowDateTime(trip.updateTime || trip.createTime) }}</span>
              </button>
            </div>
            <a-empty v-else description="还没有真实行程，先在中间区域生成第一版方案" />
          </a-spin>
        </article>

        <article class="rail-card">
          <div class="rail-head">
            <div>
              <p class="panel-kicker">配套推荐</p>
              <h3>围绕行程的小模块</h3>
            </div>
          </div>

          <a-spin :spinning="merchantLoading">
            <div v-if="recommendationCards.length" class="merchant-list">
              <article v-for="merchant in recommendationCards" :key="merchant.id" class="merchant-card">
                <div class="merchant-cover" :class="{ fallback: !merchant.coverUrl }">
                  <img v-if="merchant.coverUrl" :src="merchant.coverUrl" :alt="merchant.name || '商家封面'">
                  <img v-else :src="illustrations.aircraft" :alt="merchant.name || '推荐插画'">
                </div>
                <div class="merchant-body">
                  <strong>{{ merchant.name || '旅行配套服务' }}</strong>
                  <p>{{ merchant.location || merchant.introduction || '围绕你的行程继续补足住宿、餐饮和体验。' }}</p>
                  <div class="merchant-meta">
                    <span v-if="merchant.rating">评分 {{ merchant.rating.toFixed(1) }}</span>
                    <span>{{ formatPrice(merchant) }}</span>
                  </div>
                  <div v-if="parseTags(merchant).length" class="merchant-tags">
                    <span v-for="tag in parseTags(merchant)" :key="tag">{{ tag }}</span>
                  </div>
                </div>
              </article>
            </div>
            <a-empty v-else description="当前没有可展示的推荐商家" />
          </a-spin>
        </article>

        <article v-if="workspacePhotos[2]" class="rail-card inspiration-card">
          <img :src="photoSrc(workspacePhotos[2])" :alt="workspacePhotos[2].alt || '旅行灵感图'">
          <div class="inspiration-copy">
            <strong>最终目标不是生成页面，而是沉淀内容资产。</strong>
            <p>保存行程、上传照片、生成回忆图，再发布到灵感广场，让一次旅行留下可以分享和回看的内容。</p>
            <PexelsCredit :image="workspacePhotos[2]" compact />
          </div>
        </article>
      </aside>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getRecommendedMerchants } from '@/api/merchantController'
import { getMyTrips } from '@/api/tripController'
import PexelsCredit from '@/components/content/PexelsCredit.vue'
import { useVisualContent } from '@/composables/useVisualContent'
import PlannerPage from '@/pages/ai/PlannerPage.vue'
import ChatPage from '@/pages/user/ChatPage.vue'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import {
  formatWorkflowDateTime,
  getTripStatusLabel,
  hasTripMemoryCard,
  isTripPublished,
  normalizeTripStatus,
} from '@/utils/tripWorkflow'

const router = useRouter()
const loginUserStore = useLoginUserStore()
const { fetchCollection, illustrations } = useVisualContent()

const workspacePhotos = ref<API.ContentImageVO[]>([])
const trips = ref<API.TripVO[]>([])
const merchants = ref<API.Merchant[]>([])
const tripLoading = ref(false)
const merchantLoading = ref(false)

const isLoggedIn = computed(() => Boolean(loginUserStore.loginUser.id))

const plannedCount = computed(
  () => trips.value.filter((trip) => normalizeTripStatus(trip.status) === 'planned').length
)
const memoryReadyCount = computed(() => trips.value.filter((trip) => hasTripMemoryCard(trip)).length)
const readyToPublishCount = computed(
  () => trips.value.filter((trip) => hasTripMemoryCard(trip) && !isTripPublished(trip)).length
)

const recentTrips = computed(() =>
  [...trips.value]
    .sort((a, b) => {
      const left = new Date(b.updateTime || b.createTime || 0).getTime()
      const right = new Date(a.updateTime || a.createTime || 0).getTime()
      return left - right
    })
    .slice(0, 4)
)

const recommendationCards = computed(() => merchants.value.slice(0, 3))

const progressItems = computed(() => [
  {
    key: 'plan',
    index: '01',
    title: trips.value.length ? '继续优化已保存行程' : '先生成第一版候选方案',
    desc: trips.value.length
      ? `当前已有 ${plannedCount.value} 个待推进行程，下一步去详情页补素材。`
      : '从左侧对话和中部候选方案开始，先把需求转成结构化路线。',
    tone: trips.value.length ? 'done' : 'focus',
  },
  {
    key: 'assets',
    index: '02',
    title: memoryReadyCount.value ? '已有内容资产可继续沉淀' : '补充照片并生成回忆图',
    desc: memoryReadyCount.value
      ? `已有 ${memoryReadyCount.value} 个行程完成回忆图生成。`
      : '行程详情页会继续承接照片管理和回忆图生成。',
    tone: memoryReadyCount.value ? 'done' : 'focus',
  },
  {
    key: 'publish',
    index: '03',
    title: readyToPublishCount.value ? '优先发布待沉淀内容' : '完成后发布到灵感广场',
    desc: readyToPublishCount.value
      ? `还有 ${readyToPublishCount.value} 个行程具备发布条件但尚未沉淀。`
      : '生成成功的回忆图会自动成为灵感内容封面。',
    tone: readyToPublishCount.value ? 'focus' : 'calm',
  },
])

function photoSrc(image?: API.ContentImageVO | null) {
  return image?.landscapeUrl || image?.large2xUrl || image?.largeUrl || image?.mediumUrl || ''
}

function parseTags(merchant: API.Merchant) {
  return [merchant.tags, merchant.featureTags]
    .filter(Boolean)
    .flatMap((value) => String(value).split(/[，,]/))
    .map((item) => item.trim())
    .filter(Boolean)
    .slice(0, 3)
}

function formatPrice(merchant: API.Merchant) {
  if (merchant.minPrice && merchant.maxPrice) {
    return `¥${merchant.minPrice} - ¥${merchant.maxPrice}`
  }
  if (merchant.minPrice) {
    return `¥${merchant.minPrice} 起`
  }
  if (merchant.maxPrice) {
    return `最高 ¥${merchant.maxPrice}`
  }
  return '价格待补充'
}

async function loadVisuals() {
  workspacePhotos.value = await fetchCollection('travel itinerary planning', 3)
}

async function loadTrips() {
  if (!isLoggedIn.value) {
    trips.value = []
    return
  }

  tripLoading.value = true
  try {
    const response = await getMyTrips()
    trips.value = response?.data?.data || []
  } catch (error: any) {
    trips.value = []
    const errorMsg = error?.response?.data?.message || error?.message || '加载行程失败'
    message.error(errorMsg)
  } finally {
    tripLoading.value = false
  }
}

async function loadMerchants() {
  merchantLoading.value = true
  try {
    const response = await getRecommendedMerchants({ limit: 4 })
    merchants.value = response?.data?.data || []
  } catch {
    merchants.value = []
  } finally {
    merchantLoading.value = false
  }
}

function goTrips() {
  router.push('/trips')
}

function goInspiration() {
  router.push('/inspiration')
}

function goLogin() {
  router.push('/user/login?redirect=/workspace')
}

function openTrip(id?: number) {
  if (!id) {
    return
  }
  router.push(`/trips/${id}`)
}

onMounted(async () => {
  await Promise.all([loadVisuals(), loadMerchants(), loadTrips()])
})
</script>

<style scoped lang="scss">
.workspace-page {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.workspace-hero {
  display: grid;
  grid-template-columns: minmax(0, 1.04fr) minmax(360px, 0.96fr);
  gap: 24px;
  align-items: stretch;
  padding: 34px;
  border-radius: 32px;
  border: 1px solid rgba(15, 28, 46, 0.08);
  background:
    radial-gradient(circle at top left, rgba(47, 144, 240, 0.18), transparent 32%),
    radial-gradient(circle at bottom right, rgba(212, 139, 31, 0.14), transparent 28%),
    linear-gradient(135deg, #ffffff 0%, #f3f8ff 58%, #fff8ef 100%);

  h1 {
    margin: 8px 0 14px;
    font-size: 46px;
    line-height: 1.08;
    color: var(--color-text);
  }
}

.eyebrow,
.panel-kicker {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.14em;
  color: var(--primary-600);
}

.hero-intro {
  max-width: 720px;
  margin: 0;
  color: var(--color-text-secondary);
  font-size: 16px;
}

.hero-pills {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 22px;

  span {
    padding: 10px 14px;
    border-radius: 999px;
    background: rgba(255, 255, 255, 0.76);
    border: 1px solid rgba(15, 28, 46, 0.08);
    color: var(--color-text-secondary);
    font-size: 13px;
  }
}

.hero-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: 26px;
}

.hero-visuals {
  display: grid;
  grid-template-columns: minmax(0, 1.06fr) minmax(220px, 0.94fr);
  gap: 14px;
  min-height: 420px;
}

.hero-photo,
.illustration-card,
.metric-card,
.workspace-panel,
.rail-card {
  border-radius: 26px;
  border: 1px solid rgba(15, 28, 46, 0.08);
  background: #ffffff;
  box-shadow: 0 16px 42px rgba(18, 52, 97, 0.06);
}

.hero-photo {
  position: relative;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
}

.hero-photo-main {
  min-height: 420px;
}

.hero-stack {
  display: grid;
  gap: 14px;
  grid-template-rows: auto 1fr;
}

.photo-caption {
  position: absolute;
  inset: auto 0 0 0;
  padding: 18px;
  background: linear-gradient(180deg, rgba(15, 28, 46, 0.08) 0%, rgba(15, 28, 46, 0.86) 100%);

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

.hero-photo-side {
  min-height: 220px;
}

.side-credit {
  position: absolute;
  left: 14px;
  bottom: 12px;
}

.illustration-card {
  display: grid;
  justify-items: center;
  align-content: center;
  gap: 10px;
  padding: 22px 18px;
  text-align: center;
  background: linear-gradient(180deg, #ffffff 0%, #eef5ff 100%);

  img {
    width: min(100%, 220px);
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

.metric-grid,
.workspace-grid {
  display: grid;
  gap: 18px;
}

.metric-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.metric-card {
  padding: 22px;

  strong,
  span {
    display: block;
  }

  strong {
    font-size: 28px;
    color: var(--color-text);
  }

  span {
    margin-top: 8px;
    font-weight: 600;
    color: var(--color-text-secondary);
  }

  p {
    margin: 10px 0 0;
    color: var(--color-muted);
  }
}

.workspace-grid {
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr) 340px;
  align-items: start;
}

.workspace-panel,
.rail-card {
  padding: 0;
  overflow: hidden;
}

.panel-head,
.rail-head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 18px 24px;

  h2,
  h3 {
    margin: 0;
    color: var(--color-text);
  }

  p:not(.panel-kicker) {
    margin: 8px 0 0;
    color: var(--color-muted);
  }
}

.step-badge {
  padding: 4px 10px;
  border-radius: 6px;
  background: var(--primary-600);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
}

.panel-body {
  padding: 18px 24px 24px;
}

.panel-body-embedded {
  padding-top: 16px;
}

.login-gate {
  display: grid;
  gap: 18px;
  align-items: center;
  justify-items: center;
  padding: 28px 24px 30px;
  text-align: center;

  img {
    width: min(100%, 260px);
    height: auto;
  }

  strong {
    display: block;
    color: var(--color-text);
    font-size: 20px;
  }

  p {
    margin: 8px 0 18px;
    color: var(--color-muted);
  }
}

.workspace-rail {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.rail-card {
  padding: 22px;
}

.rail-head {
  padding: 0;
  margin-bottom: 16px;
}

.progress-list,
.trip-list,
.merchant-list {
  display: grid;
  gap: 12px;
}

.progress-list {
  margin: 0;
  padding: 0;
  list-style: none;
}

.progress-item {
  display: grid;
  grid-template-columns: 42px 1fr;
  gap: 12px;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid rgba(15, 28, 46, 0.08);
  background: #f8fbff;

  strong {
    display: block;
    color: var(--color-text);
  }

  p {
    margin: 6px 0 0;
    color: var(--color-muted);
    font-size: 13px;
  }

  &.done {
    background: #eef8f2;
  }

  &.focus {
    background: #eef5ff;
  }

  &.calm {
    background: #fff8ef;
  }
}

.progress-index {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 14px;
  background: rgba(59, 110, 220, 0.1);
  color: var(--primary-700);
  font-weight: 700;
}

.trip-item {
  width: 100%;
  padding: 16px;
  border-radius: 18px;
  border: 1px solid rgba(15, 28, 46, 0.08);
  background: #f9fbff;
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;

  &:hover {
    transform: translateY(-2px);
    border-color: rgba(59, 110, 220, 0.22);
    box-shadow: 0 12px 28px rgba(18, 52, 97, 0.08);
  }

  p {
    margin: 8px 0 10px;
    color: var(--color-muted);
  }
}

.trip-item-top,
.trip-item-meta,
.merchant-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.trip-item-top strong,
.merchant-body strong {
  color: var(--color-text);
}

.trip-item-meta,
.trip-time,
.merchant-meta {
  color: var(--color-muted);
  font-size: 13px;
}

.trip-time {
  display: block;
  margin-top: 10px;
}

.merchant-card {
  display: grid;
  grid-template-columns: 96px 1fr;
  gap: 14px;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid rgba(15, 28, 46, 0.08);
  background: #f9fbff;
}

.merchant-cover {
  overflow: hidden;
  border-radius: 16px;
  background: linear-gradient(180deg, #eef5ff 0%, #ffffff 100%);

  img {
    width: 100%;
    height: 100%;
    min-height: 96px;
    object-fit: cover;
    display: block;
  }

  &.fallback {
    padding: 10px;

    img {
      object-fit: contain;
    }
  }
}

.merchant-body {
  p {
    margin: 8px 0 10px;
    color: var(--color-muted);
    font-size: 13px;
  }
}

.merchant-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 10px;

  span {
    padding: 6px 10px;
    border-radius: 999px;
    background: rgba(59, 110, 220, 0.08);
    color: var(--primary-700);
    font-size: 12px;
  }
}

.inspiration-card {
  padding: 0;
  overflow: hidden;

  img {
    width: 100%;
    height: 180px;
    object-fit: cover;
    display: block;
  }
}

.inspiration-copy {
  padding: 18px;

  strong {
    display: block;
    color: var(--color-text);
  }

  p {
    margin: 8px 0 12px;
    color: var(--color-muted);
  }
}

@media (max-width: 1400px) {
  .workspace-grid {
    grid-template-columns: 1fr;
  }

  .workspace-rail {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1200px) {
  .workspace-hero,
  .metric-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 900px) {
  .hero-visuals,
  .workspace-rail {
    grid-template-columns: 1fr;
  }

  .merchant-card {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .workspace-hero {
    padding: 24px 18px;

    h1 {
      font-size: 34px;
    }
  }

  .panel-head,
  .panel-body,
  .rail-card {
    padding-left: 18px;
    padding-right: 18px;
  }

  .panel-head {
    padding-top: 18px;
  }

  .hero-actions,
  .trip-item-top,
  .trip-item-meta,
  .merchant-meta {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
