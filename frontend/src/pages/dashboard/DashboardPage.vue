<template>
  <div class="space-y-6">
    <!-- Hero Section -->
    <div class="relative overflow-hidden rounded-xl bg-gradient-to-br from-bg-card to-bg-active p-8">
      <div class="relative z-10">
        <h2 class="text-3xl font-bold tracking-tight">智能视觉观察平台</h2>
        <p class="mt-1 text-sm text-on-surface-variant">实时视觉智能分析仪表盘</p>
      </div>
      <div class="absolute right-8 top-6 text-6xl opacity-5">
        <Icon icon="mdi:eye-outline" />
      </div>
    </div>

    <!-- Stats Cards -->
    <div class="grid grid-cols-4 gap-4">
      <StatCard
        title="今日分析次数"
        :value="stats?.todayAnalyses ?? 0"
        :trend="stats?.todayAnalysesTrend ?? 0"
        icon="mdi:chart-line"
        trend-label="+15%"
      />
      <StatCard
        title="今日告警数量"
        :value="stats?.todayAlerts ?? 0"
        :trend="stats?.todayAlertsTrend ?? 0"
        icon="mdi:alert"
        trend-label="-5%"
        trend-type="negative"
      />
      <StatCard
        title="接入摄像头总数"
        :value="stats?.totalCameras ?? 0"
        icon="mdi:video"
        badge="在线"
      />
      <StatCard
        title="启用AI摄像头数"
        :value="stats?.aiEnabledCameras ?? 0"
        icon="mdi:memory"
        badge="AI处理中"
      />
    </div>

    <!-- Charts Row -->
    <div class="grid grid-cols-2 gap-4">
      <!-- Weekly Trend Chart -->
      <div class="rounded-xl bg-bg-card p-6">
        <div class="mb-4 flex items-center justify-between">
          <div>
            <h3 class="text-lg font-semibold">{{ dashboardStore.stats?.weeklyInferenceCount ?? 0 }}</h3>
            <p class="text-xs text-on-surface-variant">近7天推理次数趋势</p>
          </div>
        </div>
        <v-chart :option="trendChartOption" autoresize class="h-56" />
      </div>

      <!-- Alert Ranking -->
      <div class="rounded-xl bg-bg-card p-6">
        <h3 class="mb-4 text-sm font-semibold text-on-surface-variant">告警业务线排行</h3>
        <div class="space-y-4">
          <div v-for="item in alertRanking" :key="item.name" class="space-y-1">
            <div class="flex items-center justify-between text-sm">
              <span class="text-on-surface">{{ item.name }}</span>
              <span class="text-on-surface-variant">{{ item.percentage }}%</span>
            </div>
            <div class="h-1.5 overflow-hidden rounded-full bg-bg-active">
              <div
                class="h-full rounded-full bg-gradient-to-r from-primary to-primary-container transition-all"
                :style="{ width: `${item.percentage}%` }"
              />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Real-time Alerts Table -->
    <div class="rounded-xl bg-bg-card p-6">
      <div class="mb-4 flex items-center justify-between">
        <h3 class="text-sm font-semibold">实时告警列表</h3>
        <router-link to="/inference" class="text-xs text-primary hover:text-primary-dim transition-colors">
          查看全部历史 →
        </router-link>
      </div>
      <n-data-table
        :columns="alertColumns"
        :data="realtimeAlerts"
        :bordered="false"
        size="small"
        :row-class-name="() => 'bg-transparent'"
      />
    </div>

    <!-- Quick Actions -->
    <div class="grid grid-cols-3 gap-4">
      <router-link
        to="/cameras"
        class="group flex items-center gap-4 rounded-xl bg-bg-card p-5 transition-colors hover:bg-bg-active"
      >
        <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10">
          <Icon icon="mdi:video-plus" class="text-xl text-primary" />
        </div>
        <span class="text-sm font-medium">新增摄像头</span>
      </router-link>
      <router-link
        to="/models"
        class="group flex items-center gap-4 rounded-xl bg-bg-card p-5 transition-colors hover:bg-bg-active"
      >
        <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-tertiary/10">
          <Icon icon="mdi:cloud-upload" class="text-xl text-tertiary" />
        </div>
        <span class="text-sm font-medium">上传模型</span>
      </router-link>
      <router-link
        to="/rules"
        class="group flex items-center gap-4 rounded-xl bg-bg-card p-5 transition-colors hover:bg-bg-active"
      >
        <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-secondary/10">
          <Icon icon="mdi:folder-plus" class="text-xl text-secondary" />
        </div>
        <span class="text-sm font-medium">新建规则</span>
      </router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, h } from 'vue'
import { NDataTable, NTag } from 'naive-ui'
import type { DataTableColumns } from 'naive-ui'
import { Icon } from '@iconify/vue'
import 'echarts'
import VChart from 'vue-echarts'
import { useDashboardStore } from '@/stores/dashboard'
import type { AlertRecord } from '@/types'
import StatCard from './StatCard.vue'

const dashboardStore = useDashboardStore()

onMounted(() => {
  dashboardStore.fetchAll()
})

const stats = computed(() => dashboardStore.stats)
const alertRanking = computed(() => dashboardStore.alertRanking)
const realtimeAlerts = computed(() => dashboardStore.realtimeAlerts)

const trendChartOption = computed(() => ({
  backgroundColor: 'transparent',
  grid: { top: 10, right: 10, bottom: 24, left: 40 },
  xAxis: {
    type: 'category',
    data: dashboardStore.weeklyTrend.map(d => d.day),
    axisLine: { lineStyle: { color: '#40485d' } },
    axisLabel: { color: '#a3aac4', fontSize: 11 },
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: '#40485d26' } },
    axisLabel: { color: '#a3aac4', fontSize: 11 },
  },
  series: [{
    type: 'line',
    data: dashboardStore.weeklyTrend.map(d => d.count),
    smooth: true,
    symbol: 'circle',
    symbolSize: 6,
    lineStyle: { color: '#8ff5ff', width: 2 },
    itemStyle: { color: '#8ff5ff' },
    areaStyle: {
      color: {
        type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
        colorStops: [
          { offset: 0, color: 'rgba(143,245,255,0.25)' },
          { offset: 1, color: 'rgba(143,245,255,0)' },
        ],
      },
    },
  }],
  tooltip: {
    trigger: 'axis',
    backgroundColor: '#141f38',
    borderColor: '#40485d',
    textStyle: { color: '#dee5ff' },
  },
}))

const alertColumns: DataTableColumns<AlertRecord> = [
  { title: '时间', key: 'time', width: 100 },
  { title: '摄像头', key: 'camera', width: 200 },
  { title: '类型', key: 'type', width: 180 },
  {
    title: '业务线',
    key: 'businessLine',
    width: 120,
    render: (row) => h(NTag, { size: 'small', bordered: false, type: 'info' }, { default: () => row.businessLine }),
  },
  {
    title: '',
    key: 'action',
    width: 40,
    render: () => h(Icon, { icon: 'mdi:chevron-right', class: 'text-on-surface-variant' }),
  },
]
</script>
