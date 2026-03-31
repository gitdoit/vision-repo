import client from '../client'
import type { MonitorTask, MonitorTaskForm } from '@/types'

export function getMonitorTasks(params?: Record<string, unknown>) {
  return client.get<{ items: MonitorTask[]; total: number }>('/monitor-tasks', { params })
}

export function getMonitorTask(id: string) {
  return client.get<MonitorTask>(`/monitor-tasks/${encodeURIComponent(id)}`)
}

export function createMonitorTask(data: MonitorTaskForm) {
  return client.post<MonitorTask>('/monitor-tasks', data)
}

export function updateMonitorTask(id: string, data: MonitorTaskForm) {
  return client.put<MonitorTask>(`/monitor-tasks/${encodeURIComponent(id)}`, data)
}

export function deleteMonitorTask(id: string) {
  return client.delete(`/monitor-tasks/${encodeURIComponent(id)}`)
}

export function startMonitorTask(id: string) {
  return client.post(`/monitor-tasks/${encodeURIComponent(id)}/start`)
}

export function stopMonitorTask(id: string) {
  return client.post(`/monitor-tasks/${encodeURIComponent(id)}/stop`)
}
