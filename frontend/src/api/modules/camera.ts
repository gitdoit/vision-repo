import client from '../client'
import type { Camera, CameraGroup, VideoPlatform, SyncResult, PlatformImportRequest, PlatformImportResult } from '@/types'

// ========== 摄像头 CRUD ==========

export function getCameras(params?: Record<string, unknown>) {
  return client.get<{ items: Camera[]; total: number }>('/cameras', { params })
}

export function getCameraGroups() {
  return client.get<CameraGroup[]>('/cameras/groups')
}

export function createCameraGroup(data: { name: string; parentId?: string }) {
  return client.post<CameraGroup>('/cameras/groups', data)
}

export function updateCameraGroup(id: string, data: { name: string; parentId?: string | null }) {
  return client.put(`/cameras/groups/${encodeURIComponent(id)}`, data)
}

export function deleteCameraGroup(id: string) {
  return client.delete(`/cameras/groups/${encodeURIComponent(id)}`)
}

export function getCamera(id: string) {
  return client.get<Camera>(`/cameras/${encodeURIComponent(id)}`)
}

export function createCamera(data: Partial<Camera>) {
  return client.post<Camera>('/cameras', data)
}

export function updateCamera(id: string, data: Partial<Camera>) {
  return client.put<Camera>(`/cameras/${encodeURIComponent(id)}`, data)
}

export function deleteCamera(id: string) {
  return client.delete(`/cameras/${encodeURIComponent(id)}`)
}

/** 更新摄像头所属分组 */
export function updateCameraGroups(cameraId: string, groupIds: string[]) {
  return client.put(`/cameras/${encodeURIComponent(cameraId)}/groups`, { groupIds })
}

/** 批量导入摄像头（JSON 列表） */
export function importCameras(cameras: Partial<Camera>[]) {
  return client.post<{ success: number; failed: number }>('/cameras/import', { cameras })
}

// ========== 视频平台管理 ==========

export function getVideoPlatforms() {
  return client.get<VideoPlatform[]>('/video-platforms')
}

export function createVideoPlatform(data: Partial<VideoPlatform>) {
  return client.post<VideoPlatform>('/video-platforms', data)
}

export function updateVideoPlatform(id: string, data: Partial<VideoPlatform>) {
  return client.put<VideoPlatform>(`/video-platforms/${encodeURIComponent(id)}`, data)
}

export function deleteVideoPlatform(id: string) {
  return client.delete(`/video-platforms/${encodeURIComponent(id)}`)
}

/** 测试视频平台连接 */
export function testVideoPlatform(id: string) {
  return client.post<{ connected: boolean; message: string }>(`/video-platforms/${encodeURIComponent(id)}/test`)
}

/** 手动触发同步 */
export function syncVideoPlatform(id: string) {
  return client.post<SyncResult>(`/video-platforms/${encodeURIComponent(id)}/sync`)
}

/** 从视频平台批量导入摄像头 */
export function batchImportFromPlatform(data: PlatformImportRequest) {
  return client.post<PlatformImportResult>('/video-platforms/batch-import', data)
}
