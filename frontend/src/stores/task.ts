import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { MonitorTask, MonitorTaskForm } from '@/types'
import * as taskApi from '@/api/modules/task'

export const useTaskStore = defineStore('task', () => {
  const tasks = ref<MonitorTask[]>([])
  const total = ref(0)
  const selectedTask = ref<MonitorTask | null>(null)
  const loading = ref(false)

  async function fetchTasks(params?: Record<string, unknown>) {
    loading.value = true
    try {
      const res = await taskApi.getMonitorTasks(params) as unknown as { items: MonitorTask[]; total: number }
      tasks.value = res.items ?? []
      total.value = res.total ?? 0
    } finally {
      loading.value = false
    }
  }

  async function selectTask(id: string) {
    const res = await taskApi.getMonitorTask(id) as unknown as MonitorTask
    selectedTask.value = res
  }

  async function createTask(data: MonitorTaskForm) {
    const res = await taskApi.createMonitorTask(data) as unknown as MonitorTask
    await fetchTasks()
    return res
  }

  async function updateTask(id: string, data: MonitorTaskForm) {
    const res = await taskApi.updateMonitorTask(id, data) as unknown as MonitorTask
    await fetchTasks()
    return res
  }

  async function removeTask(id: string) {
    await taskApi.deleteMonitorTask(id)
    if (selectedTask.value?.id === id) {
      selectedTask.value = null
    }
    await fetchTasks()
  }

  async function startTask(id: string) {
    await taskApi.startMonitorTask(id)
    await fetchTasks()
  }

  async function stopTask(id: string) {
    await taskApi.stopMonitorTask(id)
    await fetchTasks()
  }

  return {
    tasks,
    total,
    selectedTask,
    loading,
    fetchTasks,
    selectTask,
    createTask,
    updateTask,
    removeTask,
    startTask,
    stopTask,
  }
})
