---
description: "Use when working on Python inference service: Flask routes, YOLO inference engine, model management, RTSP stream capture, or inference configuration."
applyTo: "vision-inference/**"
---
# Python 推理服务规范（vision-inference）

## 职责边界

Python 服务**只做推理**，不做业务逻辑：
- 接收图片/流地址 → 运行 YOLO → 返回检测结果
- 模型加载/卸载由 Java 发起指令，Python 执行
- 流式推理结果通过 HTTP 回调推送给 Java 服务

## 代码结构

```
app.py              # Flask 路由入口，所有 endpoint 定义在此
config.py           # 环境变量配置，提供默认值
registration.py     # 节点自动注册 + 心跳（后台线程）
state_store.py      # 本地状态持久化（JSON 文件）
inference/
  engine.py         # YOLO 推理封装（支持 detect/segment/classify/pose）
  model_manager.py  # 线程安全单例，管理已加载模型
  preprocess.py     # 图像预处理
stream/
  capture.py        # RTSP 流抓帧（OpenCV daemon thread）
  stream_task.py    # 流式任务管理器（线程安全单例）
```

## 关键约定

- 配置通过环境变量注入，默认值在 `config.py`，启动时通过 `Config.validate()` 校验
- `ModelManager` 和 `StreamTaskManager` 是线程安全单例，用 `threading.Lock` / `RLock`
- Flask 不用 Blueprint，所有路由直接在 `app.py`
- 生产部署用 Gunicorn（gthread worker），开发用 `python app.py`
- 不用 FastAPI — 公司技术路线要求

## 多节点注册

- 启动时自动向 `ADMIN_URL`（Java 服务）注册，并启动心跳后台线程
- `ADVERTISE_HOST` 配置对外可达地址（Docker/K8s 场景）
- `StateStore` 将已加载模型持久化到本地 JSON，重启后自动恢复
- 详见 [多节点推理服务架构重构.md](../../多节点推理服务架构重构.md)

## API 端点（供 Java 服务调用）

```
POST /predict           # 单张图片推理
POST /models/load       # 加载模型
POST /models/unload     # 卸载模型
GET  /models/status     # 已加载模型列表
POST /stream/start      # 启动流式推理
POST /stream/stop       # 停止流式推理
GET  /stream/tasks      # 活跃流式任务
GET  /health            # 健康检查
GET  /device/info       # 设备信息（GPU/CPU）
```

## 详细 API 文档

参见 [vision-inference/README.md](../../vision-inference/README.md) 获取完整请求/响应格式和 curl 示例。
