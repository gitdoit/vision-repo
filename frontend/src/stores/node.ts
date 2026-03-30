import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { InferenceNode } from '@/types'
import * as nodeApi from '@/api/modules/node'

export const useNodeStore = defineStore('node', () => {
  const nodes = ref<InferenceNode[]>([])
  const loading = ref(false)

  async function fetchNodes() {
    loading.value = true
    try {
      const res = await nodeApi.getNodes() as unknown as InferenceNode[]
      nodes.value = Array.isArray(res) ? res : []
    } finally {
      loading.value = false
    }
  }

  async function updateNodeName(id: string, nodeName: string) {
    await nodeApi.updateNodeName(id, nodeName)
    await fetchNodes()
  }

  async function removeNode(id: string) {
    await nodeApi.removeNode(id)
    await fetchNodes()
  }

  return { nodes, loading, fetchNodes, updateNodeName, removeNode }
})
