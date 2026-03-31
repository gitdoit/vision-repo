# vision-inference

Python 推理服务，基于 Flask + Ultralytics YOLO。支持多节点部署，启动后自动向管理服务（vision-admin）注册并定时上报心跳。

## 职责

- 单张图片目标检测推理
- YOLO 模型加载/卸载管理
- RTSP 流式抓帧 + 实时推理
- 自动向管理服务注册 & 心跳上报

## 对外接口

所有 API 仅供内部服务（vision-admin）调用。

### 推理
- `POST /predict` — 单张图片推理

### 模型管理
- `POST /models/load` — 加载模型
- `POST /models/unload` — 卸载模型
- `POST /models/upload` — 接收管理服务推送的模型文件（multipart）
- `GET /models/status` — 已加载模型列表

### 流式推理
- `POST /stream/start` — 启动流式推理
- `POST /stream/stop` — 停止流式推理
- `GET /stream/tasks` — 活跃任务列表

### 系统信息
- `GET /health` — 服务健康状态
- `GET /system/info` — 系统硬件及运行时信息
- `GET /device/info` — 可用计算设备（CPU/GPU）

## 环境变量

### 基础配置

| 变量 | 默认值 | 说明 |
|------|--------|------|
| MODEL_BASE_PATH | /data/vision/models | 模型文件基础路径 |
| DEVICE | cpu | 推理设备：cpu 或 cuda |
| HOST | 0.0.0.0 | 服务监听地址 |
| PORT | 26331 | 服务端口 |
| LOG_LEVEL | INFO | 日志级别 |
| DEFAULT_CONFIDENCE_THRESHOLD | 0.5 | 默认置信度阈值 |
| INFERENCE_TIMEOUT | 30 | 推理超时（秒） |
| DEFAULT_STREAM_FPS | 5 | 默认流式推理 FPS |
| MAX_STREAM_TASKS | 10 | 最大并发流式任务数 |

### 多节点注册配置

| 变量 | 默认值 | 说明 |
|------|--------|------|
| ADMIN_URL | http://127.0.0.1:26330 | 管理服务（vision-admin）完整地址 |
| NODE_NAME | _(自动使用主机名)_ | 自定义节点名称 |
| ADVERTISE_HOST | 127.0.0.1 | 注册到管理服务的对外可达 IP（多机部署需改为实际 IP） |
| HEARTBEAT_INTERVAL | 15 | 心跳上报间隔（秒） |
| STATE_FILE_PATH | /data/vision/node_state.json | 本地节点状态持久化文件路径 |

## 本地开发

```bash
# 安装依赖
pip install -r requirements.txt

# 运行开发服务器（默认端口 26331）
python app.py

# 或使用 Gunicorn
gunicorn -c gunicorn.conf.py app:app
```

## 测试

```bash
# 健康检查
curl http://localhost:26331/health

# 加载模型
curl -X POST http://localhost:26331/models/load \
  -H "Content-Type: application/json" \
  -d '{"model_id":"yolo","model_path":"/models/yolo.pt"}'

# 推理
curl -X POST http://localhost:26331/predict \
  -H "Content-Type: application/json" \
  -d '{"image_url":"/path/to/image.jpg","model_id":"yolo"}'

# 系统信息
curl http://localhost:26331/system/info
```

## 多节点部署

每个推理节点启动后会自动向 `ADMIN_URL` 注册，并以 `HEARTBEAT_INTERVAL` 间隔上报心跳（已加载模型、活跃任务、系统负载）。

**单机部署**（推理服务与管理服务在同一台机器）：
```bash
# 使用默认配置即可，ADMIN_URL 默认指向 127.0.0.1:26330
python app.py
```

**多机部署**（推理服务部署在独立 GPU/CPU 服务器上）：
```bash
# 指定管理服务地址和本机对外 IP
ADMIN_URL=http://192.168.1.100:26330 \
ADVERTISE_HOST=192.168.1.201 \
NODE_NAME=gpu-node-01 \
python app.py
```

**Docker 多实例**：
```bash
docker compose up --scale vision-inference=3
```

## 注意事项

- 模型文件可由管理服务通过 `/models/upload` 推送，也可预先放置到 `MODEL_BASE_PATH` 目录
- GPU 推理需要安装 CUDA 和 cuDNN
- 流式推理任务数量受 `MAX_STREAM_TASKS` 限制
- 多机部署时 `ADVERTISE_HOST` 必须设为管理服务可访问的 IP，不能使用默认的 `127.0.0.1`
- 管理服务（vision-admin）默认端口为 **26330**，推理服务默认端口为 **26331**
