function truncate(input: string, maxLength: number) {
  return input.length > maxLength ? `${input.slice(0, maxLength)}...` : input
}

export function buildConversationTitle(input?: string | null) {
  const value = input?.trim() ?? ''
  if (!value) {
    return '新会话'
  }

  return truncate(value, 14)
}

export function formatConversationTime(input?: string | null) {
  if (!input) {
    return '等待生成'
  }

  const date = new Date(input)
  if (Number.isNaN(date.getTime())) {
    return '等待生成'
  }

  return date.toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

export function summarizeConversation(conversation?: API.AIConversationVO | null) {
  return {
    title: buildConversationTitle(conversation?.title),
    subtitle: conversation?.updateTime ? '最近更新' : '等待继续提问',
    timeLabel: formatConversationTime(conversation?.updateTime ?? conversation?.createTime),
  }
}
