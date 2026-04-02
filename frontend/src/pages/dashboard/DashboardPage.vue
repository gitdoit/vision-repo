<template>
  <div class="space-y-4">
    <!-- Hero Section -->
    <div class="relative overflow-hidden rounded-xl bg-gradient-to-br from-bg-card to-bg-active p-6">
      <div class="relative z-10">
        <h2 class="text-2xl font-bold tracking-tight">智能视觉观察平台</h2>
        <p class="mt-1 text-sm text-on-surface-variant">实时视觉智能分析仪表盘</p>
      </div>
      <div class="absolute right-8 top-4 text-6xl opacity-5">
        <Icon icon="mdi:eye-outline" />
      </div>
    </div>

    <!-- Stats Cards - 6 cards in 2 rows of 3 -->
    <div class="grid grid-cols-3 gap-3">
      <StatCard
        title="今日分析次数"
        :value="stats?.todayInferenceCount ?? 0"
        :trend="stats?.todayInferenceChange ?? 0"
        icon="mdi:chart-line"
      />
      <StatCard
        title="今日告警数量"
        :value="stats?.todayAlertCount ?? 0"
        :trend="stats?.todayAlertChange ?? 0"
        icon="mdi:alert"
        trend-type="negative"
      />
      <StatCard
        title="接入摄像头"
        :value="stats?.totalCameraCount ?? 0"
        icon="mdi:video"
        badge="在线"
      />
      <StatCard
        title="AI 摄像头"
        :value="stats?.aiEnabledCameraCount ?? 0"
        icon="mdi:memory"
        badge="AI"
      />
      <StatCard
        title="运行中任务"
        :value="systemHealth?.runningTaskCount ?? 0"
        icon="mdi:play-circle"
        badge="运行中"
      />
      <StatCard
        title="未读告警"
        :value="systemHealth?.unreadAlertCount ?? 0"
        icon="mdi:bell-alert"
      />
    </div>

    <!-- Charts Row -->
    <div class="grid grid-cols-5 gap-4">
      <!-- Weekly Trend Chart (dual line) -->
      <div class="col-span-3 rounded-xl bg-bg-card p-5">
        <div class="mb-3 flex items-center justify-between">
          <div>
            <h3 class="text-lg font-semibold">{{ weeklyTotal }}</h3>
            <p class="text-xs text-on-surface-variant">近7天推理/告警趋势</p>
          </div>
        </div>
        <v-chart :option="trendChartOption" autoresize class="h-52" />
      </div>

      <!-- System Health Panel -->
      <div class="col-span-2 rounded-xl bg-bg-card p-5 space-y-4">
        <h3 class="text-sm font-semibold text-on-surface-variant">系统健康概览</h3>
        <div class="space-y-3">
          <!-- Nodes -->
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-2">
              <span class="h-2 w-2 rounded-full" :class="(systemHealth?.onlineNodeCount ?? 0) > 0 ? 'bg-green-400' : 'bg-red-400'" />
              <span class="text-sm">推理节点</span>
            </div>
            <span class="text-sm font-semibold">
              <span class="text-primary">{{ systemHealth?.onlineNodeCount ?? 0 }}</span>
              <span class="text-on-surface-variant font-normal"> / {{ systemHealth?.totalNodeCount ?? 0 }}</span>
            </span>
          </div>
          <!-- Models -->
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-2">
              <Icon icon="mdi:cube-outline" class="text-sm text-on-surface-variant" />
              <span class="text-sm">已部署模型</span>
            </div>
            <span class="text-sm font-semibold">
              <span class="text-primary">{{ systemHealth?.deployedModelCount ?? 0 }}</span>
              <span class="text-on-surface-variant font-normal"> / {{ systemHealth?.totalModelCount ?? 0 }}</span>
            </span>
          </div>
          <!-- Task Status -->
          <div>
            <div class="flex items-center justify-between mb-2">
              <span class="text-sm text-on-surface-variant">任务状态分布</span>
            </div>
            <div class="flex gap-2">
              <div class="flex-1 rounded-lg bg-bg-void px-3 py-2 text-center">
                <div class="text-xs text-on-surface-variant">运行中</div>
                <div class="text-sm font-bold text-green-400">{{ systemHealth?.runningTaskCount ?? 0 }}</div>
              </div>
              <div class="flex-1 rounded-lg bg-bg-void px-3 py-2 text-center">
                <div class="text-xs text-on-surface-variant">已停止</div>
                <div class="text-sm font-bold text-on-surface-variant">{{ systemHealth?.stoppedTaskCount ?? 0 }}</div>
              </div>
              <div class="flex-1 rounded-lg bg-bg-void px-3 py-2 text-center">
                <div class="text-xs text-on-surface-variant">异常</div>
                <div class="text-sm font-bold text-red-400">{{ systemHealth?.errorTaskCount ?? 0 }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Bottom Row -->
    <div class="grid grid-cols-5 gap-4">
      <!-- Alert Ranking (bar style) -->
      <div class="col-span-2 rounded-xl bg-bg-card p-5">
        <h3 class="mb-3 text-sm font-semibold text-on-surface-variant">告警业务线排行</h3>
        <div class="space-y-3">
          <div v-for="item in alertRanking" :key="item.name" class="flex items-center gap-3">
            <span class="text-sm text-on-surface w-24 truncate">{{ item.name }}</span>
            <div class="flex-1 h-5 overflow-hidden rounded bg-bg-active relative">
              <div
                class="h-full rounded bg-gradient-to-r from-primary to-primary-container transition-all"
                :style="{ width: `${item.percentage}%` }"
              />
            </div>
            <span class="text-xs font-mono text-on-surface-variant w-8 text-right">{{ item.count ?? 0 }}</span>
          </div>
          <div v-if="!alertRanking.length" class="text-xs text-on-surface-variant text-center py-4">暂无告警数据</div>
        </div>
      </div>

      <!-- Real-time Alerts Table -->
      <div class="col-span-3 rounded-xl bg-bg-card p-5">
        <div class="mb-3 flex items-center justify-between">
          <h3 class="text-sm font-semibold">最近告警</h3>
          <router-link to="/alerts" class="text-xs text-primary hover:text-primary-dim transition-colors">
            查看全部 →
          </router-link>
        </div>
        <n-data-table
          :columns="alertColumns"
          :data="realtimeAlerts"
          :bordered="false"
          size="small"
          :row-class-name="() => 'bg-transparent'"
          :max-height="200"
        />
      </div>
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
const systemHealth = computed(() => dashboardStore.systemHealth)
const weeklyTotal = computed(() =>
  dashboardStore.weeklyTrend.reduce((sum, d) => sum + d.count, 0)
)
const alertRanking = computed(() => dashboardStore.alertRanking)
const realtimeAlerts = computed(() => dashboardStore.realtimeAlerts)

const trendChartOption = computed(() => ({
  backgroundColor: 'transparent',
  grid: { top: 30, right: 10, bottom: 24, left: 40 },
  legend: {
    data: ['推理次数', '告警次数'],
    top: 0,
    right: 0,
    textStyle: { color: '#a3aac4', fontSize: 11 },
  },
  xAxis: {
    type: 'category',
    data: dashboardStore.weeklyTrend.map(d => d.date),
    axisLine: { lineStyle: { color: '#40485d' } },
    axisLabel: { color: '#a3aac4', fontSize: 11 },
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: '#40485d26' } },
    axisLabel: { color: '#a3aac4', fontSize: 11 },
  },
  series: [
    {
      name: '推理次数',
      type: 'line',
      data: dashboardStore.weeklyTrend.map(d => d.count),
      smooth: true,
      symbol: 'circle',
      symbolSize: 5,
      lineStyle: { color: '#8ff5ff', width: 2 },
      itemStyle: { color: '#8ff5ff' },
      areaStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(143,245,255,0.2)' },
            { offset: 1, color: 'rgba(143,245,255,0)' },
          ],
        },
      },
    },
    {
      name: '告警次数',
      type: 'line',
      data: dashboardStore.weeklyTrend.map(d => d.alertCount ?? 0),
      smooth: true,
      symbol: 'circle',
      symbolSize: 5,
      lineStyle: { color: '#ff6b6b', width: 2 },
      itemStyle: { color: '#ff6b6b' },
      areaStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(255,107,107,0.15)' },
            { offset: 1, color: 'rgba(255,107,107,0)' },
          ],
        },
      },
    },
  ],
  tooltip: {
    trigger: 'axis',
    backgroundColor: '#141f38',
    borderColor: '#40485d',
    textStyle: { color: '#dee5ff' },
  },
}))

const alertColumns: DataTableColumns<AlertRecord> = [
  { title: '时间', key: 'time', width: 100 },
  { title: '摄像头', key: 'camera', width: 160, ellipsis: { tooltip: true } },
  { title: '类型', key: 'type', width: 140 },
  {
    title: '业务线',
    key: 'businessLine',
    width: 100,
    render: (row) => h(NTag, { size: 'small', bordered: false, type: 'info' }, { default: () => row.businessLine }),
  },
]
</script>
