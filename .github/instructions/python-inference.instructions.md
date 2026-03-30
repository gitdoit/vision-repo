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
inference/
  engine.py         # YOLO 推理封装
  model_manager.py  # 线程安全单例，管理已加载模型
  preprocess.py     # 图像预处理
stream/
  capture.py        # RTSP 流抓帧（OpenCV daemon thread）
  stream_task.py    # 流式任务管理器（线程安全单例）
```

## 关键约定

- 配置通过环境变量注入，默认值在 `config.py`
- `ModelManager` 和 `StreamTaskManager` 是线程安全单例，用 `threading.Lock`
- Flask 不用 Blueprint，所有路由直接在 `app.py`
- 生产部署用 Gunicorn（gthread worker），开发用 `python app.py`
- 不用 FastAPI — 公司技术路线要求

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
```

## 详细 API 文档

参见 [vision-inference/README.md](../../vision-inference/README.md) 获取完整请求/响应格式和 curl 示例。
