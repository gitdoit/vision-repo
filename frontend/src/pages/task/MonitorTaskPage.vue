<template>
  <div class="space-y-4">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <p class="text-sm text-on-surface-variant">创建和管理监测任务，将摄像头分组与AI模型关联，配置告警条件与回调推送。</p>
      <div class="flex gap-2">
        <n-button size="small" @click="taskStore.fetchTasks()">
          <template #icon><Icon icon="mdi:refresh" /></template>
          刷新
        </n-button>
        <n-button size="small" type="primary" @click="openCreateModal">
          <template #icon><Icon icon="mdi:plus" /></template>
          新建任务
        </n-button>
      </div>
    </div>

    <!-- Filters -->
    <div class="flex gap-2">
      <n-select
        v-model:value="filterStatus"
        placeholder="全部状态"
        clearable
        size="small"
        :options="statusOptions"
        class="w-32"
      />
      <n-input
        v-model:value="filterKeyword"
        placeholder="搜索任务名称"
        clearable
        size="small"
        class="w-48"
      >
        <template #prefix><Icon icon="mdi:magnify" class="text-on-surface-variant" /></template>
      </n-input>
    </div>

    <!-- Task Cards -->
    <n-spin :show="taskStore.loading">
      <div v-if="filteredTasks.length === 0 && !taskStore.loading" class="flex flex-col items-center py-20 text-on-surface-variant">
        <Icon icon="mdi:clipboard-text-off-outline" class="text-5xl opacity-20" />
        <p class="mt-3 text-sm">暂无监测任务</p>
      </div>

      <div class="grid grid-cols-1 gap-4 xl:grid-cols-2">
        <div
          v-for="task in filteredTasks"
          :key="task.id"
          class="rounded-xl bg-bg-card p-5 transition-shadow hover:shadow-md"
        >
          <!-- Row 1: Header — Name + Status + Created Time -->
          <div class="flex items-center justify-between mb-2">
            <div class="flex items-center gap-2 min-w-0">
              <span
                class="inline-block h-2.5 w-2.5 shrink-0 rounded-full"
                :class="statusDotClass(task.status)"
              />
              <h4 class="text-sm font-semibold truncate">{{ task.name }}</h4>
              <n-tag size="tiny" :bordered="false" :type="statusTagType(task.status)">
                {{ statusLabel(task.status) }}
              </n-tag>
            </div>
            <span class="text-[11px] text-on-surface-variant/60 shrink-0 ml-2">{{ formatTime(task.createdAt) }}</span>
          </div>

          <!-- Row 2: Description -->
          <p v-if="task.description" class="text-xs text-on-surface-variant mb-3 line-clamp-1">
            {{ task.description }}
          </p>

          <!-- Row 3: Model Section -->
          <div class="mb-3 rounded-lg bg-bg-void/50 px-3 py-2">
            <div class="flex items-center gap-2 mb-1.5">
              <Icon icon="mdi:brain" class="text-primary text-sm shrink-0" />
              <span class="text-xs font-medium text-on-surface">{{ task.modelName || '-' }}</span>
              <n-tag v-if="task.modelTaskType" size="tiny" :bordered="false" class="!text-[10px]">
                {{ taskTypeLabel(task.modelTaskType) }}
              </n-tag>
              <span v-if="task.modelInputResolution" class="text-[10px] text-on-surface-variant/60 ml-auto">
                {{ task.modelInputResolution }}
              </span>
            </div>
            <div v-if="task.modelClassNames && task.modelClassNames.length > 0" class="flex flex-wrap gap-1">
              <n-tag
                v-for="cls in task.modelClassNames.slice(0, 6)"
                :key="cls"
                size="tiny"
                :bordered="false"
                class="!text-[10px] !bg-primary/8 !text-primary"
              >
                {{ cls }}
              </n-tag>
              <n-tag
                v-if="task.modelClassNames.length > 6"
                size="tiny"
                :bordered="false"
                class="!text-[10px]"
              >
                +{{ task.modelClassNames.length - 6 }}
              </n-tag>
            </div>
          </div>

          <!-- Row 4: Info Grid — Group, Frequency, Node, Alert Level -->
          <div class="grid grid-cols-4 gap-x-3 gap-y-2 text-xs text-on-surface-variant mb-3">
            <div>
              <span class="text-on-surface-variant/60 text-[10px]">摄像头分组</span>
              <div class="mt-0.5 font-medium text-on-surface truncate">{{ task.groupName || '-' }}</div>
            </div>
            <div>
              <span class="text-on-surface-variant/60 text-[10px]">抓图频率</span>
              <div class="mt-0.5 font-medium text-on-surface">{{ frequencyLabel(task.captureFrequency) }}</div>
            </div>
            <div>
              <span class="text-on-surface-variant/60 text-[10px]">推理节点</span>
              <div class="mt-0.5 font-medium text-on-surface truncate">
                <template v-if="task.nodes && task.nodes.length > 0">
                  <n-tooltip trigger="hover">
                    <template #trigger>
                      <span class="cursor-default">{{ task.nodes[0].nodeName }}</span>
                    </template>
                    <div v-for="node in task.nodes" :key="node.nodeId" class="text-xs">
                      {{ node.nodeName }} ({{ node.host }}:{{ node.port }})
                      <n-tag size="tiny" :bordered="false" :type="node.status === 'online' ? 'success' : 'default'" class="ml-1">
                        {{ node.status }}
                      </n-tag>
                    </div>
                  </n-tooltip>
                  <span v-if="task.nodes.length > 1" class="text-[10px] text-on-surface-variant/60"> +{{ task.nodes.length - 1 }}</span>
                </template>
                <span v-else class="text-on-surface-variant/60">自动调度</span>
              </div>
            </div>
            <div>
              <span class="text-on-surface-variant/60 text-[10px]">告警级别</span>
              <div class="mt-0.5">
                <n-tag size="tiny" :bordered="false" :type="alertLevelType(task.alertLevel)">
                  {{ alertLevelLabel(task.alertLevel) }}
                </n-tag>
              </div>
            </div>
          </div>

          <!-- Row 5: Stats Bar -->
          <div class="flex items-center gap-4 border-t border-outline-variant/10 pt-3 mb-3">
            <div class="text-xs">
              <span class="text-on-surface-variant/60">推理</span>
              <span class="ml-1 font-semibold text-on-surface">{{ task.totalInference }}</span>
            </div>
            <div class="text-xs">
              <span class="text-on-surface-variant/60">告警</span>
              <span class="ml-1 font-semibold" :class="task.totalAlert > 0 ? 'text-red-400' : 'text-on-surface'">
                {{ task.totalAlert }}
              </span>
            </div>
            <div v-if="task.lastInferenceTime" class="text-xs ml-auto text-on-surface-variant/60">
              最后推理 {{ formatTime(task.lastInferenceTime) }}
            </div>
          </div>

          <!-- Row 6: Actions -->
          <div class="flex gap-2">
            <n-button
              v-if="task.status !== 'running'"
              size="tiny"
              type="success"
              quaternary
              @click="handleStart(task)"
            >
              <template #icon><Icon icon="mdi:play" /></template>
              启动
            </n-button>
            <n-button
              v-else
              size="tiny"
              type="warning"
              quaternary
              @click="handleStop(task)"
            >
              <template #icon><Icon icon="mdi:stop" /></template>
              停止
            </n-button>
            <n-button size="tiny" quaternary @click="openEditModal(task)">
              <template #icon><Icon icon="mdi:pencil" /></template>
              编辑
            </n-button>
            <n-button size="tiny" quaternary type="info" @click="openDetailDrawer(task)">
              <template #icon><Icon icon="mdi:text-box-search-outline" /></template>
              详情
            </n-button>
            <n-button size="tiny" quaternary type="error" @click="handleDelete(task)">
              <template #icon><Icon icon="mdi:delete" /></template>
              删除
            </n-button>
          </div>
        </div>
      </div>
    </n-spin>

    <!-- Detail Drawer -->
    <n-drawer v-model:show="drawerVisible" :width="620" placement="right">
      <n-drawer-content v-if="drawerTask" closable>
        <template #header>
          <div class="flex items-center gap-2">
            <span
              class="inline-block h-2.5 w-2.5 rounded-full"
              :class="statusDotClass(drawerTask.status)"
            />
            <span class="font-semibold">{{ drawerTask.name }}</span>
            <n-tag size="tiny" :bordered="false" :type="statusTagType(drawerTask.status)">
              {{ statusLabel(drawerTask.status) }}
            </n-tag>
          </div>
        </template>

        <!-- Task Summary -->
        <div class="space-y-4 mb-6">
          <p v-if="drawerTask.description" class="text-xs text-on-surface-variant">{{ drawerTask.description }}</p>

          <div class="grid grid-cols-2 gap-x-4 gap-y-3 text-xs rounded-lg bg-bg-void/50 p-3">
            <div>
              <span class="text-on-surface-variant/60">摄像头分组</span>
              <div class="mt-0.5 font-medium text-on-surface">{{ drawerTask.groupName || '-' }}</div>
            </div>
            <div>
              <span class="text-on-surface-variant/60">关联模型</span>
              <div class="mt-0.5 font-medium text-on-surface">{{ drawerTask.modelName || '-' }}</div>
            </div>
            <div>
              <span class="text-on-surface-variant/60">模型类型</span>
              <div class="mt-0.5">{{ taskTypeLabel(drawerTask.modelTaskType) || '-' }}</div>
            </div>
            <div>
              <span class="text-on-surface-variant/60">输入分辨率</span>
              <div class="mt-0.5">{{ drawerTask.modelInputResolution || '-' }}</div>
            </div>
            <div>
              <span class="text-on-surface-variant/60">推理节点</span>
              <div class="mt-0.5">
                <template v-if="drawerTask.nodes && drawerTask.nodes.length > 0">
                  <span v-for="(node, idx) in drawerTask.nodes" :key="node.nodeId">
                    {{ node.nodeName }} ({{ node.host }})
                    <span v-if="idx < drawerTask.nodes.length - 1">、</span>
                  </span>
                </template>
                <span v-else class="text-on-surface-variant/60">自动调度</span>
              </div>
            </div>
            <div>
              <span class="text-on-surface-variant/60">抓图频率</span>
              <div class="mt-0.5">{{ frequencyLabel(drawerTask.captureFrequency) }}</div>
            </div>
            <div>
              <span class="text-on-surface-variant/60">告警级别</span>
              <div class="mt-0.5">
                <n-tag size="tiny" :bordered="false" :type="alertLevelType(drawerTask.alertLevel)">
                  {{ alertLevelLabel(drawerTask.alertLevel) }}
                </n-tag>
              </div>
            </div>
            <div>
              <span class="text-on-surface-variant/60">置信度阈值</span>
              <div class="mt-0.5">{{ drawerTask.alertConfidence }}</div>
            </div>
            <div>
              <span class="text-on-surface-variant/60">创建时间</span>
              <div class="mt-0.5">{{ formatFullTime(drawerTask.createdAt) }}</div>
            </div>
            <div>
              <span class="text-on-surface-variant/60">累计推理 / 告警</span>
              <div class="mt-0.5">
                <span class="font-medium">{{ drawerTask.totalInference }}</span>
                <span class="mx-1 text-on-surface-variant/40">/</span>
                <span class="font-medium" :class="drawerTask.totalAlert > 0 ? 'text-red-400' : ''">{{ drawerTask.totalAlert }}</span>
              </div>
            </div>
          </div>

          <!-- Model class names in drawer -->
          <div v-if="drawerTask.modelClassNames && drawerTask.modelClassNames.length > 0">
            <span class="text-xs text-on-surface-variant/60">检测分类</span>
            <div class="flex flex-wrap gap-1 mt-1">
              <n-tag
                v-for="cls in drawerTask.modelClassNames"
                :key="cls"
                size="tiny"
                :bordered="false"
                class="!text-[10px] !bg-primary/8 !text-primary"
              >
                {{ cls }}
              </n-tag>
            </div>
          </div>
        </div>

        <!-- Recent Inference Records -->
        <div>
          <div class="flex items-center justify-between mb-3">
            <h4 class="text-sm font-semibold text-on-surface">最近推理记录</h4>
            <span class="text-xs text-on-surface-variant/60">共 {{ drawerTotal }} 条</span>
          </div>

          <n-spin :show="drawerLoading">
            <div v-if="drawerRecords.length === 0 && !drawerLoading" class="py-8 text-center text-on-surface-variant text-xs">
              暂无推理记录
            </div>

            <div class="space-y-2">
              <div
                v-for="record in drawerRecords"
                :key="record.id"
                class="flex items-center gap-3 rounded-lg bg-bg-void/40 p-2.5"
              >
                <!-- Thumbnail -->
                <div class="h-12 w-16 shrink-0 rounded overflow-hidden bg-bg-void flex items-center justify-center">
                  <img v-if="record.thumbnailUrl" :src="record.thumbnailUrl" class="h-full w-full object-cover" alt="" />
                  <Icon v-else icon="mdi:image-off" class="text-on-surface-variant/30 text-lg" />
                </div>
                <!-- Info -->
                <div class="flex-1 min-w-0">
                  <div class="flex items-center gap-2 mb-1">
                    <span class="text-xs font-medium text-on-surface truncate">{{ record.cameraName || record.cameraId || '-' }}</span>
                    <n-tag
                      size="tiny"
                      :bordered="false"
                      :type="record.alertStatus === 'alert' ? 'error' : record.alertStatus === 'warning' ? 'warning' : 'default'"
                    >
                      {{ record.alertStatus === 'alert' ? '告警' : record.alertStatus === 'warning' ? '预警' : '正常' }}
                    </n-tag>
                  </div>
                  <div class="flex items-center gap-2 text-[10px] text-on-surface-variant/60">
                    <span v-if="record.avgConfidence != null">置信度 {{ (record.avgConfidence * 100).toFixed(0) }}%</span>
                    <span v-if="record.inferenceTimeMs">· {{ record.inferenceTimeMs }}ms</span>
                  </div>
                  <div v-if="record.detections && record.detections.length > 0" class="flex flex-wrap gap-1 mt-1">
                    <n-tag
                      v-for="det in record.detections.slice(0, 4)"
                      :key="det.label"
                      size="tiny"
                      :bordered="false"
                      class="!text-[10px]"
                    >
                      {{ det.label }} ×{{ det.count }}
                    </n-tag>
                    <span v-if="record.detections.length > 4" class="text-[10px] text-on-surface-variant/40">+{{ record.detections.length - 4 }}</span>
                  </div>
                </div>
                <!-- Time -->
                <span class="text-[10px] text-on-surface-variant/60 shrink-0">{{ formatTime(record.createdAt) }}</span>
              </div>
            </div>

            <!-- Pagination -->
            <div v-if="drawerTotal > 0" class="flex items-center justify-between pt-3 mt-3 border-t border-outline-variant/10">
              <span class="text-[10px] text-on-surface-variant/60">{{ drawerTotal }} 条记录</span>
              <n-pagination
                v-model:page="drawerPage"
                :item-count="drawerTotal"
                :page-size="drawerPageSize"
                :page-sizes="[10, 20, 50]"
                show-size-picker
                size="small"
                @update:page="fetchDrawerRecords"
                @update:page-size="handleDrawerPageSizeChange"
              />
            </div>
          </n-spin>
        </div>
      </n-drawer-content>
    </n-drawer>

    <!-- Create/Edit Modal -->
    <n-modal v-model:show="showModal" preset="card" :title="editingTask ? '编辑任务' : '新建监测任务'" :style="{ width: '640px' }">
      <n-form ref="formRef" :model="formData" label-placement="left" label-width="100" size="small">
        <n-form-item label="任务名称" path="name" :rule="{ required: true, message: '请输入任务名称' }">
          <n-input v-model:value="formData.name" placeholder="请输入任务名称" />
        </n-form-item>

        <n-form-item label="任务描述" path="description">
          <n-input v-model:value="formData.description" type="textarea" placeholder="选填" :rows="2" />
        </n-form-item>

        <n-form-item label="业务线" path="businessLine">
          <n-input v-model:value="formData.businessLine" placeholder="如：城市内涝、交通监控" />
        </n-form-item>

        <n-form-item label="摄像头分组" path="groupId" :rule="{ required: true, message: '请选择分组' }">
          <n-select
            v-model:value="formData.groupId"
            :options="groupOptions"
            placeholder="请选择摄像头分组"
          />
        </n-form-item>

        <n-form-item label="关联模型" path="modelId" :rule="{ required: true, message: '请选择模型' }">
          <n-select
            v-model:value="formData.modelId"
            :options="modelOptions"
            placeholder="请选择AI模型"
          />
        </n-form-item>

        <n-form-item label="抓图频率" path="captureFrequency">
          <n-select
            v-model:value="formData.captureFrequency"
            :options="frequencyOptions"
            placeholder="默认 5min"
          />
        </n-form-item>

        <n-divider>告警条件</n-divider>

        <n-form-item label="检测目标" path="alertTarget">
          <n-select
            v-model:value="alertTargetValues"
            multiple
            :options="classNameOptions"
            placeholder="请先选择模型，然后选择要检测的分类"
            :disabled="classNameOptions.length === 0"
            tag
            filterable
          />
        </n-form-item>

        <n-form-item label="置信度阈值" path="alertConfidence">
          <n-input-number v-model:value="formData.alertConfidence" :min="0" :max="1" :step="0.05" :precision="2" />
        </n-form-item>

        <n-form-item label="连续帧数" path="alertFrames">
          <n-input-number v-model:value="formData.alertFrames" :min="1" :max="100" />
        </n-form-item>

        <n-form-item label="告警级别" path="alertLevel">
          <n-select
            v-model:value="formData.alertLevel"
            :options="[
              { label: '严重', value: 'severe' },
              { label: '警告', value: 'warning' },
              { label: '提示', value: 'info' },
            ]"
          />
        </n-form-item>

        <n-divider>推送配置</n-divider>

        <n-form-item label="推送方式" path="pushMethods">
          <n-select
            v-model:value="formData.pushMethods"
            :options="[
              { label: 'WebSocket', value: 'websocket' },
              { label: 'HTTP回调', value: 'http_callback' },
            ]"
            multiple
            placeholder="选择推送方式"
          />
        </n-form-item>

        <n-form-item v-if="formData.pushMethods?.includes('http_callback')" label="回调地址" path="callbackUrl">
          <n-input v-model:value="formData.callbackUrl" placeholder="https://example.com/api/callback" />
        </n-form-item>

        <n-divider>调度配置（选填）</n-divider>

        <n-form-item label="生效时段">
          <div class="flex items-center gap-2">
            <n-time-picker v-model:formatted-value="formData.scheduleStartTime" format="HH:mm" placeholder="开始时间" class="w-32" value-format="HH:mm" />
            <span class="text-on-surface-variant">至</span>
            <n-time-picker v-model:formatted-value="formData.scheduleEndTime" format="HH:mm" placeholder="结束时间" class="w-32" value-format="HH:mm" />
          </div>
        </n-form-item>

        <n-form-item label="生效星期" path="scheduleWeekdays">
          <n-checkbox-group v-model:value="weekdayValues">
            <n-checkbox :value="1" label="一" />
            <n-checkbox :value="2" label="二" />
            <n-checkbox :value="3" label="三" />
            <n-checkbox :value="4" label="四" />
            <n-checkbox :value="5" label="五" />
            <n-checkbox :value="6" label="六" />
            <n-checkbox :value="7" label="日" />
          </n-checkbox-group>
        </n-form-item>
      </n-form>

      <template #footer>
        <div class="flex justify-end gap-2">
          <n-button size="small" @click="showModal = false">取消</n-button>
          <n-button size="small" type="primary" :loading="submitting" @click="handleSubmit">
            {{ editingTask ? '保存' : '创建' }}
          </n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import {
  NButton, NTag, NSpin, NSelect, NInput, NModal, NForm, NFormItem,
  NInputNumber, NDivider, NTimePicker, NCheckboxGroup, NCheckbox,
  NDrawer, NDrawerContent, NPagination, NTooltip,
  useMessage, useDialog,
  type FormInst,
} from 'naive-ui'
import { Icon } from '@iconify/vue'
import { useTaskStore } from '@/stores/task'
import { getCameraGroups } from '@/api/modules/camera'
import { getModels } from '@/api/modules/model'
import { getInferenceRecords } from '@/api/modules/inference'
import type { MonitorTask, MonitorTaskForm, CameraGroup, Model, InferenceRecord } from '@/types'

const taskStore = useTaskStore()
const message = useMessage()
const dialog = useDialog()

// --- Filters ---
const filterStatus = ref<string | null>(null)
const filterKeyword = ref('')

const statusOptions = [
  { label: '运行中', value: 'running' },
  { label: '已停止', value: 'stopped' },
  { label: '异常', value: 'error' },
]

const filteredTasks = computed(() => {
  let list = taskStore.tasks
  if (filterStatus.value) {
    list = list.filter(t => t.status === filterStatus.value)
  }
  if (filterKeyword.value) {
    const kw = filterKeyword.value.toLowerCase()
    list = list.filter(t => t.name.toLowerCase().includes(kw))
  }
  return list
})

// --- Modal ---
const showModal = ref(false)
const editingTask = ref<MonitorTask | null>(null)
const submitting = ref(false)
const formRef = ref<FormInst | null>(null)
const weekdayValues = ref<number[]>([])

const defaultForm = () => ({
  name: '',
  description: '',
  businessLine: '',
  groupId: '',
  modelId: '',
  captureFrequency: '5min',
  alertTarget: '',
  alertConfidence: 0.5,
  alertFrames: 1,
  alertLevel: 'warning',
  pushMethods: ['websocket'] as string[],
  callbackUrl: '',
  scheduleStartTime: null as string | null,
  scheduleEndTime: null as string | null,
  scheduleWeekdays: '',
})

const formData = ref<ReturnType<typeof defaultForm>>(defaultForm())

// Options for selects
const groupOptions = ref<{ label: string; value: string }[]>([])
const modelOptions = ref<{ label: string; value: string }[]>([])
const allModels = ref<Model[]>([])
const alertTargetValues = ref<string[]>([])

// Computed class names from selected model
const classNameOptions = computed(() => {
  const modelId = formData.value.modelId
  if (!modelId) return []
  const model = allModels.value.find(m => m.id === modelId)
  if (!model || !model.classNames || model.classNames.length === 0) return []
  return model.classNames.map(c => ({ label: c, value: c }))
})

// Sync alertTargetValues ↔ formData.alertTarget (comma-separated string)
watch(alertTargetValues, (vals) => {
  formData.value.alertTarget = vals.join(',')
})
const frequencyOptions = [
  { label: '30秒', value: '30s' },
  { label: '1分钟', value: '1min' },
  { label: '3分钟', value: '3min' },
  { label: '5分钟', value: '5min' },
  { label: '10分钟', value: '10min' },
  { label: '30分钟', value: '30min' },
  { label: '1小时', value: '1h' },
]

async function loadOptions() {
  try {
    const groups = await getCameraGroups() as unknown as CameraGroup[]
    groupOptions.value = (groups || []).map(g => ({ label: g.name, value: g.id }))
  } catch { /* ignore */ }

  try {
    const res = await getModels() as unknown as { items: Model[]; total: number }
    allModels.value = res.items || []
    modelOptions.value = allModels.value.map(m => ({ label: `${m.name} (${m.version})`, value: m.id }))
  } catch { /* ignore */ }
}

function openCreateModal() {
  editingTask.value = null
  formData.value = defaultForm()
  weekdayValues.value = []
  alertTargetValues.value = []
  showModal.value = true
}

function openEditModal(task: MonitorTask) {
  editingTask.value = task
  formData.value = {
    name: task.name,
    description: task.description || '',
    businessLine: task.businessLine || '',
    groupId: task.groupId,
    modelId: task.modelId,
    captureFrequency: task.captureFrequency || '5min',
    alertTarget: task.alertTarget || '',
    alertConfidence: task.alertConfidence ?? 0.5,
    alertFrames: task.alertFrames ?? 1,
    alertLevel: task.alertLevel || 'warning',
    pushMethods: task.pushMethods ? task.pushMethods.split(',') : ['websocket'],
    callbackUrl: task.callbackUrl || '',
    scheduleStartTime: task.scheduleStartTime || null,
    scheduleEndTime: task.scheduleEndTime || null,
    scheduleWeekdays: task.scheduleWeekdays || '',
  }
  alertTargetValues.value = task.alertTarget
    ? task.alertTarget.split(',').map(s => s.trim()).filter(Boolean)
    : []
  weekdayValues.value = task.scheduleWeekdays
    ? task.scheduleWeekdays.split(',').map(Number).filter(n => !isNaN(n))
    : []
  showModal.value = true
}

watch(weekdayValues, (vals) => {
  formData.value.scheduleWeekdays = vals.sort().join(',')
})

async function handleSubmit() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    const payload: MonitorTaskForm = {
      ...formData.value,
      pushMethods: Array.isArray(formData.value.pushMethods)
        ? formData.value.pushMethods.join(',')
        : formData.value.pushMethods,
    }

    if (editingTask.value) {
      await taskStore.updateTask(editingTask.value.id, payload)
      message.success('任务更新成功')
    } else {
      await taskStore.createTask(payload)
      message.success('任务创建成功')
    }
    showModal.value = false
  } catch (e: unknown) {
    message.error((e as Error).message || '操作失败')
  } finally {
    submitting.value = false
  }
}

// --- Actions ---
async function handleStart(task: MonitorTask) {
  try {
    await taskStore.startTask(task.id)
    message.success('任务已启动')
  } catch (e: unknown) {
    message.error((e as Error).message || '启动失败')
  }
}

async function handleStop(task: MonitorTask) {
  try {
    await taskStore.stopTask(task.id)
    message.success('任务已停止')
  } catch (e: unknown) {
    message.error((e as Error).message || '停止失败')
  }
}

function handleDelete(task: MonitorTask) {
  dialog.warning({
    title: '确认删除',
    content: `确定要删除任务"${task.name}"吗？此操作不可恢复。`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await taskStore.removeTask(task.id)
        message.success('任务已删除')
      } catch (e: unknown) {
        message.error((e as Error).message || '删除失败')
      }
    },
  })
}

// --- Helpers ---
function statusDotClass(status: string) {
  if (status === 'running') return 'bg-green-500'
  if (status === 'error') return 'bg-red-400'
  return 'bg-gray-400'
}

function statusTagType(status: string): 'success' | 'error' | 'default' {
  if (status === 'running') return 'success'
  if (status === 'error') return 'error'
  return 'default'
}

function statusLabel(status: string) {
  if (status === 'running') return '运行中'
  if (status === 'error') return '异常'
  return '已停止'
}

function alertLevelType(level: string): 'error' | 'warning' | 'info' {
  if (level === 'severe') return 'error'
  if (level === 'warning') return 'warning'
  return 'info'
}

function alertLevelLabel(level: string) {
  if (level === 'severe') return '严重'
  if (level === 'warning') return '警告'
  return '提示'
}

function taskTypeLabel(type?: string) {
  if (!type) return ''
  const map: Record<string, string> = { detect: '目标检测', segment: '语义分割', classify: '图像分类', pose: '姿态估计' }
  return map[type] || type
}

const frequencyMap: Record<string, string> = {
  '30s': '30秒', '1min': '1分钟', '3min': '3分钟', '5min': '5分钟',
  '10min': '10分钟', '30min': '30分钟', '1h': '1小时',
}

function frequencyLabel(freq?: string) {
  if (!freq) return '5分钟'
  return frequencyMap[freq] || freq
}

function formatTime(iso: string) {
  if (!iso) return '-'
  const d = new Date(iso)
  return d.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function formatFullTime(iso: string) {
  if (!iso) return '-'
  const d = new Date(iso)
  return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

// --- Detail Drawer ---
const drawerVisible = ref(false)
const drawerTask = ref<MonitorTask | null>(null)
const drawerRecords = ref<InferenceRecord[]>([])
const drawerTotal = ref(0)
const drawerPage = ref(1)
const drawerPageSize = ref(10)
const drawerLoading = ref(false)

function openDetailDrawer(task: MonitorTask) {
  drawerTask.value = task
  drawerPage.value = 1
  drawerRecords.value = []
  drawerTotal.value = 0
  drawerVisible.value = true
  fetchDrawerRecords()
}

async function fetchDrawerRecords() {
  if (!drawerTask.value) return
  drawerLoading.value = true
  try {
    const res = await getInferenceRecords({
      taskId: drawerTask.value.id,
      page: drawerPage.value,
      size: drawerPageSize.value,
    }) as unknown as { items: InferenceRecord[]; total: number }
    drawerRecords.value = res.items ?? []
    drawerTotal.value = res.total ?? 0
  } catch {
    drawerRecords.value = []
    drawerTotal.value = 0
  } finally {
    drawerLoading.value = false
  }
}

function handleDrawerPageSizeChange(size: number) {
  drawerPageSize.value = size
  drawerPage.value = 1
  fetchDrawerRecords()
}

// --- Lifecycle ---
onMounted(() => {
  taskStore.fetchTasks()
  loadOptions()
})
</script>
