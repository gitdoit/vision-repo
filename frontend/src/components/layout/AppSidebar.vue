<template>
  <aside class="flex w-60 flex-col bg-bg-floor border-r border-outline-variant/15">
    <!-- Logo Area -->
    <div class="flex items-center gap-3 px-5 py-5">
      <div class="flex h-9 w-9 items-center justify-center rounded-lg bg-primary/10">
        <Icon icon="mdi:eye-outline" class="text-xl text-primary" />
      </div>
      <div>
        <div class="text-sm font-bold text-on-surface">Visual AI</div>
        <div class="text-[10px] tracking-widest text-on-surface-variant">智能视觉观察平台</div>
      </div>
    </div>

    <!-- Navigation -->
    <nav class="flex-1 space-y-1 px-3 py-2">
      <router-link
        v-for="item in navItems"
        :key="item.path"
        :to="item.path"
        class="nav-item group flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm transition-colors"
        :class="[
          isActive(item.path)
            ? 'bg-primary/10 text-primary'
            : 'text-on-surface-variant hover:bg-bg-active hover:text-on-surface'
        ]"
      >
        <Icon :icon="item.icon" class="text-lg" />
        <span>{{ item.label }}</span>
      </router-link>
    </nav>

    <!-- Bottom Actions -->
    <div class="space-y-1 border-t border-outline-variant/15 p-3">
      <button class="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm text-on-surface-variant hover:bg-bg-active transition-colors">
        <Icon icon="mdi:rocket-launch" class="text-lg" />
        <span>部署模型</span>
      </button>
      <button class="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm text-on-surface-variant hover:bg-bg-active transition-colors">
        <Icon icon="mdi:cog" class="text-lg" />
        <span>系统设置</span>
      </button>
      <button class="flex w-full items-center gap-3 rounded-lg px-3 py-2 text-sm text-on-surface-variant hover:bg-bg-active transition-colors">
        <Icon icon="mdi:help-circle-outline" class="text-lg" />
        <span>帮助中心</span>
      </button>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'
import { Icon } from '@iconify/vue'

const route = useRoute()

const navItems = [
  { path: '/dashboard', label: '首页概览', icon: 'mdi:view-dashboard' },
  { path: '/cameras', label: '摄像头接入管理', icon: 'mdi:video' },
  { path: '/models', label: '模型配置管理', icon: 'mdi:brain' },
  { path: '/nodes', label: '推理节点管理', icon: 'mdi:server-network' },
  { path: '/rules', label: '业务规则管理', icon: 'mdi:file-document-edit' },
  { path: '/inference', label: '推理历史查询', icon: 'mdi:history' },
]

function isActive(path: string) {
  return route.path === path
}
</script>
