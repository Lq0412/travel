<template>
  <a-card title="流水线追踪中心" :bordered="false" class="dashboard-card tracking-card">
    <template #extra>
      <div class="search-wrap">
        <a-input-search
          v-model:value="manualTaskId"
          placeholder="追踪 Task ID"
          enter-button="监听溯源"
          @search="handleManualQueryTask"
          :loading="taskStatusLoading"
          style="width: 320px"
        />
      </div>
    </template>

    <div class="tracking-content">
      <div v-if="!currentTaskStatus" class="empty-state">
        <a-empty description="在左侧新建任务或输入 Task ID 进行追踪" :image-style="{ height: '80px' }" />
      </div>

      <div v-else class="tracking-view">
        <div class="task-banner" :class="taskStatusTheme">
          <div class="tb-left">
            <span class="tb-id">ID: {{ currentTaskStatus.taskId }}</span>
            <span class="tb-type">{{ currentTaskStatus.type }}</span>
          </div>
          <div class="tb-status">
            <span class="pulse-dot" v-if="pollingTask"></span>
            {{ taskStatusText }}
          </div>
        </div>

        <a-descriptions bordered size="middle" :column="2" class="detail-desc" style="margin-top: 20px;">
          <a-descriptions-item label="策略通道">{{ taskPayload.dataSource ?? '--' }}</a-descriptions-item>
          <a-descriptions-item label="执行深度">{{ taskPayload.effectPreset ?? '--' }}</a-descriptions-item>
          <a-descriptions-item label="限制/重试">{{ taskPayload.maxItems ?? '--' }} / {{ taskPayload.maxRetry ?? '--' }}</a-descriptions-item>
          <a-descriptions-item label="最近心跳">{{ currentTaskStatus.updateTime ?? '--' }}</a-descriptions-item>
          <a-descriptions-item label="返回来源" :span="2">
            <a-tag color="blue" v-if="taskResult.source">{{ taskResult.source }}</a-tag>
            <span v-else style="color:#bfbfbf;">等待响应装载...</span>
          </a-descriptions-item>
          <a-descriptions-item label="运行时捕获" :span="2" v-if="currentTaskStatus.errorMessage">
            <div class="error-box"><CloseCircleOutlined style="margin-right: 8px;"/> {{ String(currentTaskStatus.errorMessage) }}</div>
          </a-descriptions-item>
        </a-descriptions>
      </div>
    </div>
  </a-card>
</template>

<script setup lang="ts">
import { ref, computed, watch, onBeforeUnmount } from 'vue'
import { message } from 'ant-design-vue'
import { CloseCircleOutlined } from '@ant-design/icons-vue'
import { getAiTaskStatus } from '@/api/monitoringController'

const props = defineProps<{ initialTaskId?: string }>()

const taskStatusLoading = ref(false)
const pollingTask = ref(false)
const manualTaskId = ref(props.initialTaskId || '')
const currentTaskId = ref(props.initialTaskId || '')
const currentTaskStatus = ref<Record<string, unknown> | null>(null)

let taskPollingTimer: number | null = null
let notifiedTerminalTaskId = ''

watch(() => props.initialTaskId, (newVal) => {
  if (newVal) {
    manualTaskId.value = newVal; currentTaskId.value = newVal; notifiedTerminalTaskId = ''
    fetchTaskStatus(newVal, false).then(() => startTaskPolling())
  }
})

const taskPayload = computed(() => parseJson(currentTaskStatus.value?.payload))
const taskResult = computed(() => parseJson(currentTaskStatus.value?.result))
const taskStatusText = computed(() => String(currentTaskStatus.value?.status ?? 'UNKNOWN').toUpperCase())
const taskStatusTheme = computed(() => {
  if (taskStatusText.value === 'SUCCESS') return 'theme-success'
  if (['FAILED', 'CANCELLED'].includes(taskStatusText.value)) return 'theme-error'
  if (taskStatusText.value === 'RUNNING') return 'theme-running'
  return 'theme-default'
})

function parseJson(raw: unknown) {
  try { return (typeof raw === 'string' ? JSON.parse(raw) : raw || {}) as Record<string, unknown> } 
  catch { return {} }
}

async function handleManualQueryTask() {
  if (!manualTaskId.value.trim()) return message.warning('必须输入要追踪的 ID')
  currentTaskId.value = manualTaskId.value.trim()
  await fetchTaskStatus(currentTaskId.value, false)
  if (!['SUCCESS', 'FAILED', 'CANCELLED'].includes(taskStatusText.value)) startTaskPolling()
}

async function fetchTaskStatus(taskId: string, silent: boolean) {
  taskStatusLoading.value = true
  try {
    const res = await getAiTaskStatus(taskId)
    if (res.data.code !== 0 || !res.data.data) throw new Error(res.data.message || '探针错误')
    currentTaskStatus.value = res.data.data as Record<string, unknown>
    
    if (['SUCCESS', 'FAILED', 'CANCELLED'].includes(taskStatusText.value)) {
      stopTaskPolling()
      if (notifiedTerminalTaskId !== taskId) {
        notifiedTerminalTaskId = taskId
        if (taskStatusText.value === 'SUCCESS') message.success('流水线运行竣工')
        else message.error(`异常抛出: ${currentTaskStatus.value.errorMessage || '未知'}`)
      }
    }
  } catch (err: unknown) {
    if (!silent) message.error(err instanceof Error ? err.message : '探针连接失败')
    stopTaskPolling()
  } finally {
    taskStatusLoading.value = false
  }
}

function startTaskPolling() {
  stopTaskPolling()
  pollingTask.value = true
  taskPollingTimer = window.setInterval(() => { if (currentTaskId.value) void fetchTaskStatus(currentTaskId.value, true) }, 3000)
}

function stopTaskPolling() {
  if (taskPollingTimer) { clearInterval(taskPollingTimer); taskPollingTimer = null }
  pollingTask.value = false
}

onBeforeUnmount(() => stopTaskPolling())
</script>

<style scoped>
.dashboard-card {
  border-radius: 8px;
  box-shadow: 0 1px 2px -2px rgba(0, 0, 0, 0.08), 0 3px 6px 0 rgba(0, 0, 0, 0.06);
  height: 100%;
}
.dashboard-card :deep(.ant-card-head-title) { font-weight: 500; font-size: 16px; }

.empty-state {
  padding: 60px 0;
}

.task-banner {
  padding: 16px 20px;
  border-radius: 6px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: all 0.3s;
}

.tb-left { display: flex; align-items: center; gap: 16px; }
.tb-id { font-family: monospace; font-size: 16px; font-weight: 600; }
.tb-type { font-size: 12px; opacity: 0.8; background: rgba(0,0,0,0.05); padding: 2px 8px; border-radius: 4px; }
.tb-status { font-weight: bold; font-size: 15px; display: flex; align-items: center;}

.pulse-dot { display: inline-block; width: 8px; height: 8px; background: currentColor; border-radius: 50%; margin-right: 8px; animation: pulse 1.5s infinite; }
@keyframes pulse { 0% { opacity: 1; transform: scale(1); } 50% { opacity: 0.5; transform: scale(1.5); box-shadow: 0 0 0 4px rgba(255,255,255,0.4); } 100% { opacity: 1; transform: scale(1); } }

.theme-success { background-color: #f6ffed; border: 1px solid #b7eb8f; color: #389e0d; }
.theme-error { background-color: #fff2f0; border: 1px solid #ffccc7; color: #cf1322; }
.theme-running { background-color: #e6f7ff; border: 1px solid #91d5ff; color: #096dd9; }
.theme-default { background-color: #fafafa; border: 1px solid #d9d9d9; color: #595959; }

.detail-desc :deep(.ant-descriptions-item-label) {
  background: #fafafa;
  color: #595959;
}

.error-box {
  color: #cf1322;
  background: #fff1f0;
  padding: 8px 12px;
  border-radius: 4px;
}
</style>
