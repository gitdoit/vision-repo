import client from '../client'
import type { Rule } from '@/types'

export function getRules(params?: Record<string, unknown>) {
  return client.get<Rule[]>('/rules', { params })
}

export function getRule(id: string) {
  return client.get<Rule>(`/rules/${encodeURIComponent(id)}`)
}

export function createRule(data: Partial<Rule>) {
  return client.post<Rule>('/rules', data)
}

export function updateRule(id: string, data: Partial<Rule>) {
  return client.put<Rule>(`/rules/${encodeURIComponent(id)}`, data)
}

export function deleteRule(id: string) {
  return client.delete(`/rules/${encodeURIComponent(id)}`)
}

export function deployRule(id: string) {
  return client.post(`/rules/${encodeURIComponent(id)}/deploy`)
}

export function testRule(id: string) {
  return client.post(`/rules/${encodeURIComponent(id)}/test`)
}
