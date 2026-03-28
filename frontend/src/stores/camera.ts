import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Camera, CameraGroup } from '@/types'
import * as cameraApi from '@/api/modules/camera'

export const useCameraStore = defineStore('camera', () => {
  const cameras = ref<Camera[]>([])
  const groups = ref<CameraGroup[]>([])
  const total = ref(0)
  const loading = ref(false)
  const selectedGroupId = ref<string | null>(null)

  async function fetchCameras(params?: Record<string, unknown>) {
    loading.value = true
    try {
      const res = await cameraApi.getCameras(params) as unknown as { items: Camera[]; total: number }
      cameras.value = res.items
      total.value = res.total
    } finally {
      loading.value = false
    }
  }

  async function fetchGroups() {
    const res = await cameraApi.getCameraGroups() as unknown as CameraGroup[]
    groups.value = res
  }

  return { cameras, groups, total, loading, selectedGroupId, fetchCameras, fetchGroups }
})
