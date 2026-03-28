<template>
  <div class="flex h-full gap-4">
    <!-- Left: Group Tree -->
    <div class="w-60 shrink-0 rounded-xl bg-bg-card p-4">
      <div class="mb-3 flex items-center justify-between">
        <h3 class="text-sm font-semibold">区域分组</h3>
        <button class="text-on-surface-variant hover:text-primary transition-colors">
          <Icon icon="mdi:plus-box" class="text-lg" />
        </button>
      </div>
      <div class="space-y-1">
        <div
          v-for="group in groups"
          :key="group.id"
          class="cursor-pointer"
        >
          <div
            class="flex items-center gap-2 rounded-lg px-3 py-2 text-sm transition-colors"
            :class="selectedGroupId === group.id ? 'bg-primary/10 text-primary' : 'text-on-surface-variant hover:bg-bg-active'"
            @click="selectGroup(group.id)"
          >
            <Icon icon="mdi:chevron-down" class="text-sm" />
            <Icon icon="mdi:factory" class="text-base" />
            <span class="flex-1">{{ group.name }}</span>
            <span class="text-xs opacity-60">{{ group.cameraCount }}</span>
          </div>
          <div v-if="group.children" class="ml-5 space-y-1 mt-1">
            <div
              v-for="child in group.children"
              :key="child.id"
              class="flex items-center gap-2 rounded-lg px-3 py-1.5 text-sm transition-colors cursor-pointer"
              :class="selectedGroupId === child.id ? 'bg-primary/10 text-primary' : 'text-on-surface-variant hover:bg-bg-active'"
              @click="selectGroup(child.id)"
            >
              <span class="flex-1">{{ child.name }}</span>
              <span class="text-xs opacity-60">{{ child.cameraCount }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Right: Camera List -->
    <div class="flex-1 rounded-xl bg-bg-card p-6">
      <div class="mb-4 flex items-center justify-between">
        <div class="flex items-center gap-3">
          <n-input v-model:value="searchQuery" placeholder="搜索摄像头..." size="small" class="w-60">
            <template #prefix>
              <Icon icon="mdi:magnify" class="text-on-surface-variant" />
            </template>
          </n-input>
        </div>
        <n-button type="primary" size="small" @click="showDrawer = true">
          <template #icon><Icon icon="mdi:plus" /></template>
          新增摄像头
        </n-button>
      </div>

      <n-data-table
        :columns="columns"
        :data="cameraStore.cameras"
        :bordered="false"
        :loading="cameraStore.loading"
        size="small"
        :row-key="(row: Camera) => row.id"
      />

      <div class="mt-4 flex justify-end">
        <n-pagination
          :page-count="Math.ceil(cameraStore.total / 10)"
          size="small"
        />
      </div>
    </div>

    <!-- Add Camera Drawer -->
    <n-drawer v-model:show="showDrawer" :width="480" placement="right">
      <n-drawer-content title="新增摄像头" closable>
        <n-tabs type="line" animated>
          <n-tab-pane name="basic" tab="基本信息">
            <n-form class="space-y-4 mt-4">
              <n-form-item label="摄像头名称">
                <n-input placeholder="请输入名称" />
              </n-form-item>
              <n-form-item label="流媒体地址 (RTSP/RTMP)">
                <n-input placeholder="rtsp://..." />
              </n-form-item>
              <n-form-item label="所属业务线">
                <n-select :options="businessOptions" placeholder="选择业务线" />
              </n-form-item>
              <n-form-item label="安装地点">
                <n-input placeholder="请输入安装地点" />
              </n-form-item>
              <n-form-item label="预览流测试">
                <div class="w-full rounded-lg bg-bg-void p-8 text-center">
                  <Icon icon="mdi:video-off" class="text-3xl text-on-surface-variant" />
                  <p class="mt-2 text-xs text-on-surface-variant">点击连接查看预览</p>
                </div>
              </n-form-item>
            </n-form>
          </n-tab-pane>
          <n-tab-pane name="capture" tab="抓图配置">
            <div class="mt-4 text-sm text-on-surface-variant">
              抓图策略、Cron表达式、分辨率等配置
            </div>
          </n-tab-pane>
          <n-tab-pane name="ai" tab="AI分析配置">
            <div class="mt-4 text-sm text-on-surface-variant">
              AI启用开关、绑定业务线、触发抓图配置
            </div>
          </n-tab-pane>
        </n-tabs>
        <template #footer>
          <div class="flex gap-3">
            <n-button @click="showDrawer = false">取消</n-button>
            <n-button type="primary" @click="showDrawer = false">保存摄像头</n-button>
          </div>
        </template>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, h } from 'vue'
import {
  NInput, NButton, NDataTable, NPagination, NDrawer, NDrawerContent,
  NTabs, NTabPane, NForm, NFormItem, NSelect, NTag, NSwitch,
} from 'naive-ui'
import type { DataTableColumns } from 'naive-ui'
import { Icon } from '@iconify/vue'
import { useCameraStore } from '@/stores/camera'
import type { Camera } from '@/types'

const cameraStore = useCameraStore()
const searchQuery = ref('')
const showDrawer = ref(false)
const selectedGroupId = ref<string | null>(null)

const groups = computed(() => cameraStore.groups)

onMounted(() => {
  cameraStore.fetchCameras()
  cameraStore.fetchGroups()
})

function selectGroup(id: string) {
  selectedGroupId.value = id
}

const businessOptions = [
  { label: '安防监控', value: 'Security' },
  { label: '生产管理', value: 'Production' },
  { label: '设施管理', value: 'Facility' },
  { label: '安全合规', value: 'Safety' },
]

const columns: DataTableColumns<Camera> = [
  { title: 'ID', key: 'id', width: 140 },
  { title: '名称', key: 'name', width: 200 },
  {
    title: '业务线', key: 'businessLine', width: 120,
    render: (row) => h(NTag, { size: 'small', bordered: false, type: 'info' }, { default: () => row.businessLine }),
  },
  { title: '抓图频率', key: 'captureFrequency', width: 120 },
  {
    title: 'AI', key: 'aiEnabled', width: 60,
    render: (row) => h(NSwitch, { value: row.aiEnabled, size: 'small' }),
  },
  { title: '最近抓图', key: 'lastCaptureTime', width: 160 },
  {
    title: '状态', key: 'status', width: 80,
    render: (row) => h(NTag, {
      size: 'small', bordered: false,
      type: row.status === 'online' ? 'success' : row.status === 'offline' ? 'default' : 'error',
    }, { default: () => row.status === 'online' ? '在线' : row.status === 'offline' ? '离线' : '异常' }),
  },
  {
    title: '操作', key: 'actions', width: 120,
    render: () => h('div', { class: 'flex gap-2' }, [
      h(Icon, { icon: 'mdi:history', class: 'cursor-pointer text-on-surface-variant hover:text-primary text-base' }),
      h(Icon, { icon: 'mdi:pencil', class: 'cursor-pointer text-on-surface-variant hover:text-primary text-base' }),
      h(Icon, { icon: 'mdi:delete', class: 'cursor-pointer text-on-surface-variant hover:text-error text-base' }),
    ]),
  },
]
</script>
