<template>
  <div class="ai-admin-monitor-page">
    <a-page-header
      title="AI / Milvus 运行监控"
      sub-title="支持手动同步知识库、查询可检索条数，并展示关键运行状态"
      @back="() => $router.go(-1)"
    >
      <template #extra>
        <a-space>
          <a-input-number
            v-model:value="queryLimit"
            :min="1"
            :max="5000"
            :precision="0"
            addon-before="limit"
          />
          <a-button type="primary" :loading="queryLoading || healthLoading" @click="refreshAll">
            <template #icon><ReloadOutlined /></template>
            刷新状态
          </a-button>
        </a-space>
      </template>
    </a-page-header>

    <div class="monitor-content">
      <!-- Status Cards Row -->
      <a-row :gutter="[16, 16]" class="status-cards">
        <a-col :xs="24" :lg="8">
          <a-card :bordered="false" class="statistic-card">
            <a-statistic :value="healthStatus" title="服务健康状态">
              <template #prefix>
                <HeartOutlined :style="{ color: healthDotColor }" />
              </template>
            </a-statistic>
            <div class="card-footer">
              <span class="secondary-text">{{ healthText }}</span>
              <a-tag :color="healthDotColor">{{ healthStatus === 'UP' ? '正常' : (healthStatus === '--' ? '未加载' : '异常') }}</a-tag>
            </div>
          </a-card>
        </a-col>

        <a-col :xs="24" :lg="8">
          <a-card :bordered="false" class="statistic-card">
            <a-statistic :value="queryCountDisplay" title="Milvus 可查询条数">
              <template #prefix>
                <DatabaseOutlined :style="{ color: queryDotColor }" />
              </template>
              <template #suffix>条</template>
            </a-statistic>
            <div class="card-footer">
              <span class="secondary-text">统计方式：{{ countByDisplay }}</span>
            </div>
          </a-card>
        </a-col>

        <a-col :xs="24" :lg="8">
          <a-card :bordered="false" class="statistic-card">
            <a-statistic :value="syncStatusText" title="最近同步状态">
              <template #prefix>
                <SyncOutlined :style="{ color: syncDotColor }" :spin="syncLoading" />
              </template>
            </a-statistic>
            <div class="card-footer">
              <span class="secondary-text">{{ syncSummaryText }}</span>
            </div>
          </a-card>
        </a-col>
      </a-row>

      <!-- Detail Panels -->
      <a-row :gutter="[16, 16]" class="detail-panels" type="flex">
        <!-- 同步操作面板 -->
        <a-col :xs="24" :xl="12" style="display: flex; flex-direction: column;">
          <a-card title="知识库同步" :bordered="false" class="panel-card" style="flex: 1;">
            <template #extra>
              <a-space>
                <a-switch v-model:checked="recreateCollection" checked-children="重建" un-checked-children="增量" />
                <a-button type="primary" :loading="syncLoading" @click="handleSyncMilvus">
                  <template #icon><CloudUploadOutlined /></template>
                  触发同步
                </a-button>
              </a-space>
            </template>
            
            <a-empty v-if="!lastSyncResult" description="暂无同步记录，请触发同步查看状态" />
            <a-descriptions v-else bordered size="middle" :column="1">
              <a-descriptions-item label="Status">
                <a-tag :color="lastSyncResult.status === 'success' ? 'success' : (lastSyncResult.status === 'partial' ? 'warning' : 'error')">
                  {{ lastSyncResult.status ?? '--' }}
                </a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="Collection">
                {{ lastSyncResult.collection ?? '--' }}
              </a-descriptions-item>
              <a-descriptions-item label="Prepared Docs">
                {{ lastSyncResult.preparedDocs ?? '0' }}
              </a-descriptions-item>
              <a-descriptions-item label="Upserted Docs">
                <span style="color: #52c41a; font-weight: bold;">{{ lastSyncResult.upsertedDocs ?? '0' }}</span>
              </a-descriptions-item>
              <a-descriptions-item label="Failed Batches">
                <span :style="{ color: Number(lastSyncResult.failedBatches) > 0 ? '#f5222d' : 'inherit' }">
                  {{ lastSyncResult.failedBatches ?? '0' }}
                </span>
              </a-descriptions-item>
              <a-descriptions-item label="Loaded">
                <a-tag :color="lastSyncResult.loaded ? 'processing' : 'default'">{{ lastSyncResult.loaded ? 'Yes' : 'No' }}</a-tag>
              </a-descriptions-item>
            </a-descriptions>
          </a-card>
        </a-col>

        <!-- 计数详情面板 -->
        <a-col :xs="24" :xl="12" style="display: flex; flex-direction: column;">
          <a-card title="查询监控详情" :bordered="false" class="panel-card" style="flex: 1;">
            <a-empty v-if="!queryResult" description="暂无查询数据，请刷新获取" />
            <div v-else>
              <a-descriptions bordered size="middle" :column="{ xs: 1, sm: 2 }">
                <a-descriptions-item label="Query Count">
                  <span style="color: #1890ff; font-weight: bold; font-size: 16px;">{{ queryResult.queryCount ?? '--' }}</span>
                </a-descriptions-item>
                <a-descriptions-item label="Stats Row Count">
                  {{ queryResult.statsRowCount ?? '--' }}
                </a-descriptions-item>
                <a-descriptions-item label="Truncated" :span="2">
                  <a-tag :color="queryResult.truncated ? 'warning' : 'blue'">
                    {{ queryResult.truncated ? 'Yes (Reach Limit)' : 'No (Full Details)' }}
                  </a-tag>
                </a-descriptions-item>
                <a-descriptions-item label="Limit" :span="1">
                  {{ queryResult.limit ?? '--' }}
                </a-descriptions-item>
                <a-descriptions-item label="Collection" :span="1">
                  {{ queryResult.collection ?? '--' }}
                </a-descriptions-item>
                <a-descriptions-item label="Primary Field" :span="2">
                  {{ queryResult.primaryField ?? '--' }}
                </a-descriptions-item>
              </a-descriptions>
              
              <div v-if="sampleIds.length > 0" class="sample-box">
                <div class="sample-title">
                  <DatabaseOutlined /> Sample IDs (Top 50)
                </div>
                <div class="sample-list">
                  <a-tag color="cyan" v-for="item in sampleIds.slice(0, 50)" :key="item">{{ item }}</a-tag>
                  <a-tag v-if="sampleIds.length > 50">...and {{ sampleIds.length - 50 }} more</a-tag>
                </div>
              </div>
            </div>
          </a-card>
        </a-col>
      </a-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { health, queryMilvusCount, syncMilvusKnowledge } from '@/api/monitoringController'
import {
  ReloadOutlined,
  HeartOutlined,
  DatabaseOutlined,
  SyncOutlined,
  CloudUploadOutlined
} from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'

const router = useRouter()

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
    return '尚未触发同步任务，暂无汇总信息'
  }
  const upserted = String(lastSyncResult.value.upsertedDocs ?? '--')
  const failedBatches = String(lastSyncResult.value.failedBatches ?? '--')
  return `Upserted: ${upserted} 篇，Failed: ${failedBatches} 批次`
})

const healthDotColor = computed(() => {
  const value = healthStatus.value.toLowerCase()
  if (value === 'up') return '#52c41a'
  if (value === '--') return '#bfbfbf'
  return '#f5222d'
})

const queryDotColor = computed(() => {
  if (!queryResult.value) return '#bfbfbf'
  const status = String(queryResult.value.status ?? '').toLowerCase()
  if (status === 'success') return '#1890ff'
  if (status === 'failed') return '#f5222d'
  return '#bfbfbf'
})

const syncDotColor = computed(() => {
  if (!lastSyncResult.value) return '#bfbfbf'
  const status = String(lastSyncResult.value.status ?? '').toLowerCase()
  if (status === 'success' || status === 'partial') return '#52c41a'
  if (status === 'failed') return '#f5222d'
  return '#bfbfbf'
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

async function refreshAll() {
  await Promise.all([refreshHealth(), refreshQueryCount()]);
  message.success('状态已刷新');
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
  await Promise.all([refreshHealth(), refreshQueryCount()])
})
</script>

<style scoped>
.ai-admin-monitor-page {
  /* Give enough space and look crisp */
}

.monitor-content {
  padding: 0 24px 24px;
}

.status-cards {
  margin-bottom: 24px;
}

.statistic-card {
  border-radius: 12px;
  background: white;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
  transition: all 0.3s ease;
}

.statistic-card:hover {
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.statistic-card :deep(.ant-card-body) {
  padding: 24px 24px 20px 24px;
}

.statistic-card :deep(.ant-statistic-title) {
  font-size: 15px;
  color: #5c6b77;
  font-weight: 500;
  margin-bottom: 12px;
}

.statistic-card :deep(.ant-statistic-content) {
  font-size: 28px;
  font-weight: 600;
  color: #1e293b;
}

.card-footer {
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px solid #f0f2f5;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-footer .secondary-text {
  color: #8492a6;
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 70%;
}

.detail-panels {
  /* flex to align children heights if possible */
  align-items: stretch;
}

.panel-card {
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
}

.panel-card :deep(.ant-card-head-title) {
  font-weight: 600;
  font-size: 16px;
}

.sample-box {
  margin-top: 16px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #e2e8f0;
}

.sample-title {
  font-size: 14px;
  color: #475569;
  margin-bottom: 14px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}

.sample-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

:deep(.ant-page-header) {
  padding-left: 24px;
  padding-right: 24px;
  background-color: transparent;
}
</style>