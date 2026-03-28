import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Rule } from '@/types'
import * as ruleApi from '@/api/modules/rule'

export const useRuleStore = defineStore('rule', () => {
  const rules = ref<Rule[]>([])
  const selectedRule = ref<Rule | null>(null)
  const loading = ref(false)

  async function fetchRules(params?: Record<string, unknown>) {
    loading.value = true
    try {
      const res = await ruleApi.getRules(params) as unknown as Rule[]
      rules.value = res
    } finally {
      loading.value = false
    }
  }

  async function selectRule(id: string) {
    const res = await ruleApi.getRule(id) as unknown as Rule
    selectedRule.value = res
  }

  return { rules, selectedRule, loading, fetchRules, selectRule }
})
