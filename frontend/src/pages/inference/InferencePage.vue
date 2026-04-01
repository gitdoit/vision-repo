<template>
  <div class="space-y-6">
    <!-- Header -->
    <div>
      <p class="text-sm text-on-surface-variant">检索全量推理结果及告警详情记录</p>
    </div>

    <!-- Filters -->
    <div class="flex items-end gap-4 rounded-xl bg-bg-card p-4">
      <div class="flex items-center gap-3">
        <div class="flex gap-1">
          <button
            v-for="range in timeRanges"
            :key="range.value"
            class="rounded-full px-3 py-1 text-xs transition-colors"
            :class="selectedTimeRange === range.value ? 'bg-primary/15 text-primary' : 'text-on-surface-variant hover:bg-bg-active'"
            @click="selectedTimeRange = range.value"
          >
            {{ range.label }}
          </button>
        </div>
      </div>
      <n-select v-model:value="filterBusiness" :options="businessOptions" placeholder="业务类型" size="small" class="w-40" clearable />
      <n-input v-model:value="filterCamera" placeholder="摄像头 ID" size="small" class="w-40" />
      <div class="flex items-center gap-2 text-xs text-on-surface-variant">
        <span>置信度范围</span>
        <span class="font-mono text-primary">{{ confidenceRange[0].toFixed(2) }} - {{ confidenceRange[1].toFixed(2) }}</span>
      </div>
      <n-slider
        v-model:value="confidenceRange"
        range
        :min="0" :max="1" :step="0.05"
        class="w-40"
      />
      <n-button size="small" quaternary @click="resetFilters">重置条件</n-button>
      <n-button size="small" type="primary">查询推理结果</n-button>
      <div class="ml-auto flex gap-2">
        <n-button size="small" quaternary>
          <template #icon><Icon icon="mdi:file-delimited" /></template>
          导出 CSV
        </n-button>
        <n-button size="small" quaternary>
          <template #icon><Icon icon="mdi:table" /></template>
          导出 Excel
        </n-button>
      </div>
    </div>

    <div class="flex gap-4">
      <!-- Record List -->
      <div class="flex-1 space-y-3">
        <div
          v-for="record in inferenceStore.records"
          :key="record.id"
          class="flex gap-4 rounded-xl p-4 transition-colors cursor-pointer"
          :class="selectedRecordId === record.id ? 'bg-bg-active ring-1 ring-primary/30' : 'bg-bg-card hover:bg-bg-active'"
          @click="selectRecord(record)"
        >
          <!-- Thumbnail -->
          <div class="w-40 h-24 rounded-lg bg-bg-void overflow-hidden shrink-0 relative">
            <img v-if="record.thumbnailUrl" :src="record.thumbnailUrl" class="w-full h-full object-cover" alt="" />
            <canvas
              v-if="hasSegmentMask(record)"
              :data-record-id="record.id"
              class="thumb-mask-canvas absolute inset-0 w-full h-full pointer-events-none"
            />
            <div v-if="!record.thumbnailUrl" class="w-full h-full flex items-center justify-center">
              <Icon icon="mdi:image" class="text-3xl text-on-surface-variant opacity-30" />
            </div>
          </div>

          <!-- Info -->
          <div class="flex-1 min-w-0">
            <div class="flex items-center gap-3">
              <h4 class="text-sm font-semibold">{{ record.eventId }}</h4>
              <n-tag
                size="tiny" :bordered="false"
                :type="alertType(record.alertStatus)"
              >
                {{ alertLabel(record.alertStatus) }}
              </n-tag>
            </div>
            <p class="text-xs text-on-surface-variant mt-1">{{ record.createdAt }}</p>
            <div class="mt-2 flex items-center gap-3 text-xs text-on-surface-variant">
              <span>平均置信度 <strong class="text-primary">{{ (record.avgConfidence ?? 0).toFixed(2) }}</strong></span>
              <span class="flex items-center gap-1">
                <Icon icon="mdi:video" class="text-sm" />
                {{ record.cameraId }}
              </span>
              <span class="flex items-center gap-1">
                <Icon icon="mdi:chart-line" class="text-sm" />
                {{ record.businessType }}
              </span>
            </div>
            <div class="mt-2 flex gap-1">
              <n-tag v-for="d in record.detections" :key="d.label" size="tiny" :bordered="false" type="warning">
                {{ d.label }} x{{ d.count }}
              </n-tag>
            </div>
          </div>

          <n-button size="tiny" quaternary>查看详情</n-button>
        </div>
      </div>

      <!-- Detail Panel -->
      <div v-if="selectedRecord" class="w-[480px] shrink-0 rounded-xl bg-bg-card p-6 max-h-[calc(100vh-220px)] overflow-y-auto">
        <div class="mb-4 flex items-center justify-between">
          <h3 class="text-lg font-semibold">推理详情 - {{ selectedRecord.eventId }}</h3>
          <button class="text-on-surface-variant hover:text-on-surface" @click="selectedRecordId = null">
            <Icon icon="mdi:close" class="text-xl" />
          </button>
        </div>

        <!-- Image Viewer -->
        <div class="mb-4">
          <div class="flex gap-2 mb-3">
            <button
              v-for="tab in ['原始画面', 'AI标注']"
              :key="tab"
              class="rounded-full px-3 py-1 text-xs transition-colors"
              :class="imageTab === tab ? 'bg-primary/15 text-primary' : 'text-on-surface-variant hover:bg-bg-active'"
              @click="imageTab = tab"
            >
              {{ tab }}
            </button>
            <div class="ml-auto flex gap-2">
              <button class="text-on-surface-variant hover:text-primary"><Icon icon="mdi:download" /></button>
              <button class="text-on-surface-variant hover:text-primary"><Icon icon="mdi:content-copy" /></button>
            </div>
          </div>
          <div class="rounded-lg bg-bg-void overflow-hidden flex items-center justify-center" style="min-height: 12rem">
            <img v-if="imageTab === '原始画面' && detailImageUrl" :src="detailImageUrl" class="w-full object-contain" alt="" />
            <canvas v-show="imageTab === 'AI标注'" ref="annotationCanvas" class="w-full object-contain" />
            <div v-if="imageTab === '原始画面' && !detailImageUrl" class="text-center py-8">
              <Icon icon="mdi:image" class="text-4xl text-on-surface-variant opacity-20" />
              <p class="mt-1 text-xs text-on-surface-variant">暂无图片</p>
            </div>
          </div>
        </div>

        <!-- Raw JSON -->
        <div class="mb-4">
          <h4 class="mb-2 text-sm font-semibold text-on-surface-variant">原始JSON结果</h4>
          <pre class="rounded-lg bg-bg-void p-4 text-xs font-mono text-primary/80 overflow-auto max-h-48">{{ selectedRecord.rawJson }}</pre>
        </div>

        <!-- Related Alerts -->
        <div v-if="selectedRecord.relatedAlerts?.length">
          <h4 class="mb-2 text-sm font-semibold text-on-surface-variant">关联告警</h4>
          <div class="space-y-2">
            <div
              v-for="alert in selectedRecord.relatedAlerts"
              :key="alert.title"
              class="flex items-center justify-between rounded-lg bg-bg-floor px-4 py-2"
            >
              <span class="text-sm">{{ alert.title }}</span>
              <span class="text-xs text-on-surface-variant">{{ alert.time }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import { NSelect, NInput, NSlider, NButton, NTag } from 'naive-ui'
import { Icon } from '@iconify/vue'
import { useInferenceStore } from '@/stores/inference'
import type { InferenceRecord } from '@/types'

const inferenceStore = useInferenceStore()
const selectedRecordId = ref<string | null>(null)
const selectedTimeRange = ref('today')
const filterBusiness = ref<string | null>(null)
const filterCamera = ref('')
const confidenceRange = ref<[number, number]>([0.65, 1.0])
const imageTab = ref('原始画面')

const selectedRecord = ref<InferenceRecord | null>(null)
const annotationCanvas = ref<HTMLCanvasElement | null>(null)

const detailImageUrl = computed(() => {
  if (!selectedRecord.value) return ''
  return selectedRecord.value.originalImageUrl || selectedRecord.value.thumbnailUrl || ''
})

onMounted(() => {
  inferenceStore.fetchRecords()
})

// Render thumbnail mask overlays after records load
watch(() => inferenceStore.records, async (records) => {
  if (!records.length) return
  await nextTick()
  for (const record of records) {
    if (!hasSegmentMask(record)) continue
    const el = document.querySelector(`canvas.thumb-mask-canvas[data-record-id="${record.id}"]`) as HTMLCanvasElement | null
    renderThumbnailOverlay(el, record)
  }
}, { immediate: true })

const timeRanges = [
  { label: '今日', value: 'today' },
  { label: '7日', value: '7d' },
  { label: '本月', value: 'month' },
]

const businessOptions = [
  { label: '工业安全', value: 'industrial' },
  { label: '安全合规', value: 'compliance' },
  { label: '消防监测', value: 'fire' },
  { label: '进入管控', value: 'access' },
]

function resetFilters() {
  selectedTimeRange.value = 'today'
  filterBusiness.value = null
  filterCamera.value = ''
  confidenceRange.value = [0.65, 1.0]
}

function selectRecord(record: InferenceRecord) {
  selectedRecordId.value = record.id
  selectedRecord.value = record
  imageTab.value = '原始画面'
}

watch(imageTab, async (tab) => {
  if (tab === 'AI标注' && selectedRecord.value) {
    await nextTick()
    if (annotationCanvas.value) {
      await renderAnnotation(annotationCanvas.value, selectedRecord.value)
    }
  }
})

function alertType(status: string): 'success' | 'warning' | 'error' {
  if (status === 'alert') return 'error'
  if (status === 'warning') return 'warning'
  return 'success'
}

function alertLabel(status: string) {
  if (status === 'alert') return '告警'
  if (status === 'warning') return '警告'
  return '正常'
}

// --- Mask rendering ---

interface RawJsonObject {
  bbox?: number[]
  mask?: string
  label?: string
  confidence?: number
}
interface RawJsonResult {
  objects?: RawJsonObject[]
  task_type?: string
}

const MASK_COLORS = [
  { fill: [0, 150, 255], stroke: '#0096ff' },
  { fill: [255, 100, 0], stroke: '#ff6400' },
  { fill: [0, 220, 100], stroke: '#00dc64' },
  { fill: [255, 0, 150], stroke: '#ff0096' },
]

function parseRawJson(rawJson: string | object): RawJsonResult | null {
  try {
    if (typeof rawJson === 'string') return JSON.parse(rawJson)
    return rawJson as RawJsonResult
  } catch { return null }
}

function hasSegmentMask(record: InferenceRecord): boolean {
  if (!record.rawJson) return false
  const parsed = parseRawJson(record.rawJson)
  return parsed?.task_type === 'segment' && (parsed.objects?.some(o => !!o.mask) ?? false)
}

const renderedThumbnails = new Set<string>()

function renderThumbnailOverlay(el: HTMLCanvasElement | null, record: InferenceRecord) {
  if (!el || renderedThumbnails.has(record.id)) return
  renderedThumbnails.add(record.id)

  const parsed = parseRawJson(record.rawJson)
  if (!parsed || parsed.task_type !== 'segment' || !parsed.objects) return

  const dpr = window.devicePixelRatio || 1
  const w = 160 * dpr
  const h = 96 * dpr
  el.width = w
  el.height = h
  const ctx = el.getContext('2d')!

  parsed.objects.forEach(async (obj, i) => {
    if (!obj.mask) return
    const color = MASK_COLORS[i % MASK_COLORS.length]
    try {
      const maskImg = await loadImage('data:image/png;base64,' + obj.mask)
      const maskData = decodeMaskToImageData(maskImg)
      const sx = w / maskData.width
      const sy = h / maskData.height

      renderMaskFill(ctx, maskData, color.fill, 100, w, h)
      drawMaskContours(ctx, maskData, color.stroke, sx, sy, Math.max(1, Math.round(2 * dpr)))
    } catch (e) {
      console.warn('Failed to render thumbnail mask:', e)
    }
  })
}

function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = () => resolve(img)
    img.onerror = reject
    img.crossOrigin = 'anonymous'
    img.src = src
  })
}

function decodeMaskToImageData(maskImg: HTMLImageElement): ImageData {
  const offCanvas = document.createElement('canvas')
  offCanvas.width = maskImg.naturalWidth
  offCanvas.height = maskImg.naturalHeight
  const offCtx = offCanvas.getContext('2d')!
  offCtx.drawImage(maskImg, 0, 0)
  return offCtx.getImageData(0, 0, offCanvas.width, offCanvas.height)
}

function isMaskPixelFilled(data: Uint8ClampedArray, idx: number): boolean {
  // For grayscale PNGs drawn to canvas: R=G=B=value, A=255
  // Check alpha first, then if any channel has significant value
  if (data[idx + 3] < 10) return false
  return data[idx] > 10 || data[idx + 1] > 10 || data[idx + 2] > 10
}

function renderMaskFill(
  ctx: CanvasRenderingContext2D,
  maskData: ImageData,
  fillColor: number[],
  alpha: number,
  targetW: number,
  targetH: number,
) {
  const fillCanvas = document.createElement('canvas')
  fillCanvas.width = maskData.width
  fillCanvas.height = maskData.height
  const fillCtx = fillCanvas.getContext('2d')!
  const fillImgData = fillCtx.createImageData(maskData.width, maskData.height)
  const [fr, fg, fb] = fillColor
  for (let p = 0; p < maskData.data.length; p += 4) {
    if (isMaskPixelFilled(maskData.data, p)) {
      fillImgData.data[p] = fr
      fillImgData.data[p + 1] = fg
      fillImgData.data[p + 2] = fb
      fillImgData.data[p + 3] = alpha
    }
  }
  fillCtx.putImageData(fillImgData, 0, 0)
  ctx.drawImage(fillCanvas, 0, 0, targetW, targetH)
}

async function renderAnnotation(canvas: HTMLCanvasElement, record: InferenceRecord) {
  const imageUrl = record.originalImageUrl || record.thumbnailUrl
  if (!imageUrl) return

  const parsed = parseRawJson(record.rawJson)
  if (!parsed) return

  let img: HTMLImageElement
  try {
    img = await loadImage(imageUrl)
  } catch {
    // Retry without crossOrigin for servers without CORS
    img = await new Promise((resolve, reject) => {
      const i = new Image()
      i.onload = () => resolve(i)
      i.onerror = reject
      i.src = imageUrl
    })
  }
  canvas.width = img.naturalWidth
  canvas.height = img.naturalHeight
  const ctx = canvas.getContext('2d')!
  ctx.drawImage(img, 0, 0)

  if (!parsed.objects) return

  // Scale drawing params based on canvas resolution
  const lineScale = Math.max(1, canvas.width / 600)
  const contourRadius = Math.max(2, Math.round(canvas.width / 400))
  const fontSize = Math.round(Math.max(16, 16 * lineScale))
  const labelPadH = Math.round(Math.max(22, 22 * lineScale))

  for (let i = 0; i < parsed.objects.length; i++) {
    const obj = parsed.objects[i]
    const color = MASK_COLORS[i % MASK_COLORS.length]

    // Draw mask overlay + contours for segmentation
    if (parsed.task_type === 'segment' && obj.mask) {
      try {
        const maskImg = await loadImage('data:image/png;base64,' + obj.mask)
        const maskData = decodeMaskToImageData(maskImg)
        const scaleX = canvas.width / maskData.width
        const scaleY = canvas.height / maskData.height

        renderMaskFill(ctx, maskData, color.fill, 80, canvas.width, canvas.height)
        drawMaskContours(ctx, maskData, color.stroke, scaleX, scaleY, contourRadius)
      } catch (e) {
        console.warn('Failed to render mask:', e)
      }
    }

    // Draw bbox
    if (obj.bbox && obj.bbox.length === 4) {
      const [x1, y1, x2, y2] = obj.bbox
      ctx.strokeStyle = color.stroke
      ctx.lineWidth = Math.round(2 * lineScale)
      ctx.strokeRect(x1, y1, x2 - x1, y2 - y1)

      if (obj.label) {
        const text = `${obj.label}${obj.confidence ? ' ' + (obj.confidence * 100).toFixed(0) + '%' : ''}`
        ctx.font = `bold ${fontSize}px sans-serif`
        const tw = ctx.measureText(text).width
        ctx.fillStyle = color.stroke
        ctx.fillRect(x1, y1 - labelPadH, tw + 8 * lineScale, labelPadH)
        ctx.fillStyle = '#fff'
        ctx.fillText(text, x1 + 4 * lineScale, y1 - labelPadH * 0.27)
      }
    }
  }
}

function drawMaskContours(
  ctx: CanvasRenderingContext2D,
  maskData: ImageData,
  strokeColor: string,
  scaleX: number,
  scaleY: number,
  radius = 1,
) {
  const { width: mw, height: mh, data } = maskData
  const cw = ctx.canvas.width
  const ch = ctx.canvas.height

  // Parse stroke color
  const tmpCanvas = document.createElement('canvas')
  tmpCanvas.width = 1
  tmpCanvas.height = 1
  const tmpCtx = tmpCanvas.getContext('2d')!
  tmpCtx.fillStyle = strokeColor
  tmpCtx.fillRect(0, 0, 1, 1)
  const [cr, cg, cb] = tmpCtx.getImageData(0, 0, 1, 1).data

  const contourCanvas = document.createElement('canvas')
  contourCanvas.width = cw
  contourCanvas.height = ch
  const contourCtx = contourCanvas.getContext('2d')!
  const contourImgData = contourCtx.createImageData(cw, ch)

  function filled(x: number, y: number): boolean {
    if (x < 0 || x >= mw || y < 0 || y >= mh) return false
    return isMaskPixelFilled(data, (y * mw + x) * 4)
  }

  for (let my = 0; my < mh; my++) {
    for (let mx = 0; mx < mw; mx++) {
      if (
        filled(mx, my)
        && (!filled(mx - 1, my) || !filled(mx + 1, my) || !filled(mx, my - 1) || !filled(mx, my + 1))
      ) {
        const cx = Math.round(mx * scaleX)
        const cy = Math.round(my * scaleY)
        for (let dy = -radius; dy <= radius; dy++) {
          for (let dx = -radius; dx <= radius; dx++) {
            const px = cx + dx
            const py = cy + dy
            if (px >= 0 && px < cw && py >= 0 && py < ch) {
              const idx = (py * cw + px) * 4
              contourImgData.data[idx] = cr
              contourImgData.data[idx + 1] = cg
              contourImgData.data[idx + 2] = cb
              contourImgData.data[idx + 3] = 255
            }
          }
        }
      }
    }
  }

  contourCtx.putImageData(contourImgData, 0, 0)
  ctx.drawImage(contourCanvas, 0, 0)
}
</script>
