import { http, HttpResponse } from 'msw'
import {
  dashboardStats, weeklyTrend, alertRanking, realtimeAlerts,
  cameras, cameraGroups, models, rules, inferenceRecords,
  videoPlatforms,
} from './data'

const BASE = '/api/v1'

export const handlers = [
  // Dashboard
  http.get(`${BASE}/dashboard/stats`, () => HttpResponse.json(dashboardStats)),
  http.get(`${BASE}/dashboard/weekly-trend`, () => HttpResponse.json(weeklyTrend)),
  http.get(`${BASE}/dashboard/alert-ranking`, () => HttpResponse.json(alertRanking)),
  http.get(`${BASE}/dashboard/realtime-alerts`, () => HttpResponse.json(realtimeAlerts)),

  // Cameras
  http.get(`${BASE}/cameras`, ({ request }) => {
    const url = new URL(request.url)
    const page = parseInt(url.searchParams.get('page') || '1', 10)
    const size = parseInt(url.searchParams.get('size') || '20', 10)
    const keyword = url.searchParams.get('keyword') || ''
    const status = url.searchParams.get('status') || ''
    let filtered = cameras
    if (keyword) {
      const kw = keyword.toLowerCase()
      filtered = filtered.filter(c => c.name.toLowerCase().includes(kw) || (c.label && c.label.toLowerCase().includes(kw)))
    }
    if (status) {
      filtered = filtered.filter(c => c.status === status)
    }
    const start = (page - 1) * size
    const items = filtered.slice(start, start + size)
    return HttpResponse.json({ items, total: filtered.length })
  }),
  http.get(`${BASE}/cameras/groups`, () => HttpResponse.json(cameraGroups)),
  http.post(`${BASE}/cameras/groups`, async ({ request }) => {
    const body = await request.json() as Record<string, unknown>
    const newGroup = { id: 'g-new-' + Date.now(), name: body.name, icon: 'folder', cameraCount: 0 }
    return HttpResponse.json(newGroup, { status: 201 })
  }),
  http.delete(`${BASE}/cameras/groups/:id`, () => new HttpResponse(null, { status: 204 })),
  http.get(`${BASE}/cameras/:id`, ({ params }) => {
    const cam = cameras.find(c => c.id === params.id)
    return cam ? HttpResponse.json(cam) : new HttpResponse(null, { status: 404 })
  }),
  http.post(`${BASE}/cameras`, () => HttpResponse.json(cameras[0], { status: 201 })),
  http.put(`${BASE}/cameras/:id`, () => HttpResponse.json(cameras[0])),
  http.delete(`${BASE}/cameras/:id`, () => new HttpResponse(null, { status: 204 })),
  http.post(`${BASE}/cameras/import`, () => HttpResponse.json({ success: 3, failed: 0 })),

  // Video Platforms
  http.get(`${BASE}/video-platforms`, () => HttpResponse.json(videoPlatforms)),
  http.post(`${BASE}/video-platforms`, () => HttpResponse.json(videoPlatforms[0], { status: 201 })),
  http.put(`${BASE}/video-platforms/:id`, () => HttpResponse.json(videoPlatforms[0])),
  http.delete(`${BASE}/video-platforms/:id`, () => new HttpResponse(null, { status: 204 })),
  http.post(`${BASE}/video-platforms/:id/test`, () => HttpResponse.json({ connected: true, message: '连接成功，发现 45 个通道' })),
  http.post(`${BASE}/video-platforms/:id/sync`, () => HttpResponse.json({
    total: 45, added: 2, updated: 1, removed: 0, failed: 0,
    syncTime: new Date().toISOString(),
  })),
  http.post(`${BASE}/video-platforms/batch-import`, () => HttpResponse.json({
    total: 374, added: 350, updated: 24, failed: 0,
    syncTime: new Date().toISOString(),
  })),

  // Models
  http.get(`${BASE}/models`, () => HttpResponse.json({ items: models, total: models.length })),
  http.get(`${BASE}/models/:id`, ({ params }) => {
    const model = models.find(m => m.id === params.id)
    return model ? HttpResponse.json(model) : new HttpResponse(null, { status: 404 })
  }),
  http.post(`${BASE}/models/upload`, () => {
    const newModel = {
      ...models[0],
      id: 'm' + (models.length + 1),
      name: 'Uploaded Model',
      status: 'unloaded' as const,
      createdAt: new Date().toISOString(),
      versionHistory: [{ version: 'V1.0.0', description: '初始版本' }],
    }
    models.push(newModel)
    return HttpResponse.json(newModel)
  }),
  http.post(`${BASE}/models/:id/load`, () => HttpResponse.json({ success: true })),
  http.post(`${BASE}/models/:id/unload`, () => HttpResponse.json({ success: true })),
  http.put(`${BASE}/models/:id/config`, () => HttpResponse.json(models[0])),
  http.delete(`${BASE}/models/:id`, () => new HttpResponse(null, { status: 204 })),
  http.get(`${BASE}/models/device/info`, () => HttpResponse.json({
    devices: ['cpu', 'cuda'],
    cuda_available: true,
    gpu_name: 'NVIDIA GeForce RTX 3090',
  })),

  // Rules
  http.get(`${BASE}/rules`, () => HttpResponse.json(rules)),
  http.get(`${BASE}/rules/:id`, ({ params }) => {
    const rule = rules.find(r => r.id === params.id)
    return rule ? HttpResponse.json(rule) : new HttpResponse(null, { status: 404 })
  }),
  http.post(`${BASE}/rules`, () => HttpResponse.json(rules[0], { status: 201 })),
  http.put(`${BASE}/rules/:id`, () => HttpResponse.json(rules[0])),
  http.delete(`${BASE}/rules/:id`, () => new HttpResponse(null, { status: 204 })),
  http.post(`${BASE}/rules/:id/deploy`, () => HttpResponse.json({ success: true })),
  http.post(`${BASE}/rules/:id/test`, () => HttpResponse.json({ matched: true, details: 'Rule matched 3 events' })),

  // Inference
  http.get(`${BASE}/inference`, () => HttpResponse.json({ items: inferenceRecords, total: inferenceRecords.length })),
  http.get(`${BASE}/inference/:id`, ({ params }) => {
    const record = inferenceRecords.find(r => r.id === params.id)
    return record ? HttpResponse.json(record) : new HttpResponse(null, { status: 404 })
  }),

  // Inference Test (model test on camera frame)
  http.post(`${BASE}/inference/test`, async ({ request }) => {
    await new Promise(r => setTimeout(r, 800))
    const formData = await request.formData()
    const modelId = formData.get('modelId') as string
    const model = models.find(m => m.id === modelId)
    const taskType = model?.taskType ?? 'detect'

    if (taskType === 'classify') {
      return HttpResponse.json({
        taskType: 'classify',
        objects: [],
        classifications: [
          { label: 'industrial_scene', confidence: 0.91 },
          { label: 'warehouse', confidence: 0.72 },
          { label: 'outdoor', confidence: 0.35 },
          { label: 'parking_lot', confidence: 0.18 },
          { label: 'construction', confidence: 0.09 },
        ],
        inferenceTimeMs: 18.2,
      })
    }

    if (taskType === 'segment') {
      return HttpResponse.json({
        taskType: 'segment',
        objects: [
          { label: 'person', confidence: 0.94, bbox: [120, 80, 320, 450], mask: '' },
          { label: 'vehicle', confidence: 0.87, bbox: [400, 200, 620, 380], mask: '' },
        ],
        inferenceTimeMs: 31.7,
      })
    }

    if (taskType === 'pose') {
      return HttpResponse.json({
        taskType: 'pose',
        objects: [
          {
            label: 'person', confidence: 0.95, bbox: [120, 80, 320, 450],
            keypoints: [
              [200, 100, 0.95], [210, 95, 0.90], [190, 95, 0.88],
              [230, 100, 0.85], [170, 100, 0.82], [250, 170, 0.93],
              [150, 170, 0.91], [270, 260, 0.88], [130, 260, 0.86],
              [280, 340, 0.80], [120, 340, 0.78], [240, 300, 0.92],
              [160, 300, 0.90], [245, 380, 0.85], [155, 380, 0.83],
              [248, 440, 0.79], [152, 440, 0.77],
            ],
          },
        ],
        inferenceTimeMs: 27.3,
      })
    }

    // Default: detect
    return HttpResponse.json({
      taskType: 'detect',
      objects: [
        { label: 'person', confidence: 0.94, bbox: [120, 80, 320, 450] },
        { label: 'vehicle', confidence: 0.87, bbox: [400, 200, 620, 380] },
        { label: 'helmet', confidence: 0.72, bbox: [140, 60, 200, 110] },
      ],
      inferenceTimeMs: 23.5,
    })
  }),
]
