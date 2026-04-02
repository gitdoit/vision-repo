<template>
  <div class="space-y-4">
    <p class="text-sm text-on-surface-variant">检索全量推理结果及告警详情记录</p>

    <!-- Filters -->
    <div class="flex flex-wrap items-center gap-3 rounded-xl bg-bg-card px-4 py-3">
      <div class="flex gap-1">
        <button
          v-for="range in timeRanges"
          :key="range.value"
          class="rounded-full px-3 py-1 text-xs transition-colors"
          :class="selectedTimeRange === range.value ? 'bg-primary/15 text-primary' : 'text-on-surface-variant hover:bg-bg-active'"
          @click="applyTimeRange(range.value)"
        >
          {{ range.label }}
        </button>
      </div>
      <n-select v-model:value="filterAlertStatus" :options="alertStatusOptions" placeholder="告警状态" size="small" class="w-28" clearable />
      <n-input v-model:value="filterCamera" placeholder="摄像头ID" size="small" class="w-36" clearable @keyup.enter="doSearch" />
      <div class="flex items-center gap-2 text-xs text-on-surface-variant">
        <span>置信度</span>
        <span class="font-mono text-primary">{{ confidenceRange[0].toFixed(2) }}-{{ confidenceRange[1].toFixed(2) }}</span>
      </div>
      <n-slider v-model:value="confidenceRange" range :min="0" :max="1" :step="0.05" class="w-32" />
      <n-button size="small" quaternary @click="resetFilters">重置</n-button>
      <n-button size="small" type="primary" @click="doSearch">查询</n-button>
      <div class="ml-auto flex gap-2">
        <n-button size="small" quaternary @click="handleExport('csv')">
          <template #icon><Icon icon="mdi:file-delimited" /></template>
          CSV
        </n-button>
        <n-button size="small" quaternary @click="handleExport('excel')">
          <template #icon><Icon icon="mdi:table" /></template>
          Excel
        </n-button>
      </div>
    </div>

    <!-- Table -->
    <div class="rounded-xl bg-bg-card">
      <n-data-table
        :columns="columns"
        :data="inferenceStore.records"
        :loading="inferenceStore.loading"
        :bordered="false"
        :row-key="(row: InferenceRecord) => row.id"
        :row-props="rowProps"
        size="small"
        :pagination="false"
        :max-height="'calc(100vh - 340px)'"
        flex-height
      />
      <div class="flex items-center justify-between px-4 py-3 border-t border-outline-variant/10">
        <span class="text-xs text-on-surface-variant">共 {{ inferenceStore.total }} 条记录</span>
        <n-pagination
          v-model:page="currentPage"
          v-model:page-size="currentPageSize"
          :item-count="inferenceStore.total"
          :page-sizes="[10, 20, 50]"
          show-size-picker
          size="small"
          @update:page="handlePageChange"
          @update:page-size="handlePageSizeChange"
        />
      </div>
    </div>

    <!-- Detail Drawer -->
    <n-drawer v-model:show="drawerVisible" :width="540" placement="right">
      <n-drawer-content v-if="selectedRecord" :title="'推理详情'" closable>
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

        <!-- Info Cards -->
        <div class="mb-4 grid grid-cols-3 gap-2">
          <div class="rounded-lg bg-bg-void px-3 py-2 text-center">
            <div class="text-xs text-on-surface-variant">抓帧耗时</div>
            <div class="text-sm font-semibold text-primary">{{ selectedRecord.captureTimeMs ?? '—' }}<span class="text-xs font-normal"> ms</span></div>
          </div>
          <div class="rounded-lg bg-bg-void px-3 py-2 text-center">
            <div class="text-xs text-on-surface-variant">推理耗时</div>
            <div class="text-sm font-semibold text-primary">{{ selectedRecord.inferenceTimeMs ?? '—' }}<span class="text-xs font-normal"> ms</span></div>
          </div>
          <div class="rounded-lg bg-bg-void px-3 py-2 text-center">
            <div class="text-xs text-on-surface-variant">总耗时</div>
            <div class="text-sm font-semibold text-primary">{{ totalTimeMs }}<span class="text-xs font-normal"> ms</span></div>
          </div>
        </div>

        <!-- Metadata -->
        <div class="mb-4 space-y-2 text-sm">
          <div class="flex justify-between"><span class="text-on-surface-variant">摄像头</span><span>{{ selectedRecord.cameraName || selectedRecord.cameraId }}</span></div>
          <div class="flex justify-between"><span class="text-on-surface-variant">关联任务</span><span>{{ selectedRecord.taskName || '—' }}</span></div>
          <div class="flex justify-between"><span class="text-on-surface-variant">所属分组</span><span>{{ selectedRecord.groupName || '—' }}</span></div>
          <div class="flex justify-between"><span class="text-on-surface-variant">模型</span><span>{{ selectedRecord.modelName || '—' }}</span></div>
          <div class="flex justify-between"><span class="text-on-surface-variant">告警状态</span>
            <n-tag size="tiny" :bordered="false" :type="alertTagType(selectedRecord.alertStatus)">{{ alertLabel(selectedRecord.alertStatus) }}</n-tag>
          </div>
          <div class="flex justify-between"><span class="text-on-surface-variant">平均置信度</span><span class="text-primary font-semibold">{{ (selectedRecord.avgConfidence ?? 0).toFixed(2) }}</span></div>
          <div class="flex justify-between"><span class="text-on-surface-variant">时间</span><span>{{ selectedRecord.createdAt }}</span></div>
        </div>

        <!-- Detection Table -->
        <div v-if="selectedRecord.detections?.length" class="mb-4">
          <h4 class="mb-2 text-sm font-semibold text-on-surface-variant">检测目标</h4>
          <n-data-table
            :columns="detectionColumns"
            :data="selectedRecord.detections"
            :bordered="false"
            size="small"
            :pagination="false"
          />
        </div>

        <!-- Raw JSON (collapsed) -->
        <div>
          <h4 class="mb-2 text-sm font-semibold text-on-surface-variant cursor-pointer flex items-center gap-1" @click="showJson = !showJson">
            <Icon :icon="showJson ? 'mdi:chevron-down' : 'mdi:chevron-right'" class="text-base" />
            原始JSON
          </h4>
          <pre v-show="showJson" class="rounded-lg bg-bg-void p-3 text-xs font-mono text-primary/80 overflow-auto max-h-48">{{ formatJson(selectedRecord.rawJson) }}</pre>
        </div>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, h } from 'vue'
import { NSelect, NInput, NSlider, NButton, NTag, NDataTable, NDrawer, NDrawerContent, NPagination, NProgress } from 'naive-ui'
import type { DataTableColumns } from 'naive-ui'
import { Icon } from '@iconify/vue'
import { useInferenceStore } from '@/stores/inference'
import { exportInferenceCsv, exportInferenceExcel } from '@/api/modules/inference'
import type { InferenceRecord, Detection } from '@/types'

const inferenceStore = useInferenceStore()

// --- Filters ---
const selectedTimeRange = ref('today')
const filterAlertStatus = ref<string | null>(null)
const filterCamera = ref('')
const confidenceRange = ref<[number, number]>([0, 1.0])

const currentPage = ref(1)
const currentPageSize = ref(20)

// --- Detail ---
const drawerVisible = ref(false)
const selectedRecord = ref<InferenceRecord | null>(null)
const imageTab = ref('原始画面')
const annotationCanvas = ref<HTMLCanvasElement | null>(null)
const showJson = ref(false)

const detailImageUrl = computed(() => {
  if (!selectedRecord.value) return ''
  return selectedRecord.value.originalImageUrl || selectedRecord.value.thumbnailUrl || ''
})

const totalTimeMs = computed(() => {
  if (!selectedRecord.value) return '—'
  const c = selectedRecord.value.captureTimeMs ?? 0
  const i = selectedRecord.value.inferenceTimeMs ?? 0
  return c + i || '—'
})

// --- Time range helpers ---
const timeRanges = [
  { label: '全部', value: 'all' },
  { label: '今日', value: 'today' },
  { label: '7日', value: '7d' },
  { label: '本月', value: 'month' },
]

const alertStatusOptions = [
  { label: '正常', value: 'normal' },
  { label: '告警', value: 'alert' },
]

function getTimeRange(value: string): { startTime?: string; endTime?: string } {
  const now = new Date()
  const fmt = (d: Date) => d.toISOString().slice(0, 19)
  if (value === 'today') {
    const start = new Date(now.getFullYear(), now.getMonth(), now.getDate())
    return { startTime: fmt(start) }
  }
  if (value === '7d') {
    const start = new Date(now.getTime() - 7 * 86400000)
    return { startTime: fmt(start) }
  }
  if (value === 'month') {
    const start = new Date(now.getFullYear(), now.getMonth(), 1)
    return { startTime: fmt(start) }
  }
  return {}
}

function buildQueryParams() {
  const params: Record<string, unknown> = {
    page: currentPage.value,
    size: currentPageSize.value,
  }
  const tr = getTimeRange(selectedTimeRange.value)
  if (tr.startTime) params.startTime = tr.startTime
  if (tr.endTime) params.endTime = tr.endTime
  if (filterAlertStatus.value) params.alertType = filterAlertStatus.value
  if (filterCamera.value.trim()) params.cameraId = filterCamera.value.trim()
  if (confidenceRange.value[0] > 0) params.minConfidence = confidenceRange.value[0]
  if (confidenceRange.value[1] < 1) params.maxConfidence = confidenceRange.value[1]
  return params
}

function doSearch() {
  currentPage.value = 1
  fetchData()
}

function fetchData() {
  inferenceStore.fetchRecords(buildQueryParams())
}

function applyTimeRange(value: string) {
  selectedTimeRange.value = value
  doSearch()
}

function resetFilters() {
  selectedTimeRange.value = 'today'
  filterAlertStatus.value = null
  filterCamera.value = ''
  confidenceRange.value = [0, 1.0]
  doSearch()
}

function handlePageChange(page: number) {
  currentPage.value = page
  fetchData()
}

function handlePageSizeChange(size: number) {
  currentPageSize.value = size
  currentPage.value = 1
  fetchData()
}

// --- Export ---
async function handleExport(format: 'csv' | 'excel') {
  const params = buildQueryParams()
  try {
    const res = format === 'csv'
      ? await exportInferenceCsv(params)
      : await exportInferenceExcel(params)
    const blob = res as unknown as Blob
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `inference_export.${format === 'csv' ? 'csv' : 'xlsx'}`
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    console.error('Export failed:', e)
  }
}

// --- Table Columns ---
const columns = computed<DataTableColumns<InferenceRecord>>(() => [
  {
    key: 'thumbnail',
    title: '缩略图',
    width: 100,
    render(row) {
      return h('div', { class: 'w-20 h-12 rounded bg-bg-void overflow-hidden relative shrink-0' }, [
        row.thumbnailUrl
          ? h('img', { src: row.thumbnailUrl, class: 'w-full h-full object-cover', alt: '' })
          : h(Icon, { icon: 'mdi:image', class: 'text-xl text-on-surface-variant opacity-30 absolute inset-0 m-auto' }),
        hasSegmentMask(row)
          ? h('canvas', { 'data-record-id': row.id, class: 'thumb-mask-canvas absolute inset-0 w-full h-full pointer-events-none' })
          : null,
      ])
    },
  },
  {
    key: 'cameraName',
    title: '摄像头',
    width: 130,
    ellipsis: { tooltip: true },
    render(row) {
      return h('span', { class: 'text-xs' }, row.cameraName || row.cameraId || '—')
    },
  },
  {
    key: 'taskName',
    title: '关联任务',
    width: 130,
    ellipsis: { tooltip: true },
    render(row) {
      return h('span', { class: 'text-xs' }, row.taskName || '—')
    },
  },
  {
    key: 'groupName',
    title: '分组',
    width: 100,
    ellipsis: { tooltip: true },
    render(row) {
      return h('span', { class: 'text-xs' }, row.groupName || '—')
    },
  },
  {
    key: 'detections',
    title: '检测目标',
    width: 160,
    render(row) {
      if (!row.detections?.length) return h('span', { class: 'text-xs text-on-surface-variant' }, '无')
      return h('div', { class: 'flex flex-wrap gap-0.5' },
        row.detections.slice(0, 3).map(d =>
          h(NTag, { size: 'tiny', bordered: false, type: 'warning' }, { default: () => `${d.label}×${d.count}` })
        ).concat(row.detections.length > 3 ? [h('span', { class: 'text-xs text-on-surface-variant' }, `+${row.detections.length - 3}`)] : [])
      )
    },
  },
  {
    key: 'alertStatus',
    title: '状态',
    width: 70,
    render(row) {
      return h(NTag, { size: 'tiny', bordered: false, type: alertTagType(row.alertStatus) },
        { default: () => alertLabel(row.alertStatus) })
    },
  },
  {
    key: 'avgConfidence',
    title: '置信度',
    width: 90,
    sorter: (a, b) => (a.avgConfidence ?? 0) - (b.avgConfidence ?? 0),
    render(row) {
      const val = row.avgConfidence ?? 0
      return h('div', { class: 'flex items-center gap-1' }, [
        h(NProgress, { type: 'line', percentage: Math.round(val * 100), showIndicator: false, height: 4, class: 'flex-1' }),
        h('span', { class: 'text-xs font-mono w-10 text-right' }, (val * 100).toFixed(0) + '%'),
      ])
    },
  },
  {
    key: 'timing',
    title: '耗时',
    width: 110,
    render(row) {
      const c = row.captureTimeMs
      const i = row.inferenceTimeMs
      return h('div', { class: 'text-xs leading-relaxed' }, [
        c != null ? h('div', {}, [h('span', { class: 'text-on-surface-variant' }, '抓帧 '), h('span', {}, c + 'ms')]) : null,
        i != null ? h('div', {}, [h('span', { class: 'text-on-surface-variant' }, '推理 '), h('span', {}, i + 'ms')]) : null,
        (c == null && i == null) ? h('span', { class: 'text-on-surface-variant' }, '—') : null,
      ])
    },
  },
  {
    key: 'createdAt',
    title: '时间',
    width: 150,
    sorter: (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime(),
    render(row) {
      return h('span', { class: 'text-xs' }, row.createdAt)
    },
  },
])

const detectionColumns: DataTableColumns<Detection> = [
  { key: 'label', title: '标签', width: 100 },
  {
    key: 'confidence',
    title: '置信度',
    width: 80,
    render(row) { return h('span', {}, ((row.confidence ?? 0) * 100).toFixed(1) + '%') },
  },
  { key: 'count', title: '数量', width: 60 },
  {
    key: 'bbox',
    title: '坐标',
    ellipsis: { tooltip: true },
    render(row) { return h('span', { class: 'font-mono text-xs' }, String(row.bbox ?? '—')) },
  },
]

function rowProps(row: InferenceRecord) {
  return {
    class: 'cursor-pointer',
    onClick: () => openDetail(row),
  }
}

function openDetail(record: InferenceRecord) {
  selectedRecord.value = record
  imageTab.value = '原始画面'
  showJson.value = false
  drawerVisible.value = true
}

// --- Helpers ---
function alertTagType(status: string): 'success' | 'warning' | 'error' {
  if (status === 'alert') return 'error'
  if (status === 'warning') return 'warning'
  return 'success'
}

function alertLabel(status: string) {
  if (status === 'alert') return '告警'
  if (status === 'warning') return '警告'
  return '正常'
}

function formatJson(raw: string | object) {
  try {
    const obj = typeof raw === 'string' ? JSON.parse(raw) : raw
    return JSON.stringify(obj, null, 2)
  } catch { return String(raw) }
}

// --- Lifecycle ---
onMounted(() => {
  fetchData()
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

// Render annotation when switching to AI tab
watch(imageTab, async (tab) => {
  if (tab === 'AI标注' && selectedRecord.value) {
    await nextTick()
    if (annotationCanvas.value) {
      await renderAnnotation(annotationCanvas.value, selectedRecord.value)
    }
  }
})

// =====================================================================
// Mask rendering utilities (preserved from original implementation)
// =====================================================================

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
  const w = 80 * dpr
  const h = 48 * dpr
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

  const lineScale = Math.max(1, canvas.width / 600)
  const contourRadius = Math.max(2, Math.round(canvas.width / 400))
  const fontSize = Math.round(Math.max(16, 16 * lineScale))
  const labelPadH = Math.round(Math.max(22, 22 * lineScale))

  for (let i = 0; i < parsed.objects.length; i++) {
    const obj = parsed.objects[i]
    const color = MASK_COLORS[i % MASK_COLORS.length]

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
