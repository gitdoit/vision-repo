# vision-inference

Python 推理服务，基于 Flask + Ultralytics YOLO。

## 职责

- 单张图片目标检测推理
- YOLO 模型加载/卸载管理
- RTSP 流式抓帧 + 实时推理

## 对外接口

所有 API 仅供内部服务（vision-admin）调用。

### 推理
- `POST /predict` — 单张图片推理

### 模型管理
- `POST /models/load` — 加载模型
- `POST /models/unload` — 卸载模型
- `GET /models/status` — 已加载模型列表

### 流式推理
- `POST /stream/start` — 启动流式推理
- `POST /stream/stop` — 停止流式推理
- `GET /stream/tasks` — 活跃任务列表

### 健康检查
- `GET /health` — 服务健康状态

## 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| MODEL_BASE_PATH | /data/vision/models | 模型文件基础路径 |
| DEVICE | cpu | 推理设备：cpu 或 cuda |
| HOST | 0.0.0.0 | 服务监听地址 |
| PORT | 5000 | 服务端口 |
| LOG_LEVEL | INFO | 日志级别 |
| DEFAULT_CONFIDENCE_THRESHOLD | 0.5 | 默认置信度阈值 |
| DEFAULT_STREAM_FPS | 5 | 默认流式推理 FPS |
| MAX_STREAM_TASKS | 10 | 最大并发流式任务数 |

## 本地开发

```bash
# 安装依赖
pip install -r requirements.txt

# 运行开发服务器
python app.py

# 或使用 Gunicorn
gunicorn -c gunicorn.conf.py app:app
```

## 测试

```bash
# 健康检查
curl http://localhost:5000/health

# 加载模型
curl -X POST http://localhost:5000/models/load \
  -H "Content-Type: application/json" \
  -d '{"model_id":"yolo","model_path":"/models/yolo.pt"}'

# 推理
curl -X POST http://localhost:5000/predict \
  -H "Content-Type: application/json" \
  -d '{"image_url":"/path/to/image.jpg","model_id":"yolo"}'
```

## 注意事项

- 模型文件必须预先放置到 MODEL_BASE_PATH 目录
- GPU 推理需要安装 CUDA 和 cuDNN
- 流式推理任务数量受 MAX_STREAM_TASKS 限制
