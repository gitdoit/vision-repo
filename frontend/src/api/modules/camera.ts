import client from '../client'
import type { Camera, CameraGroup } from '@/types'

export function getCameras(params?: Record<string, unknown>) {
  return client.get<{ items: Camera[]; total: number }>('/cameras', { params })
}

export function getCameraGroups() {
  return client.get<CameraGroup[]>('/cameras/groups')
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
