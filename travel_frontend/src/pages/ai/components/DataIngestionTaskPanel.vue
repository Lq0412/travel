<template>
  <a-card title="新建数据补齐任务" :bordered="false" class="dashboard-card action-form-card">
    <a-form layout="vertical" class="modern-form">
      <a-form-item label="查询关键词及地理意图">
        <a-input v-model:value="ingestionForm.query" placeholder="例：杭州三天两夜避坑攻略 (支持长难句)" size="large" allow-clear />
      </a-form-item>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="真实来源">
            <a-select v-model:value="ingestionForm.dataSource" size="large">
              <a-select-option value="AUTO">AUTO</a-select-option>
              <a-select-option value="TAVILY">TAVILY</a-select-option>
              <a-select-option value="DASHSCOPE">DASHSCOPE</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="处理深度预设">
            <a-select v-model:value="ingestionForm.effectPreset" size="large">
              <a-select-option value="FAST">低</a-select-option>
              <a-select-option value="BALANCED">中</a-select-option>
              <a-select-option value="DEEP">高</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
      </a-row>

      <a-row :gutter="16">
        <a-col :span="8">
          <a-form-item label="提取上限">
            <a-input-number v-model:value="ingestionForm.maxItems" :min="1" :max="30" style="width: 100%;" size="large" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="降级重试">
            <a-input-number v-model:value="ingestionForm.maxRetry" :min="1" :max="10" style="width: 100%;" size="large" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="地点实体约束">
            <div class="switch-wrapper">
              <a-switch v-model:checked="ingestionForm.mustContainStoreName" />
              <span class="switch-text">{{ ingestionForm.mustContainStoreName ? '强制必须提取出真实门店名' : '不强制包含真实地点' }}</span>
            </div>
          </a-form-item>
        </a-col>
      </a-row>

      <div class="submit-action">
        <a-button type="primary" size="large" block :loading="createTaskLoading" @click="handleCreateIngestionTask">
          构建并提交异步流水线
        </a-button>
      </div>
    </a-form>
  </a-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { createKnowledgeIngestionTask } from '@/api/monitoringController'

const emit = defineEmits<{ (e: 'taskCreated', taskId: string): void }>()
const createTaskLoading = ref(false)

interface IngestionForm {
  query: string
  dataSource: 'AUTO' | 'TAVILY' | 'DASHSCOPE'
  effectPreset: 'FAST' | 'BALANCED' | 'DEEP'
  maxItems: number
  maxRetry: number
  mustContainStoreName: boolean
}

const ingestionForm = ref<IngestionForm>({
  query: '',
  dataSource: 'AUTO',
  effectPreset: 'BALANCED',
  maxItems: 10,
  maxRetry: 3,
  mustContainStoreName: true,
})

async function handleCreateIngestionTask() {
  if (!ingestionForm.value.query.trim()) return message.warning('请提供查询意图后再提交')

  createTaskLoading.value = true
  try {
    const res = await createKnowledgeIngestionTask(ingestionForm.value)
    if (res.data.code !== 0 || !res.data.data) throw new Error(res.data.message || '任务投递失败')

    const taskId = String((res.data.data as Record<string, unknown>).taskId ?? '')
    if (!taskId) throw new Error('成功生成，但未能获取 Task ID')

    message.success('流水线装载成功')
    emit('taskCreated', taskId)
  } catch (error: unknown) {
    message.error(error instanceof Error ? error.message : '创建失败')
  } finally {
    createTaskLoading.value = false
  }
}
</script>

<style scoped>
.dashboard-card {
  border-radius: 8px;
  box-shadow: 0 1px 2px -2px rgba(0, 0, 0, 0.08), 0 3px 6px 0 rgba(0, 0, 0, 0.06);
}
.dashboard-card :deep(.ant-card-head-title) { font-weight: 500; font-size: 16px; }

.modern-form :deep(.ant-form-item-label > label) {
  color: #595959;
  font-weight: 500;
}

.switch-wrapper {
  display: flex;
  align-items: center;
  height: 40px; /* match large input */
}
.switch-text {
  margin-left: 12px;
  color: #8c8c8c;
  font-size: 13px;
}
.submit-action {
  margin-top: 8px;
}
</style>
