User: 目前我们只通过ai快速生成了一个前端页面,后端以及整体的架构代码编写还没有.你来作为一个资深架构师,来协助我们将整体项目落地.  另外,我们公司的整体技术路线是java,但是由于需要使用视觉分析(yolo) 肯定也要用到python, 另外项目开发完成之后,需要能快速落地部署到各个项目中,这也意味着部署工作不能太复杂,最好一键部署,方便调试维护.
所以你需要
1. 后端整体做模块划分(技术栈/工程结构)
2. 梳理清楚各个模块如何协调工作
3. 需要用到哪些技术(比如接入视频之类的)
4. 由于这个pdf里面罗列了很多中间件,我的想法是能简化就简化, 越少的依赖 就代表系统越可靠,除了数据库部分省略不掉,其他的你自己看
5.另外领导评估说最好不要用FastAPI,因为跟公司技术路线不一致,而且没几个人会.
6. 文件存储部分需要支持minio/以及本地磁盘两种(看项目实际情况可在部署的时候选择)
7. 项目需要对AI友好,因为后面肯定会交给AI开发
将你的设计,形成md文档,放在项目根目录

请忽略frontend文件夹,这是之前通过AI快速生成的. 不要在这个文件夹里浪费token

GitHub Copilot: 

Let me first understand the existing project structure and codebase before designing the architecture.  



---

# 视觉分析平台 - 架构设计文档

## 一、设计原则

| 原则 | 说明 |
|------|------|
| **最少依赖** | 仅保留 PostgreSQL 作为必选中间件，去掉 Kafka、Redis 等，降低部署运维复杂度 |
| **Java 主导** | 核心业务全部由 Java/Spring Boot 承载，Python 仅负责 YOLO 推理这一件事 |
| **一键部署** | Docker Compose 编排，区分 `full`（含 MinIO）和 `lite`（本地磁盘）两种部署模式 |
| **AI 友好** | 按业务模块分包、单一职责、清晰命名、每个模块自带 README，方便 AI 理解和修改 |

## 二、系统架构总览

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端 (Vue 3 + Vite)                       │
│                       /api/v1/* 所有请求                         │
└───────────────────────────┬─────────────────────────────────────┘
                            │ HTTP
                            ▼
┌───────────────────────────────────────────────────────────────┐
│                  vision-admin (Java / Spring Boot)              │
│                                                                 │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐          │
│  │ 摄像头管理 │ │ 模型管理  │ │ 规则引擎  │ │ 仪表板    │          │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘          │
│  ┌──────────┐ ┌──────────┐ ┌──────────────┐                   │
│  │ 告警管理  │ │ 推理记录  │ │ 抓图任务调度  │                   │
│  └──────────┘ └──────────┘ └──────┬───────┘                   │
│  ┌──────────────────┐            │                             │
│  │ 存储抽象层        │            │ FFmpeg 子进程               │
│  │ (MinIO / 本地磁盘) │            │ (RTSP 抓帧)                │
│  └──────────────────┘            │                             │
└──────────────┬───────────────────┼─────────────────────────────┘
               │                   │
               │ SQL               │ HTTP (内网)
               ▼                   ▼
┌──────────────────┐   ┌─────────────────────────────────┐
│   PostgreSQL     │   │  vision-inference (Python/Flask)  │
│                  │   │                                   │
│  所有业务数据      │   │  ┌─────────────┐ ┌────────────┐  │
│  任务配置         │   │  │ YOLO 推理引擎 │ │ 流式抓帧    │  │
│  推理/告警记录     │   │  └─────────────┘ └────────────┘  │
└──────────────────┘   │  ┌─────────────┐                  │
                       │  │ 模型生命周期  │                  │
┌──────────────────┐   │  └─────────────┘                  │
│ MinIO (可选)      │   └─────────────────────────────────┘
│ 或 本地磁盘       │
└──────────────────┘
```

## 三、模块划分

### 3.1 vision-admin（Java / Spring Boot）— 主服务

> 承载所有业务逻辑、API 接口、任务调度、规则评估、数据持久化

| 模块 | 职责 | 关键能力 |
|------|------|---------|
| **camera** | 摄像头 & 视频平台管理 | CRUD、分组树、第三方平台同步、批量导入 |
| **model** | 模型元数据管理 | 模型注册、版本管理、加载/卸载指令下发（调用 Python 服务） |
| **rule** | 规则引擎 | 规则 CRUD、条件评估（纯 Java 实现）、告警触发 |
| **inference** | 推理记录 | 存储推理结果、查询、导出 CSV/Excel |
| **alert** | 告警管理 | 告警存储、WebSocket 实时推送 |
| **dashboard** | 仪表板统计 | 聚合查询（今日分析数、告警数、周趋势、业务线排名） |
| **capture** | 抓图任务调度 | 定时抓帧（FFmpeg 子进程）、图片推理请求编排 |
| **storage** | 文件存储抽象 | 统一 API，运行时按配置切换 MinIO / 本地磁盘 |
| **platform** | 视频平台对接 | 对接第三方视频平台 API，同步摄像头列表 |

### 3.2 vision-inference（Python / Flask）— 推理服务

> 只做一件事：接收图片，返回检测结果。保持极简。

| 模块 | 职责 |
|------|------|
| **inference engine** | 封装 Ultralytics YOLO，执行目标检测 |
| **model manager** | 模型文件加载/卸载/热切换 |
| **stream capture** | RTSP 流式抓帧 + 实时推理（高帧率场景） |

### 3.3 为什么这样分

```
Java 负责:  "管"  → 管理、调度、规则、持久化、对外 API
Python 负责: "算"  → 模型加载、图像推理、RTSP 帧提取
```

好处：
- Java 开发者不需要碰 Python，Python 代码量极小（<1000 行）
- Python 服务可独立扩缩容（GPU 节点水平拓展）
- 两个服务通过 HTTP 通信，调试方便，wireshark/curl 均可排查

## 四、技术选型

### 4.1 vision-admin（Java）

| 组件 | 技术 | 版本 | 理由 |
|------|------|------|------|
| 框架 | Spring Boot | 3.2+ | 公司技术路线，生态成熟 |
| ORM | MyBatis-Plus | 3.5+ | 灵活、学习成本低、代码生成 |
| 数据库 | PostgreSQL | 14+ | 支持 JSONB 存储灵活配置，开源免费 |
| 连接池 | HikariCP | 默认 | Spring Boot 默认，性能最优 |
| 任务调度 | Spring @Scheduled + ThreadPoolTaskExecutor | - | 替代 Kafka，内置即可 |
| WebSocket | Spring WebSocket | - | 实时告警推送 |
| 缓存 | Caffeine | 3.1+ | 本地缓存，替代 Redis，无额外部署 |
| 文件存储 | MinIO SDK / 本地IO | - | 通过 SPI 抽象切换 |
| 视频抓帧 | FFmpeg（子进程调用） | 6.0+ | 通过 ProcessBuilder 调用，无需 Java 绑定 |
| API 文档 | SpringDoc OpenAPI | 2.3+ | 自动生成 Swagger 文档 |
| 参数校验 | Jakarta Validation | - | 注解式校验 |
| 日志 | SLF4J + Logback | - | Spring Boot 默认 |

### 4.2 vision-inference（Python）

| 组件 | 技术 | 版本 | 理由 |
|------|------|------|------|
| Web 框架 | Flask | 3.0+ | 极简，Java 开发者也看得懂 |
| 推理引擎 | Ultralytics | 8.4+ | 统一 API，原生支持 YOLOv26 |
| 图像处理 | OpenCV (cv2) | 4.8+ | 抓帧、预处理 |
| ONNX 推理 | onnxruntime / onnxruntime-gpu | 1.17+ | CPU/GPU 通用 |
| 进程管理 | Gunicorn | 22+ | 生产部署，多 worker |

### 4.3 去掉了什么，为什么

| 原设计方案 | 现在的替代 | 理由 |
|-----------|----------|------|
| **Kafka** | Spring 内置线程池 + BlockingQueue | 当前规模（234 QPS）完全够用；去掉一个重量级中间件 |
| **Redis** | Caffeine 本地缓存 | 单实例部署下无需分布式缓存 |
| **FastAPI** | Flask（Python 端） + Spring Boot（Java 端） | 公司技术路线要求；Flask 几乎人人会 |
| **独立规则引擎** | Java 条件表达式评估 | 规则结构固定（目标类型+置信度+帧数+区域），不需要通用规则引擎 |
| **MinIO（强制）** | MinIO / 本地磁盘二选一 | 小项目本地磁盘即可，大项目上 MinIO |

> **未来扩展**：如果单实例扛不住，再加 Kafka（生产者/消费者模式天然兼容当前接口设计）和 Redis（缓存+分布式锁），无需改业务代码。

## 五、工程目录结构

```
vision-repo/
├── docker-compose.yml           # 一键部署编排
├── docker-compose.lite.yml      # 轻量部署（本地磁盘，无 MinIO）
├── deploy.sh                    # 部署脚本
├── .env.example                 # 环境变量模板
├── ARCHITECTURE.md              # 本文档
│
├── vision-admin/                # Java 主服务
│   ├── pom.xml
│   ├── Dockerfile
│   ├── README.md                # 模块说明（AI 友好）
│   └── src/main/
│       ├── java/com/vision/
│       │   ├── VisionApplication.java
│       │   │
│       │   ├── config/                    # --- 全局配置 ---
│       │   │   ├── WebConfig.java         # CORS、拦截器
│       │   │   ├── WebSocketConfig.java   # WebSocket 配置
│       │   │   ├── CacheConfig.java       # Caffeine 缓存配置
│       │   │   ├── ThreadPoolConfig.java  # 线程池配置
│       │   │   └── StorageConfig.java     # 存储策略配置
│       │   │
│       │   ├── common/                    # --- 公共组件 ---
│       │   │   ├── exception/
│       │   │   │   ├── BizException.java        # 业务异常
│       │   │   │   └── GlobalExceptionHandler.java
│       │   │   ├── response/
│       │   │   │   ├── R.java                   # 统一响应 { code, data, message }
│       │   │   │   └── PageResult.java          # 分页结果 { items, total }
│       │   │   └── util/
│       │   │       ├── FFmpegUtil.java           # FFmpeg 命令封装
│       │   │       └── IdUtil.java               # ID 生成
│       │   │
│       │   ├── storage/                   # --- 存储抽象模块 ---
│       │   │   ├── StorageService.java          # 接口：upload/download/delete/getUrl
│       │   │   ├── LocalStorageService.java     # 本地磁盘实现
│       │   │   ├── MinioStorageService.java     # MinIO 实现
│       │   │   └── StorageProperties.java       # 配置属性
│       │   │
│       │   ├── camera/                    # --- 摄像头管理 ---
│       │   │   ├── controller/
│       │   │   │   └── CameraController.java    # /api/v1/cameras/**
│       │   │   ├── service/
│       │   │   │   └── CameraService.java
│       │   │   ├── mapper/
│       │   │   │   └── CameraMapper.java
│       │   │   ├── entity/
│       │   │   │   ├── Camera.java
│       │   │   │   └── CameraGroup.java
│       │   │   └── dto/
│       │   │       ├── CameraCreateDTO.java
│       │   │       └── CameraVO.java
│       │   │
│       │   ├── platform/                  # --- 视频平台对接 ---
│       │   │   ├── controller/
│       │   │   │   └── VideoPlatformController.java  # /api/v1/video-platforms/**
│       │   │   ├── service/
│       │   │   │   ├── VideoPlatformService.java
│       │   │   │   └── PlatformSyncService.java     # 摄像头同步逻辑
│       │   │   ├── mapper/
│       │   │   │   └── VideoPlatformMapper.java
│       │   │   └── entity/
│       │   │       └── VideoPlatform.java
│       │   │
│       │   ├── model/                     # --- 模型管理 ---
│       │   │   ├── controller/
│       │   │   │   └── ModelController.java     # /api/v1/models/**
│       │   │   ├── service/
│       │   │   │   ├── ModelService.java
│       │   │   │   └── InferenceClient.java     # 调用 Python 推理服务的 HTTP 客户端
│       │   │   ├── mapper/
│       │   │   │   └── ModelMapper.java
│       │   │   └── entity/
│       │   │       ├── Model.java
│       │   │       └── ModelVersion.java
│       │   │
│       │   ├── rule/                      # --- 规则引擎 ---
│       │   │   ├── controller/
│       │   │   │   └── RuleController.java      # /api/v1/rules/**
│       │   │   ├── service/
│       │   │   │   ├── RuleService.java
│       │   │   │   └── RuleEvaluator.java       # 规则条件评估（纯 Java）
│       │   │   ├── mapper/
│       │   │   │   └── RuleMapper.java
│       │   │   └── entity/
│       │   │       ├── Rule.java
│       │   │       └── RuleCondition.java
│       │   │
│       │   ├── inference/                 # --- 推理记录 ---
│       │   │   ├── controller/
│       │   │   │   └── InferenceController.java # /api/v1/inference/**
│       │   │   ├── service/
│       │   │   │   └── InferenceService.java
│       │   │   ├── mapper/
│       │   │   │   └── InferenceMapper.java
│       │   │   └── entity/
│       │   │       ├── InferenceRecord.java
│       │   │       └── Detection.java
│       │   │
│       │   ├── alert/                     # --- 告警管理 ---
│       │   │   ├── controller/
│       │   │   │   └── AlertController.java
│       │   │   ├── service/
│       │   │   │   ├── AlertService.java
│       │   │   │   └── AlertPushService.java    # WebSocket 推送
│       │   │   ├── mapper/
│       │   │   │   └── AlertMapper.java
│       │   │   └── entity/
│       │   │       └── Alert.java
│       │   │
│       │   ├── capture/                   # --- 抓图任务调度 ---
│       │   │   ├── service/
│       │   │   │   ├── CaptureScheduler.java    # 定时调度器
│       │   │   │   ├── CaptureTaskService.java  # 任务执行（FFmpeg 抓帧 → 推理）
│       │   │   │   └── InferencePipeline.java   # 编排：抓帧→存储→推理→规则→告警
│       │   │   └── dto/
│       │   │       └── CaptureMessage.java      # 内部消息体
│       │   │
│       │   └── dashboard/                 # --- 仪表板 ---
│       │       ├── controller/
│       │       │   └── DashboardController.java # /api/v1/dashboard/**
│       │       └── service/
│       │           └── DashboardService.java    # 聚合统计查询
│       │
│       └── resources/
│           ├── application.yml            # 主配置
│           ├── application-dev.yml        # 开发环境
│           ├── application-prod.yml       # 生产环境
│           └── db/
│               └── migration/            # 数据库迁移脚本（Flyway）
│                   ├── V1__init_schema.sql
│                   └── V2__init_data.sql
│
├── vision-inference/            # Python 推理服务
│   ├── Dockerfile
│   ├── requirements.txt
│   ├── gunicorn.conf.py
│   ├── README.md                # 模块说明（AI 友好）
│   ├── app.py                   # Flask 入口，路由定义
│   ├── config.py                # 配置（模型路径、设备类型等）
│   ├── inference/
│   │   ├── __init__.py
│   │   ├── engine.py            # YOLO 推理引擎封装
│   │   ├── model_manager.py     # 模型加载/卸载/列表
│   │   └── preprocess.py        # 图像预处理（缩放、填充）
│   ├── stream/
│   │   ├── __init__.py
│   │   ├── capture.py           # RTSP 流抓帧（OpenCV）
│   │   └── stream_task.py       # 流式推理任务管理
│   └── tests/
│       └── test_inference.py
│
└── frontend/                    # 前端（已有，忽略）
```

## 六、模块协作流程

### 6.1 图片推理流程（定时抓图）

```
┌──────────────┐     ┌───────────────┐     ┌──────────────────┐
│ CaptureScheduler │──→│ FFmpeg 抓帧    │──→│ StorageService    │
│ (定时轮询活跃  │     │ (RTSP → JPEG) │     │ (保存图片,返回URL) │
│  摄像头配置)   │     └───────────────┘     └────────┬─────────┘
└──────────────┘                                     │
                                                     ▼
┌──────────────┐     ┌───────────────┐     ┌──────────────────┐
│ RuleEvaluator │◀──│ InferencePipeline│◀──│ InferenceClient   │
│ (评估规则条件) │     │ (编排整个流程)  │     │ (HTTP→Python服务) │
└──────┬───────┘     └───────────────┘     └──────────────────┘
       │ 命中规则
       ▼
┌──────────────┐     ┌───────────────┐
│ AlertService  │──→│ AlertPushService│──→ WebSocket → 前端
│ (存储告警)    │     │ (实时推送)     │
└──────────────┘     └───────────────┘
```

**时序描述:**
1. `CaptureScheduler` 根据摄像头配置的 `captureFrequency` 定时触发
2. 通过 `FFmpegUtil` 执行命令：`ffmpeg -rtsp_transport tcp -i {rtspUrl} -frames:v 1 -f image2 {outputPath}`
3. 图片通过 `StorageService` 保存到 MinIO 或本地磁盘，获得访问 URL
4. 构建推理请求，通过 `InferenceClient` 调用 Python 服务 `POST /predict`
5. Python 返回检测结果（目标列表 + 置信度 + bbox）
6. `InferencePipeline` 将结果存入 `inference_record` 表
7. `RuleEvaluator` 拿到检测结果，匹配当前生效的规则
8. 命中规则 → 生成告警 → 存库 → WebSocket 推送给前端

### 6.2 流式推理流程（高帧率实时场景）

```
Java (vision-admin)                    Python (vision-inference)
       │                                        │
       │  POST /stream/start                     │
       │  {streamUrl, modelId, fps, callbackUrl} │
       │───────────────────────────────────────→│
       │                                        │
       │          Python 启动后台线程:             │
       │          OpenCV 拉流 → YOLO 推理          │
       │          每帧结果 POST → callbackUrl      │
       │                                        │
       │  POST /api/v1/inference/callback        │
       │◀───────────────────────────────────────│ (逐帧回调)
       │                                        │
       │  规则评估 → 告警 → WebSocket推送          │
       │                                        │
       │  POST /stream/stop                      │
       │───────────────────────────────────────→│
```

### 6.3 模型管理流程

```
前端 → Java (POST /api/v1/models/{id}/load)
         │
         ├─→ 更新数据库模型状态为 "loading"
         │
         ├─→ 调用 Python: POST /models/load  { modelPath, modelId }
         │         Python 加载 YOLO 模型到内存/GPU
         │         返回 { success: true }
         │
         └─→ 更新数据库模型状态为 "loaded"
              响应前端
```

## 七、API 设计

### 7.1 vision-admin 对外 API（供前端调用）

与前端已有的 API 契约完全对齐：

```
前缀: /api/v1

# 摄像头
GET    /cameras                    # 摄像头列表（分页，query: page, size, groupId, status）
GET    /cameras/{id}               # 摄像头详情
POST   /cameras                    # 创建摄像头
PUT    /cameras/{id}               # 更新摄像头
DELETE /cameras/{id}               # 删除摄像头
GET    /cameras/groups             # 摄像头分组树
POST   /cameras/import             # 批量导入

# 视频平台
GET    /video-platforms            # 平台列表
POST   /video-platforms            # 创建平台
PUT    /video-platforms/{id}       # 更新平台
DELETE /video-platforms/{id}       # 删除平台
POST   /video-platforms/{id}/test  # 测试连接
POST   /video-platforms/{id}/sync  # 同步摄像头

# 模型
GET    /models                     # 模型列表
GET    /models/{id}                # 模型详情
POST   /models/{id}/load           # 加载模型
POST   /models/{id}/unload         # 卸载模型
PUT    /models/{id}/config         # 更新模型配置
DELETE /models/{id}                # 删除模型

# 规则
GET    /rules                      # 规则列表
GET    /rules/{id}                 # 规则详情
POST   /rules                      # 创建规则
PUT    /rules/{id}                 # 更新规则
DELETE /rules/{id}                 # 删除规则
POST   /rules/{id}/deploy          # 部署规则
POST   /rules/{id}/test            # 测试规则

# 推理记录
GET    /inference                  # 推理记录列表（分页）
GET    /inference/{id}             # 推理详情
GET    /inference/export/csv       # 导出 CSV
GET    /inference/export/excel     # 导出 Excel
POST   /inference/callback         # Python 推理结果回调（内部接口）

# 仪表板
GET    /dashboard/stats            # 统计概览
GET    /dashboard/weekly-trend     # 周趋势
GET    /dashboard/alert-ranking    # 业务线告警排名
GET    /dashboard/realtime-alerts  # 实时告警列表
```

### 7.2 vision-inference 内部 API（仅 Java 调用）

```
前缀: 无（内网直连，如 http://vision-inference:5000）

# 推理
POST   /predict                    # 单张图片推理
       请求: { "image_url": "...", "model_id": "...", "confidence_threshold": 0.5 }
       响应: { "objects": [...], "inference_time_ms": 45 }

# 模型管理
POST   /models/load                # 加载模型
       请求: { "model_id": "...", "model_path": "/models/xxx.pt", "device": "gpu" }
POST   /models/unload              # 卸载模型
       请求: { "model_id": "..." }
GET    /models/status              # 已加载模型列表

# 流式推理
POST   /stream/start               # 启动流式推理
       请求: { "task_id": "...", "stream_url": "rtsp://...", 
               "model_id": "...", "fps": 5, "callback_url": "http://vision-admin:8080/api/v1/inference/callback" }
POST   /stream/stop                # 停止流式推理
       请求: { "task_id": "..." }
GET    /stream/tasks               # 活跃的流式任务列表

# 健康检查
GET    /health                     # 服务健康状态
```

### 7.3 统一响应格式

```json
// 成功
{
  "code": 200,
  "message": "success",
  "data": { ... }
}

// 分页
{
  "code": 200,
  "message": "success",
  "data": {
    "items": [ ... ],
    "total": 100
  }
}

// 错误
{
  "code": 400,
  "message": "摄像头名称不能为空",
  "data": null
}
```

## 八、数据库设计

### 8.1 ER 概览

```
camera ──┐
          ├──→ inference_record ──→ detection
          │           │
camera_group          │
                      ▼
video_platform   rule ──→ rule_condition
                      │
                      ▼
                   alert
                      
model ──→ model_version

system_config
```

### 8.2 核心表结构

```sql
-- 摄像头分组
CREATE TABLE camera_group (
    id            VARCHAR(36) PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    parent_id     VARCHAR(36),
    icon          VARCHAR(50),
    sort_order    INT DEFAULT 0,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 摄像头
CREATE TABLE camera (
    id                VARCHAR(36) PRIMARY KEY,
    name              VARCHAR(200) NOT NULL,
    business_line     VARCHAR(50) NOT NULL,        -- 业务线
    location          VARCHAR(500),
    stream_url        VARCHAR(1000),               -- RTSP 地址
    capture_frequency VARCHAR(50) DEFAULT '5min',  -- 抓图频率: 1s,5s,1min,5min
    ai_enabled        BOOLEAN DEFAULT FALSE,
    status            VARCHAR(20) DEFAULT 'offline', -- online/offline/error
    last_capture_time TIMESTAMP,
    group_id          VARCHAR(36) REFERENCES camera_group(id),
    source            VARCHAR(20) DEFAULT 'manual',  -- manual/synced
    platform_id       VARCHAR(36),
    channel_no        VARCHAR(100),
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 视频平台
CREATE TABLE video_platform (
    id                VARCHAR(36) PRIMARY KEY,
    name              VARCHAR(200) NOT NULL,
    api_base          VARCHAR(500) NOT NULL,
    auth_type         VARCHAR(20) DEFAULT 'none',  -- token/basic/none
    credential        VARCHAR(500),
    auto_sync         BOOLEAN DEFAULT FALSE,
    sync_interval_min INT DEFAULT 60,
    last_sync_time    TIMESTAMP,
    last_sync_result  JSONB,                       -- SyncResult JSON
    cameras_count     INT DEFAULT 0,
    status            VARCHAR(20) DEFAULT 'disconnected',
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 模型
CREATE TABLE model (
    id                   VARCHAR(36) PRIMARY KEY,
    name                 VARCHAR(200) NOT NULL,
    version              VARCHAR(50) NOT NULL,
    business_tag         VARCHAR(100),
    engine_support       VARCHAR(200),              -- 逗号分隔: TRT,ONNX
    target_hardware      VARCHAR(100),
    status               VARCHAR(20) DEFAULT 'unloaded', -- loaded/unloaded
    confidence_threshold DECIMAL(3,2) DEFAULT 0.50,
    input_resolution     VARCHAR(20) DEFAULT '640x640',
    max_concurrency      INT DEFAULT 1,
    model_path           VARCHAR(500),
    author               VARCHAR(100),
    avg_latency          INT DEFAULT 0,             -- ms
    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 模型版本历史
CREATE TABLE model_version (
    id          VARCHAR(36) PRIMARY KEY,
    model_id    VARCHAR(36) REFERENCES model(id) ON DELETE CASCADE,
    version     VARCHAR(50) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 规则
CREATE TABLE rule (
    id              VARCHAR(36) PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    business_line   VARCHAR(50),
    enabled         BOOLEAN DEFAULT TRUE,
    priority        VARCHAR(20) DEFAULT 'warning',   -- severe/warning/info
    schedule        VARCHAR(50),                      -- '10:00-18:00' 或 '全时段'
    weekdays        VARCHAR(20),                      -- '1,2,3,4,5'
    effective_start DATE,
    effective_end   DATE,
    actions         JSONB NOT NULL,                   -- RuleAction JSON
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 规则条件
CREATE TABLE rule_condition (
    id        VARCHAR(36) PRIMARY KEY,
    rule_id   VARCHAR(36) REFERENCES rule(id) ON DELETE CASCADE,
    type      VARCHAR(20) NOT NULL,   -- target/confidence/frames/zone
    operator  VARCHAR(10) NOT NULL,   -- =, >, >=, <, in
    value     VARCHAR(200) NOT NULL
);

-- 推理记录
CREATE TABLE inference_record (
    id                 VARCHAR(36) PRIMARY KEY,
    event_id           VARCHAR(36),
    camera_id          VARCHAR(36) REFERENCES camera(id),
    business_type      VARCHAR(100),
    avg_confidence     DECIMAL(3,2),
    alert_status       VARCHAR(20) DEFAULT 'normal',  -- normal/warning/alert
    thumbnail_url      VARCHAR(500),
    original_image_url VARCHAR(500),
    annotated_image_url VARCHAR(500),
    raw_json           JSONB,                          -- 完整推理原始结果
    model_name         VARCHAR(200),
    inference_time_ms  INT,
    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 检测目标（推理结果关联表）
CREATE TABLE detection (
    id            VARCHAR(36) PRIMARY KEY,
    record_id     VARCHAR(36) REFERENCES inference_record(id) ON DELETE CASCADE,
    label         VARCHAR(100) NOT NULL,
    confidence    DECIMAL(4,3),
    bbox          VARCHAR(100),   -- 'x1,y1,x2,y2'
    count         INT DEFAULT 1,
    attributes    JSONB
);

-- 告警
CREATE TABLE alert (
    id                VARCHAR(36) PRIMARY KEY,
    alert_level       VARCHAR(20) NOT NULL,   -- critical/warning/info
    alert_type        VARCHAR(100),
    scene             VARCHAR(50),
    camera_id         VARCHAR(36) REFERENCES camera(id),
    stream_id         VARCHAR(100),
    capture_time      TIMESTAMP,
    alert_time        TIMESTAMP,
    trigger_condition TEXT,
    related_objects   JSONB,        -- 触发告警的目标列表
    evidence          JSONB,        -- { image_url, video_clip_url, snapshots }
    location          JSONB,        -- { camera_name, address, longitude, latitude }
    rule_id           VARCHAR(36),
    read_status       BOOLEAN DEFAULT FALSE,
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 系统配置
CREATE TABLE system_config (
    config_key   VARCHAR(100) PRIMARY KEY,
    config_value TEXT,
    description  VARCHAR(500),
    updated_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 索引
CREATE INDEX idx_camera_group ON camera(group_id);
CREATE INDEX idx_camera_status ON camera(status);
CREATE INDEX idx_inference_camera ON inference_record(camera_id);
CREATE INDEX idx_inference_created ON inference_record(created_at);
CREATE INDEX idx_alert_created ON alert(created_at);
CREATE INDEX idx_alert_level ON alert(alert_level);
CREATE INDEX idx_detection_record ON detection(record_id);
```

## 九、存储抽象层设计

```java
/**
 * 存储服务接口 — 所有文件操作通过此接口，运行时按配置切换实现
 */
public interface StorageService {
    
    /**
     * 上传文件
     * @param inputStream 文件流
     * @param path 存储路径，如 "images/2026-03-28/cam_001_xxx.jpg"
     * @return 可访问的 URL
     */
    String upload(InputStream inputStream, String path, String contentType);

    /**
     * 获取文件访问 URL
     */
    String getUrl(String path);

    /**
     * 删除文件
     */
    void delete(String path);

    /**
     * 检查文件是否存在
     */
    boolean exists(String path);
}
```

**配置切换**（application.yml）：

```yaml
vision:
  storage:
    # 可选值: local, minio
    type: local
    
    local:
      base-path: /data/vision/files
      # 通过 Nginx 或 Spring 静态资源映射对外提供访问
      base-url: http://localhost:8080/files
      
    minio:
      endpoint: http://minio:9000
      access-key: ${MINIO_ACCESS_KEY:minioadmin}
      secret-key: ${MINIO_SECRET_KEY:minioadmin}
      bucket: vision
```

## 十、规则引擎设计

纯 Java 实现，不引入额外依赖。规则条件结构固定，采用组合评估模式：

```java
/**
 * 规则评估器 — 将推理结果与规则条件进行匹配
 * 
 * 支持的条件类型:
 *   target:     目标类别匹配，如 class_name = "leak"
 *   confidence: 置信度阈值，如 confidence > 0.7
 *   frames:     连续帧数（需配合状态缓存），如 duration > 3
 *   zone:       区域检测（bbox 与预设区域交叉判定）
 *
 * 所有条件之间为 AND 关系（全部满足才触发）
 */
@Service
public class RuleEvaluator {

    /**
     * 评估推理结果是否命中规则
     * @param detections 推理检测结果列表
     * @param rule       待评估规则
     * @return 命中的告警信息，null 表示未命中
     */
    public Alert evaluate(List<Detection> detections, Rule rule) {
        // 1. 检查规则是否在生效时间内
        // 2. 逐条件评估（AND 逻辑）
        // 3. 全部满足 → 构建 Alert 对象返回
    }
}
```

## 十一、部署架构

### 11.1 Docker Compose（完整模式）

```yaml
# docker-compose.yml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: vision
      POSTGRES_USER: ${DB_USER:-vision}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-vision123}
    volumes:
      - pg_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

  minio:
    image: minio/minio:latest
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: ${MINIO_ACCESS_KEY:-minioadmin}
      MINIO_ROOT_PASSWORD: ${MINIO_SECRET_KEY:-minioadmin}
    volumes:
      - minio_data:/data
    ports:
      - "9000:9000"
      - "9001:9001"
    profiles:
      - full

  vision-admin:
    build: ./vision-admin
    environment:
      SPRING_PROFILES_ACTIVE: prod
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: vision
      DB_USER: ${DB_USER:-vision}
      DB_PASSWORD: ${DB_PASSWORD:-vision123}
      INFERENCE_SERVICE_URL: http://vision-inference:5000
      VISION_STORAGE_TYPE: ${STORAGE_TYPE:-local}
      MINIO_ENDPOINT: http://minio:9000
      MINIO_ACCESS_KEY: ${MINIO_ACCESS_KEY:-minioadmin}
      MINIO_SECRET_KEY: ${MINIO_SECRET_KEY:-minioadmin}
    volumes:
      - file_data:/data/vision/files
      - model_data:/data/vision/models
    ports:
      - "8080:8080"
    depends_on:
      - postgres

  vision-inference:
    build: ./vision-inference
    environment:
      MODEL_BASE_PATH: /data/vision/models
      DEVICE: ${INFERENCE_DEVICE:-cpu}  # cpu 或 cuda
    volumes:
      - model_data:/data/vision/models
      - file_data:/data/vision/files
    ports:
      - "5000:5000"
    # GPU 支持（需要 nvidia-docker）
    # deploy:
    #   resources:
    #     reservations:
    #       devices:
    #         - driver: nvidia
    #           count: 1
    #           capabilities: [gpu]

volumes:
  pg_data:
  minio_data:
  file_data:
  model_data:
```

### 11.2 一键部署脚本

```bash
#!/bin/bash
# deploy.sh - 一键部署脚本

set -e

echo "=============================="
echo "  视觉分析平台 - 部署工具"
echo "=============================="

# 部署模式选择
echo ""
echo "请选择部署模式:"
echo "  1) lite  - 轻量模式（本地磁盘存储，无 MinIO）"
echo "  2) full  - 完整模式（含 MinIO 对象存储）"
read -p "请输入 [1/2] (默认 1): " mode

# 推理设备选择
echo ""
echo "请选择推理设备:"
echo "  1) cpu   - CPU 推理"
echo "  2) gpu   - GPU 推理（需要 nvidia-docker）"
read -p "请输入 [1/2] (默认 1): " device

# 设置变量
if [ "$mode" = "2" ]; then
    STORAGE_TYPE="minio"
    COMPOSE_PROFILES="full"
else
    STORAGE_TYPE="local"
    COMPOSE_PROFILES=""
fi

if [ "$device" = "2" ]; then
    INFERENCE_DEVICE="cuda"
else
    INFERENCE_DEVICE="cpu"
fi

# 复制环境变量文件
if [ ! -f .env ]; then
    cp .env.example .env
    echo "已创建 .env 文件，请根据需要修改配置"
fi

# 导出变量
export STORAGE_TYPE=$STORAGE_TYPE
export INFERENCE_DEVICE=$INFERENCE_DEVICE

# 构建并启动
echo ""
echo "开始构建和部署..."
if [ -n "$COMPOSE_PROFILES" ]; then
    docker compose --profile $COMPOSE_PROFILES up -d --build
else
    docker compose up -d --build
fi

echo ""
echo "=============================="
echo "  部署完成!"
echo "  管理后台: http://localhost:8080"
echo "  推理服务: http://localhost:5000/health"
if [ "$STORAGE_TYPE" = "minio" ]; then
    echo "  MinIO 控制台: http://localhost:9001"
fi
echo "=============================="
```

### 11.3 环境变量模板

```env
# .env.example

# ---- 数据库 ----
DB_USER=vision
DB_PASSWORD=vision123

# ---- 存储 ----
# local 或 minio
STORAGE_TYPE=local

# ---- MinIO（STORAGE_TYPE=minio 时生效）----
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin

# ---- 推理 ----
# cpu 或 cuda
INFERENCE_DEVICE=cpu
```

## 十二、关键配置文件

### application.yml（vision-admin）

```yaml
server:
  port: 8080
  servlet:
    context-path: /

spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:vision}
    username: ${DB_USER:vision}
    password: ${DB_PASSWORD:vision123}
    driver-class-name: org.postgresql.Driver
  flyway:
    enabled: true
    locations: classpath:db/migration

mybatis-plus:
  mapper-locations: classpath*:mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true

# ---- 推理服务 ----
vision:
  inference:
    service-url: ${INFERENCE_SERVICE_URL:http://localhost:5000}
    connect-timeout: 5000
    read-timeout: 30000

  # ---- 存储 ----
  storage:
    type: ${VISION_STORAGE_TYPE:local}
    local:
      base-path: /data/vision/files
      base-url: ${LOCAL_STORAGE_URL:http://localhost:8080/files}
    minio:
      endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
      access-key: ${MINIO_ACCESS_KEY:minioadmin}
      secret-key: ${MINIO_SECRET_KEY:minioadmin}
      bucket: vision

  # ---- 抓图 ----
  capture:
    ffmpeg-path: ${FFMPEG_PATH:ffmpeg}
    thread-pool-size: 10
    temp-dir: /tmp/vision-capture
```

## 十三、AI 友好约定

为确保后续 AI 能高效理解和修改代码，全项目遵循以下约定：

### 13.1 命名规范

| 层级 | 规范 | 示例 |
|------|------|------|
| 包名 | 按业务模块划分，不按技术层划分 | `com.vision.camera`，非 `com.vision.controller` |
| Controller | `{Module}Controller` | `CameraController` |
| Service | `{Module}Service` | `CameraService` |
| Mapper | `{Module}Mapper` | `CameraMapper` |
| Entity | 单数名词 | `Camera`，非 `Cameras` |
| DTO | `{Module}{Action}DTO` | `CameraCreateDTO` |
| VO | `{Module}VO` | `CameraVO` |

### 13.2 模块 README

每个模块（camera、model、rule 等）内部包含 `README.md`，内容模板：

```markdown
# 模块名称

## 职责
一句话描述本模块功能

## 对外接口
列出 Controller 暴露的所有 API

## 依赖的其他模块
列出本模块调用了哪些其他模块的 Service

## 数据表
列出本模块管理的数据库表

## 注意事项
特殊逻辑、坑点说明
```

### 13.3 代码组织原则

1. **一个文件一个类** — Entity、DTO、VO 各自独立文件
2. **Controller 不写业务逻辑** — 只做参数校验和转发
3. **Service 不写 SQL** — SQL 全部在 Mapper（XML 或注解）
4. **配置外置** — 所有可变参数走 application.yml，不硬编码
5. **注释写在"为什么"而不是"是什么"** — 代码本身说明"是什么"

### 13.4 接口契约文件

项目根目录维护 `api-contract.md`，记录所有 API 的请求/响应格式，前后端以此为准，AI 修改代码时首先参考此文件。

## 十四、开发路线建议

按照依赖关系，推荐以下开发顺序：

```
Phase 1 — 基础骨架
  ├── Spring Boot 项目初始化 + 数据库建表
  ├── 统一响应/异常处理/CORS 配置
  ├── 存储抽象层（local 实现）
  └── Python Flask 推理服务骨架

Phase 2 — 核心 CRUD
  ├── 摄像头管理（camera + camera_group）
  ├── 模型管理（model + model_version）
  └── 规则管理（rule + rule_condition）

Phase 3 — 推理链路
  ├── FFmpeg 抓帧集成
  ├── InferenceClient → Python 推理调用
  ├── InferencePipeline 编排
  └── 推理记录存储

Phase 4 — 规则 & 告警
  ├── RuleEvaluator 实现
  ├── 告警存储 + WebSocket 推送
  └── Dashboard 统计查询

Phase 5 — 部署 & 集成
  ├── Dockerfile（Java + Python）
  ├── docker-compose.yml
  ├── MinIO 存储实现
  ├── 视频平台同步
  └── 前后端联调
```

## 十五、依赖清单

最终系统运行只需要以下组件：

| 组件 | 必选/可选 | 部署方式 |
|------|----------|---------|
| PostgreSQL 16 | **必选** | Docker 容器 |
| FFmpeg 6.0+ | **必选** | 预装在 Java 容器镜像中 |
| MinIO | 可选 | Docker 容器（full 模式） |
| JDK 17+ | **必选** | Java 容器镜像基础 |
| Python 3.11+ | **必选** | Python 容器镜像基础 |

