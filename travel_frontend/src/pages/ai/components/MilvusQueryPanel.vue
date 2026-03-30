<template>
  <a-card title="检索集合扫描特征" :bordered="false" class="dashboard-card">
    <div v-if="!queryResult" class="empty-holder">
      <a-empty description="暂无扫描数据，请在上侧刷新大盘" :image-style="{ height: '40px' }" />
    </div>
    
    <div v-else class="query-content">
      <a-row :gutter="16">
        <a-col :span="12">
          <div class="feat-box big-val-box">
            <div class="fb-label">向量检索总量 (Query Count)</div>
            <div class="fb-val text-blue">{{ queryResult.queryCount ?? '--' }}</div>
          </div>
        </a-col>
        <a-col :span="12">
          <div class="feat-box">
            <div class="fb-label">解析集合与主键</div>
            <div class="fb-val">{{ queryResult.collection ?? '--' }} / {{ queryResult.primaryField ?? '--' }}</div>
          </div>
        </a-col>
      </a-row>

      <div class="tags-group">
        <span class="tg-label">状态特征：</span>
        <a-tag :color="queryResult.truncated ? 'orange' : 'cyan'">{{ queryResult.truncated ? '已截断 (抵达单次Limit: '+queryResult.limit+')' : '全量展示 (Limit未满)' }}</a-tag>
        <a-tag>行量估算：{{ queryResult.statsRowCount ?? '--' }}</a-tag>
      </div>
      
      <div v-if="sampleIds.length > 0" class="sample-wrapper">
        <div class="sw-title"><DatabaseOutlined /> IDs 取样分布片段 (Top 50)</div>
        <div class="sw-list">
          <span class="id-item" v-for="item in sampleIds.slice(0, 50)" :key="item">{{ item }}</span>
          <span class="id-item more" v-if="sampleIds.length > 50">+ {{ sampleIds.length - 50 }} 更多...</span>
        </div>
      </div>
    </div>
  </a-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { DatabaseOutlined } from '@ant-design/icons-vue'

const props = defineProps<{
  queryResult: Record<string, unknown> | null
}>()

const sampleIds = computed(() => {
  const raw = props.queryResult?.sampleIds
  return Array.isArray(raw) ? raw.map((item) => String(item)) : []
})
</script>

<style scoped>
.dashboard-card {
  border-radius: 8px;
  box-shadow: 0 1px 2px -2px rgba(0, 0, 0, 0.08), 0 3px 6px 0 rgba(0, 0, 0, 0.06);
  height: 100%;
}
.dashboard-card :deep(.ant-card-head-title) { font-weight: 500; font-size: 16px; }

.empty-holder { padding: 40px 0; }

.query-content { padding-top: 8px; }

.feat-box {
  background: #fafafa;
  padding: 16px;
  border-radius: 6px;
  margin-bottom: 16px;
}
.big-val-box {
  background: #e6f7ff;
}
.fb-label { color: #8c8c8c; font-size: 13px; margin-bottom: 8px; }
.fb-val { font-size: 16px; color: #262626; font-weight: 500; }
.text-blue { color: #1890ff; font-size: 24px; font-family: monospace; }

.tags-group { margin-bottom: 20px; display: flex; align-items: center; }
.tg-label { color: #595959; font-weight: 500; margin-right: 8px; }

.sample-wrapper {
  border: 1px solid #f0f0f0;
  border-radius: 6px;
}
.sw-title {
  padding: 10px 16px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
  font-weight: 500;
  color: #595959;
  border-top-left-radius: 6px;
  border-top-right-radius: 6px;
}
.sw-list {
  padding: 16px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  max-height: 200px;
  overflow-y: auto;
}
.id-item {
  font-family: monospace;
  font-size: 12px;
  color: #434343;
  background: #f5f5f5;
  padding: 2px 6px;
  border-radius: 4px;
}
.id-item.more {
  background: transparent;
  color: #1890ff;
  cursor: pointer;
}
</style>
