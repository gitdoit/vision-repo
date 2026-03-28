<template>
  <div class="rounded-xl bg-bg-card p-5">
    <div class="flex items-center justify-between">
      <div>
        <p class="text-xs text-on-surface-variant">{{ title }}</p>
        <div class="mt-1 flex items-baseline gap-2">
          <span class="text-2xl font-bold">{{ formatted }}</span>
          <span v-if="trendLabel" class="text-xs" :class="trend >= 0 ? 'text-primary' : 'text-error'">
            <Icon :icon="trend >= 0 ? 'mdi:trending-up' : 'mdi:trending-down'" class="inline" />
            {{ trendLabel }}
          </span>
          <n-tag v-if="badge" size="tiny" :bordered="false" type="info">{{ badge }}</n-tag>
        </div>
      </div>
      <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-bg-active">
        <Icon :icon="icon" class="text-xl text-primary" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { NTag } from 'naive-ui'
import { Icon } from '@iconify/vue'

const props = defineProps<{
  title: string
  value: number
  icon: string
  trend?: number
  trendLabel?: string
  trendType?: 'positive' | 'negative'
  badge?: string
}>()

const formatted = computed(() => props.value.toLocaleString())
</script>
