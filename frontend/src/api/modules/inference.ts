import client from '../client'
import type { InferenceRecord } from '@/types'

export function getInferenceRecords(params?: Record<string, unknown>) {
  return client.get<{ items: InferenceRecord[]; total: number }>('/inference', { params })
}

export function getInferenceDetail(id: string) {
  return client.get<InferenceRecord>(`/inference/${encodeURIComponent(id)}`)
}

export function exportInferenceCsv(params?: Record<string, unknown>) {
  return client.get('/inference/export/csv', { params, responseType: 'blob' })
}

export function exportInferenceExcel(params?: Record<string, unknown>) {
  return client.get('/inference/export/excel', { params, responseType: 'blob' })
}
