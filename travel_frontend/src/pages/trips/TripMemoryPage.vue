<template>
  <div class="memory-page" v-if="tripId">
    <div class="page-header">
      <h1 class="page-title">生成回忆图</h1>
      <p class="page-subtitle">上传旅行照片，AI 将为您生成精美的回忆图</p>
    </div>

    <div class="content-container">
      <!-- 照片上传区域 -->
      <div class="upload-section">
        <div class="section-card">
          <h3 class="section-title">上传照片（3-6张）</h3>
          <p class="section-desc">支持 JPG、PNG 格式，单张图片不超过 5MB</p>
          
          <!-- 照片预览网格 -->
          <div class="photos-grid">
            <div 
              v-for="(photo, idx) in photos" 
              :key="idx" 
              class="photo-item"
            >
              <div class="photo-preview">
                <img :src="photo.url" :alt="`照片 ${idx + 1}`" />
                <button class="remove-btn" @click="removePhoto(idx)">
                  <img src="https://unpkg.com/lucide-static@latest/icons/x.svg" alt="remove" />
                </button>
              </div>
              <div v-if="photo.uploading" class="upload-progress">
                <span class="spinner"></span>
                <span>上传中...</span>
              </div>
            </div>
            
            <!-- 添加照片按钮 -->
            <div 
              v-if="photos.length < 6" 
              class="photo-upload-btn" 
              @click="chooseUpload"
            >
              <img src="https://unpkg.com/lucide-static@latest/icons/plus.svg" class="plus-icon" alt="add" />
              <span>添加照片</span>
            </div>
          </div>

          <!-- URL输入方式（备选） -->
          <div class="url-input-section">
            <div class="url-toggle" @click="showUrlInput = !showUrlInput">
              <span>或使用图片URL</span>
              <img 
                src="https://unpkg.com/lucide-static@latest/icons/chevron-down.svg" 
                :class="{ 'rotated': showUrlInput }"
                alt="toggle"
              />
            </div>
            <div v-if="showUrlInput" class="url-inputs">
              <div v-for="(url, idx) in urlInputs" :key="idx" class="url-row">
                <input 
                  v-model="urlInputs[idx]" 
                  placeholder="https://..." 
                  class="url-input"
                  @blur="onUrlBlur(idx)"
                />
                <button class="ghost-btn" @click="removeUrl(idx)">删除</button>
              </div>
              <button v-if="urlInputs.length < 6" class="ghost-btn" @click="addUrl">添加URL</button>
            </div>
          </div>

          <input 
            ref="fileInput" 
            type="file" 
            accept="image/*" 
            multiple 
            hidden 
            @change="onFiles" 
          />
        </div>
      </div>

      <!-- 已关联图片库 -->
      <div class="gallery-section">
        <div class="section-card">
          <h3 class="section-title">已关联图片（{{ tripPhotos.length }}/6）</h3>
          <div class="photos-grid" v-if="tripPhotos.length">
            <div
              v-for="p in tripPhotos"
              :key="p.id"
              class="photo-item"
            >
              <div class="photo-preview">
                <img :src="p.photoUrl" :alt="`照片 ${p.id}`" />
                <button class="remove-btn" @click="onDeletePhoto(p.id)">
                  <img src="https://unpkg.com/lucide-static@latest/icons/trash-2.svg" alt="delete" />
                </button>
              </div>
            </div>
          </div>
          <p v-else class="hint-text">还没有关联图片</p>
        </div>
      </div>

      <!-- 生成按钮 -->
      <div class="actions-section">
        <button 
          @click="startGenerate" 
          class="generate-btn"
          :disabled="starting || !canGenerate"
          :class="{ 'loading': starting }"
        >
          <span v-if="!starting">
            <img src="https://unpkg.com/lucide-static@latest/icons/sparkles.svg" class="btn-icon" alt="generate" />
            生成回忆图
          </span>
          <span v-else class="loading-content">
            <span class="spinner"></span>
            <span>提交中...</span>
          </span>
        </button>
        <p v-if="hasUploading" class="hint-text">正在上传图片，请稍候...</p>
        <p v-else-if="!canGenerate" class="hint-text">请至少保证已有 3 张已关联照片，或选择新增图片（总数不超过 6 张）</p>
      </div>

      <!-- 错误提示 -->
      <div v-if="errorMessage" class="error-message">
        <img src="https://unpkg.com/lucide-static@latest/icons/alert-circle.svg" class="error-icon" alt="error">
        <span>{{ errorMessage }}</span>
      </div>

      <!-- 生成进度 -->
      <div v-if="taskId" class="progress-section">
        <div class="section-card">
          <h3 class="section-title">生成进度</h3>
          <div class="progress-info">
            <div class="status-item">
              <span class="status-label">任务ID：</span>
              <span class="status-value">{{ taskId }}</span>
            </div>
            <div class="status-item">
              <span class="status-label">状态：</span>
              <span class="status-badge" :class="statusClass">{{ statusText }}</span>
            </div>
            <div class="status-item" v-if="errorMessage">
              <span class="status-label">原因：</span>
              <span class="status-value">{{ errorMessage }}</span>
            </div>
          </div>
          
          <!-- 进度条 -->
          <div v-if="status === 'pending' || status === 'processing'" class="progress-bar">
            <div class="progress-fill" :style="{ width: progressWidth }"></div>
          </div>

          <!-- 生成的回忆图 -->
          <div v-if="imageUrl" class="result-preview">
            <h4 class="result-title">生成的回忆图</h4>
            <div class="image-container">
              <img :src="imageUrl" alt="回忆图" />
              <div class="image-actions">
                <button class="action-btn" @click="downloadImage">
                  <img src="https://unpkg.com/lucide-static@latest/icons/download.svg" alt="download" />
                  下载
                </button>
                <button class="action-btn primary" @click="goToTrip">
                  <img src="https://unpkg.com/lucide-static@latest/icons/arrow-right.svg" alt="view" />
                  查看行程
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onUnmounted, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { uploadPhotos, getTripPhotos, deletePhoto } from '@/api/tripPhotoController'
import { generateMemoryCard, getMemoryCardStatus, getMemoryCardByTripId } from '@/api/memoryCardController'
import request from '@/request'
import { message } from 'ant-design-vue'

const route = useRoute()
const router = useRouter()
const tripId = Number(route.params.id) || null

// 照片数据
interface PhotoItem {
  url: string
  uploading: boolean
}

const photos = ref<PhotoItem[]>([])
const urlInputs = ref<string[]>([''])
const showUrlInput = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)
// 已关联的行程照片（来自后端）
const tripPhotos = ref<any[]>([])

// 生成状态
const starting = ref(false)
const taskId = ref<string | null>(null)
const status = ref<'pending' | 'processing' | 'success' | 'failed'>('pending')
const imageUrl = ref<string | null>(null)
const errorMessage = ref('')
let timer: any = null
let pollCount = 0

// 计算属性
const hasUploading = computed(() => photos.value.some(p => p.uploading))

const canGenerate = computed(() => {
  if (hasUploading.value) return false
  const newCount = photos.value.length + urlInputs.value.filter(u => u.trim()).length
  const existingCount = tripPhotos.value?.length || 0
  if (newCount > 0) {
    return newCount <= 6 && (existingCount + newCount) <= 6
  }
  // 无新增时，已有照片达到3张即可生成
  return existingCount >= 3
})

const statusText = computed(() => {
  const map: Record<string, string> = {
    'pending': '等待中',
    'processing': '生成中',
    'success': '生成成功',
    'failed': '生成失败'
  }
  return map[status.value] || status.value
})

const statusClass = computed(() => {
  return {
    'status-pending': status.value === 'pending',
    'status-processing': status.value === 'processing',
    'status-success': status.value === 'success',
    'status-failed': status.value === 'failed'
  }
})

const progressWidth = computed(() => {
  if (status.value === 'pending') return '30%'
  if (status.value === 'processing') return '60%'
  if (status.value === 'success') return '100%'
  return '0%'
})

// 初始化：检查是否已有回忆图
onMounted(async () => {
  if (tripId) {
    try {
      const resp = await getMemoryCardByTripId({ tripId } as any)
      const memoryCard = resp?.data?.data
      if (memoryCard) {
        if (memoryCard.imageUrl) {
          imageUrl.value = memoryCard.imageUrl
          status.value = 'success'
          // 成功时清空历史错误
          errorMessage.value = ''
        } else if (memoryCard.taskId) {
          taskId.value = memoryCard.taskId
          status.value = (memoryCard.status as any) || 'pending'
          if (status.value !== 'success' && status.value !== 'failed') {
            poll()
          }
        }
      }
    } catch (e) {
      // 忽略错误，可能是还没有回忆图
    }

    // 加载已关联图片库
    await loadTripPhotos()
  }
})

// 文件操作
function chooseUpload() {
  fileInput.value?.click()
}

async function onFiles(e: Event) {
  const input = e.target as HTMLInputElement
  const files = input.files
  if (!files || !files.length) return

  const fileArray = Array.from(files)
  if (photos.value.length + fileArray.length > 6) {
    message.warning('最多只能上传 6 张照片')
    fileArray.splice(6 - photos.value.length)
  }

  for (const file of fileArray) {
    // 验证文件类型
    if (!file.type.startsWith('image/')) {
      message.error(`${file.name} 不是有效的图片文件`)
      continue
    }

    // 验证文件大小（5MB）
    if (file.size > 5 * 1024 * 1024) {
      message.error(`${file.name} 文件大小超过 5MB`)
      continue
    }

    // 创建预览
    const previewUrl = URL.createObjectURL(file)
    const photoItem: PhotoItem = {
      url: previewUrl,
      uploading: true
    }
    photos.value.push(photoItem)

    // 上传文件
    try {
      const formData = new FormData()
      formData.append('file', file)
      formData.append('picName', file.name)

      const result = await request.post('/picture/upload', formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      })

      // API返回结构: { data: { code: 0, data: { url: '...' } } }
      if (result?.data?.code === 0 && result?.data?.data?.url) {
        photoItem.url = result.data.data.url
        message.success(`${file.name} 上传成功`)
      } else {
        // 添加调试信息
        console.error('上传响应:', result)
        throw new Error(result?.data?.message || '上传失败：未返回图片URL')
      }
    } catch (error: any) {
      console.error('上传错误详情:', error)
      const errorMsg = error?.response?.data?.message || error?.message || '未知错误'
      message.error(`${file.name} 上传失败：${errorMsg}`)
      // 移除失败的照片
      const index = photos.value.indexOf(photoItem)
      if (index > -1) {
        URL.revokeObjectURL(previewUrl)
        photos.value.splice(index, 1)
      }
      continue
    } finally {
      photoItem.uploading = false
    }
  }

  // 保险：循环完成后，强制将所有uploading标记重置为false，避免个别状态残留
  photos.value.forEach(p => { if (p.uploading) p.uploading = false })
  // 触发一次响应更新
  photos.value = photos.value.slice()

  input.value = ''
}

function removePhoto(index: number) {
  const photo = photos.value[index]
  // 如果是预览URL，需要释放
  if (photo.url.startsWith('blob:')) {
    URL.revokeObjectURL(photo.url)
  }
  photos.value.splice(index, 1)
}

// URL输入
function addUrl() {
  if (urlInputs.value.length < 6) {
    urlInputs.value.push('')
  }
}

function removeUrl(index: number) {
  urlInputs.value.splice(index, 1)
}

function onUrlBlur(index: number) {
  const url = urlInputs.value[index]?.trim()
  if (!url) {
    // 如果URL为空且不是最后一个，可以移除
    if (urlInputs.value.length > 1 && index < urlInputs.value.length - 1) {
      urlInputs.value.splice(index, 1)
    }
  }
}

// 生成回忆图
async function startGenerate() {
  if (!canGenerate.value) {
    errorMessage.value = '请上传 3-6 张照片'
    return
  }

  // 收集所有照片URL
  const photoUrls: string[] = [
    ...photos.value.map(p => p.url),
    ...urlInputs.value.filter(u => u.trim())
  ]

  if (photoUrls.length > 6) {
    errorMessage.value = '照片数量必须在 3-6 张之间'
    return
  }

  // 预检：已有照片数量 + 本次选择数量 不得超过 6
  try {
    const existResp = await getTripPhotos({ tripId: tripId as number } as any)
    const existingCount: number = Array.isArray(existResp?.data?.data) ? existResp.data.data.length : 0
    const toAddCount = photoUrls.length
    if (existingCount + toAddCount > 6) {
      const msg = `当前行程已有关联 ${existingCount} 张照片，本次选择 ${toAddCount} 张，合计 ${existingCount + toAddCount} 张，超过 6 张上限`
      errorMessage.value = msg
      message.error(msg)
      return
    }
  } catch (e) {
    // 查询失败不应继续生成，避免触发后端校验错误
    const msg = '查询已有照片失败，请稍后重试'
    errorMessage.value = msg
    message.error(msg)
    return
  }

  starting.value = true
  errorMessage.value = ''

  try {
    // 如果有新增图片，则先进行关联；否则用已有图片直接生成
    if (photoUrls.length > 0) {
      const uploadResp = await uploadPhotos({ tripId: tripId as number } as any, photoUrls)
      if (uploadResp?.data?.code !== 0) {
        const msg = uploadResp?.data?.message || '照片关联失败'
        throw new Error(msg)
      }
      // 关联成功后刷新图片库
      await loadTripPhotos()
      // 清空本地选择与输入，避免残留“上传中/预览”
      photos.value = []
      urlInputs.value = ['']
    }

    // 准备用于生成的图片URL：优先使用“新增”，否则使用“已关联”
    let generateUrls: string[] = photoUrls
    if (generateUrls.length === 0) {
      const existResp2 = await getTripPhotos({ tripId: tripId as number } as any)
      const existingList: any[] = Array.isArray(existResp2?.data?.data) ? existResp2.data.data : []
      generateUrls = existingList.map(p => p.photoUrl).slice(0, 6)
      if (generateUrls.length < 3) {
        throw new Error('请至少保证已有 3 张已关联照片或选择新增图片')
      }
    }

    // 创建回忆图生成任务
    const resp = await generateMemoryCard({
      tripId: tripId as number,
      photoUrls: generateUrls,
      templateName: 'default'
    } as any)

    taskId.value = resp?.data?.data?.taskId || null
    status.value = (resp?.data?.data?.status as any) || 'pending'

    if (taskId.value) {
      message.success('回忆图生成任务已提交，请稍候...')
      poll()
    } else {
      throw new Error('未返回任务ID')
    }
  } catch (e: any) {
    const errorMsg = e?.response?.data?.message || e?.message || '提交失败，请重试'
    errorMessage.value = errorMsg
    message.error(errorMsg)
  } finally {
    starting.value = false
  }
}

// 轮询任务状态
function poll() {
  clearInterval(timer)
  pollCount = 0
  
  timer = setInterval(async () => {
    if (!taskId.value) {
      clearInterval(timer)
      return
    }

    pollCount++
    // 最多轮询 5 分钟（100次 * 3秒）
    if (pollCount > 100) {
      clearInterval(timer)
      status.value = 'failed'
      errorMessage.value = '生成超时，请重试'
      message.error('生成超时，请重试')
      return
    }

    try {
      const r = await getMemoryCardStatus({ taskId: taskId.value } as any)
      const data = r?.data?.data

      if (data) {
        console.debug('回忆图状态返回:', data)
        status.value = (data.status as any) || status.value
        // 进入非失败态时，清理历史错误
        if (status.value === 'pending' || status.value === 'processing') {
          errorMessage.value = ''
        }
        if (data.imageUrl) {
          imageUrl.value = data.imageUrl
        }
        if (data.errorMessage && status.value !== 'success') {
          errorMessage.value = data.errorMessage
        }

        if (status.value === 'success') {
          // 成功时清空错误并停止轮询
          errorMessage.value = ''
          clearInterval(timer)
          message.success('回忆图生成成功！')
        } else if (status.value === 'failed') {
          clearInterval(timer)
          message.error(errorMessage.value || '回忆图生成失败')
        }
      }
    } catch (e: any) {
      // 保持轮询，错误忽略
      console.error('轮询状态失败:', e)
    }
  }, 3000)
}

// 加载行程图片库
async function loadTripPhotos() {
  if (!tripId) return
  try {
    const resp = await getTripPhotos({ tripId: tripId as number } as any)
    tripPhotos.value = Array.isArray(resp?.data?.data) ? resp.data.data : []
  } catch (e) {
    // 静默失败
    tripPhotos.value = []
  }
}

// 删除行程图片
async function onDeletePhoto(photoId: number) {
  try {
    const r = await deletePhoto({ photoId } as any)
    if (r?.data?.code === 0) {
      message.success('已删除照片')
      // 刷新图片库
      await loadTripPhotos()
    } else {
      const msg = r?.data?.message || '删除失败'
      message.error(msg)
    }
  } catch (e: any) {
    const msg = e?.response?.data?.message || e?.message || '删除失败'
    message.error(msg)
  }
}

// 下载图片
function downloadImage() {
  if (!imageUrl.value) return
  
  const link = document.createElement('a')
  link.href = imageUrl.value
  link.download = `回忆图-${tripId}-${Date.now()}.jpg`
  link.target = '_blank'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

// 返回行程详情
function goToTrip() {
  router.push(`/trips/${tripId}`)
}

onUnmounted(() => {
  clearInterval(timer)
  // 清理预览URL
  photos.value.forEach(photo => {
    if (photo.url.startsWith('blob:')) {
      URL.revokeObjectURL(photo.url)
    }
  })
})
</script>

<style scoped lang="scss">
.memory-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 24px;
  
  @media (max-width: 768px) {
    padding: 24px 16px;
  }
}

.page-header {
  text-align: center;
  margin-bottom: 40px;
  
  .page-title {
    font-size: 32px;
    font-weight: 700;
    color: var(--color-text);
    margin-bottom: 12px;
  }
  
  .page-subtitle {
    font-size: 16px;
    color: var(--color-muted);
    margin: 0;
  }
}

.content-container {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.section-card {
  background: var(--color-white);
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
  margin-bottom: 8px;
}

.section-desc {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-bottom: 24px;
}

// 照片网格
.photos-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
  
  @media (max-width: 768px) {
    grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  }
}

.photo-item {
  position: relative;
  aspect-ratio: 1;
  border-radius: 12px;
  overflow: hidden;
}

.photo-preview {
  position: relative;
  width: 100%;
  height: 100%;
  
  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
  
  .remove-btn {
    position: absolute;
    top: 8px;
    right: 8px;
    width: 28px;
    height: 28px;
    border: none;
    background: rgba(0, 0, 0, 0.6);
    border-radius: 50%;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.3s ease;
    
    img {
      width: 16px;
      height: 16px;
      filter: brightness(0) invert(1);
    }
    
    &:hover {
    background: var(--support-500);
      transform: scale(1.1);
    }
  }
}

.upload-progress {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(0, 0, 0, 0.6);
  color: #fff;
  padding: 8px;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.photo-upload-btn {
  aspect-ratio: 1;
  border: 2px dashed var(--color-border);
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: var(--color-bg-muted);
  color: var(--color-text-secondary);
  
  .plus-icon {
    width: 32px;
    height: 32px;
    opacity: 0.6;
  }
  
  &:hover {
    border-color: var(--color-border-strong);
    background: var(--color-bg-muted);
    color: var(--color-text);
    
    .plus-icon {
      opacity: 1;
      filter: none;
    }
  }
}

// URL输入区域
.url-input-section {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
}

.url-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  cursor: pointer;
  color: var(--color-text-secondary);
  font-size: 14px;
  border-radius: 8px;
  transition: all 0.3s ease;
  
  img {
    width: 16px;
    height: 16px;
    transition: transform 0.3s ease;
    
    &.rotated {
      transform: rotate(180deg);
    }
  }
  
  &:hover {
    background: var(--color-bg-muted);
  }
}

.url-inputs {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
}

.url-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.url-input {
  flex: 1;
  padding: 10px 14px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
  
  &:focus {
    outline: none;
    border-color: var(--color-border-strong);
    box-shadow: 0 0 0 2px rgba(31, 35, 41, 0.06);
  }
}

// 按钮
.ghost-btn {
  padding: 10px 16px;
  border: 1px solid var(--color-border);
  background: var(--color-white);
  color: var(--color-text-secondary);
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.15s ease, border-color 0.15s ease, color 0.15s ease;
  
  &:hover {
    background: var(--color-bg-muted);
    border-color: var(--color-border-strong);
    color: var(--color-text);
  }
}

.actions-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.generate-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 14px 32px;
  border: 1px solid var(--color-border);
  background: var(--color-white);
  color: var(--color-text);
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.15s ease, border-color 0.15s ease, color 0.15s ease;
  
  .btn-icon {
    width: 20px;
    height: 20px;
  }
  
  &:hover:not(:disabled) {
    background: var(--color-bg-muted);
    border-color: var(--color-border-strong);
  }
  
  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
  }
  
  &.loading {
    background: var(--color-bg-muted);
  }
}

.hint-text {
  font-size: 14px;
  color: var(--color-muted);
  margin: 0;
}

// 错误提示
.error-message {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: var(--support-50);
  border: 1px solid var(--support-100);
  border-radius: 10px;
  color: var(--support-500);
  font-size: 14px;
  
  .error-icon {
    width: 20px;
    height: 20px;
    flex-shrink: 0;
  }
}

// 进度区域
.progress-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
}

.status-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-label {
  font-size: 14px;
  color: var(--color-text-secondary);
  font-weight: 500;
}

.status-value {
  font-size: 14px;
  color: var(--color-text);
  font-family: monospace;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  
  &.status-pending {
    background: var(--status-warning-bg);
    color: var(--status-warning);
  }

  &.status-processing {
    background: var(--status-info-bg);
    color: var(--status-info);
  }

  &.status-success {
    background: var(--status-success-bg);
    color: var(--status-success);
  }
  
  &.status-failed {
    background: rgba(245, 87, 108, 0.1);
    color: var(--support-500);
  }
}

.progress-bar {
  width: 100%;
  height: 8px;
  background: var(--color-bg-muted);
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 24px;
}

.progress-fill {
  height: 100%;
  background: var(--color-text);
  border-radius: 4px;
  transition: width 0.3s ease;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.7;
  }
}

// 结果预览
.result-preview {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
}

.result-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 16px;
}

.image-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  
  img {
    width: 100%;
    max-width: 600px;
    border-radius: 12px;
    border: 1px solid var(--color-border);
    box-shadow: none;
  }
}

.image-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border: 1px solid var(--color-border);
  background: var(--color-white);
  color: var(--color-text-secondary);
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.15s ease, border-color 0.15s ease, color 0.15s ease;
  
  img {
    width: 16px;
    height: 16px;
  }
  
  &:hover {
    background: var(--color-bg-muted);
    border-color: var(--color-border-strong);
    color: var(--color-text);
  }
  
  &.primary {
    background: var(--color-bg-muted);
    border-color: var(--color-border);
    color: var(--color-text);
    
    &:hover {
      background: var(--color-bg-muted);
      border-color: var(--color-border-strong);
    }
  }
}

// 加载动画
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
</style>


