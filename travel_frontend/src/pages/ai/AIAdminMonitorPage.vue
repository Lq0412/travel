<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined, SyncOutlined, CheckCircleOutlined } from '@ant-design/icons-vue'
import {
  health, queryMilvusCount, syncMilvusKnowledge,
  createKnowledgeIngestionTask, getAiTaskStatus
} from '@/api/monitoringController'
import { getHealthTone, getQueryCountValue, summarizeSyncResult, summarizeTaskStatus } from '@/utils/monitoring'

// -------------- 1. Milvus 层数据 --------------
const milvusLoading = ref(false)
const healthData = ref<Record<string, any>>({})
const queryResult = ref<Record<string, any>>({})
const lastSyncResult = ref<Record<string, any> | null>(null)

async function fetchMilvusData() {
  milvusLoading.value = true
  try {
    const [h, q] = await Promise.all([health(), queryMilvusCount({ limit: 1 })])
    healthData.value = h.data?.data || {}
    queryResult.value = q.data?.data || {}
  } catch (e) {
    message.error('获取Milvus表盘数据失败')
  } finally {
    milvusLoading.value = false
  }
}

// -------------- 2. 任务流表单与记录层 --------------
interface TaskRecord {
  taskId: string
  query: string
  dataSource: string
  effectPreset: string
  createTime: string
  status: string
  withSync: boolean
  syncTriggered: boolean
}

interface SummaryCard {
  label: string
  value: string | number
}

const effectPresetLabels: Record<'FAST' | 'BALANCED' | 'DEEP', string> = {
  FAST: '低',
  BALANCED: '中',
  DEEP: '高',
}

// 重聚表单参数配置
const taskForm = ref({
  query: '',
  dataSource: 'AUTO' as 'AUTO' | 'TAVILY' | 'DASHSCOPE',
  effectPreset: 'BALANCED' as 'FAST' | 'BALANCED' | 'DEEP',
  maxItems: 10,
  maxRetry: 3,
  mustContainStoreName: true,
  withSync: true
})

const submitting = ref(false)
const taskRecords = ref<TaskRecord[]>([])
const refreshingList = ref(false)
const autoSyncing = ref(false)

const TERMINAL_TASK_STATUS = new Set(['SUCCESS', 'FAILED', 'CANCELLED'])
let pollingTimer: number | null = null

const taskSummary = computed(() => summarizeTaskStatus(taskRecords.value))

const summaryCards = computed<SummaryCard[]>(() => [
  {
    label: 'Milvus 健康度',
    value: healthData.value.status || '--',
  },
  {
    label: '知识库向量总数',
    value: getQueryCountValue(queryResult.value),
  },
  {
    label: '未完结任务',
    value: taskSummary.value.activeCount,
  },
  {
    label: '最近同步',
    value: summarizeSyncResult(lastSyncResult.value).label,
  },
])

const columns = [
  { title: '流水线 ID (Task ID)', key: 'taskId', width: 200 },
  { title: '指令目标 (Query)', key: 'query', ellipsis: true },
  { title: '策略配置', key: 'settings', width: 160 },
  { title: '下发时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '执行终态', key: 'status', width: 120 },
  { title: '入库同步', key: 'syncAction', width: 160 },
  { title: '动作', key: 'action', width: 160, align: 'center' }
]

const syncSummary = computed(() => summarizeSyncResult(lastSyncResult.value))
const healthTone = computed(() => getHealthTone(healthData.value))

function loadRecordsFromLocal() {
  try {
    const raw = localStorage.getItem('AI_TASK_RECORDS_V3')
    if (!raw) return

    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return

    taskRecords.value = parsed
      .filter((item) => item && typeof item === 'object')
      .map((item) => {
        const row = item as Partial<TaskRecord>
        return {
          taskId: String(row.taskId || ''),
          query: String(row.query || ''),
          dataSource: String(row.dataSource || 'AUTO'),
          effectPreset: String(row.effectPreset || 'BALANCED'),
          createTime: String(row.createTime || new Date().toLocaleString()),
          status: String(row.status || 'PENDING'),
          withSync: row.withSync !== false,
          syncTriggered: Boolean(row.syncTriggered),
        }
      })
      .filter((row) => row.taskId)
  } catch (e) {}
}

function saveRecordsToLocal() {
  localStorage.setItem('AI_TASK_RECORDS_V3', JSON.stringify(taskRecords.value))
}

function getStatusColor(status: string) {
  const s = (status || '').toUpperCase()
  if (s === 'SUCCESS') return 'success'
  if (s === 'FAILED' || s === 'CANCELLED') return 'error'
  if (s === 'RUNNING' || s === 'PENDING') return 'processing'
  return 'default'
}

function formatEffectPreset(preset: string) {
  const code = (preset || '').toUpperCase() as keyof typeof effectPresetLabels
  return effectPresetLabels[code] || preset || '--'
}

function isTerminalStatus(status: string) {
  return TERMINAL_TASK_STATUS.has((status || '').toUpperCase())
}

function getRecord(taskId: string) {
  return taskRecords.value.find((item) => item.taskId === taskId)
}

async function triggerSyncAfterSuccess(record: TaskRecord) {
  if (record.syncTriggered) {
    return
  }

  // 点击手动入库时，确保状态已更新
  record.withSync = true
  record.syncTriggered = true
  saveRecordsToLocal()

  autoSyncing.value = true
  try {
    const response = await syncMilvusKnowledge({ recreateCollection: false })
    lastSyncResult.value = response.data?.data || null
    message.success('已成功触发知识库入库同步')
    await fetchMilvusData()
  } catch (e) {
    record.syncTriggered = false
    saveRecordsToLocal()
    message.warning('同步触发失败，可再次手动尝试')
  } finally {
    autoSyncing.value = false
  }
}

async function submitTask() {
  if (!taskForm.value.query.trim()) return message.warning('请填写指令目标 (Query)')
  submitting.value = true
  try {
    // 提交主任务 - 携带所有参数配置
    const res = await createKnowledgeIngestionTask({
      query: taskForm.value.query,
      dataSource: taskForm.value.dataSource,
      effectPreset: taskForm.value.effectPreset,
      maxItems: taskForm.value.maxItems,
      maxRetry: taskForm.value.maxRetry,
      mustContainStoreName: taskForm.value.mustContainStoreName
    })
    
    const taskId = String(res.data?.data?.taskId || '')
    const deduplicated = Boolean(res.data?.data?.deduplicated)
    const returnedStatus = String(res.data?.data?.status || 'PENDING')

    if (taskId) {
      const existing = getRecord(taskId)
      if (existing) {
        existing.query = taskForm.value.query
        existing.dataSource = taskForm.value.dataSource
        existing.effectPreset = taskForm.value.effectPreset
        existing.status = returnedStatus
        existing.withSync = taskForm.value.withSync
      } else {
        taskRecords.value.unshift({
          taskId,
          query: taskForm.value.query,
          dataSource: taskForm.value.dataSource,
          effectPreset: taskForm.value.effectPreset,
          createTime: new Date().toLocaleString(),
          status: returnedStatus,
          withSync: taskForm.value.withSync,
          syncTriggered: false,
        })
      }

      if (deduplicated) {
        message.info('检测到相同参数任务，已复用已有任务')
      } else {
        message.success('流水线下发成功')
      }

      saveRecordsToLocal()

      // 清空输入框保留配置
      taskForm.value.query = ''
      await refreshSingleTask(taskId, { silent: true, triggerAutoSync: true })
      startStatusPolling()
    }
  } catch (e) {
    message.error('推送流水线任务失败')
  } finally {
    submitting.value = false
  }
}

async function refreshSingleTask(
  taskId: string,
  options: { silent?: boolean; triggerAutoSync?: boolean } = {}
) {
  const { silent = false, triggerAutoSync = false } = options

  try {
    const res = await getAiTaskStatus(taskId)
    const target = getRecord(taskId)
    if (target && res.data?.data) {
      target.status = String(res.data.data.status || target.status)

      if (triggerAutoSync && (target.status || '').toUpperCase() === 'SUCCESS') {
        if (target.withSync && !target.syncTriggered) {
          await triggerSyncAfterSuccess(target)
        }
      }

      saveRecordsToLocal()
    }
  } catch (e) {
    if (!silent) {
      message.warning(`更新任务 ${taskId} 状态失败`)
    }
  }
}

function startStatusPolling() {
  if (pollingTimer) {
    window.clearInterval(pollingTimer)
  }

  pollingTimer = window.setInterval(() => {
    const pendingTasks = taskRecords.value.filter((task) => !isTerminalStatus(task.status))
    if (!pendingTasks.length) {
      stopStatusPolling()
      return
    }

    void Promise.all(
      pendingTasks.map((task) => refreshSingleTask(task.taskId, { silent: true, triggerAutoSync: true }))
    )
  }, 4000)
}

function stopStatusPolling() {
  if (pollingTimer) {
    window.clearInterval(pollingTimer)
    pollingTimer = null
  }
}

async function refreshAllTasks() {
  if (!taskRecords.value.length) return
  refreshingList.value = true
  try {
    const pendings = taskRecords.value.filter(t => 
      !['SUCCESS', 'FAILED', 'CANCELLED'].includes((t.status||'').toUpperCase())
    )
    if (!pendings.length) {
      message.info('列表中没有未完结的任务需要溯源')
      refreshingList.value = false
      return
    }

    await Promise.all(pendings.map(t => refreshSingleTask(t.taskId, { silent: true, triggerAutoSync: true })))
    message.success('非终态任务刷新完毕')
  } finally {
    refreshingList.value = false
  }
}

function removeRecord(taskId: string) {
  taskRecords.value = taskRecords.value.filter(t => t.taskId !== taskId)
  saveRecordsToLocal()
}

onMounted(() => {
  fetchMilvusData()
  loadRecordsFromLocal()
  const pendingTasks = taskRecords.value.filter((task) => !isTerminalStatus(task.status))
  if (pendingTasks.length) {
    startStatusPolling()
    void Promise.all(
      pendingTasks.map((task) => refreshSingleTask(task.taskId, { silent: true, triggerAutoSync: true }))
    )
  }
})

onBeforeUnmount(() => {
  stopStatusPolling()
})
</script>

<template>
  <div class="ai-monitor-page">
    <section class="page-header">
      <div>
        <p class="eyebrow">AI 监控中心</p>
        <h1>AI 调度与监控控制台</h1>
        <p>这里作为答辩中的后台能力证明，只承接健康状态、知识库调度和任务追踪，不与用户主链路争主角。</p>
      </div>

      <div class="header-actions">
        <a-button type="primary" @click="fetchMilvusData" :loading="milvusLoading">
          <template #icon><ReloadOutlined /></template>
          刷新数据
        </a-button>
      </div>
    </section>

    <section class="summary-grid">
      <div v-for="card in summaryCards" :key="card.label" class="summary-card">
        <strong>{{ card.value }}</strong>
        <span>{{ card.label }}</span>
      </div>
    </section>

    <section class="evidence-strip">
      <div class="evidence-item">
        <span class="evidence-label">服务状态</span>
        <a-tag :color="healthTone">{{ healthData.status || '--' }}</a-tag>
      </div>
      <div class="evidence-item">
        <span class="evidence-label">向量规模</span>
        <span class="evidence-value">{{ getQueryCountValue(queryResult) }} 条</span>
      </div>
      <div class="evidence-item">
        <span class="evidence-label">同步结果</span>
        <span class="evidence-value">{{ syncSummary.detail }}</span>
      </div>
    </section>

    <section class="panel-grid">
      <a-card class="panel-card" title="新建数据补齐流水线" :bordered="false">
        <a-form layout="vertical">
          <a-form-item label="任务意图目标 (Query)" required>
            <a-input-search
              v-model:value="taskForm.query"
              placeholder="例：杭州西湖两天一夜避坑游..."
              enter-button="提交并执行"
              @search="submitTask"
              :loading="submitting"
            />
          </a-form-item>

          <div class="form-grid">
            <a-form-item label="真实来源">
              <a-select v-model:value="taskForm.dataSource">
                <a-select-option value="AUTO">AUTO</a-select-option>
                <a-select-option value="TAVILY">TAVILY</a-select-option>
                <a-select-option value="DASHSCOPE">DASHSCOPE</a-select-option>
              </a-select>
            </a-form-item>

            <a-form-item label="提取深度策略">
              <a-select v-model:value="taskForm.effectPreset">
                <a-select-option value="FAST">低</a-select-option>
                <a-select-option value="BALANCED">中</a-select-option>
                <a-select-option value="DEEP">高</a-select-option>
              </a-select>
            </a-form-item>

            <a-form-item label="爬取限制数">
              <a-input-number v-model:value="taskForm.maxItems" :min="1" :max="50" />
            </a-form-item>

            <a-form-item label="异常重试次数">
              <a-input-number v-model:value="taskForm.maxRetry" :min="0" :max="10" />
            </a-form-item>

            <a-form-item label="执行后级联配置" class="form-wide">
              <div class="checkbox-group">
                <a-checkbox v-model:checked="taskForm.mustContainStoreName">强制门店名</a-checkbox>
                <a-checkbox v-model:checked="taskForm.withSync">执行完自动入库</a-checkbox>
              </div>
            </a-form-item>
          </div>
        </a-form>
      </a-card>

      <a-card class="panel-card" title="本机执行追踪记录" :bordered="false">
        <template #extra>
          <a-button size="small" type="primary" ghost @click="refreshAllTasks" :loading="refreshingList">
            <template #icon><SyncOutlined /></template>
            批量刷新
          </a-button>
        </template>

        <a-table
          :dataSource="taskRecords"
          :columns="columns"
          rowKey="taskId"
          size="middle"
          :scroll="{ x: 980 }"
          :pagination="{ pageSize: 10 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'taskId'">
              <span class="task-id">{{ record.taskId }}</span>
            </template>
            <template v-if="column.key === 'query'">
              <span class="task-query">{{ record.query }}</span>
            </template>
            <template v-if="column.key === 'settings'">
              <span class="task-settings">{{ record.dataSource }} / {{ formatEffectPreset(record.effectPreset) }}</span>
            </template>
            <template v-if="column.key === 'status'">
              <a-tag :color="getStatusColor(record.status)">{{ record.status || '获取中' }}</a-tag>
            </template>
            <template v-if="column.key === 'syncAction'">
              <template v-if="!isTerminalStatus(record.status)">
                <a-switch
                  v-model:checked="record.withSync"
                  checked-children="自动入库"
                  un-checked-children="手动触发"
                  size="small"
                  @change="saveRecordsToLocal"
                />
              </template>
              <template v-else-if="(record.status || '').toUpperCase() === 'SUCCESS'">
                <span v-if="record.syncTriggered" style="color: #52c41a">
                  <CheckCircleOutlined /> 已触发入库
                </span>
                <a-button
                  v-else
                  size="small"
                  type="link"
                  @click="triggerSyncAfterSuccess(record)"
                  :loading="autoSyncing"
                >
                  <SyncOutlined /> 立即入库
                </a-button>
              </template>
              <template v-else>
                <span style="color: #999">--</span>
              </template>
            </template>
            <template v-if="column.key === 'action'">
              <div class="action-cell">
                <a-button type="link" size="small" @click="refreshSingleTask(record.taskId, { triggerAutoSync: true })">状态溯源</a-button>
                <a-popconfirm title="确定抹除这条流水线发起记录吗？" @confirm="removeRecord(record.taskId)">
                  <a-button type="link" size="small" danger>清除记录</a-button>
                </a-popconfirm>
              </div>
            </template>
          </template>
        </a-table>
      </a-card>
    </section>
  </div>
</template>

<style scoped lang="scss">
.ai-monitor-page {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.page-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: end;
  gap: 24px;
  padding: 28px 30px;
  border-radius: 28px;
  border: 1px solid rgba(15, 28, 46, 0.08);
  background: #ffffff;
}

.eyebrow {
  margin: 0;
  font-size: 13px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.14em;
  color: var(--primary-600);
}

.page-header h1 {
  margin: 8px 0 10px;
  font-size: 34px;
  color: var(--color-text);
}

.page-header p {
  margin: 0;
  color: var(--color-muted);
}

.header-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.summary-card {
  padding: 20px 22px;
  border-radius: 22px;
  border: 1px solid rgba(15, 28, 46, 0.08);
  background: #ffffff;
  box-shadow: 0 14px 36px rgba(18, 52, 97, 0.05);

  strong {
    display: block;
    font-size: 30px;
    color: var(--color-text);
    font-variant-numeric: tabular-nums;
  }

  span {
    color: var(--color-muted);
  }
}

.panel-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 1.4fr);
  gap: 16px;
}

.evidence-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.evidence-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 18px;
  border-radius: 20px;
  border: 1px solid rgba(15, 28, 46, 0.08);
  background: rgba(255, 255, 255, 0.92);
}

.evidence-label {
  color: var(--color-muted, #667085);
  font-size: 13px;
}

.evidence-value {
  color: var(--color-text, #0f1c2e);
  font-weight: 600;
  text-align: right;
}

.panel-card {
  border-radius: 24px;
  border: 1px solid rgba(15, 28, 46, 0.08);
  box-shadow: 0 16px 40px rgba(18, 52, 97, 0.05);
}

.panel-card :deep(.ant-card-body) {
  padding: 22px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.form-wide {
  grid-column: 1 / -1;
}

.checkbox-group {
  display: flex;
  flex-wrap: wrap;
  gap: 16px 20px;
  padding-top: 2px;
}

.task-id,
.task-settings {
  font-size: 12px;
  color: var(--color-subtle);
  font-variant-numeric: tabular-nums;
}

.task-query {
  display: block;
  min-width: 0;
  font-weight: 500;
  color: var(--color-text);
  word-break: break-word;
}

.action-cell {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

:deep(.ant-form-item-label > label) {
  color: #596579;
  font-weight: 500;
}

:deep(.ant-input),
:deep(.ant-select-selector),
:deep(.ant-input-number) {
  width: 100%;
}

@media (max-width: 1100px) {
  .page-header,
  .panel-grid {
    grid-template-columns: 1fr;
  }

  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .evidence-strip {
    grid-template-columns: 1fr;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .form-wide {
    grid-column: auto;
  }

  .page-header {
    align-items: start;
  }
}

@media (max-width: 768px) {
  .page-header {
    padding: 22px 20px;
  }

  .page-header h1 {
    font-size: 28px;
  }

  .summary-grid {
    grid-template-columns: 1fr;
  }

  .card-actions {
    justify-content: flex-start;
  }
}
</style>
