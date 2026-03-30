<template>
  <a-card title="知识库调度管理" :bordered="false" class="dashboard-card">
    <template #extra>
      <a-button type="primary" :loading="syncLoading" @click="handleSyncMilvus">
        <template #icon><CloudUploadOutlined /></template>手动同步
      </a-button>
    </template>
    
    <div class="action-block">
      <span class="action-label">同步策略：</span>
      <a-radio-group v-model:value="recreateCollection" button-style="solid">
        <a-radio-button :value="false">增量更新</a-radio-button>
        <a-radio-button :value="true" class="danger-radio">重建集合 (危险)</a-radio-button>
      </a-radio-group>
    </div>

    <a-divider style="margin: 16px 0" />

    <div class="result-block">
      <div class="result-title">上次同步结果：</div>
      <a-empty v-if="!lastSyncResult" description="无近期记录" :image-style="{ height: '40px' }" />
      <div v-else class="result-stats">
        <a-row :gutter="[16, 16]">
          <a-col :span="12">
            <div class="stat-item">
              <div class="stat-label">集合归属</div>
              <div class="stat-val">{{ lastSyncResult.collection ?? '--' }}</div>
            </div>
          </a-col>
          <a-col :span="12">
            <div class="stat-item">
              <div class="stat-label">运行状态</div>
              <div class="stat-val">
                <a-tag :color="lastSyncResult.status === 'success' ? 'success' : 'error'">{{ lastSyncResult.status ?? '--' }}</a-tag>
              </div>
            </div>
          </a-col>
          <a-col :span="12">
            <div class="stat-item">
              <div class="stat-label">写入成功</div>
              <div class="stat-val primary-val">{{ lastSyncResult.upsertedDocs ?? '0' }}</div>
            </div>
          </a-col>
          <a-col :span="12">
            <div class="stat-item">
              <div class="stat-label">失败批次</div>
              <div class="stat-val error-val">{{ lastSyncResult.failedBatches ?? '0' }}</div>
            </div>
          </a-col>
        </a-row>
      </div>
    </div>
  </a-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { CloudUploadOutlined } from '@ant-design/icons-vue'
import { syncMilvusKnowledge } from '@/api/monitoringController'

const props = defineProps<{
  lastSyncResult: Record<string, unknown> | null
}>()

const emit = defineEmits<{
  (e: 'update:syncLoading', loading: boolean): void
  (e: 'syncSuccess', result: Record<string, unknown>): void
}>()

const recreateCollection = ref(false)
const syncLoading = ref(false)

async function handleSyncMilvus() {
  syncLoading.value = true
  emit('update:syncLoading', true)
  try {
    const res = await syncMilvusKnowledge({ recreateCollection: recreateCollection.value })
    if (res.data.code !== 0 || !res.data.data) throw new Error(res.data.message || '同步执行失败')
    
    const result = res.data.data
    if (String(result.status ?? '').toLowerCase() === 'success') message.success('同步已完成')
    else message.warning(`同步结束: ${result.status}`)
    
    emit('syncSuccess', result)
  } catch (error: unknown) {
    message.error(error instanceof Error ? error.message : '执行失败')
  } finally {
    syncLoading.value = false
    emit('update:syncLoading', false)
  }
}
</script>

<style scoped>
.dashboard-card {
  border-radius: 8px;
  box-shadow: 0 1px 2px -2px rgba(0, 0, 0, 0.08), 0 3px 6px 0 rgba(0, 0, 0, 0.06);
}
.dashboard-card :deep(.ant-card-head-title) { font-weight: 500; font-size: 16px; }

.action-block { display: flex; align-items: center; margin-top: 8px; }
.action-label { color: #595959; margin-right: 8px; font-weight: 500;}
.danger-radio { color: #ff4d4f; }
.danger-radio.ant-radio-button-wrapper-checked { background: #fff1f0; border-color: #ffa39e; color: #cf1322; }

.result-title { font-weight: 500; color: #595959; margin-bottom: 12px; }
.stat-item { background: #fafafa; padding: 12px; border-radius: 4px; }
.stat-label { color: #8c8c8c; font-size: 12px; margin-bottom: 4px; }
.stat-val { color: #262626; font-size: 16px; font-weight: 500; display: flex; align-items: center; min-height: 24px;}
.primary-val { color: #1890ff; }
.error-val { color: #f5222d; }
</style>
