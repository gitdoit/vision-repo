import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Camera, CameraGroup, VideoPlatform, SyncResult, PlatformImportRequest, PlatformImportResult } from '@/types'
import * as cameraApi from '@/api/modules/camera'

export const useCameraStore = defineStore('camera', () => {
  const cameras = ref<Camera[]>([])
  const groups = ref<CameraGroup[]>([])
  const total = ref(0)
  const loading = ref(false)
  const selectedGroupId = ref<string | null>(null)

  // 视频平台
  const platforms = ref<VideoPlatform[]>([])
  const platformLoading = ref(false)

  const page = ref(1)
  const pageSize = ref(20)

  async function fetchCameras(params?: Record<string, unknown>) {
    loading.value = true
    try {
      const res = await cameraApi.getCameras({ page: page.value, size: pageSize.value, ...params }) as unknown as { items: Camera[]; total: number }
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

  async function createGroup(data: { name: string; parentId?: string }) {
    await cameraApi.createCameraGroup(data)
    await fetchGroups()
  }

  async function fetchPlatforms() {
    platformLoading.value = true
    try {
      const res = await cameraApi.getVideoPlatforms() as unknown as VideoPlatform[]
      platforms.value = res
    } finally {
      platformLoading.value = false
    }
  }

  async function syncPlatform(id: string): Promise<SyncResult> {
    const res = await cameraApi.syncVideoPlatform(id) as unknown as SyncResult
    await fetchPlatforms()
    await fetchCameras()
    return res
  }

  async function batchImportFromPlatform(data: PlatformImportRequest): Promise<PlatformImportResult> {
    const res = await cameraApi.batchImportFromPlatform(data) as unknown as PlatformImportResult
    await fetchPlatforms()
    await fetchCameras()
    return res
  }

  return {
    cameras, groups, total, loading, selectedGroupId,
    page, pageSize,
    platforms, platformLoading,
    fetchCameras, fetchGroups, createGroup, fetchPlatforms, syncPlatform, batchImportFromPlatform,
  }
})
