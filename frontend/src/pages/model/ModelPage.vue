<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <p class="text-sm text-on-surface-variant">管理及优化视觉分析模型，配置高性能推理引擎。</p>
      <div class="flex items-center gap-3">
        <n-input v-model:value="searchQuery" placeholder="搜索模型..." size="small" class="w-52">
          <template #prefix><Icon icon="mdi:magnify" class="text-on-surface-variant" /></template>
        </n-input>
        <n-button type="primary" size="small" @click="showUploadModal = true">
          <template #icon><Icon icon="mdi:plus" /></template>
          上传模型
        </n-button>
      </div>
    </div>

    <div class="flex gap-4">
      <!-- Model Cards -->
      <div class="w-80 shrink-0 space-y-3">
        <div
          v-for="model in modelStore.models"
          :key="model.id"
          class="cursor-pointer rounded-xl p-5 transition-colors"
          :class="selectedId === model.id ? 'bg-bg-active ring-1 ring-primary/30' : 'bg-bg-card hover:bg-bg-active'"
          @click="selectModel(model)"
        >
          <div class="flex items-center justify-between">
            <div class="flex items-center gap-2">
              <Icon :icon="getModelIcon(model.businessTag)" class="text-lg text-primary" />
              <h4 class="text-sm font-semibold">{{ model.name }}</h4>
            </div>
          </div>
          <div class="mt-2 flex items-center gap-2 flex-wrap">
            <n-tag size="tiny" :bordered="false" type="info">{{ model.businessTag }} {{ model.version }}</n-tag>
            <n-tag size="tiny" :bordered="false" :type="getTaskTypeTagType(model.taskType)">
              {{ getTaskTypeLabel(model.taskType) }}
            </n-tag>
            <n-tag
              size="tiny" :bordered="false"
              :type="model.status === 'loaded' ? 'success' : 'default'"
            >
              {{ model.status === 'loaded' ? '已加载' : '未加载' }}
            </n-tag>
            <n-tag
              v-if="model.status === 'loaded' && model.device"
              size="tiny" :bordered="false"
              :type="model.device === 'cuda' ? 'warning' : 'default'"
            >
              <template #icon>
                <Icon :icon="model.device === 'cuda' ? 'mdi:memory' : 'mdi:developer-board'" />
              </template>
              {{ model.device === 'cuda' ? (model.deviceName || 'GPU') : 'CPU' }}
            </n-tag>
            <n-tag
              v-if="model.status === 'loaded' && model.nodeName"
              size="tiny" :bordered="false" type="info"
            >
              <template #icon><Icon icon="mdi:server" /></template>
              {{ model.nodeName }}
            </n-tag>
          </div>
          <div class="mt-3 text-xs text-on-surface-variant">
            <span>引擎支持</span>
            <div class="mt-1 flex gap-1">
              <n-tag v-for="e in model.engineSupport" :key="e" size="tiny" :bordered="false">{{ e }}</n-tag>
            </div>
          </div>
          <div class="mt-2 text-xs text-on-surface-variant">
            <span>目标硬件</span>
            <div class="mt-1 flex items-center gap-1">
              <Icon :icon="model.targetHardware?.includes('GPU') ? 'mdi:memory' : 'mdi:developer-board'" class="text-sm" />
              <span>{{ model.targetHardware ?? '' }}</span>
            </div>
          </div>
          <div class="mt-3 flex gap-2">
            <n-button
              v-if="model.status === 'loaded'"
              size="tiny" quaternary type="warning"
              @click.stop="handleUnload(model)"
            >
              <template #icon><Icon icon="mdi:eject" /></template>
              卸载
            </n-button>
            <n-button
              v-else
              size="tiny" quaternary type="primary"
              @click.stop="handleLoad(model)"
            >
              <template #icon><Icon icon="mdi:play-arrow" /></template>
              加载模型
            </n-button>
            <n-button size="tiny" quaternary @click.stop="selectModel(model)">
              <template #icon><Icon icon="mdi:tune" /></template>
              参数
            </n-button>
            <n-button size="tiny" quaternary type="error" @click.stop="handleDelete(model)">
              <template #icon><Icon icon="mdi:delete" /></template>
            </n-button>
          </div>
        </div>
      </div>

      <!-- Detail Panel -->
      <div v-if="selectedModel" class="flex-1 rounded-xl bg-bg-card p-6">
        <div class="mb-6 flex items-center justify-between">
          <h3 class="text-lg font-semibold">模型配置详情</h3>
          <button class="text-on-surface-variant hover:text-on-surface" @click="selectedId = null">
            <Icon icon="mdi:close" class="text-xl" />
          </button>
        </div>

        <!-- Basic Info -->
        <section class="mb-6">
          <h4 class="mb-3 text-sm font-semibold text-on-surface-variant">基本信息</h4>
          <div class="grid grid-cols-2 gap-3">
            <InfoRow label="模型路径" :value="selectedModel.modelPath" />
            <InfoRow label="创建时间" :value="selectedModel.createdAt" />
            <InfoRow label="作者" :value="selectedModel.author" />
          </div>
        </section>

        <!-- Version History -->
        <section class="mb-6">
          <h4 class="mb-3 text-sm font-semibold text-on-surface-variant">版本历史</h4>
          <div class="space-y-2">
            <div
              v-for="v in selectedModel.versionHistory"
              :key="v.version"
              class="flex items-center gap-3 rounded-lg bg-bg-floor px-4 py-2"
            >
              <n-tag size="tiny" type="primary" :bordered="false">{{ v.version }}</n-tag>
              <span class="text-sm text-on-surface-variant">{{ v.description }}</span>
            </div>
          </div>
        </section>

        <!-- Dynamic Parameters -->
        <section class="mb-6">
          <h4 class="mb-3 text-sm font-semibold text-on-surface-variant">动态参数</h4>
          <div class="space-y-4">
            <div>
              <label class="text-xs text-on-surface-variant">置信度阈值</label>
              <n-slider v-model:value="editThreshold" :min="0" :max="1" :step="0.05" :format-tooltip="(v: number) => v.toFixed(2)" />
            </div>
            <n-form-item label="最大并发数">
              <n-input-number v-model:value="editConcurrency" :min="1" :max="16" size="small" />
            </n-form-item>
            <n-form-item label="推理分辨率">
              <n-select :options="resolutionOptions" v-model:value="editResolution" size="small" />
            </n-form-item>
          </div>
        </section>

        <!-- Live Performance -->
        <section class="mb-6">
          <h4 class="mb-3 text-sm font-semibold text-on-surface-variant">实时性能</h4>
          <div class="rounded-lg bg-bg-floor p-4">
            <div class="flex items-center gap-4">
              <div class="h-24 flex-1 rounded bg-bg-void flex items-center justify-center text-on-surface-variant text-xs">
                性能图表占位
              </div>
              <div class="text-center">
                <div class="text-2xl font-bold text-primary">{{ selectedModel.avgLatency }}ms</div>
                <div class="text-xs text-on-surface-variant">平均推理延迟</div>
              </div>
            </div>
          </div>
        </section>

        <div class="flex gap-3 justify-end">
          <n-button size="small" @click="handleDiscard">放弃修改</n-button>
          <n-button type="primary" size="small" :loading="saving" @click="handleSave">保存配置</n-button>
        </div>
      </div>

      <div v-else class="flex-1 rounded-xl bg-bg-card p-6 flex items-center justify-center">
        <div class="text-center text-on-surface-variant">
          <Icon icon="mdi:brain" class="text-5xl opacity-20" />
          <p class="mt-3 text-sm">选择一个模型查看配置详情</p>
        </div>
      </div>
    </div>

    <!-- Upload Modal -->
    <n-modal v-model:show="showUploadModal" preset="card" title="上传模型" class="w-[520px]">
      <n-form ref="uploadFormRef" :model="uploadForm" label-placement="left" label-width="80">
        <n-form-item label="模型名称" path="name" :rule="{ required: true, message: '请输入模型名称' }">
          <n-input v-model:value="uploadForm.name" placeholder="例：YOLOv8-Security" />
        </n-form-item>
        <n-form-item label="版本号" path="version" :rule="{ required: true, message: '请输入版本号' }">
          <n-input v-model:value="uploadForm.version" placeholder="例：V1.0.0" />
        </n-form-item>
        <n-form-item label="模型类型" path="taskType" :rule="{ required: true, message: '请选择模型类型' }">
          <n-select v-model:value="uploadForm.taskType" :options="taskTypeOptions" placeholder="选择模型任务类型" />
        </n-form-item>
        <n-form-item label="业务标签">
          <n-select v-model:value="uploadForm.businessTag" :options="businessTagOptions" placeholder="选择业务标签" clearable />
        </n-form-item>
        <n-form-item label="推理引擎">
          <n-input v-model:value="uploadForm.engineSupport" placeholder="例：TRT,ONNX" />
        </n-form-item>
        <n-form-item label="目标硬件">
          <n-input v-model:value="uploadForm.targetHardware" placeholder="例：GPU (NVIDIA)" />
        </n-form-item>
        <n-form-item label="作者">
          <n-input v-model:value="uploadForm.author" placeholder="上传者名称" />
        </n-form-item>
        <n-form-item label="模型文件" path="file" :rule="{ required: true, message: '请选择模型文件', validator: () => !!uploadFile }">
          <n-upload
            :max="1"
            :default-upload="false"
            accept=".pt,.onnx,.engine,.trt,.bin"
            @change="(data: any) => { uploadFile = data.fileList?.[0]?.file ?? null }"
          >
            <n-button>选择文件</n-button>
          </n-upload>
        </n-form-item>
      </n-form>
      <template #action>
        <div class="flex justify-end gap-2">
          <n-button size="small" @click="showUploadModal = false">取消</n-button>
          <n-button type="primary" size="small" :loading="uploading" @click="handleUpload">上传</n-button>
        </div>
      </template>
    </n-modal>

    <!-- Load Model Dialog -->
    <n-modal v-model:show="showLoadModal" preset="card" title="加载模型" class="w-[420px]">
      <div v-if="loadTarget" class="space-y-4">
        <div class="text-sm">
          即将加载模型 <span class="font-semibold text-primary">{{ loadTarget.name }}</span>
        </div>
        <n-spin :show="loadingDeviceInfo" description="正在检测设备...">
          <div class="space-y-3">
            <div class="text-sm font-medium">目标节点</div>
            <n-select v-model:value="loadNodeId" :options="nodeOptions" size="small" placeholder="选择推理节点" />
            <div class="text-sm font-medium">选择运行设备</div>
            <n-radio-group v-model:value="loadDevice">
              <div class="space-y-2">
                <n-radio value="cpu" class="w-full">
                  <div class="flex items-center gap-2">
                    <Icon icon="mdi:developer-board" class="text-base" />
                    <span>CPU</span>
                  </div>
                </n-radio>
                <n-radio value="cuda" :disabled="!availableDevices.includes('cuda')" class="w-full">
                  <div class="flex items-center gap-2">
                    <Icon icon="mdi:memory" class="text-base" />
                    <span>GPU{{ gpuName ? ` (${gpuName})` : '' }}</span>
                    <n-tag v-if="!availableDevices.includes('cuda')" size="tiny" type="warning" :bordered="false">
                      不可用
                    </n-tag>
                  </div>
                </n-radio>
              </div>
            </n-radio-group>
          </div>
        </n-spin>
      </div>
      <template #action>
        <div class="flex justify-end gap-2">
          <n-button size="small" @click="showLoadModal = false">取消</n-button>
          <n-button type="primary" size="small" :loading="loadingModel" :disabled="loadingDeviceInfo" @click="confirmLoad">
            加载
          </n-button>
        </div>
      </template>
    </n-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import {
  NInput, NButton, NTag, NSlider, NFormItem, NInputNumber, NSelect,
  NModal, NForm, NUpload, useMessage, useDialog, NRadioGroup, NRadio, NSpin,
} from 'naive-ui'
import { Icon } from '@iconify/vue'
import { useModelStore } from '@/stores/model'
import { useNodeStore } from '@/stores/node'
import type { Model } from '@/types'
import InfoRow from './InfoRow.vue'

const modelStore = useModelStore()
const nodeStore = useNodeStore()
const message = useMessage()
const dialog = useDialog()
const searchQuery = ref('')
const selectedId = ref<string | null>(null)
const editThreshold = ref(0.65)
const editConcurrency = ref(4)
const editResolution = ref('640x640')

// Upload dialog state
const showUploadModal = ref(false)
const uploading = ref(false)
const uploadFile = ref<File | null>(null)
const uploadForm = reactive({
  name: '',
  version: '',
  taskType: 'detect' as string,
  businessTag: null as string | null,
  engineSupport: '',
  targetHardware: '',
  author: '',
})

const businessTagOptions = [
  { label: 'Security', value: 'Security' },
  { label: 'Industrial', value: 'Industrial' },
  { label: 'Access', value: 'Access' },
]

const taskTypeOptions = [
  { label: '目标检测 (Detection)', value: 'detect' },
  { label: '实例分割 (Segmentation)', value: 'segment' },
  { label: '图像分类 (Classification)', value: 'classify' },
  { label: '姿态估计 (Pose)', value: 'pose' },
]

async function handleUpload() {
  if (!uploadFile.value) {
    message.warning('请选择模型文件')
    return
  }
  if (!uploadForm.name.trim() || !uploadForm.version.trim()) {
    message.warning('请填写模型名称和版本号')
    return
  }
  uploading.value = true
  try {
    await modelStore.uploadModel(uploadFile.value, {
      name: uploadForm.name,
      version: uploadForm.version,
      taskType: uploadForm.taskType || 'detect',
      businessTag: uploadForm.businessTag ?? undefined,
      engineSupport: uploadForm.engineSupport || undefined,
      targetHardware: uploadForm.targetHardware || undefined,
      author: uploadForm.author || undefined,
    })
    message.success('模型上传成功')
    showUploadModal.value = false
    resetUploadForm()
  } catch (e: any) {
    message.error(e?.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

function resetUploadForm() {
  uploadForm.name = ''
  uploadForm.version = ''
  uploadForm.taskType = 'detect'
  uploadForm.businessTag = null
  uploadForm.engineSupport = ''
  uploadForm.targetHardware = ''
  uploadForm.author = ''
  uploadFile.value = null
}

onMounted(() => {
  modelStore.fetchModels()
  nodeStore.fetchNodes()
})

const selectedModel = computed(() =>
  modelStore.models.find(m => m.id === selectedId.value) ?? null
)

function selectModel(model: Model) {
  selectedId.value = model.id
  editThreshold.value = model.confidenceThreshold
  editConcurrency.value = model.maxConcurrency
  editResolution.value = model.inputResolution
}

function getModelIcon(tag: string) {
  const icons: Record<string, string> = {
    Security: 'mdi:shield-check',
    Industrial: 'mdi:factory',
    Access: 'mdi:face-recognition',
  }
  return icons[tag] || 'mdi:brain'
}

function getTaskTypeLabel(taskType?: string) {
  const labels: Record<string, string> = {
    detect: '目标检测',
    segment: '实例分割',
    classify: '图像分类',
    pose: '姿态估计',
  }
  return labels[taskType ?? 'detect'] ?? '目标检测'
}

function getTaskTypeTagType(taskType?: string) {
  const types: Record<string, 'success' | 'warning' | 'info' | 'error'> = {
    detect: 'success',
    segment: 'warning',
    classify: 'info',
    pose: 'error',
  }
  return types[taskType ?? 'detect'] ?? 'success'
}

const saving = ref(false)

async function handleSave() {
  if (!selectedModel.value) return
  saving.value = true
  try {
    await modelStore.updateModelConfig(selectedModel.value.id, {
      confidenceThreshold: editThreshold.value,
      maxConcurrency: editConcurrency.value,
      inputResolution: editResolution.value,
    })
    message.success('配置已保存')
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function handleDiscard() {
  if (!selectedModel.value) return
  editThreshold.value = selectedModel.value.confidenceThreshold
  editConcurrency.value = selectedModel.value.maxConcurrency
  editResolution.value = selectedModel.value.inputResolution
  message.info('已恢复原始配置')
}

const resolutionOptions = [
  { label: '640x640', value: '640x640' },
  { label: '1280x720', value: '1280x720' },
  { label: '224x224', value: '224x224' },
  { label: '160x160', value: '160x160' },
]

async function handleLoad(model: Model) {
  loadTarget.value = model
  loadDevice.value = 'cpu'
  loadNodeId.value = '__auto__'
  loadingDeviceInfo.value = true
  showLoadModal.value = true
  // Fetch nodes and device info in parallel
  nodeStore.fetchNodes()
  try {
    const info = await modelStore.fetchDeviceInfo()
    availableDevices.value = info.devices ?? ['cpu']
    gpuName.value = info.gpu_name ?? null
    if (info.cuda_available) {
      loadDevice.value = 'cuda'
    }
  } catch {
    availableDevices.value = ['cpu']
    gpuName.value = null
  } finally {
    loadingDeviceInfo.value = false
  }
}

// Load model dialog state
const showLoadModal = ref(false)
const loadTarget = ref<Model | null>(null)
const loadDevice = ref('cpu')
const loadNodeId = ref<string | null>(null)
const availableDevices = ref<string[]>(['cpu'])
const gpuName = ref<string | null>(null)
const loadingDeviceInfo = ref(false)
const loadingModel = ref(false)

const nodeOptions = computed(() => [
  { label: '自动分配', value: '__auto__' },
  ...nodeStore.nodes
    .filter(n => n.status === 'online')
    .map(n => ({ label: `${n.nodeName} (${n.host}:${n.port})`, value: n.id })),
])

async function confirmLoad() {
  if (!loadTarget.value) return
  loadingModel.value = true
  try {
    const deviceName = loadDevice.value === 'cuda' ? gpuName.value ?? undefined : undefined
    const nodeId = loadNodeId.value === '__auto__' ? undefined : loadNodeId.value ?? undefined
    await modelStore.loadModel(loadTarget.value.id, loadDevice.value, deviceName, nodeId)
    message.success(`模型 ${loadTarget.value.name} 已加载到 ${loadDevice.value.toUpperCase()}`)
    showLoadModal.value = false
  } catch (e: any) {
    message.error(e?.message || '加载失败')
  } finally {
    loadingModel.value = false
  }
}

async function handleUnload(model: Model) {
  try {
    await modelStore.unloadModel(model.id)
    message.success(`模型 ${model.name} 已卸载`)
  } catch (e: any) {
    message.error(e?.message || '卸载失败')
  }
}

function handleDelete(model: Model) {
  if (model.status === 'loaded') {
    message.warning('请先卸载模型再删除')
    return
  }
  dialog.warning({
    title: '确认删除',
    content: `确定要删除模型「${model.name}」吗？此操作不可恢复。`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await modelStore.deleteModel(model.id)
        if (selectedId.value === model.id) selectedId.value = null
        message.success('模型已删除')
      } catch (e: any) {
        message.error(e?.message || '删除失败')
      }
    },
  })
}
</script>
