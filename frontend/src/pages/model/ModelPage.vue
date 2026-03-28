<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <p class="text-sm text-on-surface-variant">管理及优化视觉分析模型，配置高性能推理引擎。</p>
      <div class="flex items-center gap-3">
        <n-input v-model:value="searchQuery" placeholder="搜索模型..." size="small" class="w-52">
          <template #prefix><Icon icon="mdi:magnify" class="text-on-surface-variant" /></template>
        </n-input>
        <n-button type="primary" size="small">
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
          <div class="mt-2 flex items-center gap-2">
            <n-tag size="tiny" :bordered="false" type="info">{{ model.businessTag }} {{ model.version }}</n-tag>
            <n-tag
              size="tiny" :bordered="false"
              :type="model.status === 'loaded' ? 'success' : 'default'"
            >
              {{ model.status === 'loaded' ? '已加载' : '未加载' }}
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
              <Icon :icon="model.targetHardware.includes('GPU') ? 'mdi:memory' : 'mdi:developer-board'" class="text-sm" />
              <span>{{ model.targetHardware }}</span>
            </div>
          </div>
          <div class="mt-3 flex gap-2">
            <n-button
              v-if="model.status === 'loaded'"
              size="tiny" quaternary type="warning"
            >
              <template #icon><Icon icon="mdi:eject" /></template>
              卸载
            </n-button>
            <n-button
              v-else
              size="tiny" quaternary type="primary"
            >
              <template #icon><Icon icon="mdi:play-arrow" /></template>
              加载模型
            </n-button>
            <n-button size="tiny" quaternary>
              <template #icon><Icon icon="mdi:tune" /></template>
              参数
            </n-button>
            <n-button size="tiny" quaternary type="error">
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
          <n-button size="small">放弃修改</n-button>
          <n-button type="primary" size="small">保存配置</n-button>
        </div>
      </div>

      <div v-else class="flex-1 rounded-xl bg-bg-card p-6 flex items-center justify-center">
        <div class="text-center text-on-surface-variant">
          <Icon icon="mdi:brain" class="text-5xl opacity-20" />
          <p class="mt-3 text-sm">选择一个模型查看配置详情</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import {
  NInput, NButton, NTag, NSlider, NFormItem, NInputNumber, NSelect,
} from 'naive-ui'
import { Icon } from '@iconify/vue'
import { useModelStore } from '@/stores/model'
import type { Model } from '@/types'
import InfoRow from './InfoRow.vue'

const modelStore = useModelStore()
const searchQuery = ref('')
const selectedId = ref<string | null>(null)
const editThreshold = ref(0.65)
const editConcurrency = ref(4)
const editResolution = ref('640x640')

onMounted(() => {
  modelStore.fetchModels()
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

const resolutionOptions = [
  { label: '640x640', value: '640x640' },
  { label: '1280x720', value: '1280x720' },
  { label: '224x224', value: '224x224' },
  { label: '160x160', value: '160x160' },
]
</script>
