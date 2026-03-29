<template>
  <div class="planner-page" :class="{ embedded }">
    <!-- Hero Section -->
    <div v-if="!embedded" class="hero-section">
      <div class="hero-content">
        <h1 class="page-title">
          <RocketOutlined class="icon-primary" /> AI 智能行程规划
        </h1>
        <p class="page-subtitle">输入您的旅行偏好，AI 瞬间为您量身定制专属的完美假期方案</p>
      </div>
    </div>

    <div class="planner-content">
      <a-card class="form-card main-planner-card" :bordered="false">
        <a-form 
          layout="vertical" 
          class="planner-form" 
          :model="form" 
          @submit.prevent="onGenerate"
          @finish="onGenerate"
        >
          <a-row :gutter="[24, 16]">
            <a-col :xs="24" :md="8">
              <a-form-item required class="form-label-custom">
                <template #label><EnvironmentOutlined /> 目的地</template>
                <a-input 
                  size="large"
                  v-model:value="form.destination" 
                  :disabled="loading"
                  placeholder="如：云南大理、日本京都"
                  allow-clear
                />
              </a-form-item>
            </a-col>
            <a-col :xs="12" :md="5">
              <a-form-item required class="form-label-custom">
                <template #label><CalendarOutlined /> 游玩天数</template>
                <a-input-number 
                  size="large"
                  v-model:value="form.days" 
                  :min="1" 
                  :max="30"
                  :disabled="loading"
                  class="full-width"
                  addon-after="天"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="12" :md="5">
              <a-form-item class="form-label-custom">
                <template #label><PayCircleOutlined /> 预算</template>
                <a-input-number 
                  size="large"
                  v-model:value="form.budget" 
                  :min="0" 
                  :step="500"
                  class="full-width"
                  :disabled="loading"
                  placeholder="选填"
                  addon-before="¥"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="6">
              <a-form-item class="form-label-custom">
                <template #label><StarOutlined /> 旅行主题</template>
                <a-input 
                  size="large"
                  v-model:value="form.theme" 
                  :disabled="loading"
                  placeholder="选填: 美食、人文、亲子"
                  allow-clear
                />
              </a-form-item>
            </a-col>
          </a-row>
          <div class="submit-row">
            <a-button 
              type="primary" 
              html-type="submit" 
              size="large"
              shape="round"
              class="generate-btn"
              :loading="loading"
            >
              <template #icon><RobotOutlined v-if="!loading" /></template>
              {{ loading ? 'AI 正在极速规划中，请稍候...' : '开始生成专属方案' }}
            </a-button>
          </div>
        </a-form>
      </a-card>

      <a-alert 
        v-if="errorMessage" 
        type="error" 
        :message="errorMessage" 
        show-icon 
        class="mt-20 error-alert"
      />

      <div v-if="loading && !plans.length" class="loading-wrapper mt-40">
        <a-row :gutter="[24, 24]">
          <a-col :xs="24" :lg="12" v-for="i in 2" :key="i">
            <a-card class="skeleton-card" :bordered="false">
              <a-skeleton active avatar :paragraph="{ rows: 4 }" />
            </a-card>
          </a-col>
        </a-row>
      </div>

      <div v-if="plans.length > 0" class="plans-container fade-in mt-40">
        <div class="plans-header">
          <h3 class="plans-title"><BulbOutlined /> 为您推荐的 {{ plans.length }} 个备选方案</h3>
          <p class="plans-tip">选择最心仪的方案，还可进入「自定义编辑」调整细节或直接保存到「我的行程」</p>
        </div>
        
        <a-row :gutter="[24, 24]" class="plans-row">
          <a-col :xs="24" :lg="12" v-for="(p, index) in plans" :key="p.planId">
            <a-badge-ribbon :text="`方案 ${index + 1}`" color="blue" class="plan-ribbon">
              <a-card class="plan-card" hoverable :bordered="false">
                <div class="plan-card-header">
                  <div class="plan-title-wrapper">
                    <h4 class="plan-title">{{ p.destination }} · {{ p.days || form.days }}天</h4>
                    <div class="plan-tags">
                      <a-tag v-if="p.theme" color="arc" class="theme-tag"><TagOutlined /> {{ p.theme }}</a-tag>
                      <a-tag v-else color="default" class="theme-tag">通用主题</a-tag>
                      <a-tag color="orange" v-if="(p.budget ?? 0) > 0"><PayCircleOutlined /> {{ formatBudget(p.budget) }}</a-tag>
                    </div>
                  </div>
                </div>

                <p class="plan-desc">{{ p.description || '精心安排的路线，充满未知的惊喜与体验。' }}</p>

                <div class="plan-highlights-wrapper">
                  <div class="highlight-title">行程速览</div>
                  <div v-if="getPlanHighlights(p).length" class="plan-highlights">
                    <a-collapse ghost :bordered="false" expand-icon-position="end">
                      <a-collapse-panel 
                        v-for="day in getPlanHighlights(p)" 
                        :key="day.key" 
                        :header="`Day ${day.day} 亮点 (${day.items.length})`"
                        class="custom-collapse-panel"
                      >
                        <ul class="highlight-list">
                          <li v-for="(item, idx) in day.items" :key="idx">{{ item }}</li>
                        </ul>
                      </a-collapse-panel>
                    </a-collapse>
                  </div>
                  <a-empty v-else description="暂无每日亮点摘要" :image-style="{ height: '60px' }" />
                </div>

                <div class="plan-actions">
                  <a-button 
                    class="action-btn"
                    @click="openEditor(p)" 
                    :disabled="savingId === p.planId"
                  >
                    <EditOutlined /> 自定义编辑
                  </a-button>
                  <a-button 
                    type="primary" 
                    class="action-btn save-btn"
                    @click="onSave(p)" 
                    :loading="savingId === p.planId"
                  >
                    <CheckCircleOutlined /> 采用该方案
                  </a-button>
                </div>
              </a-card>
            </a-badge-ribbon>
          </a-col>
        </a-row>
      </div>
    </div>

    <!-- DIY Editor Drawer -->
    <a-drawer 
      title="🛠️ DIY 自定义调整" 
      placement="right" 
      size="large"
      :open="showEditor" 
      :mask-closable="!customSaving"
      :closable="!customSaving"
      @close="closeEditor"
      class="editor-drawer"
    >
      <div class="editor-header-info">
        温馨提示：根据您的个人喜好，对地点或天数进行灵活调整。保存后将在「我的行程」中随时查看。
      </div>
      
      <div class="drawer-scroll-area">
        <a-form layout="vertical" class="editor-form" :model="editorForm">
          <a-row :gutter="16">
            <a-col :xs="24" :md="12">
              <a-form-item label="目的地" required>
                <a-input v-model:value="editorForm.destination" placeholder="如：北京" size="large" />
              </a-form-item>
            </a-col>
            <a-col :xs="12" :md="6">
              <a-form-item label="天数" required>
                <a-input-number v-model:value="editorForm.days" :min="1" class="full-width" size="large" />
              </a-form-item>
            </a-col>
            <a-col :xs="12" :md="6">
              <a-form-item label="预算（元）">
                <a-input-number v-model:value="editorForm.budget" :min="0" class="full-width" placeholder="可选" size="large" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="12">
              <a-form-item label="主题">
                <a-input v-model:value="editorForm.theme" placeholder="休闲 / 亲子 / 美食..." size="large" />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="12">
              <a-form-item label="出发日期">
                <a-date-picker 
                  v-model:value="editorForm.startDate" 
                  value-format="YYYY-MM-DD"
                  class="full-width"
                  size="large"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="12">
              <a-form-item label="结束日期">
                <a-input v-model:value="editorForm.endDate" disabled size="large" />
              </a-form-item>
            </a-col>
            <a-col :span="24">
              <a-form-item label="行程一句话摘要">
                <a-textarea 
                  v-model:value="editorForm.description" 
                  :rows="3"
                  placeholder="可简单描述此次行程的最大特色，分享给小伙伴..."
                />
              </a-form-item>
            </a-col>
          </a-row>
        </a-form>

        <div class="editor-section-head">
          <div class="head-text-group">
            <h4 class="editor-section-title">每日规划预览</h4>
            <p class="section-hint">每一天可添加多个途经点或体验，方便后续完善打卡</p>
          </div>
          <a-button type="dashed" class="add-day-btn" @click="addDay"><PlusOutlined /> 新增一天</a-button>
        </div>

        <a-space direction="vertical" size="large" class="editor-days">
          <a-card 
            v-for="(day, dayIndex) in editorDays" 
            :key="`editor-day-${day.day}-${dayIndex}`"
            class="day-edit-card"
          >
            <template #title>
              <div class="day-edit-title">
                <span class="day-badge">D{{ dayIndex + 1 }}</span>
                <span>第 {{ dayIndex + 1 }} 天安排</span>
              </div>
            </template>
            <template #extra>
              <a-button 
                type="text" 
                danger 
                @click="removeDay(dayIndex)" 
                :disabled="editorDays.length <= 1"
                class="del-btn"
              >
                <DeleteOutlined />
              </a-button>
            </template>

            <div class="highlight-column">
              <div 
                v-for="(item, itemIdx) in day.items" 
                :key="`day-${dayIndex}-item-${itemIdx}`" 
                class="highlight-row"
              >
                <div class="drag-handle"><MenuOutlined /></div>
                <a-input 
                  v-model:value="editorDays[dayIndex].items[itemIdx]" 
                  placeholder="例如：上午前往 XX 景区，下午打卡地道美食"
                  class="highlight-input"
                />
                <a-button 
                  type="text" 
                  danger 
                  @click="removeHighlight(dayIndex, itemIdx)" 
                  :disabled="day.items.length <= 1"
                  shape="circle"
                >
                  <MinusCircleOutlined />
                </a-button>
              </div>
              <a-button type="dashed" block @click="addHighlight(dayIndex)" class="add-hl-btn">
                <PlusOutlined /> 添加行程节点
              </a-button>
            </div>
          </a-card>
        </a-space>
      </div>

      <div class="drawer-footer-fixed">
        <a-button @click="closeEditor" :disabled="customSaving" size="large">取消</a-button>
        <a-button type="primary" @click="saveCustomizedPlan" :loading="customSaving" size="large">
          <SaveOutlined /> 保存到我的行程
        </a-button>
      </div>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { generateTrip, saveTrip } from '@/api/tripController'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { 
  RobotOutlined, RocketOutlined, EnvironmentOutlined, CalendarOutlined, 
  PayCircleOutlined, StarOutlined, EditOutlined, CheckCircleOutlined,
  TagOutlined, BulbOutlined, PlusOutlined, DeleteOutlined, 
  MinusCircleOutlined, MenuOutlined, SaveOutlined
} from '@ant-design/icons-vue'

withDefaults(defineProps<{ embedded?: boolean }>(), {
  embedded: false
})

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
type PlannerPlan = API.TripPlan & {
  startDate?: string
  endDate?: string
}

const plans = ref<PlannerPlan[]>([])
const showEditor = ref(false)
const editingPlan = ref<PlannerPlan | null>(null)
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

function getPlanHighlights(plan: PlannerPlan) {
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
    errorMessage.value = '请先输入想去的目的地哦'
    return
  }
  
  loading.value = true
  errorMessage.value = ''
  plans.value = []
  window.scrollTo({ top: 0, behavior: 'smooth' })
  
  try {
    const request: API.TripGenerateRequest = {
      destination: form.value.destination.trim(),
      days: form.value.days,
      budget: form.value.budget,
      theme: form.value.theme?.trim()
    }

    const res = await generateTrip(request)
    
    const generatedPlans = res?.data?.data?.plans || []
    plans.value = generatedPlans as PlannerPlan[]
    
    if (plans.value.length === 0) {
      errorMessage.value = '暂无生成方案，可以尝试换个目的地或主题重试'
    } else {
      message.success(`太棒了！成功为您生成 ${plans.value.length} 个专属旅行方案`)
    }
  } catch (e: unknown) {
    const responseMessage =
      typeof e === 'object' && e !== null && 'response' in e
        ? (e as { response?: { data?: { message?: string } } }).response?.data?.message
        : undefined
    const runtimeMessage = e instanceof Error ? e.message : undefined
    const errorMsg = responseMessage || runtimeMessage || '生成小意外，请稍后再试'
    errorMessage.value = errorMsg
    message.error(errorMsg)
  } finally {
    loading.value = false
  }
}

async function onSave(plan: PlannerPlan, overrides?: CustomPlanOverrides) {
  if (!plan.planId) {
    message.error('方案缺少 planId，无法保存')
    return false
  }

  savingId.value = plan.planId
  let success = false
  try {
    const mergedBudget = overrides?.budget ?? plan.budget
    const request: API.TripSaveRequest = {
      planId: plan.planId,
      destination: overrides?.destination ?? plan.destination,
      days: overrides?.days ?? plan.days,
      budget: mergedBudget == null ? undefined : mergedBudget,
      theme: overrides?.theme ?? plan.theme,
      startDate: overrides?.startDate ?? plan.startDate,
      endDate: overrides?.endDate ?? plan.endDate,
      dailyHighlights: overrides?.dailyHighlights ?? plan.dailyHighlights
    }

    const resp = await saveTrip(request)
    
    const tripId = resp?.data?.data
    if (tripId) {
      message.success('保存成功，即刻开启您的旅程吧！')
      setTimeout(() => {
        router.push(`/trips/${tripId}`)
      }, 500)
      success = true
    } else {
      throw new Error('未返回行程 ID')
    }
  } catch (e: unknown) {
    const responseMessage =
      typeof e === 'object' && e !== null && 'response' in e
        ? (e as { response?: { data?: { message?: string } } }).response?.data?.message
        : undefined
    const runtimeMessage = e instanceof Error ? e.message : undefined
    const errorMsg = responseMessage || runtimeMessage || '保存失败，请稍后重试'
    message.error(errorMsg)
  } finally {
    savingId.value = null
  }
  return success
}

function openEditor(plan: PlannerPlan) {
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

function buildEditorDays(plan: PlannerPlan): EditorDay[] {
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
/* --- Variables & Presets --- */
$primary-color: var(--ant-primary-color, #1890ff);
$text-main: #333;
$text-secondary: #666;
$text-light: #999;
$border-radius: 16px;
$transition-base: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);

/* --- Page Layout --- */
.planner-page {
  min-height: 100vh;
  background-color: #f7f9fc;
  padding-bottom: 60px;
  
  &.embedded {
    min-height: auto;
    background-color: transparent;
    padding-bottom: 0;
  }
}

.planner-content {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  position: relative;
  z-index: 2;

  @media (max-width: 768px) {
    padding: 0 16px;
  }
}

/* --- Hero Section --- */
.hero-section {
  position: relative;
  background: linear-gradient(135deg, #e0f2fe 0%, #ffffff 100%);
  padding: 60px 24px 80px;
  text-align: center;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: -50%;
    left: -10%;
    width: 50%;
    height: 200%;
    background: radial-gradient(circle, rgba(24,144,255,0.05) 0%, rgba(255,255,255,0) 70%);
    transform: rotate(30deg);
  }

  &::after {
    content: '';
    position: absolute;
    bottom: -50%;
    right: -10%;
    width: 60%;
    height: 150%;
    background: radial-gradient(circle, rgba(250,173,20,0.05) 0%, rgba(255,255,255,0) 70%);
  }

  .hero-content {
    position: relative;
    z-index: 1;
    max-width: 800px;
    margin: 0 auto;
  }

  .page-title {
    font-size: 40px;
    font-weight: 800;
    color: #1f2937;
    margin-bottom: 16px;
    letter-spacing: -0.5px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;

    .icon-primary {
      color: $primary-color;
      font-size: 36px;
    }
  }

  .page-subtitle {
    font-size: 18px;
    color: #64748b;
    margin: 0;
  }

  @media (max-width: 768px) {
    padding: 40px 16px 60px;
    .page-title { font-size: 28px; }
    .page-subtitle { font-size: 15px; }
  }
}

/* --- Form Card --- */
.main-planner-card {
  margin-top: -40px;
  border-radius: $border-radius;
  box-shadow: 0 12px 32px rgba(0,0,0,0.06);
  padding: 12px;

  @media (max-width: 768px) {
    margin-top: -20px;
  }
}

.planner-form {
  .form-label-custom {
    :deep(.ant-form-item-label > label) {
      font-weight: 600;
      color: #475569;
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 15px;
      
      .anticon {
        color: $primary-color;
      }
    }
  }

  .full-width {
    width: 100%;
  }

  .submit-row {
    margin-top: 16px;
    display: flex;
    justify-content: center;
  }

  .generate-btn {
    min-width: 240px;
    height: 54px;
    font-size: 18px;
    font-weight: 600;
    box-shadow: 0 6px 16px rgba(24, 144, 255, 0.3);
    transition: transform 0.2s, box-shadow 0.2s;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 8px 24px rgba(24, 144, 255, 0.4);
    }
  }
}

/* --- Common UI --- */
.mt-20 { margin-top: 20px; }
.mt-40 { margin-top: 40px; }

.fade-in {
  animation: fadeIn 0.6s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}

.error-alert {
  border-radius: 8px;
}

.skeleton-card {
  border-radius: $border-radius;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}

/* --- Plans Section --- */
.plans-header {
  margin-bottom: 24px;
  text-align: center;

  .plans-title {
    font-size: 28px;
    font-weight: 700;
    color: #1e293b;
    margin-bottom: 8px;

    .anticon {
      color: #fadb14;
    }
  }

  .plans-tip {
    font-size: 15px;
    color: #64748b;
  }
}

/* --- Plan Cards --- */
.plan-ribbon {
  :deep(.ant-ribbon-text) {
    font-weight: 600;
    font-size: 14px;
  }
}

.plan-card {
  height: 100%;
  border-radius: $border-radius;
  box-shadow: 0 6px 20px rgba(0,0,0,0.04);
  transition: $transition-base;
  display: flex;
  flex-direction: column;

  &:hover {
    transform: translateY(-4px);
    box-shadow: 0 16px 32px rgba(0,0,0,0.08);
  }

  :deep(.ant-card-body) {
    padding: 24px;
    display: flex;
    flex-direction: column;
    height: 100%;
  }

  .plan-card-header {
    margin-bottom: 16px;
    border-bottom: 1px solid #f1f5f9;
    padding-bottom: 16px;
  }

  .plan-title-wrapper {
    .plan-title {
      font-size: 20px;
      font-weight: 700;
      color: #0f172a;
      margin: 0 0 12px 0;
      line-height: 1.3;
    }
    
    .plan-tags {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;

      .theme-tag {
        border-radius: 4px;
      }
    }
  }

  .plan-desc {
    color: #475569;
    font-size: 15px;
    line-height: 1.6;
    margin-bottom: 20px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .plan-highlights-wrapper {
    flex: 1;
    background: #f8fafc;
    border-radius: 12px;
    padding: 16px;
    margin-bottom: 20px;

    .highlight-title {
      font-weight: 600;
      color: #334155;
      margin-bottom: 12px;
      font-size: 15px;
    }
  }

  .custom-collapse-panel {
    border-bottom: 1px solid #e2e8f0 !important;
    
    &:last-child {
      border-bottom: none !important;
    }

    :deep(.ant-collapse-header) {
      padding: 10px 0 !important;
      font-weight: 600;
      color: #0f172a !important;
    }

    :deep(.ant-collapse-content-box) {
      padding: 0 0 12px 0 !important;
    }
  }

  .highlight-list {
    margin: 0;
    padding-left: 20px;
    color: #475569;
    display: flex;
    flex-direction: column;
    gap: 6px;
    
    li {
      line-height: 1.5;
      &::marker {
        color: $primary-color;
      }
    }
  }

  .plan-actions {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    margin-top: auto;
    padding-top: 12px;

    .action-btn {
      border-radius: 8px;
      font-weight: 500;
      height: 40px;
      display: flex;
      align-items: center;
      gap: 6px;
    }

    .save-btn {
      box-shadow: 0 4px 10px rgba(24, 144, 255, 0.2);
    }
  }
}

/* --- Custom Editor Drawer --- */
.editor-drawer {
  :deep(.ant-drawer-body) {
    padding: 0;
    display: flex;
    flex-direction: column;
  }
}

.editor-header-info {
  background: #fffbe6;
  color: #d48806;
  padding: 12px 24px;
  font-size: 14px;
  border-bottom: 1px solid #ffe58f;
}

.drawer-scroll-area {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}

.editor-section-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 32px 0 16px;
  padding-bottom: 12px;
  border-bottom: 2px solid #f1f5f9;

  .editor-section-title {
    font-size: 18px;
    font-weight: 700;
    margin: 0;
    color: #1e293b;
  }
  
  .section-hint {
    margin: 4px 0 0;
    font-size: 13px;
    color: #64748b;
  }

  .add-day-btn {
    border-radius: 20px;
  }
}

.day-edit-card {
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  box-shadow: none;

  .day-edit-title {
    display: flex;
    align-items: center;
    gap: 10px;
    font-weight: 600;

    .day-badge {
      background: $primary-color;
      color: #fff;
      padding: 2px 8px;
      border-radius: 6px;
      font-size: 13px;
      line-height: 1;
    }
  }

  .del-btn {
    color: #ff4d4f;
    &:hover { background: #fff1f0; }
  }

  :deep(.ant-card-head) {
    background: #f8fafc;
    border-bottom: 1px solid #e2e8f0;
    min-height: 48px;
  }

  :deep(.ant-card-body) {
    padding: 16px;
  }
}

.highlight-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  background: #fff;
  transition: all 0.2s;

  .drag-handle {
    color: #cbd5e1;
    cursor: grab;
    &:hover { color: #94a3b8; }
  }

  .highlight-input {
    border-radius: 8px;
  }
}

.add-hl-btn {
  border-radius: 8px;
  color: $primary-color;
  border-color: #bae0ff;
  
  &:hover {
    border-color: $primary-color;
    background: #e6f7ff;
  }
}

.drawer-footer-fixed {
  padding: 16px 24px;
  border-top: 1px solid #e2e8f0;
  background: #fff;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  box-shadow: 0 -4px 12px rgba(0,0,0,0.03);
  
  .ant-btn {
    border-radius: 8px;
    min-width: 120px;
  }
}
</style>