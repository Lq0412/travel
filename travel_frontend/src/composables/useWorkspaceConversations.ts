import { computed, ref } from 'vue'
import { getUserConversationsByUserId } from '@/api/chatConversationClient'

export function useWorkspaceConversations(userIdProvider: () => string | number | undefined) {
  const conversations = ref<API.AIConversationVO[]>([])
  const loading = ref(false)

  const conversationCount = computed(() => conversations.value.length)

  async function refreshConversations() {
    const userId = userIdProvider()
    if (!userId) {
      conversations.value = []
      return
    }

    loading.value = true
    try {
      const response = await getUserConversationsByUserId(userId, 1, 8)
      conversations.value = response.data.data ?? []
    } finally {
      loading.value = false
    }
  }

  function upsertConversation(conversation: API.AIConversationVO) {
    if (!conversation.id) {
      return
    }

    const index = conversations.value.findIndex((item) => item.id === conversation.id)
    if (index >= 0) {
      conversations.value[index] = {
        ...conversations.value[index],
        ...conversation,
      }
    } else {
      conversations.value.unshift(conversation)
    }

    conversations.value = [...conversations.value].sort((left, right) => {
      const leftTime = new Date(left.updateTime ?? left.createTime ?? 0).getTime()
      const rightTime = new Date(right.updateTime ?? right.createTime ?? 0).getTime()
      return rightTime - leftTime
    })
  }

  return {
    conversations,
    loading,
    conversationCount,
    refreshConversations,
    upsertConversation,
  }
}
