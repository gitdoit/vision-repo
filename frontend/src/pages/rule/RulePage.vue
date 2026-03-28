<template>
  <div class="flex h-full gap-4">
    <!-- Left: Rule List -->
    <div class="w-72 shrink-0 rounded-xl bg-bg-card p-4">
      <div class="mb-3 flex items-center justify-between">
        <h3 class="text-sm font-semibold">规则列表</h3>
        <div class="flex gap-2">
          <n-button size="tiny" quaternary><Icon icon="mdi:plus" /></n-button>
          <n-button size="tiny" quaternary><Icon icon="mdi:magnify" /></n-button>
        </div>
      </div>

      <!-- Filter Tabs -->
      <div class="mb-3 flex gap-1">
        <button
          v-for="tab in filterTabs"
          :key="tab.value"
          class="rounded-full px-3 py-1 text-xs transition-colors"
          :class="activeFilter === tab.value ? 'bg-primary/15 text-primary' : 'text-on-surface-variant hover:bg-bg-active'"
          @click="activeFilter = tab.value"
        >
          {{ tab.label }}
        </button>
      </div>

      <!-- Rules -->
      <div class="space-y-2">
        <div
          v-for="rule in filteredRules"
          :key="rule.id"
          class="cursor-pointer rounded-lg p-3 transition-colors"
          :class="selectedRuleId === rule.id ? 'bg-bg-active ring-1 ring-primary/30' : 'hover:bg-bg-active'"
          @click="selectRule(rule)"
        >
          <div class="flex items-center justify-between">
            <span class="text-sm font-medium">{{ rule.name }}</span>
            <n-switch :value="rule.enabled" size="small" />
          </div>
          <div class="mt-1 flex items-center gap-2 text-xs text-on-surface-variant">
            <Icon icon="mdi:clock-outline" class="text-sm" />
            <span>{{ rule.schedule }}</span>
          </div>
          <div class="mt-1">
            <n-tag size="tiny" :bordered="false" :type="priorityType(rule.priority)">
              {{ rule.priority }}
            </n-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- Right: Rule Detail / Config -->
    <div v-if="selectedRule" class="flex-1 rounded-xl bg-bg-card p-6">
      <div class="mb-6 flex items-center justify-between">
        <h3 class="text-lg font-semibold">{{ selectedRule.name }}</h3>
        <div class="flex gap-2">
          <n-button size="small" quaternary>
            <template #icon><Icon icon="mdi:play-arrow" /></template>
            测试
          </n-button>
          <n-button size="small" quaternary>
            <template #icon><Icon icon="mdi:pencil" /></template>
            编辑
          </n-button>
          <n-button size="small" quaternary>
            <template #icon><Icon icon="mdi:content-save" /></template>
            保存配置
          </n-button>
        </div>
      </div>

      <!-- Condition Builder -->
      <section class="mb-6">
        <h4 class="mb-3 text-sm font-semibold text-on-surface-variant">条件构建器</h4>
        <div class="rounded-lg bg-bg-floor p-4">
          <div class="mb-2 flex items-center gap-2">
            <n-tag size="small" type="primary" :bordered="false">AND</n-tag>
          </div>
          <div class="space-y-2">
            <div
              v-for="cond in selectedRule.conditions"
              :key="cond.id"
              class="flex items-center gap-3 rounded-lg bg-bg-card px-4 py-2"
            >
              <span class="text-xs text-on-surface-variant w-28">{{ conditionLabel(cond.type) }}</span>
              <span class="text-xs text-primary">{{ cond.operator }}</span>
              <span class="text-sm font-mono">{{ cond.value }}</span>
              <span v-if="cond.type === 'frames'" class="text-xs text-on-surface-variant">frames</span>
              <button class="ml-auto text-on-surface-variant hover:text-error">
                <Icon icon="mdi:delete" class="text-sm" />
              </button>
            </div>
          </div>
          <n-button size="tiny" class="mt-3" quaternary>
            <template #icon><Icon icon="mdi:plus" /></template>
            新增条件
          </n-button>
        </div>
      </section>

      <!-- Action Config -->
      <section class="mb-6">
        <h4 class="mb-3 text-sm font-semibold text-on-surface-variant">动作配置</h4>
        <div class="grid grid-cols-2 gap-4">
          <n-form-item label="告警等级">
            <n-select
              :value="selectedRule.actions.alertLevel"
              :options="alertLevelOptions"
              size="small"
            />
          </n-form-item>
          <n-form-item label="推送方式">
            <div class="flex gap-1 flex-wrap">
              <n-tag
                v-for="m in selectedRule.actions.pushMethods"
                :key="m"
                size="small" :bordered="false" type="info"
              >
                {{ m }}
              </n-tag>
            </div>
          </n-form-item>
          <n-form-item label="存证选项">
            <div class="flex gap-1 flex-wrap">
              <n-tag
                v-for="e in selectedRule.actions.evidenceSave"
                :key="e"
                size="small" :bordered="false"
              >
                {{ e }}
              </n-tag>
            </div>
          </n-form-item>
        </div>
      </section>

      <!-- Schedule -->
      <section class="mb-6">
        <h4 class="mb-3 text-sm font-semibold text-on-surface-variant">生效时段</h4>
        <div class="grid grid-cols-2 gap-4">
          <n-form-item label="开始日期">
            <n-input :value="selectedRule.effectiveStart" size="small" />
          </n-form-item>
          <n-form-item label="截止日期">
            <n-input :value="selectedRule.effectiveEnd" size="small" />
          </n-form-item>
        </div>
        <div class="mt-3">
          <label class="text-xs text-on-surface-variant">循环周期</label>
          <div class="mt-2 flex gap-2">
            <button
              v-for="(day, i) in ['一', '二', '三', '四', '五', '六', '日']"
              :key="i"
              class="flex h-8 w-8 items-center justify-center rounded-full text-xs transition-colors"
              :class="selectedRule.weekdays.includes(i + 1) ? 'bg-primary text-on-primary' : 'bg-bg-active text-on-surface-variant'"
            >
              {{ day }}
            </button>
          </div>
        </div>
        <div class="mt-4 rounded-lg bg-bg-floor p-3 flex items-start gap-2">
          <Icon icon="mdi:information" class="text-primary mt-0.5" />
          <div class="text-xs text-on-surface-variant">
            当前规则已设置为周期性生效，系统将在选定时间段内自动激活 AI 检测引擎。
          </div>
        </div>
      </section>

      <div class="flex items-center justify-between rounded-lg bg-primary/5 px-4 py-3">
        <span class="text-xs text-primary">规则已热更新，无需重启服务</span>
        <n-button type="primary" size="small">
          <template #icon><Icon icon="mdi:lightning-bolt" /></template>
          发布规则
        </n-button>
      </div>
    </div>

    <div v-else class="flex-1 rounded-xl bg-bg-card p-6 flex items-center justify-center">
      <div class="text-center text-on-surface-variant">
        <Icon icon="mdi:file-document-edit" class="text-5xl opacity-20" />
        <p class="mt-3 text-sm">选择一条规则查看配置</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { NButton, NSwitch, NTag, NSelect, NFormItem, NInput } from 'naive-ui'
import { Icon } from '@iconify/vue'
import { useRuleStore } from '@/stores/rule'
import type { Rule } from '@/types'

const ruleStore = useRuleStore()
const selectedRuleId = ref<string | null>(null)
const activeFilter = ref('all')

onMounted(() => {
  ruleStore.fetchRules()
})

const filterTabs = [
  { label: '全部', value: 'all' },
  { label: '启用中', value: 'enabled' },
  { label: '已禁用', value: 'disabled' },
]

const filteredRules = computed(() => {
  if (activeFilter.value === 'enabled') return ruleStore.rules.filter(r => r.enabled)
  if (activeFilter.value === 'disabled') return ruleStore.rules.filter(r => !r.enabled)
  return ruleStore.rules
})

const selectedRule = computed(() =>
  ruleStore.rules.find(r => r.id === selectedRuleId.value) ?? null
)

function selectRule(rule: Rule) {
  selectedRuleId.value = rule.id
}

function priorityType(p: string): 'error' | 'warning' | 'info' {
  if (p === 'severe') return 'error'
  if (p === 'warning') return 'warning'
  return 'info'
}

function conditionLabel(type: string) {
  const labels: Record<string, string> = {
    target: '检测目标',
    confidence: '置信度',
    frames: '连续帧数',
    zone: '监测区域',
  }
  return labels[type] || type
}

const alertLevelOptions = [
  { label: '严重', value: 'severe' },
  { label: '警告', value: 'warning' },
  { label: '提示', value: 'info' },
]
</script>
