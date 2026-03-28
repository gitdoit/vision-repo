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
          <div class="w-40 h-24 rounded-lg bg-bg-void overflow-hidden shrink-0 flex items-center justify-center">
            <Icon icon="mdi:image" class="text-3xl text-on-surface-variant opacity-30" />
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
            <p class="text-xs text-on-surface-variant mt-1">{{ record.timestamp }}</p>
            <div class="mt-2 flex items-center gap-3 text-xs text-on-surface-variant">
              <span>平均置信度 <strong class="text-primary">{{ record.avgConfidence.toFixed(2) }}</strong></span>
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
          <div class="h-48 rounded-lg bg-bg-void flex items-center justify-center">
            <div class="text-center">
              <Icon icon="mdi:image" class="text-4xl text-on-surface-variant opacity-20" />
              <p class="mt-1 text-xs text-on-surface-variant">{{ imageTab }}</p>
            </div>
          </div>
        </div>

        <!-- Raw JSON -->
        <div class="mb-4">
          <h4 class="mb-2 text-sm font-semibold text-on-surface-variant">原始JSON结果</h4>
          <pre class="rounded-lg bg-bg-void p-4 text-xs font-mono text-primary/80 overflow-auto max-h-48">{{ selectedRecord.rawJson }}</pre>
        </div>

        <!-- Related Alerts -->
        <div v-if="selectedRecord.relatedAlerts.length > 0">
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
import { ref, onMounted } from 'vue'
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

onMounted(() => {
  inferenceStore.fetchRecords()
})

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
</script>
