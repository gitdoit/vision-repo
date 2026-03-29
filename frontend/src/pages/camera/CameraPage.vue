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

      <!-- 视频平台快捷入口 -->
      <div class="mt-6 border-t border-border pt-4">
        <div class="mb-2 flex items-center justify-between">
          <h3 class="text-xs font-semibold text-on-surface-variant">视频平台</h3>
          <button class="text-on-surface-variant hover:text-primary transition-colors" @click="showPlatformDrawer = true">
            <Icon icon="mdi:cog" class="text-base" />
          </button>
        </div>
        <div v-for="p in cameraStore.platforms" :key="p.id" class="flex items-center gap-2 rounded-lg px-3 py-2 text-xs">
          <span class="h-2 w-2 rounded-full" :class="p.status === 'connected' ? 'bg-green-500' : p.status === 'syncing' ? 'bg-yellow-500' : 'bg-red-500'" />
          <span class="flex-1 truncate">{{ p.name }}</span>
          <span class="text-on-surface-variant">{{ p.camerasCount }}台</span>
        </div>
        <div v-if="cameraStore.platforms.length === 0" class="px-3 py-2 text-xs text-on-surface-variant">
          未配置视频平台
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
          <n-select
            v-model:value="sourceFilter"
            size="small"
            class="w-32"
            :options="sourceOptions"
            placeholder="来源"
            clearable
          />
        </div>
        <div class="flex items-center gap-2">
          <n-button size="small" @click="showImportModal = true">
            <template #icon><Icon icon="mdi:file-import" /></template>
            批量导入
          </n-button>
          <n-button type="primary" size="small" @click="showDrawer = true">
            <template #icon><Icon icon="mdi:plus" /></template>
            新增摄像头
          </n-button>
        </div>
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

    <!-- Video Platform Management Drawer -->
    <n-drawer v-model:show="showPlatformDrawer" :width="560" placement="right">
      <n-drawer-content title="视频平台管理" closable>
        <div class="space-y-4">
          <div class="flex justify-between items-center">
            <p class="text-sm text-on-surface-variant">配置公司视频管理平台地址，自动同步摄像头列表</p>
            <n-button size="small" type="primary" @click="showAddPlatform = true">
              <template #icon><Icon icon="mdi:plus" /></template>
              添加平台
            </n-button>
          </div>

          <div v-for="platform in cameraStore.platforms" :key="platform.id" class="rounded-lg border border-border p-4 space-y-3">
            <div class="flex items-center justify-between">
              <div class="flex items-center gap-2">
                <Icon icon="mdi:server-network" class="text-lg text-primary" />
                <span class="font-medium text-sm">{{ platform.name }}</span>
                <n-tag :type="platform.status === 'connected' ? 'success' : platform.status === 'syncing' ? 'warning' : 'error'" size="small" :bordered="false">
                  {{ platform.status === 'connected' ? '已连接' : platform.status === 'syncing' ? '同步中' : '未连接' }}
                </n-tag>
              </div>
              <div class="flex gap-1">
                <n-button text size="small"><Icon icon="mdi:pencil" /></n-button>
                <n-button text size="small" type="error"><Icon icon="mdi:delete" /></n-button>
              </div>
            </div>
            <div class="grid grid-cols-2 gap-2 text-xs text-on-surface-variant">
              <div>地址: {{ platform.apiBase }}</div>
              <div>认证: {{ platform.authType === 'token' ? 'Token' : platform.authType === 'basic' ? '账号密码' : '无' }}</div>
              <div>自动同步: {{ platform.autoSync ? `每 ${platform.syncIntervalMin} 分钟` : '已关闭' }}</div>
              <div>同步设备: {{ platform.camerasCount }} 台</div>
            </div>
            <div v-if="platform.lastSyncResult" class="text-xs text-on-surface-variant bg-bg-void rounded p-2">
              上次同步: {{ platform.lastSyncTime }} — 
              共{{ platform.lastSyncResult.total }}台, 
              新增{{ platform.lastSyncResult.added }}, 
              更新{{ platform.lastSyncResult.updated }}, 
              失败{{ platform.lastSyncResult.failed }}
            </div>
            <div class="flex gap-2">
              <n-button size="tiny" @click="handleTestPlatform(platform.id)">
                <template #icon><Icon icon="mdi:connection" /></template>
                测试连接
              </n-button>
              <n-button size="tiny" type="primary" @click="handleSyncPlatform(platform.id)">
                <template #icon><Icon icon="mdi:sync" /></template>
                立即同步
              </n-button>
            </div>
          </div>

          <div v-if="cameraStore.platforms.length === 0" class="py-12 text-center text-on-surface-variant">
            <Icon icon="mdi:server-off" class="text-4xl mb-2" />
            <p class="text-sm">暂未配置视频平台</p>
            <p class="text-xs mt-1">点击上方「添加平台」接入公司视频管理系统</p>
          </div>
        </div>

        <!-- Add Platform Form (inline) -->
        <div v-if="showAddPlatform" class="mt-6 border-t border-border pt-4">
          <h4 class="text-sm font-semibold mb-3">添加视频平台</h4>
          <n-form class="space-y-3">
            <n-form-item label="平台名称" size="small">
              <n-input placeholder="如: 公司视频管理平台" />
            </n-form-item>
            <n-form-item label="API 地址" size="small">
              <n-input placeholder="http://192.168.x.x:port/api" />
            </n-form-item>
            <n-form-item label="认证方式" size="small">
              <n-select :options="authOptions" placeholder="选择认证方式" />
            </n-form-item>
            <n-form-item label="凭证" size="small">
              <n-input placeholder="Token 或 用户名:密码" type="password" show-password-on="click" />
            </n-form-item>
            <n-form-item label="自动同步" size="small">
              <div class="flex items-center gap-3">
                <n-switch />
                <n-input-number placeholder="间隔(分钟)" :min="5" :max="1440" size="small" class="w-36" />
              </div>
            </n-form-item>
          </n-form>
          <div class="flex gap-2 mt-3">
            <n-button size="small" @click="showAddPlatform = false">取消</n-button>
            <n-button size="small" type="primary">保存</n-button>
          </div>
        </div>
      </n-drawer-content>
    </n-drawer>

    <!-- Import Modal -->
    <n-modal v-model:show="showImportModal" preset="card" title="批量导入摄像头" :style="{ width: '520px' }">
      <div class="space-y-4">
        <n-tabs type="line" animated size="small">
          <n-tab-pane name="json" tab="JSON 导入">
            <n-input
              type="textarea"
              :rows="8"
              placeholder='[{"name":"摄像头1","streamUrl":"rtsp://...","businessLine":"Security","location":"A区"}]'
              class="mt-3"
            />
          </n-tab-pane>
          <n-tab-pane name="csv" tab="CSV 导入">
            <div class="mt-3 rounded-lg border-2 border-dashed border-border p-8 text-center">
              <Icon icon="mdi:file-upload" class="text-3xl text-on-surface-variant mb-2" />
              <p class="text-sm text-on-surface-variant">拖拽 CSV 文件到此处，或点击选择</p>
              <p class="text-xs text-on-surface-variant mt-1">格式: 名称, 流地址, 业务线, 位置</p>
            </div>
          </n-tab-pane>
        </n-tabs>
      </div>
      <template #footer>
        <div class="flex justify-end gap-2">
          <n-button size="small" @click="showImportModal = false">取消</n-button>
          <n-button size="small" type="primary">导入</n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, h } from 'vue'
import {
  NInput, NButton, NDataTable, NPagination, NDrawer, NDrawerContent,
  NTabs, NTabPane, NForm, NFormItem, NSelect, NTag, NSwitch,
  NModal, NInputNumber,
} from 'naive-ui'
import type { DataTableColumns } from 'naive-ui'
import { Icon } from '@iconify/vue'
import { useCameraStore } from '@/stores/camera'
import type { Camera } from '@/types'
import * as cameraApi from '@/api/modules/camera'

const cameraStore = useCameraStore()
const searchQuery = ref('')
const showDrawer = ref(false)
const showPlatformDrawer = ref(false)
const showAddPlatform = ref(false)
const showImportModal = ref(false)
const selectedGroupId = ref<string | null>(null)
const sourceFilter = ref<string | null>(null)

const groups = computed(() => cameraStore.groups)

onMounted(() => {
  cameraStore.fetchCameras()
  cameraStore.fetchGroups()
  cameraStore.fetchPlatforms()
})

function selectGroup(id: string) {
  selectedGroupId.value = id
}

async function handleTestPlatform(id: string) {
  await cameraApi.testVideoPlatform(id)
}

async function handleSyncPlatform(id: string) {
  await cameraStore.syncPlatform(id)
}

const businessOptions = [
  { label: '安防监控', value: 'Security' },
  { label: '生产管理', value: 'Production' },
  { label: '设施管理', value: 'Facility' },
  { label: '安全合规', value: 'Safety' },
]

const sourceOptions = [
  { label: '全部来源', value: null },
  { label: '手动添加', value: 'manual' },
  { label: '平台同步', value: 'synced' },
]

const authOptions = [
  { label: 'Token 认证', value: 'token' },
  { label: '账号密码', value: 'basic' },
  { label: '无认证', value: 'none' },
]

const columns: DataTableColumns<Camera> = [
  { title: 'ID', key: 'id', width: 140 },
  { title: '名称', key: 'name', width: 180 },
  {
    title: '来源', key: 'source', width: 90,
    render: (row) => h(NTag, {
      size: 'small', bordered: false,
      type: row.source === 'synced' ? 'info' : 'default',
    }, { default: () => row.source === 'synced' ? '平台同步' : '手动' }),
  },
  {
    title: '业务线', key: 'businessLine', width: 100,
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
