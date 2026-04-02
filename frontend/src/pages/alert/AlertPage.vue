<template>
  <div class="space-y-4">
    <p class="text-sm text-on-surface-variant">告警事件管理与处理</p>

    <!-- Summary Strip -->
    <div class="flex items-center gap-6 rounded-xl bg-bg-card px-6 py-4">
      <div class="text-center">
        <div class="text-2xl font-bold text-on-surface">{{ alertStore.total }}</div>
        <div class="text-xs text-on-surface-variant">告警总数</div>
      </div>
      <div class="h-8 border-l border-outline-variant/15" />
      <div class="text-center">
        <div class="text-2xl font-bold text-error">{{ alertStore.unreadCount }}</div>
        <div class="text-xs text-on-surface-variant">未读</div>
      </div>
      <div class="h-8 border-l border-outline-variant/15" />
      <div class="flex gap-4">
        <div v-for="level in levelStats" :key="level.key" class="flex items-center gap-1.5">
          <span class="h-2 w-2 rounded-full" :class="levelDotClass(level.key)" />
          <span class="text-xs text-on-surface-variant">{{ level.label }}</span>
          <span class="text-sm font-semibold text-on-surface">{{ level.count }}</span>
        </div>
      </div>
      <div class="ml-auto flex gap-2">
        <n-button size="small" :disabled="alertStore.unreadCount === 0" @click="handleMarkAllRead">
          <template #icon><Icon icon="mdi:email-check" /></template>
          全部已读
        </n-button>
        <n-button size="small" :disabled="checkedKeys.length === 0" @click="handleBatchRead">
          <template #icon><Icon icon="mdi:check-all" /></template>
          批量已读 ({{ checkedKeys.length }})
        </n-button>
      </div>
    </div>

    <!-- Filters -->
    <div class="flex items-center gap-3 rounded-xl bg-bg-card px-4 py-3">
      <n-select
        v-model:value="filterLevel"
        :options="levelOptions"
        placeholder="告警级别"
        size="small"
        class="w-32"
        clearable
      />
      <n-select
        v-model:value="filterReadStatus"
        :options="readOptions"
        placeholder="已读状态"
        size="small"
        class="w-32"
        clearable
      />
      <n-input
        v-model:value="filterKeyword"
        placeholder="搜索摄像头名称"
        size="small"
        class="w-48"
        clearable
      >
        <template #prefix><Icon icon="mdi:magnify" class="text-on-surface-variant" /></template>
      </n-input>
      <n-date-picker
        v-model:value="filterDateRange"
        type="daterange"
        size="small"
        clearable
        :shortcuts="dateShortcuts"
      />
      <n-button size="small" type="primary" @click="doSearch">
        <template #icon><Icon icon="mdi:magnify" /></template>
        查询
      </n-button>
      <n-button size="small" quaternary @click="resetFilters">重置</n-button>
    </div>

    <!-- Table -->
    <div class="rounded-xl bg-bg-card overflow-hidden">
      <n-data-table
        :columns="columns"
        :data="alertStore.alerts"
        :loading="alertStore.loading"
        :bordered="false"
        :row-key="(row: Alert) => row.id"
        v-model:checked-row-keys="checkedKeys"
        size="small"
        :row-class-name="rowClassName"
        @update:checked-row-keys="(keys: string[]) => checkedKeys = keys"
      />
      <div class="flex items-center justify-between px-4 py-3 border-t border-outline-variant/10">
        <span class="text-xs text-on-surface-variant">共 {{ alertStore.total }} 条记录</span>
        <n-pagination
          v-model:page="currentPage"
          v-model:page-size="pageSize"
          :item-count="alertStore.total"
          :page-sizes="[10, 20, 50]"
          show-size-picker
          size="small"
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
        />
      </div>
    </div>

    <!-- Detail Drawer -->
    <n-drawer v-model:show="drawerVisible" :width="540" placement="right">
      <n-drawer-content v-if="selectedAlert" closable>
        <template #header>
          <div class="flex items-center gap-3">
            <span class="text-base font-semibold">告警详情</span>
            <n-tag size="small" :bordered="false" :type="levelTagType(selectedAlert.alertLevel)">
              {{ levelLabel(selectedAlert.alertLevel) }}
            </n-tag>
            <n-tag v-if="!selectedAlert.readStatus" size="small" :bordered="false" type="error">未读</n-tag>
          </div>
        </template>

        <div class="space-y-5">
          <!-- Basic Info -->
          <div class="grid grid-cols-2 gap-4">
            <div class="space-y-1">
              <span class="text-xs text-on-surface-variant">告警时间</span>
              <div class="text-sm font-medium">{{ selectedAlert.alertTime }}</div>
            </div>
            <div class="space-y-1">
              <span class="text-xs text-on-surface-variant">告警类型</span>
              <div class="text-sm font-medium">{{ selectedAlert.alertType || '—' }}</div>
            </div>
            <div class="space-y-1">
              <span class="text-xs text-on-surface-variant">摄像头</span>
              <div class="text-sm font-medium">{{ selectedAlert.cameraName || selectedAlert.cameraId || '—' }}</div>
            </div>
            <div class="space-y-1">
              <span class="text-xs text-on-surface-variant">场景</span>
              <div class="text-sm font-medium">{{ selectedAlert.scene || '—' }}</div>
            </div>
            <div class="space-y-1">
              <span class="text-xs text-on-surface-variant">关联任务</span>
              <div class="text-sm font-medium">
                <router-link
                  v-if="selectedAlert.taskId"
                  :to="`/tasks`"
                  class="text-primary hover:underline"
                >{{ selectedAlert.taskName || selectedAlert.taskId }}</router-link>
                <span v-else>—</span>
              </div>
            </div>
            <div class="space-y-1">
              <span class="text-xs text-on-surface-variant">关联规则</span>
              <div class="text-sm font-medium">{{ selectedAlert.ruleId || '—' }}</div>
            </div>
          </div>

          <!-- Trigger Condition -->
          <div v-if="selectedAlert.triggerCondition" class="space-y-2">
            <h4 class="text-sm font-semibold text-on-surface-variant">触发条件</h4>
            <div class="rounded-lg bg-bg-floor px-4 py-3 text-sm">{{ selectedAlert.triggerCondition }}</div>
          </div>

          <!-- Evidence Images -->
          <div v-if="parsedEvidence" class="space-y-2">
            <h4 class="text-sm font-semibold text-on-surface-variant">证据图片</h4>
            <div class="grid grid-cols-2 gap-2">
              <div
                v-for="(url, i) in parsedEvidence.imageUrls"
                :key="i"
                class="aspect-video rounded-lg bg-bg-void overflow-hidden"
              >
                <img :src="url" class="w-full h-full object-cover" alt="" />
              </div>
            </div>
          </div>

          <!-- Related Objects -->
          <div v-if="parsedRelatedObjects.length > 0" class="space-y-2">
            <h4 class="text-sm font-semibold text-on-surface-variant">检测目标</h4>
            <div class="flex flex-wrap gap-2">
              <n-tag v-for="obj in parsedRelatedObjects" :key="obj" size="small" :bordered="false" type="warning">
                {{ obj }}
              </n-tag>
            </div>
          </div>

          <!-- Location -->
          <div v-if="selectedAlert.location" class="space-y-2">
            <h4 class="text-sm font-semibold text-on-surface-variant">位置信息</h4>
            <pre class="rounded-lg bg-bg-floor px-4 py-3 text-xs font-mono text-on-surface-variant overflow-auto max-h-32 whitespace-pre-wrap">{{ formatJson(selectedAlert.location) }}</pre>
          </div>

          <!-- Actions -->
          <div class="flex gap-3 pt-2">
            <n-button
              v-if="!selectedAlert.readStatus"
              type="primary"
              size="small"
              @click="handleReadSingle(selectedAlert.id)"
            >
              标记为已读
            </n-button>
            <n-button size="small" quaternary @click="drawerVisible = false">关闭</n-button>
          </div>
        </div>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, h, computed, onMounted } from 'vue'
import {
  NDataTable, NTag, NButton, NSelect, NInput, NDatePicker,
  NDrawer, NDrawerContent, NPagination, useMessage,
} from 'naive-ui'
import type { DataTableColumns } from 'naive-ui'
import { Icon } from '@iconify/vue'
import { useAlertStore } from '@/stores/alert'
import type { Alert } from '@/types'

const message = useMessage()
const alertStore = useAlertStore()

const currentPage = ref(1)
const pageSize = ref(20)
const checkedKeys = ref<string[]>([])
const drawerVisible = ref(false)
const selectedAlert = ref<Alert | null>(null)

// Filters
const filterLevel = ref<string | null>(null)
const filterReadStatus = ref<boolean | null>(null)
const filterKeyword = ref('')
const filterDateRange = ref<[number, number] | null>(null)

const levelOptions = [
  { label: '严重', value: 'severe' },
  { label: '警告', value: 'warning' },
  { label: '提示', value: 'info' },
]

const readOptions = [
  { label: '未读', value: false },
  { label: '已读', value: true },
]

const dateShortcuts = {
  今天: () => {
    const now = Date.now()
    const start = new Date(new Date().setHours(0, 0, 0, 0)).getTime()
    return [start, now] as [number, number]
  },
  '近7天': () => {
    const now = Date.now()
    return [now - 7 * 24 * 3600_000, now] as [number, number]
  },
  '近30天': () => {
    const now = Date.now()
    return [now - 30 * 24 * 3600_000, now] as [number, number]
  },
}

// Level stats computed from current page data
const levelStats = computed(() => {
  const counts: Record<string, number> = { severe: 0, warning: 0, info: 0 }
  for (const a of alertStore.alerts) {
    if (counts[a.alertLevel] !== undefined) counts[a.alertLevel]++
  }
  return [
    { key: 'severe', label: '严重', count: counts.severe },
    { key: 'warning', label: '警告', count: counts.warning },
    { key: 'info', label: '提示', count: counts.info },
  ]
})

// Table columns
const columns: DataTableColumns<Alert> = [
  { type: 'selection' },
  {
    title: '告警时间',
    key: 'alertTime',
    width: 170,
    render: (row) => h('span', { class: 'text-xs' }, row.alertTime || '—'),
  },
  {
    title: '摄像头',
    key: 'cameraName',
    width: 160,
    render: (row) =>
      h('div', { class: 'flex items-center gap-1.5' }, [
        h(Icon, { icon: 'mdi:video', class: 'text-on-surface-variant text-sm shrink-0' }),
        h('span', { class: 'text-sm truncate' }, row.cameraName || row.cameraId || '—'),
      ]),
  },
  {
    title: '告警类型',
    key: 'alertType',
    width: 130,
    render: (row) => h('span', { class: 'text-sm' }, row.alertType || '—'),
  },
  {
    title: '级别',
    key: 'alertLevel',
    width: 80,
    render: (row) =>
      h(NTag, { size: 'small', bordered: false, type: levelTagType(row.alertLevel) }, { default: () => levelLabel(row.alertLevel) }),
  },
  {
    title: '状态',
    key: 'readStatus',
    width: 70,
    render: (row) =>
      h('div', { class: 'flex items-center gap-1.5' }, [
        h('span', {
          class: `h-2 w-2 rounded-full ${row.readStatus ? 'bg-on-surface-variant/30' : 'bg-error'}`,
        }),
        h('span', { class: 'text-xs text-on-surface-variant' }, row.readStatus ? '已读' : '未读'),
      ]),
  },
  {
    title: '触发条件',
    key: 'triggerCondition',
    ellipsis: { tooltip: true },
    render: (row) => h('span', { class: 'text-xs text-on-surface-variant' }, row.triggerCondition || '—'),
  },
  {
    title: '操作',
    key: 'actions',
    width: 140,
    render: (row) =>
      h('div', { class: 'flex gap-1' }, [
        h(
          NButton,
          { size: 'tiny', quaternary: true, onClick: () => openDetail(row) },
          { default: () => '详情' },
        ),
        !row.readStatus
          ? h(
              NButton,
              { size: 'tiny', quaternary: true, type: 'primary', onClick: () => handleReadSingle(row.id) },
              { default: () => '已读' },
            )
          : null,
      ]),
  },
]

function rowClassName(row: Alert) {
  return row.readStatus ? '' : 'bg-primary/[0.02]'
}

// Actions
function buildQueryParams() {
  const params: Record<string, unknown> = {
    page: currentPage.value,
    size: pageSize.value,
  }
  if (filterLevel.value) params.alertLevel = filterLevel.value
  if (filterReadStatus.value !== null && filterReadStatus.value !== undefined) params.readStatus = filterReadStatus.value
  if (filterKeyword.value) params.cameraId = filterKeyword.value
  if (filterDateRange.value) {
    params.startTime = new Date(filterDateRange.value[0]).toISOString()
    params.endTime = new Date(filterDateRange.value[1]).toISOString()
  }
  return params
}

function doSearch() {
  currentPage.value = 1
  alertStore.fetchAlerts(buildQueryParams())
}

function resetFilters() {
  filterLevel.value = null
  filterReadStatus.value = null
  filterKeyword.value = ''
  filterDateRange.value = null
  currentPage.value = 1
  alertStore.fetchAlerts(buildQueryParams())
}

function handlePageChange(page: number) {
  currentPage.value = page
  alertStore.fetchAlerts(buildQueryParams())
}

function handlePageSizeChange(size: number) {
  pageSize.value = size
  currentPage.value = 1
  alertStore.fetchAlerts(buildQueryParams())
}

async function handleReadSingle(id: string) {
  await alertStore.markAsRead(id)
  if (selectedAlert.value?.id === id) {
    selectedAlert.value = { ...selectedAlert.value, readStatus: true }
  }
  message.success('已标记为已读')
}

async function handleBatchRead() {
  await alertStore.markBatchAsRead(checkedKeys.value)
  checkedKeys.value = []
  message.success('批量标记已读成功')
}

async function handleMarkAllRead() {
  await alertStore.markAllAsRead()
  message.success('全部标记已读成功')
}

function openDetail(alert: Alert) {
  selectedAlert.value = alert
  drawerVisible.value = true
}

// Level helpers
function levelTagType(level: string): 'error' | 'warning' | 'info' {
  if (level === 'severe') return 'error'
  if (level === 'warning') return 'warning'
  return 'info'
}

function levelLabel(level: string) {
  if (level === 'severe') return '严重'
  if (level === 'warning') return '警告'
  return '提示'
}

function levelDotClass(level: string) {
  if (level === 'severe') return 'bg-error'
  if (level === 'warning') return 'bg-warning'
  return 'bg-info'
}

// JSON parsing helpers
const parsedEvidence = computed(() => {
  if (!selectedAlert.value?.evidence) return null
  try {
    const data = JSON.parse(selectedAlert.value.evidence)
    return {
      imageUrls: (data.imageUrls || data.images || []) as string[],
    }
  } catch {
    return null
  }
})

const parsedRelatedObjects = computed(() => {
  if (!selectedAlert.value?.relatedObjects) return []
  try {
    const data = JSON.parse(selectedAlert.value.relatedObjects)
    if (Array.isArray(data)) return data.map((o: any) => o.label || o.name || String(o))
    return []
  } catch {
    return []
  }
})

function formatJson(str: string) {
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch {
    return str
  }
}

// Init
onMounted(() => {
  alertStore.fetchAlerts(buildQueryParams())
  alertStore.fetchUnreadCount()
})
</script>
