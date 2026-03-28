import client from '../client'
import type { DashboardStats, AlertRecord, WeeklyTrend, BusinessLineAlert } from '@/types'

export function getDashboardStats() {
  return client.get<DashboardStats>('/dashboard/stats')
}

export function getWeeklyTrend() {
  return client.get<WeeklyTrend[]>('/dashboard/weekly-trend')
}

export function getAlertRanking() {
  return client.get<BusinessLineAlert[]>('/dashboard/alert-ranking')
}

export function getRealtimeAlerts() {
  return client.get<AlertRecord[]>('/dashboard/realtime-alerts')
}
