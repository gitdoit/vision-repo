import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Model } from '@/types'
import * as modelApi from '@/api/modules/model'

export const useModelStore = defineStore('model', () => {
  const models = ref<Model[]>([])
  const selectedModel = ref<Model | null>(null)
  const loading = ref(false)

  async function fetchModels(params?: Record<string, unknown>) {
    loading.value = true
    try {
      const res = await modelApi.getModels(params) as unknown as Model[]
      models.value = res
    } finally {
      loading.value = false
    }
  }

  async function selectModel(id: string) {
    const res = await modelApi.getModel(id) as unknown as Model
    selectedModel.value = res
  }

  return { models, selectedModel, loading, fetchModels, selectModel }
})
