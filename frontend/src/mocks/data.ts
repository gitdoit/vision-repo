import type {
  DashboardStats, WeeklyTrend, BusinessLineAlert, AlertRecord,
  Camera, CameraGroup, Model, Rule, InferenceRecord, VideoPlatform
} from '@/types'

export const dashboardStats: DashboardStats = {
  todayAnalyses: 12450,
  todayAnalysesTrend: 15,
  todayAlerts: 856,
  todayAlertsTrend: -5,
  totalCameras: 124,
  aiEnabledCameras: 1024,
  weeklyInferenceCount: 856,
}

export const weeklyTrend: WeeklyTrend[] = [
  { day: 'Mon', count: 720 },
  { day: 'Tue', count: 850 },
  { day: 'Wed', count: 790 },
  { day: 'Thu', count: 920 },
  { day: 'Fri', count: 1050 },
  { day: 'Sat', count: 680 },
  { day: 'Sun', count: 560 },
]

export const alertRanking: BusinessLineAlert[] = [
  { name: 'Security Center', percentage: 42 },
  { name: 'Production Line A', percentage: 28 },
  { name: 'Logistics Yard', percentage: 15 },
  { name: 'Entrance Monitoring', percentage: 10 },
  { name: 'Staff Lounge', percentage: 5 },
]

export const realtimeAlerts: AlertRecord[] = [
  { time: '10:24:05', camera: 'Gate 1 Camera', type: 'Intrusion Detection', businessLine: 'Security' },
  { time: '10:22:12', camera: 'Assembly Line 04', type: 'Equipment Failure', businessLine: 'Production' },
  { time: '10:21:55', camera: 'Parking Zone B', type: 'Illegal Parking', businessLine: 'Facility' },
  { time: '10:20:44', camera: 'Server Room 01', type: 'Smoke Detection', businessLine: 'Safety' },
  { time: '10:19:30', camera: 'Gate 2 Camera', type: 'Intrusion Detection', businessLine: 'Security' },
]

export const cameraGroups: CameraGroup[] = [
  {
    id: 'g1', name: 'Industrial Park', icon: 'factory', cameraCount: 12,
    children: [
      { id: 'g1-1', name: 'Main Gate', icon: 'door', cameraCount: 12 },
      { id: 'g1-2', name: 'Warehouse A', icon: 'warehouse', cameraCount: 8 },
      { id: 'g1-3', name: 'Office Building', icon: 'business', cameraCount: 5 },
    ],
  },
  { id: 'g2', name: 'Smart City Area', icon: 'location_city', cameraCount: 20 },
]

export const cameras: Camera[] = [
  {
    id: 'CAM-0824-01', name: 'Entrance Gate 01', businessLine: 'Security',
    location: 'Main Gate', streamUrl: 'rtsp://192.168.1.101:554/stream1',
    captureFrequency: '每10分钟一次', aiEnabled: true, status: 'online',
    lastCaptureTime: '2026-10-24 10:30', groupId: 'g1-1',
    source: 'synced', platformId: 'vp1', channelNo: 'CH-001',
    recentTasks: [
      { time: '10:30', success: true }, { time: '10:20', success: true },
      { time: '10:10', success: false }, { time: '10:00', success: true },
    ],
  },
  {
    id: 'CAM-0824-02', name: 'Warehouse Loading Dock', businessLine: 'Production',
    location: 'Warehouse A', streamUrl: 'rtsp://192.168.1.102:554/stream1',
    captureFrequency: '每10分钟一次', aiEnabled: true, status: 'online',
    lastCaptureTime: '2026-10-24 10:28', groupId: 'g1-2',
    source: 'synced', platformId: 'vp1', channelNo: 'CH-002',
    recentTasks: [
      { time: '10:28', success: true }, { time: '10:18', success: true },
    ],
  },
  {
    id: 'CAM-0824-03', name: 'Parking Zone B', businessLine: 'Facility',
    location: 'Parking', streamUrl: 'rtsp://192.168.1.103:554/stream1',
    captureFrequency: '每5分钟一次', aiEnabled: true, status: 'online',
    lastCaptureTime: '2026-10-24 10:25', groupId: 'g2',
    source: 'manual',
    recentTasks: [
      { time: '10:25', success: true }, { time: '10:20', success: true },
    ],
  },
  {
    id: 'CAM-0824-04', name: 'Server Room 01', businessLine: 'Safety',
    location: 'IT Building', streamUrl: 'rtsp://192.168.1.104:554/stream1',
    captureFrequency: '每1分钟一次', aiEnabled: false, status: 'offline',
    lastCaptureTime: '2026-10-24 09:15', groupId: 'g1-3',
    source: 'manual',
    recentTasks: [],
  },
]

export const models: Model[] = [
  {
    id: 'm1', name: 'YOLOv8-Security', version: 'V2.1.0', businessTag: 'Security',
    engineSupport: ['TRT', 'ONNX'], targetHardware: 'GPU (NVIDIA)',
    status: 'loaded', confidenceThreshold: 0.65, inputResolution: '640x640',
    maxConcurrency: 4, modelPath: '/opt/models/security/yolov8_s_2.1.engine',
    createdAt: '2023-10-24 14:20:11', author: 'AI_Deploy_Bot',
    versionHistory: [
      { version: 'V2.1.0', description: 'Optimization for night scenes' },
      { version: 'V2.0.5', description: 'Baseline security model' },
    ],
    avgLatency: 14.2,
  },
  {
    id: 'm2', name: 'ResNet50-Classifier', version: 'V1.0.4', businessTag: 'Industrial',
    engineSupport: ['ONNX', 'EXC'], targetHardware: 'CPU (x86)',
    status: 'unloaded', confidenceThreshold: 0.70, inputResolution: '224x224',
    maxConcurrency: 2, modelPath: '/opt/models/industrial/resnet50_v1.onnx',
    createdAt: '2023-09-15 09:30:00', author: 'ML_Team',
    versionHistory: [
      { version: 'V1.0.4', description: 'Industrial classifier' },
    ],
    avgLatency: 22.5,
  },
  {
    id: 'm3', name: 'FaceNet-PRO-X', version: 'V4.2.0', businessTag: 'Access',
    engineSupport: ['TRT'], targetHardware: 'GPU (NVIDIA)',
    status: 'loaded', confidenceThreshold: 0.80, inputResolution: '160x160',
    maxConcurrency: 8, modelPath: '/opt/models/access/facenet_pro_x.engine',
    createdAt: '2023-11-01 16:00:00', author: 'AI_Deploy_Bot',
    versionHistory: [
      { version: 'V4.2.0', description: 'Enhanced face recognition' },
      { version: 'V4.1.0', description: 'Mask detection support' },
    ],
    avgLatency: 8.7,
  },
]

export const rules: Rule[] = [
  {
    id: 'r1', name: '区域违停检测_01', businessLine: 'Facility', enabled: true,
    priority: 'severe', schedule: '10:00-18:00',
    conditions: [
      { id: 'c1', type: 'target', operator: '=', value: 'vehicle' },
      { id: 'c2', type: 'confidence', operator: '>', value: 0.85 },
      { id: 'c3', type: 'frames', operator: '>=', value: 15 },
      { id: 'c4', type: 'zone', operator: 'in', value: 'parking-zone-b' },
    ],
    actions: { alertLevel: 'severe', pushMethods: ['websocket', 'http'], evidenceSave: ['image', 'video'] },
    effectiveStart: '2023-10-01', effectiveEnd: '2024-12-31',
    weekdays: [1, 2, 3, 4, 5],
  },
  {
    id: 'r2', name: '人员聚集报警_A区', businessLine: 'Security', enabled: true,
    priority: 'severe', schedule: '全时段',
    conditions: [
      { id: 'c5', type: 'target', operator: '=', value: 'person' },
      { id: 'c6', type: 'confidence', operator: '>', value: 0.75 },
    ],
    actions: { alertLevel: 'severe', pushMethods: ['websocket'], evidenceSave: ['image'] },
    effectiveStart: '2023-10-01', effectiveEnd: '2024-12-31',
    weekdays: [1, 2, 3, 4, 5, 6, 7],
  },
  {
    id: 'r3', name: '烟火隐患实时监测', businessLine: 'Safety', enabled: true,
    priority: 'severe', schedule: '全时段',
    conditions: [
      { id: 'c7', type: 'target', operator: '=', value: 'fire_smoke' },
      { id: 'c8', type: 'confidence', operator: '>', value: 0.60 },
    ],
    actions: { alertLevel: 'severe', pushMethods: ['websocket', 'http', 'kafka'], evidenceSave: ['image', 'video'] },
    effectiveStart: '2023-10-01', effectiveEnd: '2024-12-31',
    weekdays: [1, 2, 3, 4, 5, 6, 7],
  },
]

export const inferenceRecords: InferenceRecord[] = [
  {
    id: 'inf1', eventId: 'EV-9021', timestamp: '2023-11-24 14:22:05',
    cameraId: 'CAM_04_NORTH_02', businessType: '工业安全: 油液泄漏监测',
    avgConfidence: 0.92, alertStatus: 'normal',
    detections: [
      { label: '泄漏', confidence: 0.9412, bbox: [124, 452, 230, 510], count: 1 },
      { label: '阀门', confidence: 0.8921, bbox: [55, 310, 88, 342], count: 2 },
    ],
    thumbnailUrl: 'https://placehold.co/320x200/0f1930/8ff5ff?text=EV-9021',
    originalImageUrl: 'https://placehold.co/800x600/0f1930/8ff5ff?text=Original',
    annotatedImageUrl: 'https://placehold.co/800x600/0f1930/ff716c?text=Annotated',
    rawJson: JSON.stringify({
      event_id: 'EV-9021', timestamp: '2023-11-24T14:22:05.122Z',
      detections: [
        { label: 'leakage', confidence: 0.9412, bbox: [124, 452, 230, 510] },
        { label: 'valve', confidence: 0.8921, bbox: [55, 310, 88, 342] },
      ],
      camera_metadata: { id: 'CAM_04', focal_length: '35mm' },
    }, null, 2),
    relatedAlerts: [
      { title: 'Severe Leakage', time: '2 mins ago', severity: 'severe' },
      { title: 'Temp Anomaly', time: '14 mins ago', severity: 'warning' },
    ],
  },
  {
    id: 'inf2', eventId: 'EV-9022', timestamp: '2023-11-24 14:18:33',
    cameraId: 'CAM_12_GATE_01', businessType: '安全合规: 安全帽佩戴检测',
    avgConfidence: 0.88, alertStatus: 'alert',
    detections: [
      { label: '安全帽', confidence: 0.88, bbox: [100, 200, 150, 250], count: 5 },
    ],
    thumbnailUrl: 'https://placehold.co/320x200/0f1930/65afff?text=EV-9022',
    originalImageUrl: 'https://placehold.co/800x600/0f1930/65afff?text=Original',
    annotatedImageUrl: 'https://placehold.co/800x600/0f1930/ff716c?text=Annotated',
    rawJson: JSON.stringify({
      event_id: 'EV-9022', timestamp: '2023-11-24T14:18:33.000Z',
      detections: [{ label: 'helmet', confidence: 0.88, bbox: [100, 200, 150, 250] }],
    }, null, 2),
    relatedAlerts: [],
  },
  {
    id: 'inf3', eventId: 'EV-9023', timestamp: '2023-11-24 14:05:12',
    cameraId: 'CAM_01_WEST_05', businessType: '消防监测: 明火烟雾检测',
    avgConfidence: 0.96, alertStatus: 'normal',
    detections: [
      { label: '烟雾', confidence: 0.96, bbox: [200, 100, 400, 300], count: 1 },
    ],
    thumbnailUrl: 'https://placehold.co/320x200/0f1930/ff716c?text=EV-9023',
    originalImageUrl: 'https://placehold.co/800x600/0f1930/ff716c?text=Original',
    annotatedImageUrl: 'https://placehold.co/800x600/0f1930/ff716c?text=Annotated',
    rawJson: JSON.stringify({
      event_id: 'EV-9023', timestamp: '2023-11-24T14:05:12.000Z',
      detections: [{ label: 'smoke', confidence: 0.96, bbox: [200, 100, 400, 300] }],
    }, null, 2),
    relatedAlerts: [],
  },
  {
    id: 'inf4', eventId: 'EV-9024', timestamp: '2023-11-24 13:55:00',
    cameraId: 'CAM_33_LAB_02', businessType: '进入管控: 区域准入识别',
    avgConfidence: 0.91, alertStatus: 'normal',
    detections: [
      { label: '人员', confidence: 0.91, bbox: [300, 150, 400, 350], count: 0 },
    ],
    thumbnailUrl: 'https://placehold.co/320x200/0f1930/cad5ed?text=EV-9024',
    originalImageUrl: 'https://placehold.co/800x600/0f1930/cad5ed?text=Original',
    annotatedImageUrl: 'https://placehold.co/800x600/0f1930/cad5ed?text=Annotated',
    rawJson: JSON.stringify({
      event_id: 'EV-9024', timestamp: '2023-11-24T13:55:00.000Z',
      detections: [{ label: 'person', confidence: 0.91, bbox: [300, 150, 400, 350] }],
    }, null, 2),
    relatedAlerts: [],
  },
]

export const videoPlatforms: VideoPlatform[] = [
  {
    id: 'vp1',
    name: '公司视频管理平台',
    apiBase: 'http://192.168.10.50:8080/api',
    authType: 'token',
    credential: '******',
    autoSync: true,
    syncIntervalMin: 30,
    lastSyncTime: '2026-03-28 09:30:00',
    lastSyncResult: {
      total: 45, added: 2, updated: 1, removed: 0, failed: 0,
      syncTime: '2026-03-28 09:30:00',
    },
    camerasCount: 45,
    status: 'connected',
  },
]
