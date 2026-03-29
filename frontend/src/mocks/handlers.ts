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
  http.get(`${BASE}/cameras`, () => HttpResponse.json({ items: cameras, total: cameras.length })),
  http.get(`${BASE}/cameras/groups`, () => HttpResponse.json(cameraGroups)),
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

  // Models
  http.get(`${BASE}/models`, () => HttpResponse.json(models)),
  http.get(`${BASE}/models/:id`, ({ params }) => {
    const model = models.find(m => m.id === params.id)
    return model ? HttpResponse.json(model) : new HttpResponse(null, { status: 404 })
  }),
  http.post(`${BASE}/models/:id/load`, () => HttpResponse.json({ success: true })),
  http.post(`${BASE}/models/:id/unload`, () => HttpResponse.json({ success: true })),
  http.put(`${BASE}/models/:id/config`, () => HttpResponse.json(models[0])),
  http.delete(`${BASE}/models/:id`, () => new HttpResponse(null, { status: 204 })),

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
]
