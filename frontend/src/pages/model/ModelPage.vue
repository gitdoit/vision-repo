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

    <!-- Models Grid -->
    <div v-if="filteredModels.length" class="grid grid-cols-1 xl:grid-cols-3 gap-6">
      <div
        v-for="model in filteredModels"
        :key="model.id"
        class="flex flex-col rounded-xl bg-bg-card p-6 shadow-sm border border-transparent hover:border-primary/20 transition-all"
      >
        <!-- Card Header -->
        <div class="flex items-start justify-between mb-4">
          <div class="flex items-center gap-3">
            <div class="flex h-10 w-10 items-center justify-center rounded-lg bg-primary/10 text-primary">
              <Icon :icon="getModelIcon(model.businessTag ?? '')" class="text-2xl" />
            </div>
            <div>
              <h3 class="text-base font-semibold text-on-surface leading-tight">{{ model.name }}</h3>
              <p class="text-xs text-on-surface-variant mt-0.5">{{ model.version }} · {{ model.author || '系统' }}</p>
            </div>
          </div>
          <div class="flex gap-1">
            <n-button size="tiny" quaternary type="error" @click="handleDelete(model)" title="删除模型">
              <template #icon><Icon icon="mdi:delete" /></template>
            </n-button>
          </div>
        </div>

        <!-- Tags -->
        <div class="flex flex-wrap gap-2 mb-5">
          <n-tag size="small" :bordered="false" type="info" v-if="model.businessTag">{{ model.businessTag }}</n-tag>
          <n-tag size="small" :bordered="false" :type="getTaskTypeTagType(model.taskType)">
            {{ getTaskTypeLabel(model.taskType) }}
          </n-tag>
          <n-tag size="small" :bordered="false" :type="parsedStatusType(model.parsedStatus)">
            {{ parsedStatusLabel(model.parsedStatus) }}
          </n-tag>
        </div>

        <n-divider class="!my-0" />

        <!-- Card Body Splits -->
        <div class="flex flex-col gap-4 mt-4 text-sm flex-1">
          <!-- Basic Info -->
          <div class="bg-bg-floor rounded-lg p-3 space-y-2">
            <div class="flex justify-between">
              <span class="text-on-surface-variant text-xs">模型路径</span>
              <span class="font-mono text-xs truncate max-w-[180px]" :title="model.modelPath">{{ model.modelPath }}</span>
            </div>
            <div class="flex justify-between">
              <span class="text-on-surface-variant text-xs">分类数量</span>
              <span class="text-xs font-semibold">{{ model.numClasses ?? 0 }} 类</span>
            </div>
            <div class="flex justify-between">
              <span class="text-on-surface-variant text-xs">推理分辨率</span>
              <span class="text-xs font-mono">{{ model.inputResolution || '-' }}</span>
            </div>
             <div class="flex justify-between">
              <span class="text-on-surface-variant text-xs">创建时间</span>
              <span class="text-xs">{{ model.createdAt }}</span>
            </div>
          </div>

          <!-- Deployments -->
          <div class="flex flex-col">
            <div class="flex items-center justify-between mb-2">
              <span class="text-xs font-semibold text-on-surface-variant">部署节点</span>
              <n-button
                v-if="loadedDeployments(model).length > 0"
                size="tiny" quaternary type="warning"
                @click="handleUnloadAll(model)"
              >
                <template #icon><Icon icon="mdi:eject" /></template>
                全部卸载
              </n-button>
            </div>
            
            <div v-if="loadedDeployments(model).length === 0" class="text-xs text-on-surface-variant italic py-1">
              未部署
            </div>
            <div v-else class="space-y-1.5 flex-1 max-h-32 overflow-y-auto pr-1">
               <div
                v-for="dep in loadedDeployments(model)"
                :key="dep.id"
                class="flex items-center gap-2 rounded bg-bg-floor px-2 py-1.5"
              >
                <Icon :icon="dep.device === 'cuda' ? 'mdi:memory' : 'mdi:developer-board'" 
                      class="text-sm shrink-0" 
                      :class="dep.device === 'cuda' ? 'text-warning' : 'text-on-surface-variant'" />
                <span class="text-xs truncate flex-1" :title="dep.nodeName">{{ dep.nodeName }}</span>
                 <n-button size="tiny" quaternary type="warning" @click="handleUnloadNode(model, dep.nodeId)" class="!p-0" title="卸载">
                  <template #icon><Icon icon="mdi:close" /></template>
                </n-button>
              </div>
            </div>

            <n-button size="small" dashed type="primary" class="mt-2 w-full" @click="handleLoad(model)">
              <template #icon><Icon icon="mdi:plus" /></template>
              新增节点部署
            </n-button>
          </div>
          
          <div class="mt-auto pt-2">
             <div class="flex items-center justify-between mb-3 text-xs font-semibold text-on-surface-variant">
               动态参数
               <div class="flex gap-2" v-if="hasConfigChanges(model.id)">
                  <n-button size="tiny" quaternary @click="handleDiscard(model.id)">重置</n-button>
                  <n-button size="tiny" type="primary" :loading="saving === model.id" @click="handleSave(model)">保存</n-button>
               </div>
             </div>
             <div class="space-y-3">
               <div>
                  <div class="flex justify-between text-xs mb-1">
                    <span class="text-on-surface-variant">置信度阈值</span>
                    <span class="font-mono">{{ getDraftConfig(model).confidenceThreshold.toFixed(2) }}</span>
                  </div>
                  <n-slider 
                    v-model:value="draftConfigs[model.id].confidenceThreshold" 
                    :min="0" :max="1" :step="0.05" 
                    :format-tooltip="(v: number) => v.toFixed(2)" 
                  />
               </div>
               <div>
                  <div class="text-xs text-on-surface-variant mb-1">最大并发</div>
                  <n-input-number v-model:value="draftConfigs[model.id].maxConcurrency" :min="1" :max="16" size="small" class="w-full" />
               </div>
             </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="flex-1 rounded-xl bg-bg-card p-12 flex flex-col items-center justify-center border border-dashed border-primary/20">
      <Icon icon="mdi:database-search-outline" class="text-5xl text-on-surface-variant opacity-50 mb-3" />
      <p class="text-sm text-on-surface-variant">没有找到相关的模型</p>
      <n-button type="primary" size="small" class="mt-4" @click="showUploadModal = true">
         立即上传
      </n-button>
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

    <!-- Load to Node Modal -->
    <n-modal v-model:show="showLoadModal" preset="card" title="加载到节点" class="w-[400px]">
      <div v-if="loadTarget" class="space-y-4">
        <div class="text-sm">
          将模型 <span class="font-semibold text-primary">{{ loadTarget.name }}</span> 加载到推理节点
        </div>
        <n-spin :show="loadingNodes" description="加载节点列表...">
          <div class="space-y-4">
            <!-- Node selection -->
            <div>
              <div class="mb-2 text-xs font-medium text-on-surface-variant">选择目标节点</div>
              <div class="space-y-2 max-h-48 overflow-y-auto pr-1">
                <div
                  v-for="node in onlineNodes"
                  :key="node.id"
                  class="flex items-center gap-3 rounded-lg px-3 py-2 cursor-pointer transition-colors"
                  :class="selectedNodeId === node.id ? 'bg-primary/10 ring-1 ring-primary/30' : 'bg-bg-floor hover:bg-bg-active'"
                  @click="selectLoadNode(node)"
                >
                  <span class="inline-block h-2 w-2 rounded-full bg-green-500 shrink-0" />
                  <div class="flex-1 min-w-0">
                    <div class="text-sm font-medium">{{ node.nodeName }}</div>
                    <div class="text-xs text-on-surface-variant font-mono">{{ node.host }}:{{ node.port }}</div>
                  </div>
                  <n-tag size="tiny" :bordered="false" :type="node.deviceType === 'cuda' ? 'warning' : 'default'">
                    <template #icon>
                      <Icon :icon="node.deviceType === 'cuda' ? 'mdi:memory' : 'mdi:developer-board'" />
                    </template>
                    {{ node.deviceType === 'cuda' ? (node.gpuName ?? 'GPU') : 'CPU' }}
                  </n-tag>
                </div>
                <div v-if="onlineNodes.length === 0 && !loadingNodes" class="py-4 text-center text-xs text-on-surface-variant">
                  没有在线的推理节点
                </div>
              </div>
            </div>
            <!-- Device selection -->
            <div v-if="selectedNodeId">
              <div class="mb-2 text-xs font-medium text-on-surface-variant">选择运行设备</div>
              <n-radio-group v-model:value="loadDevice">
                <div class="space-y-2">
                  <n-radio value="cpu">
                    <div class="flex items-center gap-2">
                      <Icon icon="mdi:developer-board" class="text-base" />
                      <span>CPU</span>
                    </div>
                  </n-radio>
                  <n-radio value="cuda" :disabled="selectedNodeForLoad?.deviceType !== 'cuda'">
                    <div class="flex items-center gap-2">
                      <Icon icon="mdi:memory" class="text-base" />
                      <span>GPU{{ selectedNodeForLoad?.gpuName ? ` (${selectedNodeForLoad.gpuName})` : '' }}</span>
                      <n-tag v-if="selectedNodeForLoad?.deviceType !== 'cuda'" size="tiny" type="warning" :bordered="false">
                        不可用
                      </n-tag>
                    </div>
                  </n-radio>
                </div>
              </n-radio-group>
            </div>
          </div>
        </n-spin>
      </div>
      <template #action>
        <div class="flex justify-end gap-2">
          <n-button size="small" @click="showLoadModal = false">取消</n-button>
          <n-button
            type="primary" size="small"
            :loading="loadingModel"
            :disabled="!selectedNodeId"
            @click="confirmLoad"
          >
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
  NInput, NButton, NTag, NSlider, NFormItem, NInputNumber,
  NSelect, NModal, NForm, NUpload, useMessage, useDialog,
  NRadioGroup, NRadio, NSpin, NDivider
} from 'naive-ui'
import { Icon } from '@iconify/vue'
import { useModelStore } from '@/stores/model'
import { useNodeStore } from '@/stores/node'
import type { Model, ModelNodeDeployment, InferenceNode } from '@/types'

const modelStore = useModelStore()
const nodeStore = useNodeStore()
const message = useMessage()
const dialog = useDialog()
const searchQuery = ref('')

const filteredModels = computed(() => {
  const q = searchQuery.value.trim().toLowerCase()
  if (!q) return modelStore.models
  return modelStore.models.filter(m =>
    m.name.toLowerCase().includes(q) ||
    m.businessTag?.toLowerCase().includes(q) ||
    m.version?.toLowerCase().includes(q)
  )
})

function loadedDeployments(model: Model): ModelNodeDeployment[] {
  return model.deployments?.filter(d => d.status === 'loaded') ?? []
}

function parsedStatusType(status?: string): 'success' | 'warning' | 'error' | 'default' {
  if (status === 'parsed') return 'success'
  if (status === 'pending') return 'warning'
  if (status === 'failed') return 'error'
  return 'default'
}

function parsedStatusLabel(status?: string): string {
  if (status === 'parsed') return '已解析'
  if (status === 'pending') return '解析中'
  if (status === 'failed') return '解析失败'
  return '未知'
}

// ─── Local Draft Configs ──────────────────────────────────────
const draftConfigs = reactive<Record<string, { confidenceThreshold: number, maxConcurrency: number }>>({})

function getDraftConfig(model: Model) {
  if (!draftConfigs[model.id]) {
    draftConfigs[model.id] = {
      confidenceThreshold: model.confidenceThreshold,
      maxConcurrency: model.maxConcurrency,
    }
  }
  return draftConfigs[model.id]
}

function hasConfigChanges(modelId: string) {
  const model = modelStore.models.find(m => m.id === modelId)
  if (!model || !draftConfigs[modelId]) return false
  const draft = draftConfigs[modelId]
  return draft.confidenceThreshold !== model.confidenceThreshold ||
         draft.maxConcurrency !== model.maxConcurrency
}

const saving = ref<string | null>(null)

async function handleSave(model: Model) {
  const draft = draftConfigs[model.id]
  if (!draft) return
  saving.value = model.id
  try {
    await modelStore.updateModelConfig(model.id, {
      confidenceThreshold: draft.confidenceThreshold,
      maxConcurrency: draft.maxConcurrency,
    })
    message.success(`${model.name} 配置已保存`)
  } catch (e: any) {
    message.error(e?.message || '保存失败')
  } finally {
    saving.value = null
  }
}

function handleDiscard(modelId: string) {
  const model = modelStore.models.find(m => m.id === modelId)
  if (!model) return
  draftConfigs[modelId] = {
    confidenceThreshold: model.confidenceThreshold,
    maxConcurrency: model.maxConcurrency,
  }
}


// ─── Upload ───────────────────────────────────────────────────
const showUploadModal = ref(false)
const uploading = ref(false)
const uploadFile = ref<File | null>(null)
const uploadForm = reactive({
  name: '',
  version: '',
  taskType: 'detect' as string,
  businessTag: null as string | null,
  targetHardware: '',
  author: '',
})

const businessTagOptions = [
  { label: '排水转型', value: 'WATER' },
  { label: '燃气专项', value: 'GAS' },
  { label: '桥梁专项', value: 'BRIDGE' },
]

const taskTypeOptions = [
  { label: '目标检测 (Detection)', value: 'detect' },
  { label: '实例分割 (Segmentation)', value: 'segment' },
  { label: '图像分类 (Classification)', value: 'classify' },
  { label: '姿态估计 (Pose)', value: 'pose' },
]

async function handleUpload() {
  if (!uploadFile.value) { message.warning('请选择模型文件'); return }
  if (!uploadForm.name.trim() || !uploadForm.version.trim()) {
    message.warning('请填写模型名称和版本号'); return
  }
  uploading.value = true
  try {
    await modelStore.uploadModel(uploadFile.value, {
      name: uploadForm.name,
      version: uploadForm.version,
      taskType: uploadForm.taskType || 'detect',
      businessTag: uploadForm.businessTag ?? undefined,
      engineSupport: 'Ultralytics',
      targetHardware: uploadForm.targetHardware || undefined,
      author: uploadForm.author || undefined,
    })
    message.success('模型上传成功，正在解析元数据...')
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
  uploadForm.targetHardware = ''
  uploadForm.author = ''
  uploadFile.value = null
}

onMounted(() => {
  modelStore.fetchModels()
})

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
    detect: '目标检测', segment: '实例分割', classify: '图像分类', pose: '姿态估计',
  }
  return labels[taskType ?? 'detect'] ?? '目标检测'
}

function getTaskTypeTagType(taskType?: string): 'success' | 'warning' | 'info' | 'error' {
  const types: Record<string, 'success' | 'warning' | 'info' | 'error'> = {
    detect: 'success', segment: 'warning', classify: 'info', pose: 'error',
  }
  return types[taskType ?? 'detect'] ?? 'success'
}

// ─── Load to Node ─────────────────────────────────────────────
const showLoadModal = ref(false)
const loadTarget = ref<Model | null>(null)
const loadDevice = ref<'cpu' | 'cuda'>('cpu')
const selectedNodeId = ref<string | null>(null)
const loadingNodes = ref(false)
const loadingModel = ref(false)

const onlineNodes = computed(() =>
  nodeStore.nodes.filter(n => n.status === 'online')
)

const selectedNodeForLoad = computed<InferenceNode | null>(() =>
  onlineNodes.value.find(n => n.id === selectedNodeId.value) ?? null
)

function selectLoadNode(node: InferenceNode) {
  selectedNodeId.value = node.id
  loadDevice.value = node.deviceType === 'cuda' ? 'cuda' : 'cpu'
}

async function handleLoad(model: Model) {
  loadTarget.value = model
  selectedNodeId.value = null
  loadDevice.value = 'cpu'
  showLoadModal.value = true
  loadingNodes.value = true
  try {
    await nodeStore.fetchNodes()
    if (onlineNodes.value.length === 1) {
      selectLoadNode(onlineNodes.value[0])
    }
  } finally {
    loadingNodes.value = false
  }
}

async function confirmLoad() {
  if (!loadTarget.value || !selectedNodeId.value) return
  loadingModel.value = true
  try {
    const deviceName = loadDevice.value === 'cuda' ? selectedNodeForLoad.value?.gpuName ?? undefined : undefined
    await modelStore.loadModel(loadTarget.value.id, loadDevice.value, deviceName, selectedNodeId.value)
    message.success(`模型 ${loadTarget.value.name} 已开始加载`)
    showLoadModal.value = false
  } catch (e: any) {
    message.error(e?.message || '加载失败')
  } finally {
    loadingModel.value = false
  }
}

// ─── Unload ────────────────────────────────────────────────────
function handleUnloadAll(model: Model) {
  dialog.warning({
    title: '确认全部卸载',
    content: `确定要从所有节点卸载模型「${model.name}」吗？`,
    positiveText: '卸载',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await modelStore.unloadModel(model.id)
        message.success(`模型 ${model.name} 已从所有节点卸载`)
      } catch (e: any) {
        message.error(e?.message || '卸载失败')
      }
    },
  })
}

async function handleUnloadNode(model: Model, nodeId: string) {
  try {
    await modelStore.unloadModel(model.id, nodeId)
    message.success('节点卸载成功')
    await modelStore.fetchModels()
  } catch (e: any) {
    message.error(e?.message || '卸载失败')
  }
}

// ─── Delete ────────────────────────────────────────────────────
function handleDelete(model: Model) {
  if (model.deployments && model.deployments.length > 0) {
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
        message.success('模型已删除')
      } catch (e: any) {
        message.error(e?.message || '删除失败')
      }
    },
  })
}
</script>