<template>
  <div class="message-wall-page">
    <div class="header">
      <h2>景点留言墙</h2>
      <!-- 如果景点ID未设置，显示景点选择器 -->
      <div v-if="!scenicSpotId" class="spot-selector">
        <a-select
          v-model:value="selectedScenicSpotId"
          placeholder="请选择景点"
          style="width: 300px"
          :loading="loadingSpots"
          @change="onSpotChange"
        >
          <a-select-option v-for="spot in scenicSpots" :key="spot.id" :value="spot.id">
            {{ spot.name }}
          </a-select-option>
        </a-select>
      </div>
      <div v-else class="spot-info">
        <span>景点: {{ currentSpotName || `ID: ${scenicSpotId}` }}</span>
        <a-button type="link" size="small" @click="resetSpot">切换景点</a-button>
      </div>
    </div>

    <div 
      class="message-wall-container" 
      ref="container"
      :style="getContainerStyle()"
    >
      <!-- Loading 层 -->
      <div v-if="loading" class="loading-overlay">
        <a-spin size="large" tip="加载留言中..." />
      </div>
      
      <!-- 弹幕层 -->
      <div v-if="scenicSpotId" class="barrage-layer">
        <div
          v-for="(item, idx) in messages"
          :key="`barrage-${item.id || idx}`"
          class="message-item"
          :style="getMessageStyle(item, idx)"
        >
          {{ item.content }}
        </div>
      </div>
      
      <!-- 空状态提示 -->
      <div v-if="!scenicSpotId && !loading" class="empty">
        <a-empty description="请先选择景点" />
      </div>
    </div>

    <div class="send-box">
      <div class="send-container">
        <!-- 弹幕输入区域 -->
        <div class="input-section">
          <div class="input-header">
            <span class="input-label">💬 发送弹幕</span>
            <span class="char-count">{{ content.length }}/200</span>
          </div>
          <a-textarea
            v-model:value="content"
            :rows="2"
            placeholder="说点什么吧~ 支持 emoji 😊"
            :maxlength="200"
            :disabled="sending || loading || !scenicSpotId"
            class="textarea-input"
            :auto-size="{ minRows: 2, maxRows: 4 }"
          />
        </div>

        <!-- 弹幕设置工具栏 -->
        <div class="toolbar">
          <div class="toolbar-left">
            <!-- 颜色选择 -->
            <div class="color-picker">
              <span class="toolbar-label">颜色:</span>
              <div class="color-options">
                <div
                  v-for="color in colorOptions"
                  :key="color.value"
                  class="color-item"
                  :class="{ active: selectedColor === color.value }"
                  :style="{ background: color.value }"
                  :title="color.name"
                  @click="selectedColor = color.value"
                />
              </div>
            </div>

            <!-- 字体大小 -->
            <div class="size-picker">
              <span class="toolbar-label">大小:</span>
              <a-radio-group v-model:value="selectedSize" size="small" button-style="solid">
                <a-radio-button :value="16">小</a-radio-button>
                <a-radio-button :value="20">中</a-radio-button>
                <a-radio-button :value="24">大</a-radio-button>
              </a-radio-group>
            </div>
          </div>

          <div class="toolbar-right">
            <a-button
              type="primary"
              @click="sendMessage"
              :loading="sending"
              :disabled="!scenicSpotId || !content.trim()"
              class="send-button"
              size="large"
            >
              <template #icon>
                <span class="send-icon">🚀</span>
              </template>
              发射弹幕
            </a-button>
          </div>
        </div>

        <!-- 弹幕预览 -->
        <div v-if="content.trim()" class="preview-section">
          <span class="preview-label">预览效果：</span>
          <div 
            class="preview-barrage"
            :style="{
              color: selectedColor,
              fontSize: selectedSize + 'px'
            }"
          >
            {{ content }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { listSpots } from '@/api/scenicController'
import { useLoginUserStore } from '@/stores/useLoginUserStore'
import { messageWallApi } from '@/api/messageWallApi'
import { handleApiError } from '@/utils/errorHandler'
import { createComponentLogger } from '@/utils/logger'
import { MESSAGE_WALL, STORAGE_KEYS, DEFAULT_IMAGES, ROUTES } from '@/constants'

// 创建组件专用logger
const logger = createComponentLogger('MessageWallDisplay')

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

// 景点ID的多种获取方式（优先级：路由参数 > 查询参数 > localStorage > 默认景点）
const getScenicSpotId = (): number => {
  const paramId = route.params.scenicSpotId
  const queryId = route.query.scenicSpotId
  const storedId = localStorage.getItem(STORAGE_KEYS.SCENIC_SPOT_ID)

  if (paramId && !isNaN(Number(paramId))) {
    return Number(paramId)
  }
  if (queryId && !isNaN(Number(queryId))) {
    return Number(queryId)
  }
  if (storedId && !isNaN(Number(storedId))) {
    return Number(storedId)
  }
  return MESSAGE_WALL.DEFAULT_SCENIC_SPOT_ID
}

const scenicSpotId = ref<number>(getScenicSpotId())
const selectedScenicSpotId = ref<number>()
const scenicSpots = ref<API.ScenicSpot[]>([])
const loadingSpots = ref(false)

// 加载景点列表
const loadScenicSpots = async () => {
  loadingSpots.value = true
  try {
    logger.debug('开始加载景点列表')
    const res = await listSpots()
    if (res.data.code === 0 && res.data.data) {
      scenicSpots.value = res.data.data.filter((spot: API.ScenicSpot) => spot.id)
      logger.info('景点列表加载成功', { count: scenicSpots.value.length })
    } else {
      throw new Error(res.data.message || '获取景点列表失败')
    }
  } catch (error) {
    logger.error('加载景点列表失败', error)
    handleApiError(error, { customMessage: '获取景点列表失败' })
  } finally {
    loadingSpots.value = false
  }
}

const intervalTimer = ref<ReturnType<typeof window.setInterval> | null>(null)

// 监听路由参数变化
watch(() => route.params.scenicSpotId, (newId) => {
  if (newId && !isNaN(Number(newId))) {
    scenicSpotId.value = Number(newId)
    localStorage.setItem(STORAGE_KEYS.SCENIC_SPOT_ID, String(scenicSpotId.value))
    load()
  }
})

// 景点选择变化
const onSpotChange = (spotId: number) => {
  if (!spotId) return
  scenicSpotId.value = spotId
  localStorage.setItem(STORAGE_KEYS.SCENIC_SPOT_ID, String(spotId))
  
  // 更新URL
  router.push({
    path: ROUTES.MESSAGE_WALL_DETAIL(spotId),
    replace: true
  })
}

// 重置景点选择
const resetSpot = () => {
  scenicSpotId.value = MESSAGE_WALL.DEFAULT_SCENIC_SPOT_ID
  localStorage.removeItem(STORAGE_KEYS.SCENIC_SPOT_ID)
  selectedScenicSpotId.value = undefined
  router.push(ROUTES.MESSAGE_WALL)
}

// 当前景点名称
const currentSpotName = computed(() => {
  const spot = scenicSpots.value.find(s => s.id && s.id === scenicSpotId.value)
  return spot ? spot.name : ''
})

// 获取当前景点的背景图片URL
const getCurrentSpotBackgroundUrl = (): string => {
  if (!scenicSpotId.value || scenicSpots.value.length === 0) {
    return DEFAULT_IMAGES.BACKGROUND
  }
  const spot = scenicSpots.value.find(s => s.id && s.id === scenicSpotId.value)
  // 优先使用景点的封面图作为背景，如果没有则使用默认背景
  if (spot && spot.coverUrl) {
    return spot.coverUrl
  }
  return DEFAULT_IMAGES.BACKGROUND
}

// 获取容器样式
const getContainerStyle = () => {
  const backgroundUrl = getCurrentSpotBackgroundUrl()
  return {
    background: `rgba(0, 0, 0, 0.2) url(${backgroundUrl})`,
    backgroundSize: 'cover',
    backgroundPosition: 'center',
    backgroundRepeat: 'no-repeat'
  }
}

const messages = ref<API.MessageWallVO[]>([])
const content = ref<string>('')
const sending = ref(false)
const loading = ref(false)

const container = ref<HTMLElement | null>(null)

// 弹幕样式选项
const selectedColor = ref('var(--color-bg-secondary)')
const selectedSize = ref(20)

// 颜色选项
const colorOptions = [
  { name: '海面白', value: 'var(--color-bg-secondary)' },
  { name: '深海蓝', value: 'var(--primary-700)' },
  { name: '海岸蓝', value: 'var(--primary-500)' },
  { name: '浪花青', value: 'var(--primary-300)' },
  { name: '日光金', value: 'var(--accent-500)' },
  { name: '暖砂橙', value: 'var(--accent-700)' },
  { name: '潮汐绿', value: 'var(--success-500)' },
  { name: '珊瑚红', value: 'var(--support-500)' },
  { name: '暮霞金', value: 'var(--warning-400)' },
]

// 加载留言
const load = async () => {
  if (!scenicSpotId.value) {
    logger.warn('景点ID未设置，无法加载留言')
    return
  }
  loading.value = true
  try {
    logger.debug('开始加载留言列表', { scenicSpotId: scenicSpotId.value })
    
    // 调用留言墙接口，查询isBarrage=true的留言（动画展示效果）
    const res = await messageWallApi.listMessages({
      scenicSpotId: scenicSpotId.value,
      isBarrage: true,
      status: MESSAGE_WALL.STATUS.APPROVED,
      pageSize: MESSAGE_WALL.PAGE_SIZE,
      current: 1
    })
    
    // 处理响应数据
    if (res.data.code === 0 && res.data.data) {
      messages.value = res.data.data.records || []
      logger.info('留言列表加载成功', { 
        count: messages.value.length,
        messages: messages.value.map(m => ({ id: m.id, content: m.content }))
      })
      
      // 如果没有留言，输出提示
      if (messages.value.length === 0) {
        logger.warn('当前景点暂无弹幕留言')
      }
    } else {
      throw new Error(res.data.message || '获取留言列表失败')
    }
  } catch (error) {
    logger.error('加载留言失败', error)
    // 不显示401错误（可能是未登录状态），其他错误才显示
    handleApiError(error, {
      customMessage: '加载留言失败',
      showMessage: (error as any)?.response?.status !== 401
    })
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  // 始终尝试获取登录状态，确保状态同步
  await loginUserStore.fetchLoginUser()
  logger.debug('登录状态', { 
    isLoggedIn: !!loginUserStore.loginUser.id,
    userId: loginUserStore.loginUser.id 
  })
  
  // 先加载景点列表
  loadScenicSpots().then(async () => {
    // 如果有景点ID，加载留言
    if (scenicSpotId.value) {
      await load()
      
      // 轮询刷新留言
      intervalTimer.value = window.setInterval(load, MESSAGE_WALL.REFRESH_INTERVAL)
      logger.debug('启动定时刷新', { interval: MESSAGE_WALL.REFRESH_INTERVAL })
    }
  })
})

onUnmounted(() => {
  // 清理定时器
  if (intervalTimer.value) {
    clearInterval(intervalTimer.value)
    intervalTimer.value = null
  }
})

const sendMessage = async () => {
  const contentValue = content.value.trim()

  logger.debug('发送留言', { 
    contentLength: contentValue.length, 
    scenicSpotId: scenicSpotId.value 
  })

  // 检查登录状态 - 先尝试刷新用户信息
  if (!loginUserStore.loginUser.id) {
    logger.debug('用户未登录，尝试刷新用户信息')
    await loginUserStore.fetchLoginUser()
  }
  
  // 检查用户是否已登录
  if (!loginUserStore.loginUser.id) {
    logger.warn('用户未登录，跳转到登录页')
    message.warning('请先登录后再发送留言')
    router.push({
      path: ROUTES.USER_LOGIN,
      query: { redirect: route.fullPath }
    })
    return
  }
  
  logger.debug('用户已登录', { userId: loginUserStore.loginUser.id })

  // 输入验证
  if (!contentValue) {
    message.warning('请输入留言内容')
    return
  }

  if (contentValue.length > MESSAGE_WALL.MAX_MESSAGE_LENGTH) {
    message.warning(`留言内容不能超过${MESSAGE_WALL.MAX_MESSAGE_LENGTH}字`)
    return
  }

  if (!scenicSpotId.value) {
    message.error('请先选择景点')
    return
  }

  sending.value = true
  try {
    const payload = { 
      scenicSpotId: scenicSpotId.value, 
      content: contentValue,
      isBarrage: true,  // 标记为动画展示效果
      textColor: selectedColor.value,  // 弹幕颜色
      fontSize: selectedSize.value  // 弹幕字体大小
    }
    
    logger.debug('发送留言请求', payload)
    const res = await messageWallApi.addMessage(payload)
    logger.debug('留言发送响应', { code: res.data.code })
    
    // 处理响应数据
    if (res.data.code === 0 || res.data.code === 200) {
      message.success('留言发送成功，等待审核通过后显示')
      logger.info('留言发送成功')
      // 成功后刷新（新留言需要审核，可能不会立即显示）
      content.value = ''
      await load()
    } else if (res.data.code === 401 || res.data.message?.includes('请先登录')) {
      // 业务逻辑返回 401，说明认证失败
      throw new Error(res.data.message || '请先登录')
    } else {
      throw new Error(res.data.message || '发送留言失败')
    }
  } catch (error: any) {
    logger.error('发送留言失败', error)
    
    const errorMsg = error?.response?.data?.message || error?.message || '未知错误'
    const statusCode = error?.response?.status
    
    // 如果是认证错误（401/403状态码），提示登录并跳转
    if (statusCode === 401 || statusCode === 403) {
      message.warning('请先登录后再发送留言')
      router.push({
        path: ROUTES.USER_LOGIN,
        query: { redirect: route.fullPath }
      })
    } else if (errorMsg.includes('请先登录') || error?.response?.data?.code === 401) {
      // 如果错误消息是"请先登录"或业务逻辑返回401，可能是认证问题
      logger.warn('后端返回认证错误，尝试刷新登录状态')
      
      // 先尝试刷新登录状态
      await loginUserStore.fetchLoginUser()
      
      if (!loginUserStore.loginUser.id) {
        // 如果刷新后还是没有登录信息，说明真的未登录
        message.warning('登录状态已过期，请重新登录')
        router.push({
          path: ROUTES.USER_LOGIN,
          query: { redirect: route.fullPath }
        })
      } else {
        // 如果刷新后还有登录信息，但请求还是失败，说明是后端认证拦截器问题
        logger.error('用户已登录但后端认证失败', {
          userId: loginUserStore.loginUser.id,
          apiStatus: statusCode
        })
        message.error({
          content: '发送留言失败：后端认证问题。请尝试重新登录或联系管理员。',
          duration: 5
        })
      }
    } else {
      handleApiError(error, {
        customMessage: '发送留言失败'
      })
    }
  } finally {
    sending.value = false
  }
}

const getMessageStyle = (item: API.MessageWallVO, idx: number) => {
  // 计算弹幕的垂直位置（轨道）
  const rowHeight = 50  // 每条弹幕轨道高度
  const totalRows = 10  // 总共10条轨道
  const row = idx % totalRows
  const top = row * rowHeight + 50  // 从顶部50px开始

  // 随机延迟启动，避免所有留言同时开始
  const delay = idx * 3  // 每条弹幕间隔3秒启动

  // 计算动画时长：20秒完成一次滚动（更慢更悠闲）
  const duration = 20

  // 支持自定义颜色和字体大小
  const color = item.textColor || 'var(--color-bg-secondary)'
  const fontSize = item.fontSize || 20

  const style = {
    top: `${top}px`,
    color: color,
    fontSize: `${fontSize}px`,
    animation: `barrage-move ${duration}s linear infinite`,
    animationDelay: `${delay}s`,
  }

  return style
}
</script>

<style>
/* 全局动画定义 - 不能使用 scoped，否则动画无法工作 */
@keyframes barrage-move {
  0% {
    left: 100%;
    opacity: 1;
  }
  100% {
    left: -20%;
    opacity: 1;
  }
}

</style>

<style scoped>
.message-wall-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  background: var(--color-bg-secondary);
  border-bottom: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
}

.header h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 500;
  color: var(--color-text);
}

.spot-selector {
  display: flex;
  align-items: center;
  gap: 8px;
}

.spot-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.message-wall-container {
  flex: 1;
  position: relative;
  overflow: hidden;
  transition: background-image 0.5s ease-in-out;
  min-height: 500px;
}

/* Loading 遮罩层 */
.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(4, 29, 36, 0.32);
  z-index: 100;
}

/* 弹幕层容器 */
.barrage-layer {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  pointer-events: none;
  z-index: 1000;
}

.message-item {
  position: absolute;
  white-space: nowrap;
  font-weight: bold;
  text-shadow: none;
  pointer-events: none;
  z-index: 1001;
  background: rgba(4, 29, 36, 0.65);
  padding: 8px 16px;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  transition: opacity 0.3s ease;
}

.empty {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: var(--color-bg-muted);
  font-size: 18px;
  text-align: center;
  text-shadow: none;
  z-index: 20;
}

.send-box {
  padding: 20px 24px;
  background: var(--color-bg-secondary);
  border-top: 1px solid var(--color-border);
  box-shadow: var(--shadow-sm);
}

.send-container {
  max-width: 1200px;
  margin: 0 auto;
}

/* 输入区域 */
.input-section {
  margin-bottom: 16px;
}

.input-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.input-label {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
}

.char-count {
  font-size: 13px;
  color: var(--color-muted);
}

.textarea-input {
  border-radius: 12px;
  border: 1px solid var(--color-border);
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
  font-size: 14px;
  
  &:hover {
    border-color: var(--color-border-strong);
  }
  
  &:focus {
    border-color: var(--color-border-strong);
    box-shadow: 0 0 0 2px rgba(31, 35, 41, 0.06);
  }
}

/* 工具栏 */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
  margin-bottom: 12px;
  
  @media (max-width: 768px) {
    flex-direction: column;
    align-items: stretch;
  }
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
}

.toolbar-right {
  display: flex;
  align-items: center;
}

.toolbar-label {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-secondary);
  margin-right: 8px;
}

/* 颜色选择器 */
.color-picker {
  display: flex;
  align-items: center;
}

.color-options {
  display: flex;
  gap: 8px;
}

.color-item {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  cursor: pointer;
  border: 2px solid var(--color-border);
  transition: background-color 0.15s ease, transform 0.15s ease, border-color 0.15s ease;
  box-shadow: none;
  
  &:hover {
    transform: scale(1.08);
    border-color: var(--color-border-strong);
  }
  
  &.active {
    border-color: var(--color-border-strong);
    transform: scale(1.08);
    box-shadow: none;
  }
}

/* 字体大小选择器 */
.size-picker {
  display: flex;
  align-items: center;
}

/* 发送按钮 */
.send-button,
.send-button.ant-btn-primary {
  min-width: 140px;
  height: 44px;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  background: var(--gradient-tertiary);
  border: none;
  color: #fff;
  box-shadow: var(--shadow-md);
  transition: transform 0.15s ease, box-shadow 0.15s ease, opacity 0.15s ease;
  
  &:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: var(--shadow-lg);
  }
  
  &:active:not(:disabled) {
    transform: translateY(0);
    box-shadow: var(--shadow-md);
  }
  
  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
  
  .send-icon {
    font-size: 18px;
    margin-right: 4px;
  }
}

/* 预览区域 */
.preview-section {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--color-bg-secondary);
  border-radius: 8px;
  border: 1px dashed var(--color-border);
}

.preview-label {
  font-size: 13px;
  color: var(--color-text-secondary);
  white-space: nowrap;
}

.preview-barrage {
  font-weight: bold;
  text-shadow: none;
  background: rgba(4, 29, 36, 0.7);
  padding: 6px 12px;
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.4);
  animation: preview-pulse 2s ease-in-out infinite;
}

@keyframes preview-pulse {
  0%, 100% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
}
</style>

