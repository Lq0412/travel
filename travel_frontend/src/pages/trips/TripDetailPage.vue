<template>
  <div class="trip-detail-page" v-if="trip">
    <div class="page-header">
      <h1 class="page-title">{{ trip.destination }} · {{ trip.days }}天</h1>
      <div class="meta-info">
        <span class="meta-item">
          <img src="https://unpkg.com/lucide-static@latest/icons/calendar.svg" class="meta-icon" alt="status" />
          <span>状态：<b>{{ statusText }}</b></span>
        </span>
        <span class="meta-item">
          <img src="https://unpkg.com/lucide-static@latest/icons/tag.svg" class="meta-icon" alt="theme" />
          <span>主题：{{ trip.theme || '通用' }}</span>
        </span>
      </div>
    </div>

    <div class="content-container">
      <!-- 回忆图展示 -->
      <div v-if="trip.memoryCard?.imageUrl" class="memory-card-section">
        <div class="section-card">
          <h3 class="section-title">回忆图</h3>
          <div class="memory-image">
            <img :src="trip.memoryCard.imageUrl" :alt="`${trip.destination} 回忆图`" />
          </div>
        </div>
      </div>

      <!-- 行程亮点（按天折叠） -->
      <div class="highlights-section">
        <div class="section-card">
          <h3 class="section-title">行程亮点（按天）</h3>
          <div class="highlights-content">
            <a-collapse v-if="days.length" accordion>
              <a-collapse-panel v-for="(d, idx) in days" :key="d" :header="`第${d}天`">
                <p class="highlights-text">
                  {{ (trip.dailyHighlights?.[d] || []).join('，') || '无' }}
                </p>
              </a-collapse-panel>
            </a-collapse>
            <p v-else class="highlights-text">无</p>
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="actions-section">
        <button 
          v-if="trip.status !== 'completed'" 
          @click="markCompleted" 
          class="action-btn"
          :disabled="completing"
        >
          <img src="https://unpkg.com/lucide-static@latest/icons/check-circle.svg" class="btn-icon" alt="complete" />
          {{ completing ? '提交中...' : '标记为已完成' }}
        </button>
        <button 
          @click="goMemory" 
          class="action-btn"
          :class="{ 'primary': !trip.memoryCard?.imageUrl }"
        >
          <img src="https://unpkg.com/lucide-static@latest/icons/image.svg" class="btn-icon" alt="memory" />
          {{ trip.memoryCard?.imageUrl ? '重新生成回忆图' : '生成回忆图' }}
        </button>
        <button 
          class="action-btn"
          @click="onRegenerate"
          :disabled="regenerating"
        >
          <img src="https://unpkg.com/lucide-static@latest/icons/refresh-ccw.svg" class="btn-icon" alt="retry" />
          {{ regenerating ? '重新生成中...' : '重新生成' }}
        </button>
        <button 
          class="action-btn"
          @click="openHistory"
        >
          <img src="https://unpkg.com/lucide-static@latest/icons/history.svg" class="btn-icon" alt="history" />
          历史版本
        </button>
        <button 
          @click="openPublish" 
          class="action-btn primary"
          :disabled="!trip.memoryCard?.imageUrl"
        >
          <img src="https://unpkg.com/lucide-static@latest/icons/send.svg" class="btn-icon" alt="publish" />
          一键发布到论坛
        </button>
        <p v-if="!trip.memoryCard?.imageUrl" class="hint-text">
          请先生成回忆图后再发布
        </p>
      </div>
    </div>

    <!-- 历史版本弹窗 -->
    <div v-if="showHistory" class="dialog-overlay" @click.self="showHistory = false">
      <div class="dialog-card">
        <div class="dialog-header">
          <h3 class="dialog-title">历史版本</h3>
          <button class="close-btn" @click="showHistory = false">
            <img src="https://unpkg.com/lucide-static@latest/icons/x.svg" alt="close" />
          </button>
        </div>
        <div class="dialog-content">
          <div v-if="historyLoading" class="hint-text">加载中...</div>
          <div v-else>
            <div v-if="historyList.length" class="history-list">
              <div v-for="h in historyList" :key="h.id" class="history-item">
                <img :src="h.imageUrl" alt="history" class="history-thumb" />
                <div class="history-meta">
                  <div class="line">时间：{{ formatTime(h.createTime) }}</div>
                  <div class="line">模板：{{ h.templateName || 'default' }}</div>
                </div>
                <div class="history-actions">
                  <button class="action-btn" @click="setCurrent(h.id)">设为当前</button>
                  <a class="action-btn" :href="h.imageUrl" target="_blank" rel="noopener">查看</a>
                </div>
              </div>
            </div>
            <p v-else class="hint-text">暂无历史版本</p>
          </div>
        </div>
        <div class="dialog-actions">
          <button class="cancel-btn" @click="showHistory = false">关闭</button>
        </div>
      </div>
    </div>

    <!-- 发布对话框 -->
    <div v-if="showPublish" class="dialog-overlay" @click.self="showPublish = false">
      <div class="dialog-card">
        <div class="dialog-header">
          <h3 class="dialog-title">发布到论坛</h3>
          <button class="close-btn" @click="showPublish = false">
            <img src="https://unpkg.com/lucide-static@latest/icons/x.svg" alt="close" />
          </button>
        </div>
        <div class="dialog-content">
          <div class="form-group">
            <label class="form-label">
              <span class="label-text">标题 <span class="required">*</span></span>
              <input v-model="publish.title" class="form-input" placeholder="请输入帖子标题" />
            </label>
          </div>
          <div class="form-group">
            <label class="form-label">
              <span class="label-text">正文 <span class="required">*</span></span>
              <textarea v-model="publish.content" rows="6" class="form-textarea" placeholder="请输入帖子内容"></textarea>
            </label>
          </div>
          <div class="form-group">
            <label class="form-label">
              <span class="label-text">分类ID</span>
              <input v-model.number="publish.categoryId" type="number" min="1" class="form-input" placeholder="1" />
            </label>
          </div>
          <div class="form-group">
            <label class="form-label">
              <span class="label-text">标签（逗号分隔）</span>
              <input v-model="tagsInput" class="form-input" placeholder="从化,3天,#AI生成" />
            </label>
          </div>
          <div v-if="trip.memoryCard?.imageUrl" class="preview-section">
            <p class="preview-label">回忆图将作为帖子封面</p>
            <div class="preview-image">
              <img :src="trip.memoryCard.imageUrl" alt="预览" />
            </div>
          </div>
        </div>
        <div class="dialog-actions">
          <button class="cancel-btn" @click="showPublish = false">取消</button>
          <button class="publish-btn" @click="doPublish" :disabled="publishing">
            <span v-if="!publishing">发布</span>
            <span v-else class="loading-content">
              <span class="spinner"></span>
              <span>发布中...</span>
            </span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getTripById, completeTrip, publishToForum } from '@/api/tripController'
import { regenerateMemoryCard, getMemoryCardHistory, setMemoryCardCurrentFromHistory, getMemoryCardByTripId } from '@/api/memoryCardController'
import { message } from 'ant-design-vue'

const route = useRoute()
const router = useRouter()
const trip = ref<any>(null)
const attemptedEnsureCurrent = ref(false)
const completing = ref(false)
const regenerating = ref(false)
const showPublish = ref(false)
const showHistory = ref(false)
const publishing = ref(false)
const publish = ref<{ title: string; content: string; categoryId?: number; tags?: string[] }>({
  title: '',
  content: '',
  categoryId: 1,
  tags: ['#AI生成']
})
const tagsInput = ref('')
const historyLoading = ref(false)
const historyList = ref<any[]>([])

const statusText = computed(() => {
  const map: Record<string, string> = {
    'planning': '规划中',
    'ongoing': '进行中',
    'completed': '已完成',
    'cancelled': '已取消'
  }
  return map[trip.value?.status] || trip.value?.status || '未知'
})

onMounted(load)
async function load() {
  const id = Number(route.params.id)
  if (!id) return
  try {
    const resp = await getTripById({ id } as any)
    trip.value = resp?.data?.data
    if (trip.value) {
      // 预填发布内容
      updatePublishContent()
      // 补充获取当前回忆图信息（Trip详情可能不含 memoryCard）
      await loadMemoryCard(id)
      // 若当前无回忆图，尝试自动使用最近的成功历史设为当前（仅尝试一次，避免循环）
      if (!trip.value?.memoryCard?.imageUrl && !attemptedEnsureCurrent.value) {
        attemptedEnsureCurrent.value = true
        await autoEnsureCurrentMemoryCard()
        // 再次拉取
        await loadMemoryCard(id)
      }
    }
  } catch (e: any) {
    const errorMsg = e?.response?.data?.message || e?.message || '加载行程失败'
    message.error(errorMsg)
  }
}

// 拉取并绑定当前回忆图
async function loadMemoryCard(tripId: number) {
  try {
    const resp = await getMemoryCardByTripId({ tripId } as any)
    const card = resp?.data?.data
    if (trip.value) {
      trip.value.memoryCard = card || null
    }
  } catch (e) {
    // 忽略错误，保持页面可用
  }
}

function updatePublishContent() {
  if (!trip.value) return
  
  publish.value.title = `${trip.value.destination} ${trip.value.days}天 ✈️`
  
  const highlightsText = formatHighlights(trip.value?.dailyHighlights, 3)
  publish.value.content = `目的地：${trip.value.destination}
天数：${trip.value.days}天
主题：${trip.value.theme || '通用'}

行程亮点：
${highlightsText}

${trip.value.memoryCard?.imageUrl ? '✨ 已生成精美回忆图，记录这次美好的旅程！' : '💡 提示：生成回忆图后可作为帖子封面'}

Tips：合理安排行程，注意安全出行。`
  
  tagsInput.value = `${trip.value.destination},${trip.value.days}天,#AI生成${trip.value.theme ? ',' + trip.value.theme : ''}`
}

const formattedHighlights = computed(() => formatHighlights(trip.value?.dailyHighlights))
const days = computed(() => {
  const dh = trip.value?.dailyHighlights
  if (!dh) return []
  return Object.keys(dh).sort((a,b)=>Number(a)-Number(b))
})
function formatHighlights(highlights: any, take?: number) {
  if (!highlights) return '无'
  const days = Object.keys(highlights).sort((a,b)=>Number(a)-Number(b))
  const lines: string[] = []
  for (const d of days) {
    const arr = Array.isArray(highlights[d]) ? highlights[d] : []
    const slice = typeof take === 'number' ? arr.slice(0, take) : arr
    if (slice.length > 0) {
      lines.push(`第${d}天：${slice.join('，')}`)
    }
  }
  return lines.length > 0 ? lines.join('\n') : '无'
}

async function markCompleted() {
  if (!trip.value?.id) return
  completing.value = true
  try {
    await completeTrip({ id: trip.value.id } as any)
    message.success('已标记为已完成')
    await load()
  } catch (e: any) {
    const errorMsg = e?.response?.data?.message || e?.message || '标记失败'
    message.error(errorMsg)
  } finally {
    completing.value = false
  }
}

function goMemory() {
  if (trip.value?.id) {
    router.push(`/trips/${trip.value.id}/memory`)
  }
}

function openPublish() {
  if (trip.value?.memoryCard?.imageUrl) {
    updatePublishContent()
    showPublish.value = true
    return
  }
  // 无当前图片时，自动尝试从历史中设为当前
  autoEnsureCurrentMemoryCard().then((ok) => {
    if (ok && trip.value?.memoryCard?.imageUrl) {
      updatePublishContent()
      showPublish.value = true
    } else {
      message.warning('请先生成回忆图后再发布')
    }
  })
}

async function doPublish() {
  if (!trip.value?.id) return
  
  if (!publish.value.title?.trim()) {
    message.error('请输入帖子标题')
    return
  }
  
  if (!publish.value.content?.trim()) {
    message.error('请输入帖子内容')
    return
  }
  
  publishing.value = true
  try {
    const tags = tagsInput.value.split(',').map(s=>s.trim()).filter(Boolean)
    const resp = await publishToForum({ id: trip.value.id } as any, {
      title: publish.value.title.trim(),
      content: publish.value.content.trim(),
      categoryId: publish.value.categoryId || 1,
      tags: tags.length > 0 ? tags : ['#AI生成'],
      memoryCardId: trip.value.memoryCard?.id
    } as any)
    
    const postId = resp?.data?.data
    if (postId) {
      message.success('发布成功！')
      showPublish.value = false
      setTimeout(() => {
        router.push('/user/forum')
      }, 500)
    } else {
      throw new Error('未返回帖子ID')
    }
  } catch (e: any) {
    const errorMsg = e?.response?.data?.message || e?.message || '发布失败，请重试'
    message.error(errorMsg)
  } finally {
    publishing.value = false
  }
}

// 重新生成回忆图（直接调用后端API）
async function onRegenerate() {
  if (!trip.value?.id) return
  regenerating.value = true
  try {
    const resp = await regenerateMemoryCard({ tripId: trip.value.id })
    const taskId = resp?.data?.data?.taskId
    if (taskId) {
      message.success('已提交重新生成任务')
      // 跳转到回忆图页面查看进度
      router.push(`/trips/${trip.value.id}/memory`)
    } else {
      throw new Error('未返回任务ID')
    }
  } catch (e: any) {
    const errorMsg = e?.response?.data?.message || e?.message || '重新生成失败，请重试'
    message.error(errorMsg)
  } finally {
    regenerating.value = false
  }
}

function openHistory() {
  showHistory.value = true
  loadHistory()
}

async function loadHistory() {
  if (!trip.value?.id) return
  historyLoading.value = true
  try {
    const resp = await getMemoryCardHistory({ tripId: trip.value.id })
    historyList.value = resp?.data?.data || []
  } catch (e: any) {
    const errorMsg = e?.response?.data?.message || e?.message || '加载历史失败'
    message.error(errorMsg)
  } finally {
    historyLoading.value = false
  }
}

// 兜底：若当前无回忆图，尝试用最新成功历史设为当前
async function autoEnsureCurrentMemoryCard(): Promise<boolean> {
  try {
    const id = trip.value?.id
    if (!id) return false
    const resp = await getMemoryCardHistory({ tripId: id })
    const list: any[] = resp?.data?.data || []
    if (!list.length) return false
    // 优先选择状态为 success 的最近一条，否则取第一条
    const success = list.find((x: any) => x.status === 'success') || list[0]
    if (!success?.id) return false
    await setMemoryCardCurrentFromHistory({ historyId: success.id })
    await load()
    return true
  } catch {
    return false
  }
}

async function setCurrent(id: number) {
  if (!id) return
  try {
    await setMemoryCardCurrentFromHistory({ historyId: id })
    message.success('已设为当前版本')
    showHistory.value = false
    await load()
  } catch (e: any) {
    const errorMsg = e?.response?.data?.message || e?.message || '设置失败'
    message.error(errorMsg)
  }
}

function formatTime(time: string) {
  if (!time) return ''
  return new Date(time).toLocaleString()
}
</script>

<style scoped lang="scss">
.trip-detail-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 24px;
  
  @media (max-width: 768px) {
    padding: 24px 16px;
  }
}

.page-header {
  margin-bottom: 32px;
  
  .page-title {
    font-size: 32px;
    font-weight: 700;
    color: var(--color-text);
    margin-bottom: 12px;
  }
  
  .meta-info {
    display: flex;
    gap: 24px;
    flex-wrap: wrap;
  }
  
  .meta-item {
    display: flex;
    align-items: center;
    gap: 6px;
    font-size: 14px;
    color: var(--color-text-secondary);
    
    .meta-icon {
      width: 16px;
      height: 16px;
      opacity: 0.6;
    }
    
    b {
      color: var(--color-text);
      font-weight: 600;
    }
  }
}

.content-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.section-card {
  background: #ffffff;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: 32px;
  box-shadow: none;
  
  @media (max-width: 768px) {
    padding: 24px;
  }
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 20px;
}

// 回忆图区域
.memory-image {
  img {
    width: 100%;
    max-width: 600px;
    border-radius: 8px;
    border: 1px solid var(--color-border);
    box-shadow: none;
  }
}

// 行程亮点
.highlights-content {
  background: var(--color-bg-secondary);
  border-radius: 8px;
  padding: 20px;
}

.highlights-text {
  margin: 0;
  font-size: 14px;
  line-height: 1.8;
  color: var(--color-text-secondary);
  white-space: pre-wrap;
  word-wrap: break-word;
}

// 操作按钮区域
.actions-section {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 12px 20px;
  border: 1px solid var(--color-border-strong);
  background: #fff;
  color: var(--color-muted);
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.15s ease, border-color 0.15s ease, color 0.15s ease;
  
  .btn-icon {
    width: 18px;
    height: 18px;
  }
  
  &:hover:not(:disabled) {
    background: var(--accent-50);
    border-color: var(--accent-border);
    color: var(--accent-600);
  }
  
  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
  
  &.primary {
    background: var(--accent-50);
    border-color: var(--accent-border);
    color: var(--accent-600);
    
    &:hover:not(:disabled) {
      background: var(--accent-100);
      border-color: var(--accent-border);
    }
  }
}

.hint-text {
  font-size: 14px;
  color: var(--color-muted);
  margin: 0;
}

// 发布对话框
.dialog-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
  backdrop-filter: none;
}

.dialog-card {
  width: 100%;
  max-width: 600px;
  max-height: 90vh;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: none;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid var(--color-border);
}

.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px;
  border-bottom: 1px solid var(--color-border);
}

.dialog-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0;
}

.close-btn {
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.15s ease;
  
  img {
    width: 20px;
    height: 20px;
    opacity: 0.6;
  }
  
  &:hover {
    background: var(--color-bg-muted);
    
    img {
      opacity: 1;
    }
  }
}

.dialog-content {
  padding: 24px;
  overflow-y: auto;
  flex: 1;
}

.form-group {
  margin-bottom: 20px;
}

.form-label {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.label-text {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary);
  
  .required {
    color: var(--support-500);
  }
}

.form-input,
.form-textarea {
  padding: 12px 16px;
  border: 1px solid var(--color-border-strong);
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
  font-family: inherit;
  
  &:focus {
    outline: none;
    border-color: var(--accent-600);
    box-shadow: var(--focus-ring);
  }
  
  &::placeholder {
    color: var(--color-subtle);
  }
}

.form-textarea {
  resize: vertical;
  min-height: 120px;
}

.preview-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid var(--color-border);
}

.preview-label {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-bottom: 12px;
}

.preview-image {
  img {
    width: 100%;
    max-width: 300px;
    border-radius: 8px;
    border: 1px solid var(--color-border);
  }
}

.dialog-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  padding: 24px;
  border-top: 1px solid var(--color-border);
}

.cancel-btn {
  padding: 10px 20px;
  border: 1px solid var(--color-border-strong);
  background: #fff;
  color: var(--color-muted);
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.15s ease, border-color 0.15s ease, color 0.15s ease;
  
  &:hover {
    background: var(--accent-50);
    border-color: var(--accent-border);
    color: var(--accent-600);
  }
}

.publish-btn {
  padding: 10px 24px;
  border: 1px solid var(--color-border-strong);
  background: #fff;
  color: var(--accent-600);
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.15s ease, border-color 0.15s ease, color 0.15s ease;
  
  &:hover:not(:disabled) {
    background: var(--accent-50);
    border-color: var(--accent-border);
  }
  
  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
}

.loading-content {
  display: flex;
  align-items: center;
  gap: 8px;
}

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(47, 49, 51, 0.2);
  border-top-color: var(--color-text);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.history-item {
  display: flex;
  gap: 12px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 12px;
  align-items: center;
  background: var(--color-bg-secondary);
}

.history-thumb {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #eeeeee;
}

.history-meta {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 14px;
  color: var(--color-text-secondary);
}

.history-actions {
  display: flex;
  gap: 8px;
}

.history-actions .action-btn {
  padding: 8px 16px;
  border-radius: 8px;
}
</style>


