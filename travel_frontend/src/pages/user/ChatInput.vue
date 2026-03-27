<template>
  <div class="chat-input">
    <div class="input-container">
      <textarea
        ref="inputRef"
        v-model="inputMessage"
        :placeholder="placeholder"
        :disabled="disabled"
        class="input-textarea"
        rows="1"
        @keydown="handleKeyDown"
        @input="adjustHeight"
      />

      <button
        :disabled="disabled || !inputMessage.trim()"
        class="send-button"
        @click="sendMessage"
      >
        <img
          v-if="disabled"
          src="https://unpkg.com/lucide-static@latest/icons/loader-2.svg"
          alt="加载中"
          class="icon loading"
        />
        <img
          v-else
          src="https://unpkg.com/lucide-static@latest/icons/send.svg"
          alt="发送"
          class="icon"
        />
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'

const props = withDefaults(
  defineProps<{
    disabled?: boolean
    placeholder?: string
  }>(),
  {
    disabled: false,
    placeholder: '随便问我什么...',
  }
)

const emit = defineEmits<{
  (e: 'send-message', value: string): void
}>()

const inputRef = ref<HTMLTextAreaElement | null>(null)
const inputMessage = ref('')

function sendMessage() {
  if (!inputMessage.value.trim() || props.disabled) {
    return
  }
  emit('send-message', inputMessage.value.trim())
  inputMessage.value = ''
  adjustHeight()
}

function handleKeyDown(event: KeyboardEvent) {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    sendMessage()
  }
}

function adjustHeight() {
  nextTick(() => {
    if (!inputRef.value) {
      return
    }
    inputRef.value.style.height = 'auto'
    inputRef.value.style.height = `${Math.min(inputRef.value.scrollHeight, 120)}px`
  })
}

function focus() {
  try {
    inputRef.value?.focus({ preventScroll: true } as FocusOptions)
  } catch {
    inputRef.value?.focus()
  }
}

defineExpose({ focus })

onMounted(adjustHeight)
</script>

<style scoped>
.chat-input {
  padding: 12px;
  background: #fff;
  border-top: 1px solid var(--color-border);
  flex-shrink: 0;
}

.input-container {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  background: #f5f5f5;
  border-radius: 12px;
  padding: 8px 12px;
}

.input-textarea {
  flex: 1;
  padding: 4px 0;
  border: none;
  background: transparent;
  font-size: 14px;
  line-height: 1.5;
  resize: none;
  outline: none;
  color: var(--color-text);
  min-height: 20px;
  max-height: 100px;
  overflow-y: auto;
}

.input-textarea::placeholder {
  color: var(--color-muted);
}

.send-button {
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 8px;
  background: var(--primary-600);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.send-button:disabled {
  background: #ddd;
  cursor: not-allowed;
}

.icon {
  width: 16px;
  height: 16px;
  filter: brightness(0) invert(1);
}

.loading {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
