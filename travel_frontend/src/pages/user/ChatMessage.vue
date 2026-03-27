<template>
  <div class="chat-message" :class="{ 'user-message': isUser, 'ai-message': !isUser }">
    <div class="message-content">
      <div class="message-bubble">
        <pre v-if="isUser" class="message-text">{{ message }}</pre>
        <div v-else>
          <!-- 思考过程切换按钮 -->
          <div class="debug-toggle" v-if="hasDebugInfo">
            <button @click="showDebug = !showDebug" class="debug-btn">
              <span>{{ showDebug ? '隐藏思考过程' : '显示思考过程' }}</span>
            </button>
          </div>

          <!-- 思考过程面板 -->
          <transition name="expand">
            <div v-if="showDebug && hasDebugInfo" class="debug-panel">
              <div class="debug-section">
                <strong>思考</strong>
                <div class="debug-content">{{ parsedData.reasoning }}</div>
              </div>
              <div class="debug-section">
                <strong>行动</strong>
                <div class="debug-content">{{ parsedData.action }}</div>
              </div>
            </div>
          </transition>

          <!-- AI回复内容 -->
          <div class="message-markdown" v-html="renderedMessage"></div>
        </div>
      </div>
      <div class="message-footer">
        <div class="message-time">{{ formatTime(timestamp) }}</div>
        <div v-if="!isUser" class="message-actions">
          <button class="action-btn" title="复制">
            <img src="https://unpkg.com/lucide-static@latest/icons/copy.svg" alt="复制" class="icon" />
          </button>
          <button class="action-btn" title="重新生成">
            <img src="https://unpkg.com/lucide-static@latest/icons/refresh-cw.svg" alt="重新生成" class="icon" />
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent, computed, ref } from 'vue'
import { marked } from 'marked'
import { formatChatTime } from '@/util/timeUtils'
import userAvatar from '@/assets/user-avatar.jpg'
import aiAvatar from '@/assets/user-avatar.jpg'

export default defineComponent({
  name: 'ChatMessage',
  props: {
    message: { type: String, required: true },
    isUser: { type: Boolean, default: false },
    timestamp: { type: [Date, String, Number], default: () => new Date() },
  },
  setup(props) {
    const showDebug = ref(false)

    const escapeHtml = (input: string) =>
      input.replace(/[&<>"']/g, (ch) => {
        const map: Record<string, string> = {
          '&': '&amp;',
          '<': '&lt;',
          '>': '&gt;',
          '"': '&quot;',
          "'": '&#39;',
        }
        return map[ch] || ch
      })

    // 解析后端响应
    const parsedData = computed(() => {
      if (props.isUser) return { reasoning: '', action: '', observation: props.message }

      const messageText = typeof props.message === 'string'
        ? props.message
        : String(props.message || '')

      // 默认值
      const result = {
        reasoning: '',
        action: '',
        observation: messageText
      }

      try {
        // 解析三段式响应
        const regex = /思考[:：]\s*(.*?)\s*行动[:：]\s*(.*?)\s*观察[:：]\s*([\s\S]*)/i
        const match = messageText.match(regex)

        if (match && match.length >= 4) {
          result.reasoning = match[1].trim()
          result.action = match[2].trim()
          result.observation = match[3].trim()
        }
      } catch (e) {
        console.error('解析AI响应失败', e)
      }

      return result
    })

    // 检查是否有调试信息
    const hasDebugInfo = computed(() => {
      return !props.isUser &&
        parsedData.value.reasoning &&
        parsedData.value.action
    })

    // 渲染最终回复
    const renderedMessage = computed(() => {
      if (props.isUser) return props.message

      try {
        marked.setOptions({ breaks: true, gfm: true, headerIds: false, mangle: false })
        const safeSource = escapeHtml(parsedData.value.observation)
        return marked.parse(safeSource)
      } catch (error) {
        console.error('Markdown解析错误:', error)
        return parsedData.value.observation
      }
    })

    return {
      renderedMessage,
      formatTime: formatChatTime,
      userAvatar,
      aiAvatar,
      parsedData,
      showDebug,
      hasDebugInfo
    }
  },
})
</script>

<style scoped>
/* 消息容器 */
.chat-message {
  display: flex;
  margin-bottom: 16px;
  padding: 0 12px;
}

.user-message {
  justify-content: flex-end;
}

.ai-message {
  justify-content: flex-start;
}

/* 消息内容 */
.message-content {
  max-width: 80%;
  display: flex;
  flex-direction: column;
}

.message-bubble {
  padding: 10px 14px;
  border-radius: 12px;
  word-wrap: break-word;
  word-break: break-word;
}

.user-message .message-bubble {
  background: var(--primary-600);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.ai-message .message-bubble {
  background: #f5f5f5;
  color: var(--color-text);
  border-bottom-left-radius: 4px;
}

/* 消息文本 */
.message-text,
.message-markdown {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-size: 15px;
  line-height: 1.6;
  margin: 0;
}

.message-markdown {
  color: var(--color-text);
}

.message-markdown code {
  background-color: var(--color-bg-muted);
  color: var(--color-text);
  padding: 0.2em 0.5em;
  border-radius: 4px;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 0.9em;
}

.message-markdown pre {
  background-color: var(--primary-900);
  color: var(--color-bg-secondary);
  padding: 16px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 12px 0;
  box-shadow: none;
}

.message-markdown pre code {
  background-color: transparent;
  color: inherit;
  padding: 0;
}

.message-markdown blockquote {
  border-left: 4px solid var(--color-border);
  padding-left: 16px;
  margin: 12px 0;
  font-style: italic;
  color: var(--color-muted);
  background: var(--color-bg-muted);
  padding: 12px 16px;
  border-radius: 4px;
}

/* 思考过程切换 */
.debug-toggle {
  margin-bottom: 8px;
  display: flex;
  justify-content: flex-end;
}

.debug-btn {
  background: transparent;
  border: none;
  border-radius: 4px;
  padding: 4px 8px;
  font-size: 12px;
  cursor: pointer;
  color: var(--color-muted);
}

.debug-btn:hover {
  color: var(--color-text);
}

/* 思考过程面板 */
.debug-panel {
  background: #f0f0f0;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 8px;
}

.debug-section {
  margin-bottom: 8px;
}

.debug-section:last-child {
  margin-bottom: 0;
}

.debug-section strong {
  color: var(--color-text);
  font-size: 13px;
}

.debug-content {
  padding-left: 8px;
  font-size: 13px;
  line-height: 1.5;
  color: var(--color-muted);
}

/* 消息底部 */
.message-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
  padding: 0 2px;
}

.message-time {
  font-size: 11px;
  color: var(--color-muted);
}

.message-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
}

.chat-message:hover .message-actions {
  opacity: 1;
}

.action-btn {
  width: 24px;
  height: 24px;
  border: none;
  background: transparent;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.action-btn:hover {
  background: var(--color-bg-muted);
}

.action-btn .icon {
  width: 14px;
  height: 14px;
}
</style>
