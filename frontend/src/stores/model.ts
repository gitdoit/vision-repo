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
      const res = await modelApi.getModels(params) as unknown as { items: Model[]; total: number }
      models.value = res.items ?? []
    } finally {
      loading.value = false
    }
  }

  async function selectModel(id: string) {
    const res = await modelApi.getModel(id) as unknown as Model
    selectedModel.value = res
  }

  async function uploadModel(file: File, meta: {
    name: string
    version: string
    taskType?: string
    businessTag?: string
    engineSupport?: string
    targetHardware?: string
    author?: string
  }) {
    await modelApi.uploadModel(file, meta)
    await fetchModels()
  }

  async function updateModelConfig(id: string, data: Partial<Model>) {
    await modelApi.updateModelConfig(id, data)
    await fetchModels()
  }

  async function loadModel(id: string, device: string = 'cpu', deviceName?: string, nodeId?: string) {
    await modelApi.loadModel(id, device, deviceName, nodeId)
    await fetchModels()
  }

  async function unloadModel(id: string) {
    await modelApi.unloadModel(id)
    await fetchModels()
  }

  async function fetchDeviceInfo(nodeId: string) {
    const res = await modelApi.getDeviceInfo(nodeId) as unknown as { devices: string[]; cuda_available: boolean; gpu_name: string | null }
    return res
  }

  async function deleteModel(id: string) {
    await modelApi.deleteModel(id)
    await fetchModels()
  }

  return { models, selectedModel, loading, fetchModels, selectModel, uploadModel, updateModelConfig, loadModel, unloadModel, deleteModel, fetchDeviceInfo }
})
