import client from '../client'
import type { InferenceNode } from '@/types'

export function getNodes() {
  return client.get<InferenceNode[]>('/nodes')
}

export function getNode(id: string) {
  return client.get<InferenceNode>(`/nodes/${encodeURIComponent(id)}`)
}

export function updateNodeName(id: string, nodeName: string) {
  return client.put(`/nodes/${encodeURIComponent(id)}/name`, null, { params: { nodeName } })
}

export function removeNode(id: string) {
  return client.delete(`/nodes/${encodeURIComponent(id)}`)
}
