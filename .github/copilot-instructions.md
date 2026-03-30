# 视觉分析平台 — AI 开发指南

## 项目概述

工业/安防视觉分析平台，核心链路：**摄像头接入 → 定时抓图 → YOLO 推理 → 规则匹配 → 告警推送**。
详见 [PROJECT.md](../PROJECT.md) 获取完整架构设计，[业务需求.md](../业务需求.md) 获取功能规格。

## 仓库结构

```
vision-repo/
├── vision-admin/        # Java 后端（Spring Boot 3.2 + MyBatis-Plus）— 全部业务逻辑
├── vision-inference/    # Python 推理服务（Flask + Ultralytics YOLO）— 仅做推理
├── frontend/            # Vue 3 前端（Vite + Naive UI + Tailwind + Pinia）
├── docker-compose.yml   # 完整部署（含 MinIO）
├── docker-compose.lite.yml  # 轻量部署（本地磁盘）
└── deploy.sh            # 一键部署脚本
```

## 构建与运行

```bash
# Java 后端
cd vision-admin
mvn clean package -DskipTests          # 构建 JAR
mvn spring-boot:run -Dspring-boot.run.profiles=dev  # 开发运行

# Python 推理
cd vision-inference
pip install -r requirements.txt
python app.py                           # 开发运行
gunicorn -c gunicorn.conf.py app:app    # 生产运行

# 前端
cd frontend
npm install
npm run dev                             # 开发 http://localhost:5173

# Docker 一键部署
./deploy.sh --mode lite --device cpu    # 轻量 CPU
./deploy.sh --mode full --device gpu    # 完整 GPU
```

## 技术栈约定

### Java（vision-admin）

| 组件 | 技术 | 备注 |
|------|------|------|
| 框架 | Spring Boot 3.2.5 / Java 17 | |
| ORM | MyBatis-Plus 3.5.6 | mapper XML 在 `resources/mapper/` |
| 数据库 | PostgreSQL 16 | Flyway 迁移在 `resources/db/migration/` |
| 缓存 | Caffeine（本地缓存） | 不用 Redis |
| 任务调度 | Spring @Scheduled | 不用 Kafka |
| WebSocket | Spring WebSocket | 实时告警推送 |
| 文件存储 | StorageService 接口 | 本地磁盘 / MinIO 双实现，配置切换 |
| API 文档 | SpringDoc OpenAPI 2.4 | Swagger UI |

### Python（vision-inference）

| 组件 | 技术 | 备注 |
|------|------|------|
| Web | Flask 3.0 | 不用 FastAPI |
| 推理 | Ultralytics 8.4+ | YOLO 系列 |
| 图像 | OpenCV headless | |
| 部署 | Gunicorn | gthread worker |

### 前端（frontend）

| 组件 | 技术 |
|------|------|
| 框架 | Vue 3.5 + TypeScript 5.6 |
| 构建 | Vite 5.4 |
| UI 库 | Naive UI |
| 样式 | Tailwind CSS 4 |
| 状态 | Pinia |
| 图表 | ECharts |
| Mock | MSW（Mock Service Worker） |

## 代码规范

### Java 分包规则 — 按业务模块，非技术层

```
com.vision.{module}/
├── controller/    # REST API，不写业务逻辑
├── service/       # 业务逻辑
├── mapper/        # MyBatis 接口
├── entity/        # 数据库实体
└── dto/           # 请求/响应 DTO（含 VO）
```

模块列表：`camera`, `model`, `rule`, `inference`, `alert`, `dashboard`, `capture`, `storage`, `config`, `common`

### 命名约定

| 类型 | 模式 | 示例 |
|------|------|------|
| Controller | `{Module}Controller` | `CameraController` |
| Service | `{Module}Service` | `CameraService` |
| Mapper | `{Module}Mapper` | `CameraMapper` |
| Entity | 单数名词 | `Camera` |
| 请求 DTO | `{Module}CreateDTO` / `{Module}UpdateDTO` | `CameraCreateDTO` |
| 响应 VO | `{Module}VO` | `CameraVO` |

### API 契约

- 路径前缀：`/api/v1`
- 统一响应：`{ code: number, message: string, data: T }`
- 分页请求：`?page=1&size=20`，响应：`{ items: [], total: number }`
- 所有主键：UUID（VARCHAR 36）
- 时间格式：ISO 8601（`yyyy-MM-dd'T'HH:mm:ss`）
- 状态枚举：英文小写（`online`/`offline`/`loaded`/`unloaded`/`enabled`/`disabled`）

### 数据库规范

- 表名/列名：snake_case
- MyBatis-Plus 自动驼峰映射（`map-underscore-to-camel-case: true`）
- 主键策略：`assign_uuid`
- 软删除：`deleted` 字段
- JSONB 用于灵活配置字段（`raw_json`, `actions`, `evidence`, `related_objects`）
- Schema 变更必须通过 Flyway 迁移脚本（`V{n}__{description}.sql`）

### Python 规范

- Flask 路由定义在 `app.py`
- 业务逻辑在 `inference/` 和 `stream/` 子包
- 配置通过环境变量，默认值在 `config.py`
- 线程安全：ModelManager 和 StreamTaskManager 均为线程安全单例

## 模块间通信

```
前端 ──HTTP──→ vision-admin（Java）──HTTP──→ vision-inference（Python）
                    │                              │
                    ├── PostgreSQL                  ├── YOLO 模型文件
                    └── StorageService              └── RTSP 流抓帧
                        (本地磁盘/MinIO)

Python → Java 回调：POST /api/v1/inference/callback（流式推理结果）
```

## 关键文件索引

| 用途 | 文件 |
|------|------|
| 架构设计 | [PROJECT.md](../PROJECT.md) |
| 业务需求 | [业务需求.md](../业务需求.md) |
| 已知 Bug | [bugs.md](../bugs.md) |
| 数据库初始化 | [V1__init_schema.sql](../vision-admin/src/main/resources/db/migration/V1__init_schema.sql) |
| 种子数据 | [V2__init_data.sql](../vision-admin/src/main/resources/db/migration/V2__init_data.sql) |
| Java 主配置 | [application.yml](../vision-admin/src/main/resources/application.yml) |
| Python 入口 | [vision-inference/app.py](../vision-inference/app.py) |
| Python API 文档 | [vision-inference/README.md](../vision-inference/README.md) |
| 前端 API 客户端 | [frontend/src/api/client.ts](../frontend/src/api/client.ts) |
| 前端类型定义 | [frontend/src/types/index.ts](../frontend/src/types/index.ts) |
| Docker 完整部署 | [docker-compose.yml](../docker-compose.yml) |
| Docker 轻量部署 | [docker-compose.lite.yml](../docker-compose.lite.yml) |

## 注意事项

- **修改 API 接口时**：前后端类型定义必须同步更新（`frontend/src/types/index.ts` ↔ Java DTO/VO）
- **新增数据库表/列时**：必须编写 Flyway 迁移脚本，不要手动改库
- **模型相关操作**：加载/卸载指令从 Java 发起，Python 只执行
- **存储文件路径**：`{类型}/{日期}/{文件名}`，如 `images/2026-03-29/cam001_abc123.jpg`
- **已加载模型不允许删除**：需先卸载再删
- **规则发布后即时生效**：无需重启服务
- 查看 [bugs.md](../bugs.md) 了解当前已知问题
