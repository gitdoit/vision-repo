import client from '../client'
import type { Model } from '@/types'

export function getModels(params?: Record<string, unknown>) {
  return client.get<Model[]>('/models', { params })
}

export function getModel(id: string) {
  return client.get<Model>(`/models/${encodeURIComponent(id)}`)
}

export function loadModel(id: string) {
  return client.post(`/models/${encodeURIComponent(id)}/load`)
}

export function unloadModel(id: string) {
  return client.post(`/models/${encodeURIComponent(id)}/unload`)
}

export function updateModelConfig(id: string, data: Partial<Model>) {
  return client.put(`/models/${encodeURIComponent(id)}/config`, data)
}

export function deleteModel(id: string) {
  return client.delete(`/models/${encodeURIComponent(id)}`)
}
