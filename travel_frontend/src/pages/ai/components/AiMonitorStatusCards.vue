<template>
  <a-row :gutter="24" class="status-cards">
    <a-col :xs="24" :lg="8">
      <div class="dash-card">
        <div class="dash-icon" style="background: #e6f7ff; color: #1890ff;">
          <HeartOutlined />
        </div>
        <div class="dash-content">
          <div class="dash-title">服务健康状态</div>
          <div class="dash-value">{{ healthStatus }} <a-tag :color="healthDotColor" class="dash-tag">{{ healthStatus === 'UP' ? '正常' : (healthStatus === '--' ? '未加载' : '异常') }}</a-tag></div>
          <div class="dash-footer">{{ healthText }}</div>
        </div>
      </div>
    </a-col>

    <a-col :xs="24" :lg="8">
      <div class="dash-card">
        <div class="dash-icon" style="background: #f6ffed; color: #52c41a;">
          <DatabaseOutlined />
        </div>
        <div class="dash-content">
          <div class="dash-title">Milvus 可查询条数</div>
          <div class="dash-value">{{ queryCountDisplay }} <span style="font-size: 14px; color: #8c8c8c; font-weight: normal;">条</span></div>
          <div class="dash-footer">统计方式：{{ countByDisplay }}</div>
        </div>
      </div>
    </a-col>

    <a-col :xs="24" :lg="8">
      <div class="dash-card">
        <div class="dash-icon" style="background: #fffb8f; color: #faad14;">
          <SyncOutlined :spin="syncLoading" />
        </div>
        <div class="dash-content">
          <div class="dash-title">KB 最近同步状态</div>
          <div class="dash-value" :style="{ color: syncDotColor }">{{ syncStatusText }}</div>
          <div class="dash-footer">{{ syncSummaryText }}</div>
        </div>
      </div>
    </a-col>
  </a-row>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { HeartOutlined, DatabaseOutlined, SyncOutlined } from '@ant-design/icons-vue'

const props = defineProps<{
  healthData: Record<string, unknown> | null
  queryResult: Record<string, unknown> | null
  lastSyncResult: Record<string, unknown> | null
  syncLoading: boolean
}>()

const healthStatus = computed(() => props.healthData ? String(props.healthData.status ?? '--') : '--')
const healthText = computed(() => {
  if (!props.healthData) return '尚未拉取健康状态'
  const service = String(props.healthData.service ?? 'AI Module')
  const timestamp = props.healthData.timestamp
  if (!timestamp) return service
  const date = new Date(Number(timestamp))
  return `${service} · ${Number.isNaN(date.getTime()) ? '--' : date.toLocaleString()}`
})
const healthDotColor = computed(() => {
  const value = healthStatus.value.toLowerCase()
  if (value === 'up') return '#52c41a'
  if (value === '--') return '#d9d9d9'
  return '#f5222d'
})

const queryCountDisplay = computed(() => props.queryResult ? String(props.queryResult.queryCount ?? '--') : '--')
const countByDisplay = computed(() => props.queryResult ? String(props.queryResult.countBy ?? 'entities/query') : '--')

const syncStatusText = computed(() => props.lastSyncResult ? String(props.lastSyncResult.status ?? 'UNKNOWN').toUpperCase() : '未执行')
const syncSummaryText = computed(() => {
  if (!props.lastSyncResult) return '暂无汇总信息'
  const upserted = String(props.lastSyncResult.upsertedDocs ?? '--')
  const failedBatches = String(props.lastSyncResult.failedBatches ?? '--')
  return `Upserted: ${upserted} 篇，Failed: ${failedBatches} 批次`
})
const syncDotColor = computed(() => {
  if (!props.lastSyncResult) return '#262626'
  const status = String(props.lastSyncResult.status ?? '').toLowerCase()
  if (status === 'success' || status === 'partial') return '#52c41a'
  if (status === 'failed') return '#f5222d'
  return '#262626'
})
</script>

<style scoped>
.status-cards {
  margin-bottom: 24px;
}

.dash-card {
  background: #ffffff;
  border-radius: 8px;
  padding: 24px;
  display: flex;
  align-items: center;
  box-shadow: 0 1px 2px -2px rgba(0, 0, 0, 0.08), 0 3px 6px 0 rgba(0, 0, 0, 0.06), 0 5px 12px 4px rgba(0, 0, 0, 0.04);
  transition: all 0.3s;
}

.dash-card:hover {
  box-shadow: 0 3px 6px -4px rgba(0, 0, 0, 0.12), 0 6px 16px 0 rgba(0, 0, 0, 0.08), 0 9px 28px 8px rgba(0, 0, 0, 0.05);
}

.dash-icon {
  width: 54px;
  height: 54px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-right: 20px;
}

.dash-content {
  flex: 1;
  min-width: 0;
}

.dash-title {
  color: #8c8c8c;
  font-size: 14px;
  margin-bottom: 4px;
}

.dash-value {
  color: #262626;
  font-size: 26px;
  font-weight: 600;
  line-height: 1.2;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
}

.dash-tag {
  margin-left: 8px;
  transform: translateY(-2px);
}

.dash-footer {
  color: #bfbfbf;
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
