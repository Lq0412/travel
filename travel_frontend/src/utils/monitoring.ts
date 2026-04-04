type MonitorTone = 'success' | 'warning' | 'error' | 'processing' | 'default'

const TERMINAL_TASK_STATUS = new Set(['SUCCESS', 'FAILED', 'CANCELLED'])

export function getHealthTone(healthData?: Record<string, unknown> | null): MonitorTone {
  const status = String(healthData?.status ?? '').toUpperCase()
  if (status === 'UP') {
    return 'success'
  }
  if (!status) {
    return 'default'
  }
  return 'error'
}

export function getQueryCountValue(queryResult?: Record<string, unknown> | null) {
  const value = Number(queryResult?.statsRowCount ?? queryResult?.queryCount ?? 0)
  return Number.isFinite(value) ? value : 0
}

export function summarizeSyncResult(lastSyncResult?: Record<string, unknown> | null) {
  if (!lastSyncResult) {
    return {
      label: '未执行',
      tone: 'default' as MonitorTone,
      detail: '暂无同步记录',
    }
  }

  const status = String(lastSyncResult.status ?? '').toUpperCase()
  if (status === 'SUCCESS' || status === 'PARTIAL') {
    return {
      label: status === 'SUCCESS' ? '同步成功' : '部分成功',
      tone: 'success' as MonitorTone,
      detail: `已写入 ${Number(lastSyncResult.upsertedDocs ?? 0)} 条，失败 ${Number(lastSyncResult.failedBatches ?? 0)} 批`,
    }
  }

  if (status === 'FAILED') {
    return {
      label: '同步失败',
      tone: 'error' as MonitorTone,
      detail: `失败批次 ${Number(lastSyncResult.failedBatches ?? 0)}，可重新触发`,
    }
  }

  return {
    label: status || '处理中',
    tone: 'processing' as MonitorTone,
    detail: '结果正在回传中',
  }
}

export function summarizeTaskStatus(tasks: Array<{ status?: string }>) {
  let activeCount = 0
  let finishedCount = 0

  for (const task of tasks) {
    const status = String(task.status ?? '').toUpperCase()
    if (TERMINAL_TASK_STATUS.has(status)) {
      finishedCount += 1
    } else {
      activeCount += 1
    }
  }

  return {
    activeCount,
    finishedCount,
  }
}
