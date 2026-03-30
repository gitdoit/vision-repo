<template>
  <n-modal v-model:show="visible" preset="card" title="模型测试" :style="{ width: '860px' }" @after-leave="handleClose">
    <div class="flex gap-4">
      <!-- Left: Video + Capture -->
      <div class="flex-1 space-y-3">
        <div class="relative">
          <!-- Live Video -->
          <div v-show="!capturedImage">
            <FlvPlayer v-if="visible && camera?.streamUrl" ref="playerRef" :url="camera.streamUrl" />
            <div v-else class="flex aspect-video items-center justify-center rounded-lg bg-black">
              <div class="text-center text-white">
                <Icon icon="mdi:video-off" class="text-4xl" />
                <p class="mt-2 text-xs">该摄像头暂无视频流地址</p>
              </div>
            </div>
          </div>
          <!-- Captured Frame with overlays -->
          <div v-if="capturedImage" class="relative">
            <img :src="capturedImage" class="w-full rounded-lg" alt="截取帧" />

            <!-- Segmentation masks (colorized per-object overlays) -->
            <template v-if="testResult && capturedSize && testResult.objects.some(o => o.mask)">
              <canvas
                v-for="(det, idx) in testResult.objects.filter(o => o.mask)"
                :key="'mask-' + idx"
                :ref="(el) => colorizeMask(el as HTMLCanvasElement, det.mask!, boxColors[idx % boxColors.length])"
                class="absolute inset-0 h-full w-full rounded-lg pointer-events-none"
              />
            </template>

            <!-- SVG overlay: boxes, labels, keypoints -->
            <svg
              v-if="testResult && capturedSize && resultTaskType !== 'classify'"
              class="absolute inset-0 h-full w-full"
              :viewBox="`0 0 ${capturedSize.width} ${capturedSize.height}`"
              preserveAspectRatio="xMidYMid meet"
            >
              <g v-for="(det, idx) in testResult.objects" :key="idx">
                <!-- Bounding box -->
                <rect
                  :x="det.bbox[0]" :y="det.bbox[1]"
                  :width="det.bbox[2] - det.bbox[0]" :height="det.bbox[3] - det.bbox[1]"
                  fill="none" :stroke="boxColors[idx % boxColors.length]" stroke-width="2"
                />
                <!-- Label background -->
                <rect
                  :x="det.bbox[0]" :y="Math.max(0, det.bbox[1] - 20)"
                  :width="Math.max(60, (det.label.length + 6) * 8)" height="20"
                  :fill="boxColors[idx % boxColors.length]" rx="2"
                />
                <!-- Label text -->
                <text
                  :x="det.bbox[0] + 4" :y="Math.max(0, det.bbox[1] - 20) + 14"
                  fill="white" font-size="12" font-family="monospace"
                >{{ det.label }} {{ (det.confidence * 100).toFixed(1) }}%</text>

                <!-- Pose keypoints -->
                <template v-if="resultTaskType === 'pose' && det.keypoints">
                  <!-- Skeleton lines -->
                  <line
                    v-for="([a, b], si) in skeletonPairs"
                    :key="'sk-' + idx + '-' + si"
                    v-show="det.keypoints![a] && det.keypoints![b] && det.keypoints![a][2] > 0.3 && det.keypoints![b][2] > 0.3"
                    :x1="det.keypoints![a][0]" :y1="det.keypoints![a][1]"
                    :x2="det.keypoints![b][0]" :y2="det.keypoints![b][1]"
                    stroke="#facc15" stroke-width="2" stroke-linecap="round"
                  />
                  <!-- Keypoint dots -->
                  <circle
                    v-for="(kpt, ki) in det.keypoints"
                    :key="'kpt-' + idx + '-' + ki"
                    v-show="kpt[2] > 0.3"
                    :cx="kpt[0]" :cy="kpt[1]" r="4"
                    fill="#f43f5e" stroke="white" stroke-width="1.5"
                  />
                </template>
              </g>
            </svg>
          </div>
        </div>
        <div class="flex gap-2">
          <n-button v-if="!capturedImage" :disabled="!camera?.streamUrl" @click="captureFrame">
            <template #icon><Icon icon="mdi:camera" /></template>
            截帧
          </n-button>
          <n-button v-if="capturedImage" @click="resetCapture">
            <template #icon><Icon icon="mdi:refresh" /></template>
            重新截帧
          </n-button>
          <n-button
            type="primary"
            :disabled="!capturedImage || !selectedModelId"
            :loading="testing"
            @click="runTest"
          >
            <template #icon><Icon icon="mdi:play" /></template>
            运行测试
          </n-button>
        </div>
      </div>

      <!-- Right: Config + Results -->
      <div class="w-72 shrink-0 space-y-4">
        <!-- Model Selection -->
        <div>
          <p class="mb-1.5 text-xs font-semibold text-on-surface-variant">选择模型</p>
          <n-select
            v-model:value="selectedModelId"
            :options="modelOptions"
            placeholder="请选择已加载的模型"
            size="small"
            :loading="loadingModels"
            :render-label="renderModelLabel"
          />
          <p v-if="loadedModels.length === 0 && !loadingModels" class="mt-1 text-xs text-warning">
            暂无已加载模型，请先在模型管理中加载模型
          </p>
        </div>

        <!-- Selected model task type hint -->
        <div v-if="selectedModelTaskType" class="flex items-center gap-2">
          <n-tag size="small" :bordered="false" :type="taskTypeTagType(selectedModelTaskType)">
            {{ taskTypeLabel(selectedModelTaskType) }}
          </n-tag>
        </div>

        <!-- Confidence Threshold (not for classify) -->
        <div v-if="selectedModelTaskType !== 'classify'">
          <p class="mb-1.5 text-xs font-semibold text-on-surface-variant">置信度阈值</p>
          <n-slider v-model:value="confidenceThreshold" :min="0.1" :max="0.99" :step="0.05" :format-tooltip="(v: number) => (v * 100).toFixed(0) + '%'" />
          <p class="mt-0.5 text-right text-xs text-on-surface-variant">{{ (confidenceThreshold * 100).toFixed(0) }}%</p>
        </div>

        <!-- Test Results -->
        <div v-if="testResult" class="space-y-2">
          <div class="flex items-center justify-between">
            <p class="text-xs font-semibold text-on-surface-variant">测试结果</p>
            <n-tag size="small" :bordered="false" type="success">
              {{ testResult.inferenceTimeMs.toFixed(1) }}ms
            </n-tag>
          </div>

          <!-- Classification results -->
          <template v-if="resultTaskType === 'classify'">
            <div v-if="!testResult.classifications?.length" class="rounded-lg bg-bg-void p-4 text-center text-xs text-on-surface-variant">
              未能识别出类别
            </div>
            <div v-else class="max-h-72 space-y-1.5 overflow-y-auto">
              <div
                v-for="(cls, idx) in testResult.classifications"
                :key="idx"
                class="flex items-center justify-between rounded-lg bg-bg-void px-3 py-2"
              >
                <div class="flex items-center gap-2">
                  <span class="inline-flex h-5 w-5 items-center justify-center rounded bg-primary/20 text-xs font-bold text-primary">
                    {{ idx + 1 }}
                  </span>
                  <span class="text-sm">{{ cls.label }}</span>
                </div>
                <div class="flex items-center gap-2">
                  <div class="h-1.5 w-16 rounded-full bg-bg-void overflow-hidden">
                    <div class="h-full rounded-full bg-primary" :style="{ width: (cls.confidence * 100) + '%' }" />
                  </div>
                  <span class="text-xs font-mono text-on-surface-variant w-12 text-right">
                    {{ (cls.confidence * 100).toFixed(1) }}%
                  </span>
                </div>
              </div>
            </div>
            <div class="rounded-lg border border-border p-2 text-xs text-on-surface-variant">
              <p>识别出 <strong>{{ testResult.classifications?.length ?? 0 }}</strong> 个类别</p>
              <p>推理耗时 <strong>{{ testResult.inferenceTimeMs.toFixed(1) }}ms</strong></p>
            </div>
          </template>

          <!-- Detection / Segment / Pose results -->
          <template v-else>
            <div v-if="testResult.objects.length === 0" class="rounded-lg bg-bg-void p-4 text-center text-xs text-on-surface-variant">
              未检测到目标对象
            </div>
            <div v-else class="max-h-72 space-y-1.5 overflow-y-auto">
              <div
                v-for="(det, idx) in testResult.objects"
                :key="idx"
                class="flex items-center justify-between rounded-lg bg-bg-void px-3 py-2"
              >
                <div class="flex items-center gap-2">
                  <span
                    class="inline-flex h-5 w-5 items-center justify-center rounded text-xs font-bold text-white"
                    :style="{ backgroundColor: boxColors[idx % boxColors.length] }"
                  >
                    {{ idx + 1 }}
                  </span>
                  <span class="text-sm">{{ det.label }}</span>
                  <Icon v-if="det.mask" icon="mdi:texture-box" class="text-xs text-on-surface-variant" title="含分割掩码" />
                  <Icon v-if="resultTaskType === 'pose' && det.keypoints" icon="mdi:human" class="text-xs text-on-surface-variant" title="含关键点" />
                </div>
                <n-tag
                  size="small" :bordered="false"
                  :type="det.confidence >= 0.8 ? 'success' : det.confidence >= 0.5 ? 'warning' : 'error'"
                >
                  {{ (det.confidence * 100).toFixed(1) }}%
                </n-tag>
              </div>
            </div>
            <div class="rounded-lg border border-border p-2 text-xs text-on-surface-variant">
              <p>检测到 <strong>{{ testResult.objects.length }}</strong> 个目标</p>
              <p v-if="testResult.objects.some(o => o.mask)">其中 <strong>{{ testResult.objects.filter(o => o.mask).length }}</strong> 个含分割掩码</p>
              <p v-if="resultTaskType === 'pose'">其中 <strong>{{ testResult.objects.filter(o => o.keypoints?.length).length }}</strong> 个含关键点</p>
              <p>推理耗时 <strong>{{ testResult.inferenceTimeMs.toFixed(1) }}ms</strong></p>
            </div>
          </template>
        </div>

        <!-- Error Message -->
        <div v-if="testError" class="rounded-lg bg-red-500/10 p-3 text-xs text-red-400">
          {{ testError }}
        </div>
      </div>
    </div>
  </n-modal>
</template>

<script setup lang="ts">
import { ref, computed, watch, h } from 'vue'
import { NModal, NSelect, NButton, NTag, NSlider, useMessage } from 'naive-ui'
import type { SelectOption } from 'naive-ui'
import { Icon } from '@iconify/vue'
import FlvPlayer from '@/components/FlvPlayer.vue'
import type { Camera, Model, ModelTestResult } from '@/types'
import * as modelApi from '@/api/modules/model'
import * as inferenceApi from '@/api/modules/inference'

const props = defineProps<{
  camera: Camera | null
}>()

const visible = defineModel<boolean>('show', { default: false })
const message = useMessage()

const playerRef = ref<InstanceType<typeof FlvPlayer> | null>(null)
const selectedModelId = ref<string | null>(null)
const confidenceThreshold = ref(0.5)
const capturedImage = ref<string | null>(null)
const capturedBlob = ref<Blob | null>(null)
const capturedSize = ref<{ width: number; height: number } | null>(null)
const testing = ref(false)
const testResult = ref<ModelTestResult | null>(null)
const testError = ref('')

// Models
const loadedModels = ref<Model[]>([])
const loadingModels = ref(false)

// Box colors for different detections
const boxColors = ['#22c55e', '#3b82f6', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#14b8a6', '#f97316']

/** Parse hex color to [r,g,b] */
function hexToRgb(hex: string): [number, number, number] {
  const v = parseInt(hex.replace('#', ''), 16)
  return [(v >> 16) & 255, (v >> 8) & 255, v & 255]
}

/** Draw a colorized semi-transparent mask onto a canvas element */
function colorizeMask(canvas: HTMLCanvasElement | null, maskB64: string, color: string) {
  if (!canvas || !maskB64) return
  const img = new Image()
  img.onload = () => {
    canvas.width = img.width
    canvas.height = img.height
    const ctx = canvas.getContext('2d')!
    ctx.clearRect(0, 0, img.width, img.height)
    ctx.drawImage(img, 0, 0)
    const imageData = ctx.getImageData(0, 0, img.width, img.height)
    const data = imageData.data
    const [r, g, b] = hexToRgb(color)
    for (let i = 0; i < data.length; i += 4) {
      const alpha = data[i] // grayscale mask: use R channel as intensity
      if (alpha > 30) {
        data[i] = r
        data[i + 1] = g
        data[i + 2] = b
        data[i + 3] = Math.round(alpha * 0.5) // 50% opacity
      } else {
        data[i + 3] = 0 // fully transparent
      }
    }
    ctx.putImageData(imageData, 0, 0)
  }
  img.src = 'data:image/png;base64,' + maskB64
}

// COCO skeleton pairs for pose visualization
const skeletonPairs: [number, number][] = [
  [5, 6], [5, 7], [7, 9], [6, 8], [8, 10], // arms
  [5, 11], [6, 12], [11, 12], // torso
  [11, 13], [13, 15], [12, 14], [14, 16], // legs
  [0, 1], [0, 2], [1, 3], [2, 4], // face
]

const taskTypeLabelMap: Record<string, string> = {
  detect: '目标检测',
  segment: '实例分割',
  classify: '图像分类',
  pose: '姿态估计',
}

function taskTypeLabel(t?: string) {
  return taskTypeLabelMap[t ?? 'detect'] ?? '目标检测'
}

function taskTypeTagType(t?: string): 'success' | 'warning' | 'info' | 'error' {
  const map: Record<string, 'success' | 'warning' | 'info' | 'error'> = {
    detect: 'success', segment: 'warning', classify: 'info', pose: 'error',
  }
  return map[t ?? 'detect'] ?? 'success'
}

const selectedModel = computed(() =>
  loadedModels.value.find(m => m.id === selectedModelId.value) ?? null
)

const selectedModelTaskType = computed(() =>
  selectedModel.value?.taskType ?? null
)

const resultTaskType = computed(() =>
  testResult.value?.taskType ?? selectedModelTaskType.value ?? 'detect'
)

// Auto-adjust confidence threshold when model selection changes
watch(selectedModelId, (newId) => {
  if (!newId) return
  const model = loadedModels.value.find(m => m.id === newId)
  if (model?.confidenceThreshold) {
    confidenceThreshold.value = model.confidenceThreshold
  }
})

const modelOptions = computed(() =>
  loadedModels.value.map(m => ({
    label: `${m.name} (${m.version})`,
    value: m.id,
    taskType: m.taskType,
  })),
)

function renderModelLabel(option: SelectOption) {
  const taskType = (option as { taskType?: string }).taskType
  return h('div', { class: 'flex items-center gap-2' }, [
    h('span', option.label as string),
    taskType
      ? h(NTag, { size: 'tiny', bordered: false, type: taskTypeTagType(taskType) },
          { default: () => taskTypeLabel(taskType) })
      : null,
  ])
}

watch(visible, async (show) => {
  if (show) {
    loadingModels.value = true
    try {
      const res = await modelApi.getModels() as unknown as { items: Model[] } | Model[]
      const list = Array.isArray(res) ? res : res.items
      loadedModels.value = list.filter(m => m.status === 'loaded')
    } catch {
      loadedModels.value = []
    } finally {
      loadingModels.value = false
    }
  }
})

function captureFrame() {
  const playerEl = playerRef.value?.$el as HTMLElement | undefined
  if (!playerEl) {
    message.warning('视频播放器未就绪')
    return
  }
  const video = playerEl.querySelector('video') as HTMLVideoElement | null
  if (!video || video.readyState < 2) {
    message.warning('视频流尚未加载，请稍后重试')
    return
  }

  const canvas = document.createElement('canvas')
  canvas.width = video.videoWidth
  canvas.height = video.videoHeight
  const ctx = canvas.getContext('2d')!
  ctx.drawImage(video, 0, 0)

  capturedSize.value = { width: video.videoWidth, height: video.videoHeight }
  capturedImage.value = canvas.toDataURL('image/jpeg', 0.9)

  canvas.toBlob((blob) => {
    capturedBlob.value = blob
  }, 'image/jpeg', 0.9)

  testResult.value = null
  testError.value = ''
}

function resetCapture() {
  capturedImage.value = null
  capturedBlob.value = null
  capturedSize.value = null
  testResult.value = null
  testError.value = ''
}

async function runTest() {
  if (!capturedBlob.value || !selectedModelId.value) return

  testing.value = true
  testResult.value = null
  testError.value = ''

  try {
    const formData = new FormData()
    formData.append('image', capturedBlob.value, 'frame.jpg')
    formData.append('modelId', selectedModelId.value)
    formData.append('confidenceThreshold', String(confidenceThreshold.value))

    const res = await inferenceApi.testInference(formData) as unknown as Record<string, unknown>
    // Normalize snake_case keys from Python/backend to camelCase
    testResult.value = {
      taskType: (res.taskType ?? res.task_type ?? 'detect') as ModelTestResult['taskType'],
      inferenceTimeMs: (res.inferenceTimeMs ?? res.inference_time_ms ?? 0) as number,
      objects: ((res.objects ?? []) as Record<string, unknown>[]).map(o => ({
        label: o.label as string,
        confidence: o.confidence as number,
        bbox: o.bbox as number[],
        mask: o.mask as string | undefined,
        keypoints: o.keypoints as number[][] | undefined,
      })),
      classifications: res.classifications
        ? ((res.classifications as Record<string, unknown>[]).map(c => ({
            label: c.label as string,
            confidence: c.confidence as number,
          })))
        : undefined,
    }
  } catch (e: unknown) {
    testError.value = '测试失败: ' + (e instanceof Error ? e.message : String(e))
  } finally {
    testing.value = false
  }
}

function handleClose() {
  resetCapture()
  selectedModelId.value = null
  confidenceThreshold.value = 0.5
}
</script>
