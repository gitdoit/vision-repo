<template>
  <div class="rounded-lg bg-bg-floor px-4 py-2">
    <div class="text-xs text-on-surface-variant">{{ label }}</div>
    <div class="mt-0.5 text-sm text-on-surface font-mono truncate">{{ value }}</div>
  </div>
</template>

<script setup lang="ts">
defineProps<{ label: string; value: string }>()
</script>
