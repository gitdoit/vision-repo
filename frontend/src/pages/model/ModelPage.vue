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
          v-for="model in filteredModels"
          :key="model.id"
          class="cursor-pointer rounded-xl p-5 transition-colors"
          :class="selectedId === model.id ? 'bg-bg-active ring-1 ring-primary/30' : 'bg-bg-card hover:bg-bg-active'"
          @click="selectModelCard(model)"
        >
          <!-- Name -->
          <div class="flex items-center gap-2">
            <Icon :icon="getModelIcon(model.businessTag)" class="text-lg text-primary" />
            <h4 class="text-sm font-semibold">{{ model.name }}</h4>
          </div>
          <!-- Tags row -->
          <div class="mt-2 flex items-center gap-2 flex-wrap">
            <n-tag size="tiny" :bordered="false" type="info">{{ model.businessTag }} {{ model.version }}</n-tag>
            <n-tag size="tiny" :bordered="false" :type="getTaskTypeTagType(model.taskType)">
              {{ getTaskTypeLabel(model.taskType) }}
            </n-tag>
            <n-tag size="tiny" :bordered="false" :type="parsedStatusType(model.parsedStatus)">
              {{ parsedStatusLabel(model.parsedStatus) }}
            </n-tag>
          </div>
          <!-- Deployment summary -->
          <div class="mt-2">
            <n-tag v-if="loadedDeployments(model).length > 0" size="tiny" :bordered="false" type="success">
              已部署 {{ loadedDeployments(model).length }} 节点
            </n-tag>
            <n-tag v-else size="tiny" :bordered="false" type="default">未部署</n-tag>
          </div>
          <!-- Deployment node chips -->
          <div v-if="loadedDeployments(model).length > 0" class="mt-2 flex flex-wrap gap-1">
            <n-tag
              v-for="dep in loadedDeployments(model)"
              :key="dep.id"
              size="tiny"
              :bordered="false"
              :type="dep.device === 'cuda' ? 'warning' : 'default'"
            >
              <template #icon>
                <Icon :icon="dep.device === 'cuda' ? 'mdi:memory' : 'mdi:developer-board'" />
              </template>
              {{ dep.nodeName }}
            </n-tag>
          </div>
          <!-- Actions -->
          <div class="mt-3 flex gap-2 flex-wrap">
            <n-button size="tiny" quaternary type="primary" @click.stop="handleLoad(model)">
              <template #icon><Icon icon="mdi:play-arrow" /></template>
              加载到节点
            </n-button>
            <n-button
              v-if="loadedDeployments(model).length > 0"
              size="tiny" quaternary type="warning"
              @click.stop="handleUnloadAll(model)"
            >
              <template #icon><Icon icon="mdi:eject" /></template>
              全部卸载
            </n-button>
            <n-button size="tiny" quaternary @click.stop="selectModelCard(model)">
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
      <div v-if="selectedModel" class="flex-1 rounded-xl bg-bg-card p-6 overflow-y-auto max-h-[calc(100vh-160px)]">
        <div class="mb-6 flex items-center justify-between">
          <h3 class="text-lg font-semibold">模型配置详情</h3>
          <button class="text-on-surface-variant hover:text-on-surface" @click="selectedId = null">
            <Icon icon="mdi:close" class="text-xl" />
          </button>
        </div>

        <!-- 基本信息 -->
        <section class="mb-6">
          <h4 class="mb-3 text-sm font-semibold text-on-surface-variant">基本信息</h4>
          <div class="grid grid-cols-2 gap-3">
            <InfoRow label="模型路径" :value="selectedModel.modelPath ?? ''" />
            <InfoRow label="创建时间" :value="selectedModel.createdAt ?? ''" />
            <InfoRow label="作者" :value="selectedModel.author ?? ''" />
            <div class="rounded-lg bg-bg-floor px-4 py-2">
              <div class="text-xs text-on-surface-variant">解析状态</div>
              <div class="mt-1">
                <n-tag size="small" :bordered="false" :type="parsedStatusType(selectedModel.parsedStatus)">
                  {{ parsedStatusLabel(selectedModel.parsedStatus) }}
                </n-tag>
              </div>
            </div>
            <div class="rounded-lg bg-bg-floor px-4 py-2">
              <div class="text-xs text-on-surface-variant">分类数量</div>
              <div class="mt-0.5 text-sm font-semibold text-on-surface">{{ selectedModel.numClasses ?? 0 }} 类</div>
            </div>
          </div>
        </section>

        <!-- 模型分类名称 -->
        <section v-if="selectedModel.classNames?.length" class="mb-6">
          <h4 class="mb-3 text-sm font-semibold text-on-surface-variant">模型分类名称</h4>
          <div class="flex flex-wrap gap-2 rounded-lg bg-bg-floor p-3">
            <n-tag
              v-for="cls in selectedModel.classNames"
              :key="cls"
              size="small"
              :bordered="false"
              type="info"
            >{{ cls }}</n-tag>
          </div>
        </section>
        <section v-else-if="selectedModel.parsedStatus === 'pending'" class="mb-6">
          <h4 class="mb-3 text-sm font-semibold text-on-surface-variant">模型分类名称</h4>
          <div class="rounded-lg bg-bg-floor p-3 text-xs text-on-surface-variant">
            解析中，等待推理节点上线后自动解析...
          </div>
        </section>

        <!-- 部署节点 -->
        <section class="mb-6">
          <div class="mb-3 flex items-center justify-between">
            <h4 class="text-sm font-semibold text-on-surface-variant">部署节点</h4>
            <n-button
              v-if="selectedModel.deployments?.length"
              size="tiny" type="warning" quaternary
              @click="handleUnloadAll(selectedModel)"
            >
              <template #icon><Icon icon="mdi:eject-outline" /></template>
              全部卸载
            </n-button>
          </div>
          <div v-if="!selectedModel.deployments?.length" class="rounded-lg bg-bg-floor p-3 text-xs text-on-surface-variant">
            该模型尚未部署到任何节点
          </div>
          <div v-else class="space-y-2">
            <div
              v-for="dep in selectedModel.deployments"
              :key="dep.id"
              class="flex items-center gap-3 rounded-lg bg-bg-floor px-4 py-2"
            >
              <Icon icon="mdi:server" class="text-sm text-on-surface-variant shrink-0" />
              <span class="flex-1 text-sm">{{ dep.nodeName }}</span>
              <n-tag size="tiny" :bordered="false" :type="dep.device === 'cuda' ? 'warning' : 'default'">
                <template #icon>
                  <Icon :icon="dep.device === 'cuda' ? 'mdi:memory' : 'mdi:developer-board'" />
                </template>
                {{ dep.device === 'cuda' ? (dep.deviceName ?? 'GPU') : 'CPU' }}
              </n-tag>
              <n-tag size="tiny" :bordered="false" :type="deployStatusType(dep.status)">
                {{ deployStatusLabel(dep.status) }}
              </n-tag>
              <n-button size="tiny" quaternary type="warning" @click="handleUnloadNode(selectedModel, dep.nodeId)">
                <template #icon><Icon icon="mdi:eject" /></template>
                卸载
              </n-button>
            </div>
          </div>
        </section>

        <!-- 版本历史 -->
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

        <!-- 动态参数 -->
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
    <n-modal v-model:show="showLoadModal" preset="card" title="加载到节点" class="w-[500px]">
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
  NInput, NButton, NTag, NSlider, NFormItem, NInputNumber, NSelect,
  NModal, NForm, NUpload, useMessage, useDialog, NRadioGroup, NRadio, NSpin,
} from 'naive-ui'
import { Icon } from '@iconify/vue'
import { useModelStore } from '@/stores/model'
import { useNodeStore } from '@/stores/node'
import type { Model, ModelNodeDeployment, InferenceNode } from '@/types'
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

function deployStatusType(status?: string): 'success' | 'warning' | 'error' | 'default' {
  if (status === 'loaded') return 'success'
  if (status === 'loading') return 'warning'
  if (status === 'error') return 'error'
  return 'default'
}

function deployStatusLabel(status?: string): string {
  if (status === 'loaded') return '已加载'
  if (status === 'loading') return '加载中'
  if (status === 'error') return '错误'
  return status ?? ''
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

const selectedModel = computed(() =>
  modelStore.models.find(m => m.id === selectedId.value) ?? null
)

function selectModelCard(model: Model) {
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
        if (selectedId.value === model.id) selectedId.value = null
        message.success('模型已删除')
      } catch (e: any) {
        message.error(e?.message || '删除失败')
      }
    },
  })
}
</script>
