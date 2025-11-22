<template>
  <div class="chat-input">
    <div class="input-wrapper">
      <div class="input-container">
        <!-- 快捷操作按钮 -->
        <div class="quick-actions">
          <button 
            class="action-icon-btn" 
            title="图片"
            @click="handleImageClick"
          >
            <img src="https://unpkg.com/lucide-static@latest/icons/image.svg" alt="图片" class="icon" />
          </button>
          <button 
            class="action-icon-btn" 
            title="附件"
            @click="handleAttachmentClick"
          >
            <img src="https://unpkg.com/lucide-static@latest/icons/paperclip.svg" alt="附件" class="icon" />
          </button>
        </div>

        <!-- 输入框 -->
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

        <!-- 发送按钮 -->
        <button
          :disabled="disabled || !inputMessage.trim()"
          @click="sendMessage"
          class="send-button"
          :class="{ active: inputMessage.trim() }"
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

      <!-- 快捷提示 -->
      <div class="input-hint">
        <span class="hint-text">
          <kbd>Enter</kbd> 发送 · <kbd>Shift</kbd> + <kbd>Enter</kbd> 换行
        </span>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
export default {
  name: 'ChatInput',
  props: {
    disabled: {
      type: Boolean,
      default: false
    },
    placeholder: {
      type: String,
      default: '随便问我什么...'
    }
  },
  data() {
    return {
      inputMessage: '',
    }
  },
  methods: {
    sendMessage() {
      if (this.inputMessage.trim() && !this.disabled) {
        this.$emit('send-message', this.inputMessage.trim())
        this.inputMessage = ''
        this.adjustHeight()
      }
    },
    handleKeyDown(event: KeyboardEvent) {
      if (event.key === 'Enter' && !event.shiftKey) {
        event.preventDefault()
        this.sendMessage()
      }
    },
    adjustHeight() {
      this.$nextTick(() => {
        const textarea = this.$refs.inputRef as unknown as HTMLTextAreaElement | undefined
        if (!textarea) return
        textarea.style.height = 'auto'
        textarea.style.height = Math.min(textarea.scrollHeight, 120) + 'px'
      })
    },
    focus() {
      const textarea = this.$refs.inputRef as unknown as HTMLTextAreaElement | undefined
      try {
        textarea?.focus({ preventScroll: true } as FocusOptions)
      } catch {
        textarea?.focus()
      }
    },
    handleImageClick() {
      // TODO: 实现图片上传功能
      console.log('图片上传功能待实现')
    },
    handleAttachmentClick() {
      // TODO: 实现附件上传功能
      console.log('附件上传功能待实现')
    }
  },
  mounted() {
    this.adjustHeight()
  }
}
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
  box-shadow: none;
  transition: border-color 0.15s ease, box-shadow 0.15s ease, background-color 0.15s ease;
  position: relative;
}

.input-container:focus-within {
  border-color: var(--color-border-strong);
  box-shadow: 0 0 0 2px rgba(31, 35, 41, 0.06);
  background: var(--color-bg-secondary);
}

/* 快捷操作按钮 */
.quick-actions {
  display: flex;
  gap: 4px;
  align-items: center;
}

.action-icon-btn {
  width: 36px;
  height: 36px;
  border: none;
  background: var(--color-bg-muted);
  border-radius: 10px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.15s ease, transform 0.15s ease;
}

.action-icon-btn:hover {
  background: var(--color-bg-muted);
  transform: translateY(-2px);
}

.action-icon-btn:active {
  transform: translateY(0);
}

.action-icon-btn .icon {
  width: 18px;
  height: 18px;
  filter: none;
}

/* 输入框 */
.input-textarea {
  flex: 1;
  padding: 8px 12px;
  border: none;
  background: transparent;
  font-size: 15px;
  line-height: 1.5;
  resize: none;
  outline: none;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  color: var(--color-text);
  min-height: 24px;
  max-height: 120px;
  overflow-y: auto;
}

.input-textarea::placeholder {
  color: var(--color-subtle);
}

.input-textarea:disabled {
  color: var(--color-subtle);
  cursor: not-allowed;
}

/* 自定义滚动条 */
.input-textarea::-webkit-scrollbar {
  width: 6px;
}

.input-textarea::-webkit-scrollbar-track {
  background: transparent;
}

.input-textarea::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 3px;
}

.input-textarea::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.3);
}

/* 发送按钮 */
.send-button {
  width: 40px;
  height: 40px;
  background: #ffffff;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.15s ease, border-color 0.15s ease, transform 0.15s ease, box-shadow 0.15s ease;
  flex-shrink: 0;
  position: relative;
  overflow: hidden;
}

.send-button.active {
  background: var(--color-text);
  color: #fff;
  border-color: var(--color-text);
  box-shadow: none;
}

.send-button.active:hover {
  transform: translateY(-1px);
}

.send-button:active {
  transform: translateY(0);
}

.send-button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.send-button .icon {
  width: 20px;
  height: 20px;
  filter: none;
  transition: filter 0.3s ease;
}

.send-button.active .icon {
  filter: brightness(0) invert(1);
}

.send-button .icon.loading {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

/* 快捷提示 */
.input-hint {
  margin-top: 8px;
  text-align: center;
}

.hint-text {
  font-size: 12px;
  color: var(--color-subtle);
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.hint-text kbd {
  display: inline-block;
  padding: 2px 6px;
  background: rgba(0, 0, 0, 0.05);
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 4px;
  font-family: monospace;
  font-size: 11px;
  line-height: 1;
  color: var(--color-text-secondary);
}

/* 移动端适配 */
@media (max-width: 768px) {
  .chat-input {
    padding: 16px 20px 20px;
  }

  .input-container {
    padding: 10px 12px;
    border-radius: 20px;
  }

  .input-textarea {
    font-size: 16px; /* 防止iOS缩放 */
  }

  .quick-actions {
    gap: 2px;
  }

  .action-icon-btn {
    width: 32px;
    height: 32px;
  }

  .action-icon-btn .icon {
    width: 16px;
    height: 16px;
  }

  .send-button {
    width: 36px;
    height: 36px;
  }

  .input-hint {
    display: none; /* 移动端隐藏提示 */
  }
}
</style>

