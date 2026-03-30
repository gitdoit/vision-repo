<template>
  <div class="space-y-6">
    <!-- Header -->
    <div class="flex items-center justify-between">
      <p class="text-sm text-on-surface-variant">管理推理服务节点，监控运行状态与资源使用。</p>
      <n-button size="small" @click="nodeStore.fetchNodes()">
        <template #icon><Icon icon="mdi:refresh" /></template>
        刷新
      </n-button>
    </div>

    <!-- Node Cards -->
    <n-spin :show="nodeStore.loading">
      <div v-if="nodeStore.nodes.length === 0 && !nodeStore.loading" class="flex flex-col items-center py-20 text-on-surface-variant">
        <Icon icon="mdi:server-off" class="text-5xl opacity-20" />
        <p class="mt-3 text-sm">暂无推理节点注册</p>
      </div>

      <div class="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
        <div
          v-for="node in nodeStore.nodes"
          :key="node.id"
          class="rounded-xl bg-bg-card p-5 transition-shadow hover:shadow-md cursor-pointer"
          :class="selectedId === node.id ? 'ring-1 ring-primary/30' : ''"
          @click="selectNode(node)"
        >
          <!-- Status & Name -->
          <div class="flex items-center justify-between mb-3">
            <div class="flex items-center gap-2">
              <span
                class="inline-block h-2.5 w-2.5 rounded-full"
                :class="node.status === 'online' ? 'bg-green-500' : 'bg-red-400'"
              />
              <h4 class="text-sm font-semibold">{{ node.nodeName }}</h4>
            </div>
            <n-tag size="tiny" :bordered="false" :type="node.status === 'online' ? 'success' : 'error'">
              {{ node.status === 'online' ? '在线' : '离线' }}
            </n-tag>
          </div>

          <!-- Info Grid -->
          <div class="grid grid-cols-2 gap-y-2 text-xs text-on-surface-variant">
            <div>
              <span class="text-on-surface-variant/60">地址</span>
              <div class="font-mono mt-0.5">{{ node.host }}:{{ node.port }}</div>
            </div>
            <div>
              <span class="text-on-surface-variant/60">设备</span>
              <div class="flex items-center gap-1 mt-0.5">
                <Icon :icon="node.deviceType === 'cuda' ? 'mdi:memory' : 'mdi:developer-board'" class="text-sm" />
                <span>{{ node.deviceType === 'cuda' ? (node.gpuName || 'GPU') : 'CPU' }}</span>
              </div>
            </div>
            <div>
              <span class="text-on-surface-variant/60">最后心跳</span>
              <div class="mt-0.5">{{ node.lastHeartbeat ? formatTime(node.lastHeartbeat) : '-' }}</div>
            </div>
            <div>
              <span class="text-on-surface-variant/60">注册时间</span>
              <div class="mt-0.5">{{ formatTime(node.registeredAt) }}</div>
            </div>
          </div>

          <!-- Runtime bars -->
          <div v-if="node.runtimeInfo?.systemLoad" class="mt-3 space-y-1.5">
            <div class="flex items-center gap-2 text-xs">
              <span class="w-8 text-on-surface-variant/60">CPU</span>
              <n-progress type="line" :percentage="node.runtimeInfo.systemLoad.cpuPercent" :show-indicator="false" :height="6" />
              <span class="w-10 text-right">{{ node.runtimeInfo.systemLoad.cpuPercent.toFixed(0) }}%</span>
            </div>
            <div class="flex items-center gap-2 text-xs">
              <span class="w-8 text-on-surface-variant/60">内存</span>
              <n-progress type="line" :percentage="node.runtimeInfo.systemLoad.memoryPercent" :show-indicator="false" :height="6" />
              <span class="w-10 text-right">{{ node.runtimeInfo.systemLoad.memoryPercent.toFixed(0) }}%</span>
            </div>
            <div v-if="node.runtimeInfo.systemLoad.gpuPercent != null" class="flex items-center gap-2 text-xs">
              <span class="w-8 text-on-surface-variant/60">GPU</span>
              <n-progress type="line" :percentage="node.runtimeInfo.systemLoad.gpuPercent" :show-indicator="false" :height="6" />
              <span class="w-10 text-right">{{ node.runtimeInfo.systemLoad.gpuPercent.toFixed(0) }}%</span>
            </div>
          </div>

          <!-- Actions -->
          <div class="mt-3 flex gap-2">
            <n-button size="tiny" quaternary @click.stop="handleRename(node)">
              <template #icon><Icon icon="mdi:pencil" /></template>
              重命名
            </n-button>
            <n-button size="tiny" quaternary type="error" @click.stop="handleRemove(node)">
              <template #icon><Icon icon="mdi:delete" /></template>
              移除
            </n-button>
          </div>
        </div>
      </div>
    </n-spin>

    <!-- Detail Drawer -->
    <n-drawer v-model:show="showDrawer" :width="420" placement="right">
      <n-drawer-content :title="selectedNode?.nodeName ?? '节点详情'">
        <div v-if="selectedNode" class="space-y-5">
          <!-- Loaded Models -->
          <section>
            <h4 class="mb-2 text-sm font-semibold text-on-surface-variant">已加载模型</h4>
            <div v-if="loadedModels.length === 0" class="text-xs text-on-surface-variant">无</div>
            <div v-else class="space-y-2">
              <div v-for="m in loadedModels" :key="m.modelId" class="rounded-lg bg-bg-floor px-3 py-2 text-xs">
                <div class="font-semibold">{{ m.modelId }}</div>
                <div class="text-on-surface-variant">{{ m.device }} · {{ m.modelPath }}</div>
              </div>
            </div>
          </section>

          <!-- Active Tasks -->
          <section>
            <h4 class="mb-2 text-sm font-semibold text-on-surface-variant">活跃任务</h4>
            <div v-if="activeTasks.length === 0" class="text-xs text-on-surface-variant">无</div>
            <div v-else class="space-y-2">
              <div v-for="t in activeTasks" :key="t.taskId" class="rounded-lg bg-bg-floor px-3 py-2 text-xs">
                <div class="font-semibold">{{ t.taskId }}</div>
                <div class="text-on-surface-variant">{{ t.modelId }} · {{ t.fps }} FPS</div>
              </div>
            </div>
          </section>

          <!-- Hardware Info -->
          <section>
            <h4 class="mb-2 text-sm font-semibold text-on-surface-variant">硬件信息</h4>
            <div class="grid grid-cols-2 gap-2 text-xs">
              <div>CPU：{{ selectedNode.cpuInfo || '-' }}</div>
              <div>内存：{{ formatBytes(selectedNode.memoryTotal) }}</div>
              <div v-if="selectedNode.gpuName">GPU：{{ selectedNode.gpuName }} × {{ selectedNode.gpuCount }}</div>
            </div>
          </section>
        </div>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { NButton, NTag, NSpin, NProgress, NDrawer, NDrawerContent, useMessage, useDialog } from 'naive-ui'
import { Icon } from '@iconify/vue'
import { useNodeStore } from '@/stores/node'
import type { InferenceNode } from '@/types'

const nodeStore = useNodeStore()
const message = useMessage()
const dialog = useDialog()

const selectedId = ref<string | null>(null)
const showDrawer = ref(false)

const selectedNode = computed(() => nodeStore.nodes.find(n => n.id === selectedId.value) ?? null)
const loadedModels = computed(() => selectedNode.value?.runtimeInfo?.loadedModels ?? [])
const activeTasks = computed(() => selectedNode.value?.runtimeInfo?.activeTasks ?? [])

function selectNode(node: InferenceNode) {
  selectedId.value = node.id
  showDrawer.value = true
}

function formatTime(iso: string) {
  if (!iso) return '-'
  return iso.replace('T', ' ').slice(0, 19)
}

function formatBytes(bytes: number) {
  if (!bytes) return '-'
  const gb = bytes / (1024 * 1024 * 1024)
  return gb.toFixed(1) + ' GB'
}

function handleRename(node: InferenceNode) {
  const newName = prompt('输入新名称', node.nodeName)
  if (newName && newName !== node.nodeName) {
    nodeStore.updateNodeName(node.id, newName)
      .then(() => message.success('已重命名'))
      .catch((e: any) => message.error(e?.message || '重命名失败'))
  }
}

function handleRemove(node: InferenceNode) {
  dialog.warning({
    title: '确认移除',
    content: `确定要移除节点「${node.nodeName}」吗？`,
    positiveText: '移除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await nodeStore.removeNode(node.id)
        if (selectedId.value === node.id) {
          selectedId.value = null
          showDrawer.value = false
        }
        message.success('节点已移除')
      } catch (e: any) {
        message.error(e?.message || '移除失败')
      }
    },
  })
}

// Auto refresh every 10s
let timer: ReturnType<typeof setInterval>

onMounted(() => {
  nodeStore.fetchNodes()
  timer = setInterval(() => nodeStore.fetchNodes(), 10000)
})

onUnmounted(() => {
  clearInterval(timer)
})
</script>
