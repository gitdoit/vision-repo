import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/components/layout/AppLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/pages/dashboard/DashboardPage.vue'),
        meta: { title: '首页概览', icon: 'mdi:view-dashboard' },
      },
      {
        path: 'cameras',
        name: 'Cameras',
        component: () => import('@/pages/camera/CameraPage.vue'),
        meta: { title: '摄像头接入管理', icon: 'mdi:video' },
      },
      {
        path: 'models',
        name: 'Models',
        component: () => import('@/pages/model/ModelPage.vue'),
        meta: { title: '模型配置管理', icon: 'mdi:brain' },
      },
      {
        path: 'nodes',
        name: 'Nodes',
        component: () => import('@/pages/node/NodePage.vue'),
        meta: { title: '推理节点管理', icon: 'mdi:server-network' },
      },
      {
        path: 'tasks',
        name: 'Tasks',
        component: () => import('@/pages/task/MonitorTaskPage.vue'),
        meta: { title: '监测任务管理', icon: 'mdi:clipboard-check-outline' },
      },
      {
        path: 'rules',
        name: 'Rules',
        component: () => import('@/pages/rule/RulePage.vue'),
        meta: { title: '业务规则管理', icon: 'mdi:file-document-edit' },
      },
      {
        path: 'inference',
        name: 'Inference',
        component: () => import('@/pages/inference/InferencePage.vue'),
        meta: { title: '推理历史查询', icon: 'mdi:history' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

export default router
