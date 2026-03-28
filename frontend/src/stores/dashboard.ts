import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { DashboardStats, AlertRecord, WeeklyTrend, BusinessLineAlert } from '@/types'
import * as dashboardApi from '@/api/modules/dashboard'

export const useDashboardStore = defineStore('dashboard', () => {
  const stats = ref<DashboardStats | null>(null)
  const weeklyTrend = ref<WeeklyTrend[]>([])
  const alertRanking = ref<BusinessLineAlert[]>([])
  const realtimeAlerts = ref<AlertRecord[]>([])
  const loading = ref(false)

  async function fetchAll() {
    loading.value = true
    try {
      const [s, w, a, r] = await Promise.all([
        dashboardApi.getDashboardStats(),
        dashboardApi.getWeeklyTrend(),
        dashboardApi.getAlertRanking(),
        dashboardApi.getRealtimeAlerts(),
      ])
      stats.value = s as any
      weeklyTrend.value = w as any
      alertRanking.value = a as any
      realtimeAlerts.value = r as any
    } catch (e) {
      console.error('[Dashboard] fetchAll error:', e)
    } finally {
      loading.value = false
    }
  }

  return { stats, weeklyTrend, alertRanking, realtimeAlerts, loading, fetchAll }
})
