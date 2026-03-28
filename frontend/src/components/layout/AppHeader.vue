<template>
  <header class="flex h-14 items-center justify-between border-b border-outline-variant/15 bg-bg-floor px-6">
    <!-- Page Title -->
    <h1 class="text-lg font-semibold text-on-surface">
      {{ currentTitle }}
    </h1>

    <!-- Right Actions -->
    <div class="flex items-center gap-4">
      <button class="text-on-surface-variant hover:text-on-surface transition-colors">
        <Icon icon="mdi:magnify" class="text-xl" />
      </button>
      <button class="relative text-on-surface-variant hover:text-on-surface transition-colors">
        <Icon icon="mdi:bell-outline" class="text-xl" />
        <span class="absolute -right-1 -top-1 flex h-4 w-4 items-center justify-center rounded-full bg-error text-[10px] text-white">3</span>
      </button>
      <button class="text-on-surface-variant hover:text-on-surface transition-colors">
        <Icon icon="mdi:cog-outline" class="text-xl" />
      </button>
      <div class="flex items-center gap-2 pl-2 border-l border-outline-variant/15">
        <div class="h-8 w-8 rounded-full bg-primary/20 flex items-center justify-center">
          <Icon icon="mdi:account" class="text-primary" />
        </div>
        <span class="text-sm text-on-surface">用户名称</span>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Icon } from '@iconify/vue'

const route = useRoute()

const currentTitle = computed(() => {
  return (route.meta?.title as string) || '视觉分析平台'
})
</script>
