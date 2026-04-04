<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, LinkOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { deletePhoto, getTripPhotos, uploadPhoto } from '@/api/tripPhotoController'
import { canAddMorePhotos, normalizeTripPhotos, sanitizePhotoUrl } from '@/utils/tripPhoto'

const props = defineProps<{
  tripId: number
  initialPhotos?: API.TripPhotoVO[]
}>()

const emit = defineEmits<{
  photosChanged: [photos: API.TripPhotoVO[]]
}>()

const loading = ref(false)
const submitting = ref(false)
const photoUrl = ref('')
const photos = ref<API.TripPhotoVO[]>(normalizeTripPhotos(props.initialPhotos))

const photoCount = computed(() => photos.value.length)
const canUpload = computed(() => canAddMorePhotos(photos.value))

watch(
  () => props.initialPhotos,
  (nextPhotos) => {
    photos.value = normalizeTripPhotos(nextPhotos)
  },
)

watch(
  () => props.tripId,
  () => {
    void refreshPhotos()
  },
  { immediate: true },
)

async function refreshPhotos() {
  loading.value = true
  try {
    const response = await getTripPhotos({ tripId: props.tripId })
    const nextPhotos = normalizeTripPhotos(response.data.data)
    photos.value = nextPhotos
    emit('photosChanged', nextPhotos)
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : '加载照片失败'
    message.error(errorMessage)
  } finally {
    loading.value = false
  }
}

async function submitPhoto() {
  const sanitizedUrl = sanitizePhotoUrl(photoUrl.value)
  if (!sanitizedUrl) {
    message.warning('请输入有效的 http 或 https 图片链接')
    return
  }

  if (!canUpload.value) {
    message.warning('当前行程最多保留 6 张照片')
    return
  }

  submitting.value = true
  try {
    const response = await uploadPhoto({ tripId: props.tripId, photoUrl: sanitizedUrl })
    if (response.data.code !== 0 || !response.data.data) {
      throw new Error(response.data.message || '上传失败')
    }
    photoUrl.value = ''
    message.success('照片已加入当前行程')
    await refreshPhotos()
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : '上传失败'
    message.error(errorMessage)
  } finally {
    submitting.value = false
  }
}

async function removePhoto(photoId?: number) {
  if (!photoId) {
    return
  }

  try {
    const response = await deletePhoto({ photoId })
    if (response.data.code !== 0) {
      throw new Error(response.data.message || '删除失败')
    }
    message.success('照片已移除')
    await refreshPhotos()
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : '删除失败'
    message.error(errorMessage)
  }
}
</script>

<template>
  <div class="trip-photo-manager">
    <div class="toolbar">
      <div>
        <h3 class="toolbar-title">照片资产</h3>
        <p class="toolbar-desc">当前已关联 {{ photoCount }} / 6 张照片，支持直接粘贴图片链接形成可演示闭环。</p>
      </div>

      <a-button :loading="loading" @click="refreshPhotos">
        <template #icon><ReloadOutlined /></template>
        刷新列表
      </a-button>
    </div>

    <a-alert
      show-icon
      type="info"
      message="后端当前提供 URL 入库接口，这一版以“贴链接即可沉淀照片素材”为主，便于答辩时稳定演示。"
      class="intro-alert"
    />

    <div class="composer">
      <a-input
        v-model:value="photoUrl"
        size="large"
        placeholder="粘贴旅行照片链接，例如 https://..."
        :disabled="submitting || !canUpload"
        @pressEnter="submitPhoto"
      >
        <template #prefix><LinkOutlined /></template>
      </a-input>
      <a-button type="primary" size="large" :loading="submitting" :disabled="!canUpload" @click="submitPhoto">
        添加照片
      </a-button>
    </div>

    <a-empty v-if="!loading && !photos.length" description="当前还没有照片资产，先添加一张行程照片" />

    <div v-else class="photo-grid">
      <article v-for="photo in photos" :key="photo.id" class="photo-card">
        <img :src="photo.photoUrl" alt="trip photo" class="photo-image" />
        <div class="photo-body">
          <div class="photo-meta">
            <span>顺序 {{ photo.sortOrder || '--' }}</span>
            <span>{{ photo.createTime ? new Date(photo.createTime).toLocaleString('zh-CN') : '刚刚添加' }}</span>
          </div>
          <a-button danger type="link" class="delete-btn" @click="removePhoto(photo.id)">
            <template #icon><DeleteOutlined /></template>
            删除
          </a-button>
        </div>
      </article>
    </div>
  </div>
</template>

<style scoped lang="scss">
.trip-photo-manager {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.toolbar-title {
  margin: 0 0 6px;
  font-size: 20px;
  color: var(--color-text);
}

.toolbar-desc {
  margin: 0;
  color: var(--color-muted);
}

.intro-alert {
  border-radius: 16px;
}

.composer {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 140px;
  gap: 12px;
}

.photo-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.photo-card {
  overflow: hidden;
  border: 1px solid rgba(15, 28, 46, 0.08);
  border-radius: 20px;
  background: #ffffff;
  box-shadow: 0 10px 30px rgba(18, 52, 97, 0.05);
}

.photo-image {
  width: 100%;
  aspect-ratio: 4 / 3;
  object-fit: cover;
  background: #edf2f7;
}

.photo-body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
}

.photo-meta {
  display: grid;
  gap: 4px;
  font-size: 12px;
  color: var(--color-subtle);
}

.delete-btn {
  padding-inline: 0;
}

@media (max-width: 960px) {
  .composer,
  .photo-grid {
    grid-template-columns: 1fr;
  }

  .toolbar {
    flex-direction: column;
  }
}
</style>
