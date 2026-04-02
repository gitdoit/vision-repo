import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { DashboardStats, AlertRecord, WeeklyTrend, BusinessLineAlert, SystemHealth } from '@/types'
import * as dashboardApi from '@/api/modules/dashboard'

export const useDashboardStore = defineStore('dashboard', () => {
  const stats = ref<DashboardStats | null>(null)
  const weeklyTrend = ref<WeeklyTrend[]>([])
  const alertRanking = ref<BusinessLineAlert[]>([])
  const realtimeAlerts = ref<AlertRecord[]>([])
  const systemHealth = ref<SystemHealth | null>(null)
  const loading = ref(false)

  async function fetchAll() {
    loading.value = true
    try {
      const [s, w, a, r, h] = await Promise.all([
        dashboardApi.getDashboardStats(),
        dashboardApi.getWeeklyTrend(),
        dashboardApi.getAlertRanking(),
        dashboardApi.getRealtimeAlerts(),
        dashboardApi.getSystemHealth(),
      ])
      stats.value = s as any
      weeklyTrend.value = w as any
      alertRanking.value = a as any
      realtimeAlerts.value = r as any
      systemHealth.value = h as any
    } catch (e) {
      console.error('[Dashboard] fetchAll error:', e)
    } finally {
      loading.value = false
    }
  }

  return { stats, weeklyTrend, alertRanking, realtimeAlerts, systemHealth, loading, fetchAll }
})
