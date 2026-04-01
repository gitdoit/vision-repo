import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { InferenceRecord } from '@/types'
import * as inferenceApi from '@/api/modules/inference'

export const useInferenceStore = defineStore('inference', () => {
  const records = ref<InferenceRecord[]>([])
  const selectedRecord = ref<InferenceRecord | null>(null)
  const total = ref(0)
  const loading = ref(false)

  async function fetchRecords(params?: Record<string, unknown>) {
    loading.value = true
    try {
      const res = await inferenceApi.getInferenceRecords(params) as unknown as { items: InferenceRecord[]; total: number }
      records.value = res.items ?? []
      total.value = res.total ?? 0
    } catch (e) {
      console.error('Failed to fetch inference records:', e)
    } finally {
      loading.value = false
    }
  }

  async function selectRecord(id: string) {
    const res = await inferenceApi.getInferenceDetail(id) as unknown as InferenceRecord
    selectedRecord.value = res
  }

  return { records, selectedRecord, total, loading, fetchRecords, selectRecord }
})
