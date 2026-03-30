import client from '../client'
import type { InferenceRecord, ModelTestResult } from '@/types'

export function getInferenceRecords(params?: Record<string, unknown>) {
  return client.get<{ items: InferenceRecord[]; total: number }>('/inference', { params })
}

export function getInferenceDetail(id: string) {
  return client.get<InferenceRecord>(`/inference/${encodeURIComponent(id)}`)
}

export function testInference(formData: FormData) {
  return client.post<ModelTestResult>('/inference/test', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function exportInferenceCsv(params?: Record<string, unknown>) {
  return client.get('/inference/export/csv', { params, responseType: 'blob' })
}

export function exportInferenceExcel(params?: Record<string, unknown>) {
  return client.get('/inference/export/excel', { params, responseType: 'blob' })
}
