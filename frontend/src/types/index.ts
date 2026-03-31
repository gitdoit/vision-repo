/* Types */

/** 摄像头来源：手动添加 / 视频平台同步 */
export type CameraSource = 'manual' | 'synced'

export interface Camera {
  id: string
  name: string
  businessLine: string
  location: string
  streamUrl: string
  captureFrequency: string
  aiEnabled: boolean
  status: 'online' | 'offline' | 'error'
  lastCaptureTime: string
  groups: GroupSimple[]
  recentTasks: TaskStatus[]
  /** 来源 */
  source: CameraSource
  /** 关联的视频平台ID（synced 时有值） */
  platformId?: string
  /** 视频平台中的通道编号 */
  channelNo?: string
  /** 标签名称（来自视频平台） */
  label?: string
}

export interface GroupSimple {
  id: string
  name: string
}

export interface TaskStatus {
  time: string
  success: boolean
}

export interface CameraGroup {
  id: string
  name: string
  icon: string
  cameraCount: number
  children?: CameraGroup[]
}

/** 视频平台配置 */
export interface VideoPlatform {
  id: string
  name: string
  /** 平台 API 基础地址 */
  apiBase: string
  /** 认证方式 */
  authType: 'token' | 'basic' | 'none'
  /** 凭证（token 或 user:pass） */
  credential: string
  /** 自动同步开关 */
  autoSync: boolean
  /** 同步间隔（分钟） */
  syncIntervalMin: number
  /** 上次同步时间 */
  lastSyncTime?: string
  /** 上次同步结果 */
  lastSyncResult?: SyncResult
  /** 同步过来的摄像头数量 */
  camerasCount: number
  status: 'connected' | 'disconnected' | 'syncing'
}

/** 同步结果 */
export interface SyncResult {
  total: number
  added: number
  updated: number
  removed: number
  failed: number
  syncTime: string
}

/** 从视频平台批量导入请求 */
export interface PlatformImportRequest {
  apiBase: string
  username: string
  password: string
}

/** 批量导入结果 */
export interface PlatformImportResult {
  total: number
  added: number
  updated: number
  failed: number
  syncTime: string
}

/** 模型任务类型 */
export type ModelTaskType = 'detect' | 'segment' | 'classify' | 'pose'

export interface Model {
  id: string
  name: string
  version: string
  businessTag: string
  taskType: ModelTaskType
  engineSupport: string[]
  targetHardware: string
  status: 'loaded' | 'unloaded'
  device?: string
  deviceName?: string
  nodeId?: string
  nodeName?: string
  confidenceThreshold: number
  inputResolution: string
  maxConcurrency: number
  modelPath: string
  createdAt: string
  author: string
  versionHistory: VersionEntry[]
  avgLatency: number
}

export interface VersionEntry {
  version: string
  description: string
}

export interface Rule {
  id: string
  name: string
  businessLine: string
  enabled: boolean
  priority: 'severe' | 'warning' | 'info'
  schedule: string
  conditions: RuleCondition[]
  actions: RuleAction
  effectiveStart: string
  effectiveEnd: string
  weekdays: number[]
}

export interface RuleCondition {
  id: string
  type: 'target' | 'confidence' | 'frames' | 'zone'
  operator: string
  value: string | number
}

export interface RuleAction {
  alertLevel: 'severe' | 'warning' | 'info'
  pushMethods: string[]
  evidenceSave: string[]
}

export interface InferenceRecord {
  id: string
  eventId: string
  timestamp: string
  cameraId: string
  businessType: string
  avgConfidence: number
  alertStatus: 'normal' | 'warning' | 'alert'
  detections: Detection[]
  thumbnailUrl: string
  originalImageUrl: string
  annotatedImageUrl: string
  rawJson: string
  relatedAlerts: RelatedAlert[]
}

export interface Detection {
  label: string
  confidence: number
  bbox: number[]
  count: number
}

export interface RelatedAlert {
  title: string
  time: string
  severity: 'severe' | 'warning' | 'info'
}

/** 模型测试请求 */
export interface ModelTestRequest {
  modelId: string
  confidenceThreshold?: number
}

/** 模型测试结果 */
export interface ModelTestResult {
  taskType: ModelTaskType
  objects: ModelTestDetection[]
  classifications?: ModelTestClassification[]
  inferenceTimeMs: number
}

/** 模型测试检测对象（detect / segment / pose） */
export interface ModelTestDetection {
  label: string
  confidence: number
  bbox: number[] // [x1, y1, x2, y2]
  /** Base64 PNG mask（仅 segment 模型） */
  mask?: string
  /** 关键点 [[x,y,conf], ...]（仅 pose 模型） */
  keypoints?: number[][]
}

/** 模型测试分类结果（classify 模型） */
export interface ModelTestClassification {
  label: string
  confidence: number
}

export interface DashboardStats {
  todayInferenceCount: number
  todayInferenceChange: number
  todayAlertCount: number
  todayAlertChange: number
  totalCameraCount: number
  aiEnabledCameraCount: number
}

export interface AlertRecord {
  time: string
  camera: string
  type: string
  businessLine: string
}

export interface WeeklyTrend {
  date: string
  count: number
}

export interface BusinessLineAlert {
  name: string
  percentage: number
}

/** 推理节点 */
export interface InferenceNode {
  id: string
  nodeName: string
  host: string
  port: number
  status: 'online' | 'offline' | 'unknown'
  deviceType: 'cpu' | 'cuda'
  gpuName?: string
  gpuCount: number
  cpuInfo?: string
  memoryTotal: number
  lastHeartbeat?: string
  registeredAt: string
  runtimeInfo?: NodeRuntimeInfo
}

export interface NodeRuntimeInfo {
  loadedModels?: NodeModelInfo[]
  activeTasks?: NodeTaskInfo[]
  systemLoad?: SystemLoad
}

export interface NodeModelInfo {
  modelId: string
  modelPath: string
  device: string
  loadedAt: number
}

export interface NodeTaskInfo {
  taskId: string
  streamUrl: string
  modelId: string
  fps: number
  running: boolean
}

export interface SystemLoad {
  cpuPercent: number
  memoryPercent: number
  gpuPercent?: number
}

/** 监测任务状态 */
export type MonitorTaskStatus = 'stopped' | 'running' | 'error'

/** 监测任务 */
export interface MonitorTask {
  id: string
  name: string
  description?: string
  businessLine?: string
  groupId: string
  groupName?: string
  modelId: string
  modelName?: string
  status: MonitorTaskStatus
  captureFrequency: string
  scheduleStartTime?: string
  scheduleEndTime?: string
  scheduleWeekdays?: string
  effectiveStart?: string
  effectiveEnd?: string
  alertTarget?: string
  alertConfidence: number
  alertFrames: number
  alertLevel: 'severe' | 'warning' | 'info'
  pushMethods?: string
  callbackUrl?: string
  callbackHeaders?: string
  nodeIds?: string
  totalInference: number
  totalAlert: number
  lastInferenceTime?: string
  lastAlertTime?: string
  createdAt: string
  updatedAt?: string
}

/** 监测任务创建/更新请求 */
export interface MonitorTaskForm {
  name: string
  description?: string
  businessLine?: string
  groupId: string
  modelId: string
  captureFrequency?: string
  scheduleStartTime?: string
  scheduleEndTime?: string
  scheduleWeekdays?: string
  effectiveStart?: string
  effectiveEnd?: string
  alertTarget?: string
  alertConfidence?: number
  alertFrames?: number
  alertLevel?: string
  pushMethods?: string
  callbackUrl?: string
  callbackHeaders?: string
  nodeIds?: string
}
