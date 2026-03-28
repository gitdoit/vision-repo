/* Types */
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
  groupId: string
  recentTasks: TaskStatus[]
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

export interface Model {
  id: string
  name: string
  version: string
  businessTag: string
  engineSupport: string[]
  targetHardware: string
  status: 'loaded' | 'unloaded'
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

export interface DashboardStats {
  todayAnalyses: number
  todayAnalysesTrend: number
  todayAlerts: number
  todayAlertsTrend: number
  totalCameras: number
  aiEnabledCameras: number
  weeklyInferenceCount: number
}

export interface AlertRecord {
  time: string
  camera: string
  type: string
  businessLine: string
}

export interface WeeklyTrend {
  day: string
  count: number
}

export interface BusinessLineAlert {
  name: string
  percentage: number
}
