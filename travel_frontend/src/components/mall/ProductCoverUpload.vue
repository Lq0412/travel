<script setup lang="ts">
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import { LoadingOutlined, PlusOutlined } from '@ant-design/icons-vue'

import { uploadProductCoverFile, validateProductCoverFile } from '@/utils/productCoverUpload'

const props = defineProps<{
  modelValue: string
  defaultCover: string
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const fileInput = ref<HTMLInputElement | null>(null)
const uploading = ref(false)

const previewUrl = computed(() => props.modelValue || props.defaultCover)

function openFileDialog() {
  if (uploading.value) {
    return
  }
  fileInput.value?.click()
}

async function handleFileChange(event: Event) {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  target.value = ''

  if (!file) {
    return
  }

  const validationMessage = validateProductCoverFile(file)
  if (validationMessage) {
    message.error(validationMessage)
    return
  }

  uploading.value = true
  try {
    const uploadedUrl = await uploadProductCoverFile(file)
    emit('update:modelValue', uploadedUrl)
    message.success('商品封面上传成功')
  } catch (error: unknown) {
    const errorMessage = error instanceof Error ? error.message : '商品封面上传失败'
    message.error(errorMessage)
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <div class="product-cover-upload">
    <button type="button" class="upload-trigger" :disabled="uploading" @click="openFileDialog">
      <div class="preview-box" :style="{ backgroundImage: `url(${previewUrl})` }">
        <span v-if="!modelValue && !uploading">点击上传商品封面</span>
        <LoadingOutlined v-else-if="uploading" />
      </div>
      <div class="upload-hint">
        <PlusOutlined />
        <span>{{ uploading ? '上传中...' : '上传图片到 COS' }}</span>
      </div>
    </button>

    <div v-if="modelValue" class="uploaded-url">{{ modelValue }}</div>
    <input
      ref="fileInput"
      type="file"
      accept="image/jpeg,image/jpg,image/png,image/webp"
      class="hidden-input"
      @change="handleFileChange"
    />
  </div>
</template>

<style scoped lang="scss">
.product-cover-upload {
  display: grid;
  gap: 12px;
}

.upload-trigger {
  border: 0;
  padding: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.upload-trigger:disabled {
  cursor: not-allowed;
}

.preview-box {
  height: 180px;
  border-radius: 12px;
  background-color: #f5f5f5;
  background-size: cover;
  background-position: center;
  border: 1px dashed #d9d9d9;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #8c8c8c;
  font-size: 14px;
}

.upload-hint {
  margin-top: 10px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: #1677ff;
  font-size: 14px;
}

.uploaded-url {
  padding: 10px 12px;
  border-radius: 8px;
  background: #fafafa;
  color: #595959;
  font-size: 12px;
  line-height: 1.6;
  word-break: break-all;
}

.hidden-input {
  display: none;
}
</style>
