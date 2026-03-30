<template>
  <div class="ai-admin-monitor-page">
    <section class="monitor-hero">
      <div>
        <p class="eyebrow">Admin Console</p>
        <h1>AI / Milvus 运行监控</h1>
        <p class="subtitle">支持手动同步知识库、查询可检索条数，并展示关键运行状态。</p>
      </div>
      <div class="hero-actions">
        <a-space>
          <a-input-number
            v-model:value="queryLimit"
            :min="1"
            :max="5000"
            :precision="0"
            addon-before="limit"
          />
          <a-button :loading="queryLoading" @click="refreshQueryCount">刷新条数</a-button>
        </a-space>
      </div>
    </section>

    <section class="status-grid">
      <article class="status-card">
        <header>
          <span class="dot" :class="healthDotClass" />
          <h3>服务健康</h3>
        </header>
        <p class="primary-value">{{ healthStatus }}</p>
        <p class="secondary">{{ healthText }}</p>
      </article>

      <article class="status-card">
        <header>
          <span class="dot" :class="queryDotClass" />
          <h3>Milvus 可查询条数</h3>
        </header>
        <p class="primary-value">{{ queryCountDisplay }}</p>
        <p class="secondary">统计方式：{{ countByDisplay }}</p>
      </article>

      <article class="status-card">
        <header>
          <span class="dot" :class="syncDotClass" />
          <h3>最近同步状态</h3>
        </header>
        <p class="primary-value">{{ syncStatusText }}</p>
        <p class="secondary">{{ syncSummaryText }}</p>
      </article>
    </section>

    <section class="panel">
      <header class="panel-header">
        <h2>Milvus 同步操作</h2>
        <a-space>
          <a-switch v-model:checked="recreateCollection">
            <template #checkedChildren>重建集合</template>
            <template #unCheckedChildren>增量覆盖</template>
          </a-switch>
          <a-button type="primary" :loading="syncLoading" @click="handleSyncMilvus">
            触发知识库同步
          </a-button>
        </a-space>
      </header>

      <div class="sync-meta" v-if="lastSyncResult">
        <a-descriptions :column="2" bordered size="small">
          <a-descriptions-item label="status">
            {{ String(lastSyncResult.status ?? '--') }}
          </a-descriptions-item>
          <a-descriptions-item label="collection">
            {{ String(lastSyncResult.collection ?? '--') }}
          </a-descriptions-item>
          <a-descriptions-item label="preparedDocs">
            {{ String(lastSyncResult.preparedDocs ?? '--') }}
          </a-descriptions-item>
          <a-descriptions-item label="upsertedDocs">
            {{ String(lastSyncResult.upsertedDocs ?? '--') }}
          </a-descriptions-item>
          <a-descriptions-item label="failedBatches">
            {{ String(lastSyncResult.failedBatches ?? '--') }}
          </a-descriptions-item>
          <a-descriptions-item label="loaded">
            {{ String(lastSyncResult.loaded ?? '--') }}
          </a-descriptions-item>
        </a-descriptions>
      </div>
    </section>

    <section class="panel">
      <header class="panel-header">
        <h2>Milvus 计数详情</h2>
      </header>
      <a-descriptions :column="2" bordered size="small" v-if="queryResult">
        <a-descriptions-item label="queryCount">
          {{ String(queryResult.queryCount ?? '--') }}
        </a-descriptions-item>
        <a-descriptions-item label="statsRowCount">
          {{ String(queryResult.statsRowCount ?? '--') }}
        </a-descriptions-item>
        <a-descriptions-item label="truncated">
          {{ String(queryResult.truncated ?? '--') }}
        </a-descriptions-item>
        <a-descriptions-item label="limit">
          {{ String(queryResult.limit ?? '--') }}
        </a-descriptions-item>
        <a-descriptions-item label="collection">
          {{ String(queryResult.collection ?? '--') }}
        </a-descriptions-item>
        <a-descriptions-item label="primaryField">
          {{ String(queryResult.primaryField ?? '--') }}
        </a-descriptions-item>
      </a-descriptions>

      <div v-if="sampleIds.length > 0" class="sample-box">
        <h4>sampleIds</h4>
        <div class="sample-list">
          <span v-for="item in sampleIds" :key="item">{{ item }}</span>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { health, queryMilvusCount, syncMilvusKnowledge } from '@/api/monitoringController'

const queryLimit = ref(200)
const recreateCollection = ref(false)

const healthLoading = ref(false)
const queryLoading = ref(false)
const syncLoading = ref(false)

const healthData = ref<Record<string, unknown> | null>(null)
const queryResult = ref<Record<string, unknown> | null>(null)
const lastSyncResult = ref<Record<string, unknown> | null>(null)

const healthStatus = computed(() => {
  if (!healthData.value) {
    return '--'
  }
  return String(healthData.value.status ?? '--')
})

const healthText = computed(() => {
  if (!healthData.value) {
    return '尚未拉取健康状态'
  }
  const service = String(healthData.value.service ?? 'AI Module')
  const timestamp = healthData.value.timestamp
  if (!timestamp) {
    return service
  }
  const date = new Date(Number(timestamp))
  return `${service} · ${Number.isNaN(date.getTime()) ? '--' : date.toLocaleString()}`
})

const queryCountDisplay = computed(() => {
  if (!queryResult.value) {
    return '--'
  }
  return String(queryResult.value.queryCount ?? '--')
})

const countByDisplay = computed(() => {
  if (!queryResult.value) {
    return '--'
  }
  return String(queryResult.value.countBy ?? 'entities/query')
})

const syncStatusText = computed(() => {
  if (!lastSyncResult.value) {
    return '未执行'
  }
  return String(lastSyncResult.value.status ?? 'unknown')
})

const syncSummaryText = computed(() => {
  if (!lastSyncResult.value) {
    return '尚未触发同步任务'
  }
  const upserted = String(lastSyncResult.value.upsertedDocs ?? '--')
  const failedBatches = String(lastSyncResult.value.failedBatches ?? '--')
  return `upserted=${upserted}, failedBatches=${failedBatches}`
})

const healthDotClass = computed(() => {
  const value = healthStatus.value.toLowerCase()
  if (value === 'up') {
    return 'ok'
  }
  if (value === '--') {
    return 'idle'
  }
  return 'error'
})

const queryDotClass = computed(() => {
  if (!queryResult.value) {
    return 'idle'
  }
  const status = String(queryResult.value.status ?? '').toLowerCase()
  if (status === 'success') {
    return 'ok'
  }
  if (status === 'failed') {
    return 'error'
  }
  return 'idle'
})

const syncDotClass = computed(() => {
  if (!lastSyncResult.value) {
    return 'idle'
  }
  const status = String(lastSyncResult.value.status ?? '').toLowerCase()
  if (status === 'success' || status === 'partial') {
    return 'ok'
  }
  if (status === 'failed') {
    return 'error'
  }
  return 'idle'
})

const sampleIds = computed(() => {
  const raw = queryResult.value?.sampleIds
  return Array.isArray(raw) ? raw.map((item) => String(item)) : []
})

async function refreshHealth() {
  healthLoading.value = true
  try {
    const res = await health()
    if (res.data.code !== 0 || !res.data.data) {
      throw new Error(res.data.message || '健康检查失败')
    }
    healthData.value = res.data.data
  } catch (error: unknown) {
    const msg = error instanceof Error ? error.message : '健康检查失败'
    message.error(msg)
  } finally {
    healthLoading.value = false
  }
}

async function refreshQueryCount() {
  queryLoading.value = true
  try {
    const res = await queryMilvusCount({ limit: queryLimit.value })
    if (res.data.code !== 0 || !res.data.data) {
      throw new Error(res.data.message || '查询条数失败')
    }
    queryResult.value = res.data.data
    if (String(res.data.data.status ?? '').toLowerCase() !== 'success') {
      message.warning('Milvus 查询返回非成功状态，请查看详情')
    }
  } catch (error: unknown) {
    const msg = error instanceof Error ? error.message : '查询条数失败'
    message.error(msg)
  } finally {
    queryLoading.value = false
  }
}

async function handleSyncMilvus() {
  syncLoading.value = true
  try {
    const res = await syncMilvusKnowledge({ recreateCollection: recreateCollection.value })
    if (res.data.code !== 0 || !res.data.data) {
      throw new Error(res.data.message || '同步执行失败')
    }
    lastSyncResult.value = res.data.data
    const status = String(res.data.data.status ?? '').toLowerCase()
    if (status === 'success' || status === 'partial') {
      message.success('Milvus 同步执行完成')
    } else {
      message.warning(`Milvus 同步结束，状态: ${status || 'unknown'}`)
    }
    await refreshQueryCount()
  } catch (error: unknown) {
    const msg = error instanceof Error ? error.message : '同步执行失败'
    message.error(msg)
  } finally {
    syncLoading.value = false
  }
}

onMounted(async () => {
  await refreshHealth()
  await refreshQueryCount()
})
</script>

<style scoped lang="scss">
.ai-admin-monitor-page {
  display: grid;
  gap: 18px;
}

.monitor-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
  padding: 22px;
  border-radius: 16px;
  background: linear-gradient(135deg, #163056 0%, #2f6fda 55%, #56a8ff 100%);
  color: #ffffff;

  .eyebrow {
    font-size: 12px;
    letter-spacing: 0.08em;
    text-transform: uppercase;
    opacity: 0.9;
  }

  h1 {
    margin-top: 4px;
    font-size: 28px;
    line-height: 1.2;
  }

  .subtitle {
    margin-top: 8px;
    opacity: 0.92;
  }

  .hero-actions {
    :deep(.ant-input-number) {
      min-width: 150px;
    }
  }
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.status-card {
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid rgba(15, 28, 46, 0.08);
  padding: 16px;

  header {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  h3 {
    font-size: 15px;
    color: #223046;
  }

  .primary-value {
    margin-top: 10px;
    font-size: 26px;
    font-weight: 700;
    color: #0f1c2e;
  }

  .secondary {
    margin-top: 6px;
    color: #5b6475;
    font-size: 13px;
  }
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  display: inline-block;

  &.idle {
    background: #9aa6b2;
  }

  &.ok {
    background: #1ea672;
    box-shadow: 0 0 0 6px rgba(30, 166, 114, 0.16);
  }

  &.error {
    background: #d64545;
    box-shadow: 0 0 0 6px rgba(214, 69, 69, 0.16);
  }
}

.panel {
  background: #ffffff;
  border-radius: 14px;
  border: 1px solid rgba(15, 28, 46, 0.08);
  padding: 16px;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;

  h2 {
    font-size: 17px;
    color: #0f1c2e;
  }
}

.sync-meta {
  margin-top: 10px;
}

.sample-box {
  margin-top: 14px;
  padding: 12px;
  border-radius: 10px;
  border: 1px solid rgba(15, 28, 46, 0.08);
  background: #f8fbff;

  h4 {
    color: #223046;
    margin-bottom: 8px;
  }
}

.sample-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  span {
    padding: 4px 8px;
    border-radius: 999px;
    font-size: 12px;
    color: #1f3476;
    background: #eaf1ff;
    border: 1px solid #adc4ff;
  }
}

@media (max-width: 1080px) {
  .monitor-hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .status-grid {
    grid-template-columns: 1fr;
  }

  .panel-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}
</style>
