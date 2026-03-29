<template>
  <div class="trips-page">
    <section class="page-header">
      <div>
        <p class="eyebrow">我的行程</p>
        <h1>行程库只保留旅程推进所需的信息。</h1>
        <p>支持按状态、是否已发布筛选，并默认按最近修改排序。</p>
      </div>
      <a-button type="primary" size="large" @click="goWorkspace">新建行程</a-button>
    </section>

    <section class="summary-grid">
      <div class="summary-card">
        <strong>{{ trips.length }}</strong>
        <span>全部行程</span>
      </div>
      <div class="summary-card">
        <strong>{{ plannedCount }}</strong>
        <span>待出行</span>
      </div>
      <div class="summary-card">
        <strong>{{ publishedCount }}</strong>
        <span>已发布到灵感广场</span>
      </div>
    </section>

    <section class="filter-bar">
      <a-input
        v-model:value="keyword"
        allow-clear
        placeholder="搜索目的地或主题"
        class="search-input"
      />
      <a-select v-model:value="statusFilter" class="filter-item">
        <a-select-option value="all">全部状态</a-select-option>
        <a-select-option value="planned">待出行</a-select-option>
        <a-select-option value="completed">已完成</a-select-option>
      </a-select>
      <a-select v-model:value="assetFilter" class="filter-item">
        <a-select-option value="all">全部资产</a-select-option>
        <a-select-option value="published">已发布</a-select-option>
      </a-select>
    </section>

    <section v-if="filteredTrips.length" class="trip-grid">
      <button
        v-for="trip in filteredTrips"
        :key="trip.id"
        type="button"
        class="trip-card"
        @click="openTrip(trip.id)"
      >
        <div class="trip-top">
          <div>
            <h2>{{ trip.destination || '未命名行程' }}</h2>
            <p>{{ trip.days || 0 }} 天 · {{ trip.theme || '通用主题' }}</p>
          </div>
          <a-tag :color="getTripStatusTone(trip.status)">
            {{ getTripStatusLabel(trip.status) }}
          </a-tag>
        </div>

        <div class="trip-meta">
          <span>更新时间：{{ formatWorkflowDateTime(trip.updateTime || trip.createTime) }}</span>
          <span>{{ trip.startDate ? `出发：${formatWorkflowDate(trip.startDate)}` : '出发时间待定' }}</span>
        </div>

        <div v-if="highlightPreview(trip).length" class="highlights">
          <span v-for="item in highlightPreview(trip)" :key="item" class="highlight-pill">
            {{ item }}
          </span>
        </div>

        <div class="trip-foot">
          <span>{{ isTripPublished(trip) ? '已发布' : '未发布' }}</span>
          <span>{{ trip.photos?.length || 0 }} 张照片</span>
        </div>
      </button>
    </section>

    <a-empty v-else description="没有符合条件的行程，先去规划行程生成第一版方案" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { getMyTrips } from '@/api/tripController'
import {
  formatWorkflowDate,
  formatWorkflowDateTime,
  getTripStatusLabel,
  getTripStatusTone,
  isTripPublished,
  normalizeTripStatus,
} from '@/utils/tripWorkflow'

const router = useRouter()
const trips = ref<API.TripVO[]>([])
const keyword = ref('')
const statusFilter = ref<'all' | 'planned' | 'completed'>('all')
const assetFilter = ref<'all' | 'published'>('all')

const plannedCount = computed(
  () => trips.value.filter((trip) => normalizeTripStatus(trip.status) === 'planned').length
)
const publishedCount = computed(() => trips.value.filter((trip) => isTripPublished(trip)).length)

const filteredTrips = computed(() => {
  const text = keyword.value.trim().toLowerCase()

  return [...trips.value]
    .filter((trip) => {
      if (statusFilter.value !== 'all' && normalizeTripStatus(trip.status) !== statusFilter.value) {
        return false
      }

      if (assetFilter.value === 'published' && !isTripPublished(trip)) {
        return false
      }

      if (!text) {
        return true
      }

      const destination = trip.destination?.toLowerCase() || ''
      const theme = trip.theme?.toLowerCase() || ''
      return destination.includes(text) || theme.includes(text)
    })
    .sort((a, b) => {
      const left = new Date(b.updateTime || b.createTime || 0).getTime()
      const right = new Date(a.updateTime || a.createTime || 0).getTime()
      return left - right
    })
})

function highlightPreview(trip: API.TripVO) {
  const highlights = trip.dailyHighlights || {}
  return Object.values(highlights)
    .flat()
    .filter(Boolean)
    .slice(0, 4)
}

async function loadTrips() {
  try {
    const resp = await getMyTrips()
    trips.value = resp?.data?.data || []
  } catch (error: any) {
    const errorMsg = error?.response?.data?.message || error?.message || '加载行程失败'
    message.error(errorMsg)
  }
}

function openTrip(id?: number) {
  if (!id) {
    return
  }
  router.push(`/trips/${id}`)
}

function goWorkspace() {
  router.push('/workspace')
}

onMounted(loadTrips)
</script>

<style scoped lang="scss">
.trips-page {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  padding: 28px 30px;
  border-radius: 28px;
  border: 1px solid rgba(15, 28, 46, 0.08);
  background:
    radial-gradient(circle at top right, rgba(47, 144, 240, 0.14), transparent 30%),
    linear-gradient(135deg, #ffffff 0%, #f2f7ff 100%);

  h1 {
    margin: 8px 0 10px;
    font-size: 34px;
    color: var(--color-text);
  }

  p {
    margin: 0;
    color: var(--color-muted);
  }
}

.eyebrow {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.14em;
  color: var(--primary-600);
}

.summary-grid,
.trip-grid {
  display: grid;
  gap: 16px;
}

.summary-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.summary-card {
  padding: 20px 22px;
  border-radius: 22px;
  border: 1px solid rgba(15, 28, 46, 0.08);
  background: #ffffff;
  box-shadow: 0 14px 36px rgba(18, 52, 97, 0.05);

  strong {
    display: block;
    font-size: 32px;
    color: var(--color-text);
  }

  span {
    color: var(--color-muted);
  }
}

.filter-bar {
  display: grid;
  grid-template-columns: minmax(260px, 1fr) 180px 180px;
  gap: 14px;
}

.search-input,
.filter-item {
  width: 100%;
}

.trip-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.trip-card {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 22px;
  border-radius: 24px;
  border: 1px solid rgba(15, 28, 46, 0.08);
  background: #ffffff;
  text-align: left;
  cursor: pointer;
  box-shadow: 0 16px 40px rgba(18, 52, 97, 0.05);
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;

  &:hover {
    transform: translateY(-2px);
    border-color: rgba(59, 110, 220, 0.24);
    box-shadow: 0 22px 48px rgba(18, 52, 97, 0.08);
  }

  h2 {
    margin: 0;
    font-size: 22px;
    color: var(--color-text);
  }

  p {
    margin: 6px 0 0;
    color: var(--color-text-secondary);
  }
}

.trip-top,
.trip-meta,
.trip-foot {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
}

.trip-meta,
.trip-foot {
  font-size: 13px;
  color: var(--color-subtle);
}

.highlights {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.highlight-pill {
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(59, 110, 220, 0.08);
  color: var(--primary-700);
  font-size: 12px;
  font-weight: 600;
}

@media (max-width: 1100px) {
  .summary-grid,
  .trip-grid,
  .filter-bar {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    padding: 22px 20px;

    h1 {
      font-size: 28px;
    }
  }
}
</style>
