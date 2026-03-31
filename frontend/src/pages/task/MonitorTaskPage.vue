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

      <div class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
        <div
          v-for="task in filteredTasks"
          :key="task.id"
          class="rounded-xl bg-bg-card p-5 transition-shadow hover:shadow-md"
        >
          <!-- Header: Name & Status -->
          <div class="flex items-center justify-between mb-3">
            <div class="flex items-center gap-2">
              <span
                class="inline-block h-2.5 w-2.5 rounded-full"
                :class="statusDotClass(task.status)"
              />
              <h4 class="text-sm font-semibold">{{ task.name }}</h4>
            </div>
            <n-tag size="tiny" :bordered="false" :type="statusTagType(task.status)">
              {{ statusLabel(task.status) }}
            </n-tag>
          </div>

          <!-- Description -->
          <p v-if="task.description" class="text-xs text-on-surface-variant mb-3 line-clamp-2">
            {{ task.description }}
          </p>

          <!-- Info Grid -->
          <div class="grid grid-cols-2 gap-y-2 text-xs text-on-surface-variant">
            <div>
              <span class="text-on-surface-variant/60">摄像头分组</span>
              <div class="mt-0.5 font-medium text-on-surface">{{ task.groupName || '-' }}</div>
            </div>
            <div>
              <span class="text-on-surface-variant/60">关联模型</span>
              <div class="mt-0.5 font-medium text-on-surface">{{ task.modelName || '-' }}</div>
            </div>
            <div>
              <span class="text-on-surface-variant/60">抓图频率</span>
              <div class="mt-0.5">{{ task.captureFrequency || '5min' }}</div>
            </div>
            <div>
              <span class="text-on-surface-variant/60">告警级别</span>
              <div class="mt-0.5">
                <n-tag size="tiny" :bordered="false" :type="alertLevelType(task.alertLevel)">
                  {{ task.alertLevel }}
                </n-tag>
              </div>
            </div>
          </div>

          <!-- Stats -->
          <div class="mt-3 flex gap-4 border-t border-outline-variant/10 pt-3">
            <div class="text-xs">
              <span class="text-on-surface-variant/60">推理次数</span>
              <div class="text-sm font-semibold text-on-surface">{{ task.totalInference }}</div>
            </div>
            <div class="text-xs">
              <span class="text-on-surface-variant/60">告警次数</span>
              <div class="text-sm font-semibold" :class="task.totalAlert > 0 ? 'text-red-400' : 'text-on-surface'">
                {{ task.totalAlert }}
              </div>
            </div>
            <div v-if="task.lastInferenceTime" class="text-xs ml-auto">
              <span class="text-on-surface-variant/60">最后推理</span>
              <div class="mt-0.5">{{ formatTime(task.lastInferenceTime) }}</div>
            </div>
          </div>

          <!-- Actions -->
          <div class="mt-3 flex gap-2">
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
            <n-button size="tiny" quaternary type="error" @click="handleDelete(task)">
              <template #icon><Icon icon="mdi:delete" /></template>
              删除
            </n-button>
          </div>
        </div>
      </div>
    </n-spin>

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
          <n-input v-model:value="formData.alertTarget" placeholder="目标类别，多个用逗号分隔" />
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
  useMessage, useDialog,
  type FormInst,
} from 'naive-ui'
import { Icon } from '@iconify/vue'
import { useTaskStore } from '@/stores/task'
import { getCameraGroups } from '@/api/modules/camera'
import { getModels } from '@/api/modules/model'
import type { MonitorTask, MonitorTaskForm, CameraGroup, Model } from '@/types'

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
  scheduleStartTime: '',
  scheduleEndTime: '',
  scheduleWeekdays: '',
})

const formData = ref<ReturnType<typeof defaultForm>>(defaultForm())

// Options for selects
const groupOptions = ref<{ label: string; value: string }[]>([])
const modelOptions = ref<{ label: string; value: string }[]>([])

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
    modelOptions.value = (res.items || []).map(m => ({ label: `${m.name} (${m.version})`, value: m.id }))
  } catch { /* ignore */ }
}

function openCreateModal() {
  editingTask.value = null
  formData.value = defaultForm()
  weekdayValues.value = []
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
    scheduleStartTime: task.scheduleStartTime || '',
    scheduleEndTime: task.scheduleEndTime || '',
    scheduleWeekdays: task.scheduleWeekdays || '',
  }
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

function formatTime(iso: string) {
  if (!iso) return '-'
  const d = new Date(iso)
  return d.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

// --- Lifecycle ---
onMounted(() => {
  taskStore.fetchTasks()
  loadOptions()
})
</script>
