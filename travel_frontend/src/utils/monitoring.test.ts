import { describe, expect, it } from 'vitest'
import {
  getHealthTone,
  getQueryCountValue,
  summarizeSyncResult,
  summarizeTaskStatus,
} from './monitoring'

describe('monitoring utils', () => {
  it('maps health status to success tone', () => {
    expect(getHealthTone({ status: 'UP' })).toBe('success')
  })

  it('prefers stats row count for query totals', () => {
    expect(getQueryCountValue({ statsRowCount: 128, queryCount: 12 })).toBe(128)
  })

  it('summarizes sync result with defaults', () => {
    expect(summarizeSyncResult(null)).toEqual({
      label: '未执行',
      tone: 'default',
      detail: '暂无同步记录',
    })
  })

  it('counts active tasks from local records', () => {
    expect(
      summarizeTaskStatus([
        { status: 'SUCCESS' },
        { status: 'RUNNING' },
        { status: 'PENDING' },
      ]),
    ).toEqual({
      activeCount: 2,
      finishedCount: 1,
    })
  })
})
