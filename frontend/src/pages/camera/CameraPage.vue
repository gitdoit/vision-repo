<template>
  <div class="flex h-full gap-4">
    <!-- Left: Group Tree -->
    <div class="w-60 shrink-0 rounded-xl bg-bg-card p-4">
      <div class="mb-3 flex items-center justify-between">
        <h3 class="text-sm font-semibold">区域分组</h3>
        <n-popover trigger="click" placement="bottom" :show="showGroupPopover">
          <template #trigger>
            <button class="text-on-surface-variant hover:text-primary transition-colors" @click="showGroupPopover = true">
              <Icon icon="mdi:plus-box" class="text-lg" />
            </button>
          </template>
          <div class="w-52 space-y-2">
            <p class="text-xs font-semibold">新增分组</p>
            <n-input v-model:value="newGroupName" size="small" placeholder="分组名称" @keydown.enter="handleCreateGroup" />
            <div class="flex justify-end gap-1">
              <n-button size="tiny" @click="showGroupPopover = false">取消</n-button>
              <n-button size="tiny" type="primary" @click="handleCreateGroup">确定</n-button>
            </div>
          </div>
        </n-popover>
      </div>
      <div class="space-y-1">
        <!-- 全部 -->
        <div
          class="flex items-center gap-2 rounded-lg px-3 py-2 text-sm transition-colors cursor-pointer"
          :class="selectedGroupId === null ? 'bg-primary/10 text-primary' : 'text-on-surface-variant hover:bg-bg-active'"
          @click="selectGroup(null)"
        >
          <Icon icon="mdi:view-grid" class="text-base" />
          <span class="flex-1">全部摄像头</span>
        </div>
        <div
          v-for="group in groups"
          :key="group.id"
          class="cursor-pointer"
        >
          <div
            class="group/item flex items-center gap-2 rounded-lg px-3 py-2 text-sm transition-colors"
            :class="selectedGroupId === group.id ? 'bg-primary/10 text-primary' : 'text-on-surface-variant hover:bg-bg-active'"
            @click="selectGroup(group.id)"
          >
            <Icon icon="mdi:chevron-down" class="text-sm" />
            <Icon icon="mdi:factory" class="text-base" />
            <span class="flex-1 truncate">{{ group.name }}</span>
            <span class="text-xs opacity-60">{{ group.cameraCount }}</span>
            <n-dropdown trigger="click" :options="groupMenuOptions" size="small" @select="(key: string) => handleGroupMenuSelect(key, group.id, group.name)" @click.stop>
              <button class="invisible group-hover/item:visible text-on-surface-variant hover:text-primary ml-1" @click.stop>
                <Icon icon="mdi:dots-vertical" class="text-sm" />
              </button>
            </n-dropdown>
          </div>
          <div v-if="group.children" class="ml-5 space-y-1 mt-1">
            <div
              v-for="child in group.children"
              :key="child.id"
              class="group/child flex items-center gap-2 rounded-lg px-3 py-1.5 text-sm transition-colors cursor-pointer"
              :class="selectedGroupId === child.id ? 'bg-primary/10 text-primary' : 'text-on-surface-variant hover:bg-bg-active'"
              @click="selectGroup(child.id)"
            >
              <span class="flex-1 truncate">{{ child.name }}</span>
              <span class="text-xs opacity-60">{{ child.cameraCount }}</span>
              <n-dropdown trigger="click" :options="groupMenuOptions" size="small" @select="(key: string) => handleGroupMenuSelect(key, child.id, child.name)" @click.stop>
                <button class="invisible group-hover/child:visible text-on-surface-variant hover:text-primary ml-1" @click.stop>
                  <Icon icon="mdi:dots-vertical" class="text-sm" />
                </button>
              </n-dropdown>
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
    <div class="flex flex-1 flex-col overflow-hidden rounded-xl bg-bg-card p-6">
      <div class="mb-4 flex shrink-0 items-center justify-between">
        <div class="flex items-center gap-3">
          <n-input v-model:value="searchQuery" placeholder="搜索名称/标签..." size="small" class="w-60" clearable @keydown.enter="handleSearch" @clear="handleSearch">
            <template #prefix>
              <Icon icon="mdi:magnify" class="text-on-surface-variant" />
            </template>
          </n-input>
          <n-select
            v-model:value="statusFilter"
            size="small"
            class="w-28"
            :options="statusOptions"
            placeholder="状态"
            clearable
            @update:value="handleSearch"
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
        flex-height
        class="flex-1"
      />

      <div class="mt-4 flex shrink-0 justify-end">
        <n-pagination
          v-model:page="cameraStore.page"
          v-model:page-size="cameraStore.pageSize"
          :item-count="cameraStore.total"
          :page-sizes="[10, 20, 50]"
          show-size-picker
          size="small"
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
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
              <n-input v-model:value="platformForm.name" placeholder="如: 合肥视频平台" />
            </n-form-item>
            <n-form-item label="API 地址" size="small">
              <n-input v-model:value="platformForm.apiBase" placeholder="http://10.100.121.12:18080" />
            </n-form-item>
            <n-form-item label="用户名" size="small">
              <n-input v-model:value="platformForm.username" placeholder="请输入用户名" />
            </n-form-item>
            <n-form-item label="密码" size="small">
              <n-input v-model:value="platformForm.password" placeholder="请输入密码" type="password" show-password-on="click" />
            </n-form-item>
            <n-form-item label="自动同步" size="small">
              <div class="flex items-center gap-3">
                <n-switch v-model:value="platformForm.autoSync" />
                <n-input-number v-model:value="platformForm.syncIntervalMin" placeholder="间隔(分钟)" :min="5" :max="1440" size="small" class="w-36" :disabled="!platformForm.autoSync" />
              </div>
            </n-form-item>
          </n-form>
          <div class="flex gap-2 mt-3">
            <n-button size="small" @click="showAddPlatform = false">取消</n-button>
            <n-button size="small" type="primary" :loading="savingPlatform" @click="handleSavePlatform">保存</n-button>
          </div>
        </div>
      </n-drawer-content>
    </n-drawer>

    <!-- Import Modal -->
    <n-modal v-model:show="showImportModal" preset="card" title="批量导入摄像头" :style="{ width: '520px' }">
      <div class="space-y-4">
        <n-tabs type="line" animated size="small" default-value="platform">
          <n-tab-pane name="platform" tab="从视频平台导入">
            <n-form class="mt-3 space-y-1" label-placement="left" label-width="90">
              <n-form-item label="平台地址">
                <n-input v-model:value="importForm.apiBase" placeholder="http://10.100.121.12:18080" />
              </n-form-item>
              <n-form-item label="用户名">
                <n-input v-model:value="importForm.username" placeholder="admin" />
              </n-form-item>
              <n-form-item label="密码">
                <n-input v-model:value="importForm.password" placeholder="请输入密码（MD5）" type="password" show-password-on="click" />
              </n-form-item>
            </n-form>
            <div class="mt-2 rounded bg-bg-void p-3 text-xs text-on-surface-variant leading-relaxed">
              <p>填写视频平台地址和账号密码，将自动登录并全量同步设备信息。</p>
              <p class="mt-1">同步规则：以设备ID去重，已存在的设备将更新信息，新设备自动创建。</p>
            </div>
          </n-tab-pane>
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
          <n-button size="small" type="primary" :loading="importing" @click="handleBatchImport">导入</n-button>
        </div>
      </template>
    </n-modal>

    <!-- Video Preview Modal -->
    <n-modal v-model:show="showPreviewModal" preset="card" :title="previewCamera?.name || '实时监控'" :style="{ width: '720px' }">
      <FlvPlayer v-if="showPreviewModal && previewCamera?.streamUrl" :url="previewCamera.streamUrl" />
      <div v-else class="flex items-center justify-center py-16 text-on-surface-variant">
        <div class="text-center">
          <Icon icon="mdi:video-off" class="text-4xl mb-2" />
          <p class="text-sm">该摄像头暂无视频流地址</p>
        </div>
      </div>
    </n-modal>

    <!-- Model Test Modal -->
    <ModelTestModal v-model:show="showModelTestModal" :camera="testCamera" />

    <!-- Group Assignment Modal -->
    <n-modal v-model:show="showGroupModal" preset="card" title="设置所属分组" :style="{ width: '400px' }">
      <div class="space-y-3">
        <p class="text-sm text-on-surface-variant">为「{{ groupEditCamera?.name }}」选择所属分组（可多选）：</p>
        <n-checkbox-group v-model:value="selectedGroupIds">
          <div class="space-y-2 max-h-60 overflow-y-auto">
            <template v-for="group in flatGroups" :key="group.id">
              <n-checkbox :value="group.id" :label="group.label" />
            </template>
          </div>
        </n-checkbox-group>
      </div>
      <template #footer>
        <div class="flex justify-end gap-2">
          <n-button size="small" @click="showGroupModal = false">取消</n-button>
          <n-button size="small" type="primary" :loading="savingGroups" @click="handleSaveGroups">保存</n-button>
        </div>
      </template>
    </n-modal>

    <!-- Edit Group Modal -->
    <n-modal v-model:show="showEditGroupModal" preset="card" title="编辑分组" :style="{ width: '360px' }">
      <n-form-item label="分组名称">
        <n-input v-model:value="editGroupName" placeholder="请输入分组名称" @keydown.enter="handleSaveGroupEdit" />
      </n-form-item>
      <template #footer>
        <div class="flex justify-end gap-2">
          <n-button size="small" @click="showEditGroupModal = false">取消</n-button>
          <n-button size="small" type="primary" :loading="savingGroupEdit" @click="handleSaveGroupEdit">保存</n-button>
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
  NModal, NInputNumber, NPopover, NCheckbox, NCheckboxGroup, NDropdown, useMessage, useDialog,
} from 'naive-ui'
import type { DataTableColumns } from 'naive-ui'
import { Icon } from '@iconify/vue'
import { useCameraStore } from '@/stores/camera'
import type { Camera } from '@/types'
import * as cameraApi from '@/api/modules/camera'
import FlvPlayer from '@/components/FlvPlayer.vue'
import ModelTestModal from './ModelTestModal.vue'

const cameraStore = useCameraStore()
const message = useMessage()
const dialog = useDialog()
const searchQuery = ref('')
const showDrawer = ref(false)
const showPlatformDrawer = ref(false)
const showAddPlatform = ref(false)
const showImportModal = ref(false)
const selectedGroupId = ref<string | null>(null)
const statusFilter = ref<string | null>(null)

// 新增分组
const showGroupPopover = ref(false)
const newGroupName = ref('')

// 编辑分组
const showEditGroupModal = ref(false)
const editGroupId = ref<string | null>(null)
const editGroupName = ref('')
const savingGroupEdit = ref(false)

// 视频预览
const showPreviewModal = ref(false)
const previewCamera = ref<Camera | null>(null)

// 模型测试
const showModelTestModal = ref(false)
const testCamera = ref<Camera | null>(null)

// 分组编辑弹窗
const showGroupModal = ref(false)
const groupEditCamera = ref<Camera | null>(null)
const selectedGroupIds = ref<string[]>([])
const savingGroups = ref(false)

// 添加平台表单
const platformForm = ref({
  name: '',
  apiBase: '',
  username: '',
  password: '',
  autoSync: false,
  syncIntervalMin: 60,
})
const savingPlatform = ref(false)

// 批量导入表单
const importForm = ref({
  apiBase: '',
  username: '',
  password: '',
})
const importing = ref(false)

const groups = computed(() => cameraStore.groups)

/** 将树状分组扁平化（含层级缩进标签）供 checkbox 使用 */
const flatGroups = computed(() => {
  const result: { id: string; label: string }[] = []
  function walk(list: typeof cameraStore.groups, depth: number) {
    for (const g of list) {
      result.push({ id: g.id, label: '　'.repeat(depth) + g.name })
      if (g.children?.length) walk(g.children, depth + 1)
    }
  }
  walk(cameraStore.groups, 0)
  return result
})

onMounted(() => {
  cameraStore.fetchCameras()
  cameraStore.fetchGroups()
  cameraStore.fetchPlatforms()
})

function selectGroup(id: string | null) {
  selectedGroupId.value = id
  cameraStore.page = 1
  cameraStore.fetchCameras(getSearchParams())
}

function getSearchParams(): Record<string, unknown> {
  const params: Record<string, unknown> = {}
  if (searchQuery.value) params.keyword = searchQuery.value
  if (statusFilter.value) params.status = statusFilter.value
  if (selectedGroupId.value) params.groupId = selectedGroupId.value
  return params
}

function handleSearch() {
  cameraStore.page = 1
  cameraStore.fetchCameras(getSearchParams())
}

function handlePageChange() {
  cameraStore.fetchCameras(getSearchParams())
}

function handlePageSizeChange() {
  cameraStore.page = 1
  cameraStore.fetchCameras(getSearchParams())
}

async function handleCreateGroup() {
  if (!newGroupName.value.trim()) {
    message.warning('请输入分组名称')
    return
  }
  try {
    await cameraStore.createGroup({ name: newGroupName.value.trim() })
    message.success('分组创建成功')
    newGroupName.value = ''
    showGroupPopover.value = false
  } catch (e: unknown) {
    message.error('创建失败: ' + (e instanceof Error ? e.message : String(e)))
  }
}

function handleEditGroup(groupId: string, groupName: string) {
  editGroupId.value = groupId
  editGroupName.value = groupName
  showEditGroupModal.value = true
}

async function handleSaveGroupEdit() {
  if (!editGroupId.value || !editGroupName.value.trim()) {
    message.warning('请输入分组名称')
    return
  }
  savingGroupEdit.value = true
  try {
    await cameraStore.updateGroup(editGroupId.value, { name: editGroupName.value.trim() })
    message.success('分组更新成功')
    showEditGroupModal.value = false
  } catch (e: unknown) {
    message.error('更新失败: ' + (e instanceof Error ? e.message : String(e)))
  } finally {
    savingGroupEdit.value = false
  }
}

function handleDeleteGroup(groupId: string, groupName: string) {
  dialog.warning({
    title: '确认删除',
    content: `确定要删除分组「${groupName}」吗？删除后该分组下的摄像头关联将被移除。`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await cameraStore.deleteGroup(groupId)
        if (selectedGroupId.value === groupId) {
          selectGroup(null)
        }
        message.success('分组删除成功')
        cameraStore.fetchCameras(getSearchParams())
      } catch (e: unknown) {
        message.error('删除失败: ' + (e instanceof Error ? e.message : String(e)))
      }
    },
  })
}

const groupMenuOptions = [
  { label: '重命名', key: 'edit' },
  { label: '删除', key: 'delete' },
]

function handleGroupMenuSelect(key: string, groupId: string, groupName: string) {
  if (key === 'edit') handleEditGroup(groupId, groupName)
  else if (key === 'delete') handleDeleteGroup(groupId, groupName)
}

function handlePreview(camera: Camera) {
  previewCamera.value = camera
  showPreviewModal.value = true
}

function handleModelTest(camera: Camera) {
  testCamera.value = camera
  showModelTestModal.value = true
}

function handleEditGroups(camera: Camera) {
  groupEditCamera.value = camera
  selectedGroupIds.value = camera.groups.map(g => g.id)
  showGroupModal.value = true
}

async function handleSaveGroups() {
  if (!groupEditCamera.value) return
  savingGroups.value = true
  try {
    await cameraStore.updateCameraGroups(groupEditCamera.value.id, selectedGroupIds.value)
    message.success('分组设置成功')
    showGroupModal.value = false
  } catch (e: unknown) {
    message.error('设置失败: ' + (e instanceof Error ? e.message : String(e)))
  } finally {
    savingGroups.value = false
  }
}

async function handleTestPlatform(id: string) {
  await cameraApi.testVideoPlatform(id)
}

async function handleSyncPlatform(id: string) {
  await cameraStore.syncPlatform(id)
}

async function handleSavePlatform() {
  const f = platformForm.value
  if (!f.name || !f.apiBase || !f.username || !f.password) {
    message.warning('请填写完整的平台信息')
    return
  }
  savingPlatform.value = true
  try {
    await cameraApi.createVideoPlatform({
      name: f.name,
      apiBase: f.apiBase,
      authType: 'basic',
      credential: f.username + ':' + f.password,
      autoSync: f.autoSync,
      syncIntervalMin: f.syncIntervalMin,
    })
    message.success('平台添加成功')
    showAddPlatform.value = false
    platformForm.value = { name: '', apiBase: '', username: '', password: '', autoSync: false, syncIntervalMin: 60 }
    await cameraStore.fetchPlatforms()
  } catch (e: unknown) {
    message.error('添加失败: ' + (e instanceof Error ? e.message : String(e)))
  } finally {
    savingPlatform.value = false
  }
}

async function handleBatchImport() {
  if (!importForm.value.apiBase || !importForm.value.username || !importForm.value.password) {
    message.warning('请填写完整的平台信息')
    return
  }
  importing.value = true
  try {
    const result = await cameraStore.batchImportFromPlatform({
      apiBase: importForm.value.apiBase,
      username: importForm.value.username,
      password: importForm.value.password,
    })
    message.success(`导入完成：共 ${result.total} 台，新增 ${result.added}，更新 ${result.updated}，失败 ${result.failed}`)
    showImportModal.value = false
    importForm.value = { apiBase: '', username: '', password: '' }
  } catch (e: unknown) {
    message.error('导入失败: ' + (e instanceof Error ? e.message : String(e)))
  } finally {
    importing.value = false
  }
}

const businessOptions = [
  { label: '安防监控', value: 'Security' },
  { label: '生产管理', value: 'Production' },
  { label: '设施管理', value: 'Facility' },
  { label: '安全合规', value: 'Safety' },
]

const statusOptions = [
  { label: '在线', value: 'online' },
  { label: '离线', value: 'offline' },
]

const columns: DataTableColumns<Camera> = [
  { title: '名称', key: 'name', width: 180 },
  { title: '标签', key: 'label', width: 120, ellipsis: { tooltip: true } },
  {
    title: '所属分组', key: 'groups', width: 160,
    render: (row) => {
      if (!row.groups || row.groups.length === 0) {
        return h('span', { class: 'text-xs text-on-surface-variant' }, '未分组')
      }
      return h('div', { class: 'flex flex-wrap gap-1' },
        row.groups.map(g => h(NTag, { size: 'small', bordered: false, type: 'info' }, { default: () => g.name }))
      )
    },
  },
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
    title: '操作', key: 'actions', width: 180,
    render: (row) => h('div', { class: 'flex gap-2' }, [
      h(Icon, { icon: 'mdi:play-circle', class: 'cursor-pointer text-on-surface-variant hover:text-primary text-base', title: '实时预览', onClick: () => handlePreview(row) }),
      h(Icon, { icon: 'mdi:brain', class: 'cursor-pointer text-on-surface-variant hover:text-primary text-base', title: '模型测试', onClick: () => handleModelTest(row) }),
      h(Icon, { icon: 'mdi:folder-plus', class: 'cursor-pointer text-on-surface-variant hover:text-primary text-base', title: '设置分组', onClick: () => handleEditGroups(row) }),
      h(Icon, { icon: 'mdi:pencil', class: 'cursor-pointer text-on-surface-variant hover:text-primary text-base', title: '编辑' }),
      h(Icon, { icon: 'mdi:delete', class: 'cursor-pointer text-on-surface-variant hover:text-error text-base', title: '删除' }),
    ]),
  },
]
</script>
