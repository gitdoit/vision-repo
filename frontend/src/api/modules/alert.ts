import client from '../client'
import type { Alert } from '@/types'

export function getAlerts(params?: Record<string, unknown>) {
  return client.get<{ items: Alert[]; total: number }>('/alerts', { params })
}

export function getAlert(id: string) {
  return client.get<Alert>(`/alerts/${encodeURIComponent(id)}`)
}

export function markAsRead(id: string) {
  return client.post<void>(`/alerts/${encodeURIComponent(id)}/read`)
}

export function markBatchAsRead(ids: string[]) {
  return client.post<void>('/alerts/batch-read', ids)
}

export function markAllAsRead() {
  return client.post<void>('/alerts/all-read')
}

export function getUnreadCount() {
  return client.get<number>('/alerts/unread-count')
}

export function getLatestAlerts(limit = 5) {
  return client.get<Alert[]>('/alerts/latest', { params: { limit } })
}
