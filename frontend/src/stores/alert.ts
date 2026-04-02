import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Alert } from '@/types'
import * as alertApi from '@/api/modules/alert'

export const useAlertStore = defineStore('alert', () => {
  const alerts = ref<Alert[]>([])
  const total = ref(0)
  const unreadCount = ref(0)
  const loading = ref(false)

  async function fetchAlerts(params?: Record<string, unknown>) {
    loading.value = true
    try {
      const res = await alertApi.getAlerts(params) as unknown as { items: Alert[]; total: number }
      alerts.value = res.items ?? []
      total.value = res.total ?? 0
    } catch (e) {
      console.error('Failed to fetch alerts:', e)
    } finally {
      loading.value = false
    }
  }

  async function fetchUnreadCount() {
    try {
      unreadCount.value = await alertApi.getUnreadCount() as unknown as number
    } catch (e) {
      console.error('Failed to fetch unread count:', e)
    }
  }

  async function markAsRead(id: string) {
    await alertApi.markAsRead(id)
    const alert = alerts.value.find(a => a.id === id)
    if (alert && !alert.readStatus) {
      alert.readStatus = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    }
  }

  async function markBatchAsRead(ids: string[]) {
    await alertApi.markBatchAsRead(ids)
    let decremented = 0
    for (const id of ids) {
      const alert = alerts.value.find(a => a.id === id)
      if (alert && !alert.readStatus) {
        alert.readStatus = true
        decremented++
      }
    }
    unreadCount.value = Math.max(0, unreadCount.value - decremented)
  }

  async function markAllAsRead() {
    await alertApi.markAllAsRead()
    alerts.value.forEach(a => { a.readStatus = true })
    unreadCount.value = 0
  }

  return { alerts, total, unreadCount, loading, fetchAlerts, fetchUnreadCount, markAsRead, markBatchAsRead, markAllAsRead }
})
