<template>
  <div class="planner-page">
    <div class="page-header">
      <h1 class="page-title">AI 行程规划</h1>
      <p class="page-subtitle">输入您的旅行需求，AI 将为您生成个性化行程方案</p>
    </div>

    <a-card class="form-card" :bordered="false">
      <template #title>
        <div class="card-title">
          <span>规划需求</span>
          <span class="card-subtitle">填写出行信息，AI 将生成多个备选方案</span>
        </div>
      </template>
      <a-form 
        layout="vertical" 
        class="planner-form" 
        :model="form" 
        @submit.prevent="onGenerate"
        @finish="onGenerate"
      >
        <a-row :gutter="16">
          <a-col :xs="24" :md="12">
            <a-form-item label="目的地" required>
              <a-input 
                v-model:value="form.destination" 
                :disabled="loading"
                placeholder="如：从化、北京、上海"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :md="6">
            <a-form-item label="天数" required>
              <a-input-number 
                v-model:value="form.days" 
                :min="1" 
                :disabled="loading"
                class="full-width"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :md="6">
            <a-form-item label="预算（元）">
              <a-input-number 
                v-model:value="form.budget" 
                :min="0" 
                :step="100"
                class="full-width"
                :disabled="loading"
                placeholder="可选"
              />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="主题">
              <a-input 
                v-model:value="form.theme" 
                :disabled="loading"
                placeholder="如：休闲、探险、文化、美食"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item>
          <a-space>
            <a-button 
              type="primary" 
              html-type="submit" 
              :loading="loading"
            >
              生成方案
            </a-button>
            <span class="hint-text">生成结果可进入 DIY 编辑，再保存到行程</span>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <a-alert 
      v-if="errorMessage" 
      type="error" 
      :message="errorMessage" 
      show-icon 
      class="mt-20"
    />

    <div v-if="plans.length" class="plans-container">
      <h3 class="plans-title">候选方案</h3>
      <a-space direction="vertical" size="large" class="plans-space">
        <a-card 
          v-for="p in plans" 
          :key="p.planId" 
          class="plan-card" 
          :title="`${p.destination} · ${p.days || form.days}天`"
        >
          <template #extra>
            <a-tag v-if="p.theme" color="blue">{{ p.theme }}</a-tag>
            <a-tag v-else color="default">通用主题</a-tag>
          </template>

          <p class="plan-desc">{{ p.description || 'AI 已包含详细亮点，您也可以稍后在 DIY 编辑中继续调整。' }}</p>

          <a-descriptions size="small" :column="3" class="plan-descriptions">
            <a-descriptions-item label="预算">{{ formatBudget(p.budget) }}</a-descriptions-item>
            <a-descriptions-item label="天数">{{ p.days || form.days }}天</a-descriptions-item>
            <a-descriptions-item label="目的地">{{ p.destination }}</a-descriptions-item>
          </a-descriptions>

          <div v-if="getPlanHighlights(p).length" class="plan-highlights">
            <a-collapse ghost>
              <a-collapse-panel 
                v-for="day in getPlanHighlights(p)" 
                :key="day.key" 
                :header="`第${day.day}天亮点 (${day.items.length})`"
              >
                <ul class="highlight-list">
                  <li v-for="(item, idx) in day.items" :key="idx">{{ item }}</li>
                </ul>
              </a-collapse-panel>
            </a-collapse>
          </div>
          <a-empty v-else description="暂无每日亮点，保存后可补充" />

          <div class="plan-actions">
            <a-space>
              <a-button @click="openEditor(p)" :disabled="savingId === p.planId">
                自定义编辑
              </a-button>
              <a-button 
                type="primary" 
                @click="onSave(p)" 
                :loading="savingId === p.planId"
              >
                接受并保存
              </a-button>
            </a-space>
          </div>
        </a-card>
      </a-space>
    </div>

    <a-drawer 
      title="DIY 自定义行程" 
      placement="right" 
      size="large"
      :open="showEditor" 
      :mask-closable="!customSaving"
      :closable="!customSaving"
      @close="closeEditor"
    >
      <div class="editor-subtitle">
        根据需要调整行程要素与每日亮点，保存后即可在「我的行程」继续管理
      </div>
      <a-form layout="vertical" class="editor-form" :model="editorForm">
        <a-row :gutter="16">
          <a-col :xs="24" :md="12">
            <a-form-item label="目的地" required>
              <a-input v-model:value="editorForm.destination" placeholder="如：北京" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :md="6">
            <a-form-item label="天数" required>
              <a-input-number v-model:value="editorForm.days" :min="1" class="full-width" />
            </a-form-item>
          </a-col>
          <a-col :xs="12" :md="6">
            <a-form-item label="预算（元）">
              <a-input-number v-model:value="editorForm.budget" :min="0" class="full-width" placeholder="可选" />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="主题">
              <a-input v-model:value="editorForm.theme" placeholder="休闲 / 亲子 / 美食..." />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="出发日期">
              <a-date-picker 
                v-model:value="editorForm.startDate" 
                value-format="YYYY-MM-DD"
                class="full-width"
              />
            </a-form-item>
          </a-col>
          <a-col :xs="24" :md="12">
            <a-form-item label="结束日期">
              <a-input v-model:value="editorForm.endDate" disabled />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="亮点摘要">
              <a-textarea 
                v-model:value="editorForm.description" 
                :rows="3"
                placeholder="可简单描述此次行程亮点，保存后将展示在行程详情页"
              />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>

      <div class="editor-section-head">
        <div>
          <h4 class="editor-section-title">每日亮点</h4>
          <p class="section-hint">每一天可添加多个亮点，便于后续扩展或生成素材</p>
        </div>
        <a-button type="dashed" @click="addDay">新增一天</a-button>
      </div>

      <a-space direction="vertical" size="middle" class="editor-days">
        <a-card 
          v-for="(day, dayIndex) in editorDays" 
          :key="`editor-day-${day.day}-${dayIndex}`"
          :title="`第 ${dayIndex + 1} 天亮点`"
          size="small"
        >
          <template #extra>
            <a-button 
              type="link" 
              danger 
              @click="removeDay(dayIndex)" 
              :disabled="editorDays.length <= 1"
            >
              删除
            </a-button>
          </template>

          <a-space direction="vertical" class="highlight-column">
            <div 
              v-for="(item, itemIdx) in day.items" 
              :key="`day-${dayIndex}-item-${itemIdx}`" 
              class="highlight-row"
            >
              <a-input 
                v-model:value="editorDays[dayIndex].items[itemIdx]" 
                placeholder="例如：上午前往 XX 景区，体验当地文化"
              />
              <a-button 
                type="link" 
                danger 
                @click="removeHighlight(dayIndex, itemIdx)" 
                :disabled="day.items.length <= 1"
              >
                移除
              </a-button>
            </div>
            <a-button type="dashed" block @click="addHighlight(dayIndex)">
              添加亮点
            </a-button>
          </a-space>
        </a-card>
      </a-space>

      <div class="drawer-footer">
        <a-space>
          <a-button @click="closeEditor" :disabled="customSaving">取消</a-button>
          <a-button type="primary" @click="saveCustomizedPlan" :loading="customSaving">
            保存到我的行程
          </a-button>
        </a-space>
      </div>
    </a-drawer>
  </div>
</template>
<script setup lang="ts">
import { ref, watch } from 'vue'
import { generateTrip, saveTrip } from '@/api/tripController'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'

const router = useRouter()
const loading = ref(false)
const savingId = ref<string | null>(null)
const errorMessage = ref('')
const form = ref<{ destination: string; days: number; budget?: number; theme?: string }>({
  destination: '',
  days: 3,
  budget: undefined,
  theme: ''
})
const plans = ref<any[]>([])
const showEditor = ref(false)
const editingPlan = ref<any | null>(null)
const customSaving = ref(false)

type EditorDay = {
  day: number
  items: string[]
}

type EditorFormState = {
  destination: string
  days: number
  budget?: number | null
  theme: string
  startDate: string
  endDate: string
  description: string
}

type CustomPlanOverrides = {
  destination?: string
  days?: number
  budget?: number | null
  theme?: string
  startDate?: string
  endDate?: string
  dailyHighlights?: Record<string, string[]>
  description?: string
}

const editorForm = ref<EditorFormState>({
  destination: '',
  days: 1,
  budget: null,
  theme: '',
  startDate: '',
  endDate: '',
  description: ''
})
const editorDays = ref<EditorDay[]>([])

function formatBudget(value?: number | string) {
  if (value === undefined || value === null || value === '') {
    return '待定'
  }
  const num = typeof value === 'number' ? value : Number(value)
  if (Number.isNaN(num)) return '待定'
  return `¥${num.toLocaleString('zh-CN')}`
}

function getPlanHighlights(plan: any) {
  const result: Array<{ day: number; key: string; items: string[] }> = []
  const highlights = plan?.dailyHighlights
  if (!highlights || typeof highlights !== 'object') {
    return result
  }
  const entries = Object.entries(highlights)
  entries.sort((a, b) => Number(a[0]) - Number(b[0]))
  entries.forEach(([day, list], index) => {
    let items: string[] = []
    if (Array.isArray(list)) {
      items = list
    } else if (typeof list === 'string') {
      items = list.split(/[,，、\s]+/)
    }
    const cleaned = items
      .map(item => (typeof item === 'string' ? item.trim() : ''))
      .filter(Boolean)
      .slice(0, 4)
    if (cleaned.length) {
      const numericDay = Number(day)
      const displayDay = Number.isNaN(numericDay) ? index + 1 : numericDay
      result.push({
        day: displayDay,
        key: `${day}-${index}`,
        items: cleaned
      })
    }
  })
  return result
}

watch(
  () => editorForm.value.days,
  (newVal) => {
    if (!showEditor.value) return
    const safeDays = Math.max(1, Number(newVal) || 1)
    resizeEditorDays(safeDays)
  }
)

watch(
  () => editorForm.value.startDate,
  () => {
    if (!showEditor.value) return
    syncEndDate()
  }
)

async function onGenerate() {
  if (!form.value.destination?.trim()) {
    errorMessage.value = '请输入目的地'
    return
  }
  
  loading.value = true
  errorMessage.value = ''
  plans.value = []
  
  try {
    const res = await generateTrip({
      destination: form.value.destination.trim(),
      days: form.value.days,
      budget: form.value.budget,
      theme: form.value.theme?.trim()
    } as any)
    
    plans.value = res?.data?.data?.plans || []
    
    if (plans.value.length === 0) {
      errorMessage.value = '暂无生成方案，请调整需求后重试'
    } else {
      message.success(`成功生成 ${plans.value.length} 个方案`)
    }
  } catch (e: any) {
    const errorMsg = e?.response?.data?.message || e?.message || '生成失败，请重试'
    errorMessage.value = errorMsg
    message.error(errorMsg)
  } finally {
    loading.value = false
  }
}

async function onSave(plan: any, overrides?: CustomPlanOverrides) {
  savingId.value = plan.planId
  let success = false
  try {
    const resp = await saveTrip({
      planId: plan.planId,
      destination: overrides?.destination ?? plan.destination,
      days: overrides?.days ?? plan.days,
      budget: overrides?.budget ?? plan.budget,
      theme: overrides?.theme ?? plan.theme,
      startDate: overrides?.startDate ?? plan.startDate,
      endDate: overrides?.endDate ?? plan.endDate,
      dailyHighlights: overrides?.dailyHighlights ?? plan.dailyHighlights
    } as any)
    
    const tripId = resp?.data?.data
    if (tripId) {
      message.success('保存成功，正在跳转到行程详情...')
      setTimeout(() => {
        router.push(`/trips/${tripId}`)
      }, 500)
      success = true
    } else {
      throw new Error('未返回行程 ID')
    }
  } catch (e: any) {
    const errorMsg = e?.response?.data?.message || e?.message || '保存失败，请重试'
    message.error(errorMsg)
  } finally {
    savingId.value = null
  }
  return success
}

function openEditor(plan: any) {
  if (!plan) return
  editingPlan.value = plan
  editorForm.value = {
    destination: plan.destination || form.value.destination,
    days: Number(plan.days) || form.value.days || 1,
    budget: plan.budget ?? form.value.budget ?? null,
    theme: plan.theme || form.value.theme || '',
    startDate: plan.startDate || '',
    endDate: plan.endDate || '',
    description: plan.description || ''
  }
  editorDays.value = buildEditorDays(plan)
  showEditor.value = true
  syncEndDate()
}

function closeEditor() {
  if (customSaving.value) return
  showEditor.value = false
  editingPlan.value = null
  editorDays.value = []
}

function buildEditorDays(plan: any): EditorDay[] {
  const highlights = plan?.dailyHighlights
  const days = Number(plan?.days) || 1
  const result: EditorDay[] = []
  if (highlights && typeof highlights === 'object') {
    Object.entries(highlights)
      .sort((a, b) => Number(a[0]) - Number(b[0]))
      .forEach(([dayKey, list], index) => {
        let items: string[] = []
        if (Array.isArray(list)) {
          items = list
        } else if (typeof list === 'string') {
      items = list.split(/[,，、\s]+/)
        }
        const cleaned = items
          .map(item => (typeof item === 'string' ? item.trim() : ''))
          .filter(Boolean)
        result.push({
          day: Number.isNaN(Number(dayKey)) ? index + 1 : Number(dayKey),
          items: cleaned.length ? cleaned : ['']
        })
      })
  }

  if (!result.length) {
    for (let i = 0; i < days; i++) {
      result.push({
        day: i + 1,
        items: ['']
      })
    }
  }

  if (result.length < days) {
    const start = result.length
    for (let i = start; i < days; i++) {
      result.push({
        day: i + 1,
        items: ['']
      })
    }
  }

  return result.map((day, index) => ({
    day: index + 1,
    items: day.items.length ? [...day.items] : ['']
  }))
}

function resizeEditorDays(target: number) {
  if (target < 1) target = 1
  let list = [...editorDays.value]
  if (list.length > target) {
    list = list.slice(0, target)
  } else {
    while (list.length < target) {
      list.push({
        day: list.length + 1,
        items: ['']
      })
    }
  }
  editorDays.value = list.map((day, index) => ({
    day: index + 1,
    items: day.items.length ? [...day.items] : ['']
  }))
}

function addDay() {
  const nextDay = editorDays.value.length + 1
  editorDays.value.push({
    day: nextDay,
    items: ['']
  })
  editorForm.value.days = nextDay
}

function removeDay(index: number) {
  if (editorDays.value.length <= 1) {
    message.warning('请至少保留 1 天行程')
    return
  }
  editorDays.value.splice(index, 1)
  editorDays.value = editorDays.value.map((day, idx) => ({
    ...day,
    day: idx + 1
  }))
  editorForm.value.days = editorDays.value.length
}

function addHighlight(dayIndex: number) {
  editorDays.value[dayIndex].items.push('')
}

function removeHighlight(dayIndex: number, itemIndex: number) {
  const items = editorDays.value[dayIndex].items
  if (items.length <= 1) {
    items[0] = ''
    return
  }
  items.splice(itemIndex, 1)
}

function buildDailyHighlightsPayload() {
  const payload: Record<string, string[]> = {}
  editorDays.value.forEach((day, index) => {
    const cleaned = day.items
      .map(item => item?.trim())
      .filter((item): item is string => Boolean(item))
    if (cleaned.length) {
      payload[String(index + 1)] = cleaned
    }
  })
  return payload
}

async function saveCustomizedPlan() {
  if (!editingPlan.value) return
  if (!editorForm.value.destination.trim()) {
    message.warning('请填写目的地')
    return
  }
  customSaving.value = true
  const overrides: CustomPlanOverrides = {
    destination: editorForm.value.destination.trim(),
    days: editorForm.value.days,
    budget: editorForm.value.budget ?? null,
    theme: editorForm.value.theme?.trim(),
    startDate: editorForm.value.startDate || undefined,
    endDate: editorForm.value.endDate || undefined,
    dailyHighlights: buildDailyHighlightsPayload()
  }
  const success = await onSave(editingPlan.value, overrides)
  customSaving.value = false
  if (success) {
    closeEditor()
  }
}

function syncEndDate() {
  const start = editorForm.value.startDate
  if (!start) {
    editorForm.value.endDate = ''
    return
  }
  const startDate = new Date(start)
  if (Number.isNaN(startDate.getTime())) {
    editorForm.value.endDate = ''
    return
  }
  const days = Math.max(1, Number(editorForm.value.days) || 1)
  const endDate = new Date(startDate)
  endDate.setDate(endDate.getDate() + days - 1)
  editorForm.value.endDate = endDate.toISOString().split('T')[0]
}
</script>

<style scoped lang="scss">
.planner-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 24px;
  @media (max-width: 768px) {
    padding: 24px 16px;
  }
}

.page-header {
  text-align: center;
  margin-bottom: 24px;
  .page-title {
    font-size: 32px;
    font-weight: 700;
    color: var(--color-text);
    margin-bottom: 8px;
  }
  .page-subtitle {
    font-size: 16px;
    color: var(--color-muted);
    margin: 0;
  }
}

.form-card {
  margin-bottom: 24px;
}

.card-title {
  display: flex;
  flex-direction: column;
  gap: 4px;
  span:first-child {
    font-weight: 600;
    font-size: 18px;
    color: var(--color-text);
  }
}

.card-subtitle {
  font-size: 13px;
  color: var(--color-muted);
}

.planner-form {
  margin-top: 12px;
}

.full-width {
  width: 100%;
}

.hint-text {
  font-size: 13px;
  color: var(--color-muted);
}

.mt-20 {
  margin-top: 20px;
}

.plans-container {
  margin-top: 40px;
}

.plans-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 16px;
}

.plans-space {
  width: 100%;
}

.plan-card {
  border-radius: 12px;
}

.plan-desc {
  color: var(--color-text-secondary);
  line-height: 1.6;
  margin-bottom: 16px;
}

.plan-descriptions {
  margin-bottom: 16px;
}

.plan-highlights {
  margin-bottom: 16px;
}

.highlight-list {
  margin: 0;
  padding-left: 18px;
  color: var(--color-text-secondary);
  display: flex;
  flex-direction: column;
  gap: 4px;
  list-style: disc;
}

.plan-actions {
  display: flex;
  justify-content: flex-end;
}

.editor-subtitle {
  color: var(--color-muted);
  margin-bottom: 16px;
}

.editor-form {
  margin-bottom: 16px;
}

.editor-section-head {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.editor-section-title {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
}

.section-hint {
  margin: 4px 0 0;
  font-size: 13px;
  color: var(--color-muted);
}

.editor-days {
  width: 100%;
}

.highlight-column {
  width: 100%;
}

.highlight-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.drawer-footer {
  margin-top: 24px;
  display: flex;
  justify-content: flex-end;
}
</style>


