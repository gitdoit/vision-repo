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
      <n-popover trigger="click" placement="bottom-end" :width="360" @update:show="onPopoverShow">
        <template #trigger>
          <button class="relative text-on-surface-variant hover:text-on-surface transition-colors">
            <Icon icon="mdi:bell-outline" class="text-xl" />
            <span
              v-if="alertStore.unreadCount > 0"
              class="absolute -right-1 -top-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-error px-0.5 text-[10px] text-white"
            >{{ alertStore.unreadCount > 99 ? '99+' : alertStore.unreadCount }}</span>
          </button>
        </template>
        <div class="py-1">
          <div class="flex items-center justify-between px-3 py-2">
            <span class="text-sm font-semibold">告警通知</span>
            <span class="text-xs text-on-surface-variant">{{ alertStore.unreadCount }} 条未读</span>
          </div>
          <div v-if="latestAlerts.length" class="max-h-64 overflow-y-auto">
            <div
              v-for="alert in latestAlerts"
              :key="alert.id"
              class="flex items-start gap-2 px-3 py-2 cursor-pointer hover:bg-bg-active transition-colors"
              @click="goToAlert(alert.id)"
            >
              <span
                class="mt-1 h-2 w-2 shrink-0 rounded-full"
                :class="alert.alertLevel === 'severe' ? 'bg-red-400' : alert.alertLevel === 'warning' ? 'bg-amber-400' : 'bg-blue-400'"
              />
              <div class="min-w-0 flex-1">
                <p class="text-xs truncate">{{ alert.cameraName || alert.cameraId }} · {{ alert.alertType }}</p>
                <p class="text-xs text-on-surface-variant">{{ alert.alertTime }}</p>
              </div>
            </div>
          </div>
          <div v-else class="px-3 py-6 text-center text-xs text-on-surface-variant">暂无告警</div>
          <div class="border-t border-outline-variant/15 px-3 py-2 text-center">
            <router-link to="/alerts" class="text-xs text-primary hover:text-primary-dim">查看全部告警 →</router-link>
          </div>
        </div>
      </n-popover>
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
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { NPopover } from 'naive-ui'
import { Icon } from '@iconify/vue'
import { useAlertStore } from '@/stores/alert'
import { getLatestAlerts } from '@/api/modules/alert'
import type { Alert } from '@/types'

const route = useRoute()
const router = useRouter()
const alertStore = useAlertStore()

const latestAlerts = ref<Alert[]>([])
let pollTimer: ReturnType<typeof setInterval> | null = null

const currentTitle = computed(() => {
  return (route.meta?.title as string) || '视觉分析平台'
})

async function fetchUnread() {
  await alertStore.fetchUnreadCount()
}

async function onPopoverShow(show: boolean) {
  if (show) {
    try {
      latestAlerts.value = await getLatestAlerts(5) as unknown as Alert[]
    } catch (e) {
      console.error('Failed to fetch latest alerts:', e)
    }
  }
}

function goToAlert(id: string) {
  router.push(`/alerts?highlight=${encodeURIComponent(id)}`)
}

onMounted(() => {
  fetchUnread()
  pollTimer = setInterval(fetchUnread, 30000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>
