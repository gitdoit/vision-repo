import client from '../client'
import type { Model } from '@/types'

export function getModels(params?: Record<string, unknown>) {
  return client.get<Model[]>('/models', { params })
}

export function getModel(id: string) {
  return client.get<Model>(`/models/${encodeURIComponent(id)}`)
}

export function loadModel(id: string, device: string = 'cpu', deviceName?: string, nodeId?: string) {
  return client.post(`/models/${encodeURIComponent(id)}/load`, null, { params: { device, deviceName, nodeId } })
}

export function unloadModel(id: string, nodeId?: string) {
  return client.post(`/models/${encodeURIComponent(id)}/unload`, null, { params: nodeId ? { nodeId } : undefined })
}

export function getDeviceInfo(nodeId: string) {
  return client.get<{ devices: string[]; cuda_available: boolean; gpu_name: string | null }>('/models/device/info', { params: { nodeId } })
}

export function updateModelConfig(id: string, data: Partial<Model>) {
  return client.put(`/models/${encodeURIComponent(id)}/config`, data)
}

export function deleteModel(id: string) {
  return client.delete(`/models/${encodeURIComponent(id)}`)
}

export function uploadModel(file: File, meta: {
  name: string
  version: string
  taskType?: string
  businessTag?: string
  engineSupport?: string
  targetHardware?: string
  author?: string
}) {
  const form = new FormData()
  form.append('file', file)
  form.append('name', meta.name)
  form.append('version', meta.version)
  if (meta.taskType) form.append('taskType', meta.taskType)
  if (meta.businessTag) form.append('businessTag', meta.businessTag)
  if (meta.engineSupport) form.append('engineSupport', meta.engineSupport)
  if (meta.targetHardware) form.append('targetHardware', meta.targetHardware)
  if (meta.author) form.append('author', meta.author)
  return client.post<Model>('/models/upload', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
