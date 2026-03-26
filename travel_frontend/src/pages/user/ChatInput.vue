<template>
  <div class="chat-input">
    <div class="input-wrapper">
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
          :class="{ active: inputMessage.trim() }"
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

      <div class="input-hint">
        <span class="hint-text"><kbd>Enter</kbd> 发送 · <kbd>Shift</kbd> + <kbd>Enter</kbd> 换行</span>
      </div>
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
  position: relative;
  width: 100%;
  z-index: 100;
  background: var(--color-bg-secondary);
  border-top: 1px solid var(--color-border);
  padding: 20px 24px 24px;
  box-sizing: border-box;
  flex-shrink: 0;
}

.input-wrapper {
  max-width: 900px;
  margin: 0 auto;
}

.input-container {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  width: 100%;
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: 16px;
  padding: 12px 16px;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.input-container:focus-within {
  border-color: var(--color-border-strong);
  box-shadow: 0 0 0 2px rgba(31, 35, 41, 0.06);
}

.input-textarea {
  flex: 1;
  padding: 8px 12px;
  border: none;
  background: transparent;
  font-size: 15px;
  line-height: 1.5;
  resize: none;
  outline: none;
  color: var(--color-text);
  min-height: 24px;
  max-height: 120px;
  overflow-y: auto;
}

.input-textarea::placeholder {
  color: var(--color-subtle);
}

.send-button {
  width: 42px;
  height: 42px;
  border: none;
  border-radius: 12px;
  background: #e8eef6;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.15s ease, background-color 0.15s ease;
}

.send-button.active {
  background: var(--gradient-primary);
}

.send-button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.send-button:not(:disabled):hover {
  transform: translateY(-1px);
}

.icon {
  width: 18px;
  height: 18px;
}

.send-button.active .icon {
  filter: brightness(0) invert(1);
}

.loading {
  animation: spin 1s linear infinite;
}

.input-hint {
  margin-top: 8px;
  color: var(--color-subtle);
  font-size: 12px;
}

kbd {
  padding: 1px 6px;
  border-radius: 6px;
  background: #eef3fa;
  border: 1px solid var(--color-border);
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
