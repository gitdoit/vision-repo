# 视觉分析平台 — AI 开发指南

## 项目概述

工业/安防视觉分析平台，核心链路：**摄像头接入 → 定时抓图 → YOLO 推理 → 规则匹配 → 告警推送**。
支持多推理节点水平扩展（GPU 节点自动注册 + 心跳）。

| 文档 | 说明 |
|------|------|
| [PROJECT.md](../PROJECT.md) | 完整架构设计 |
| [业务需求.md](../业务需求.md) | 功能规格 |
| [监测任务模块设计与实现.md](../监测任务模块设计与实现.md) | 监测任务调度设计 |
| [多节点推理服务架构重构.md](../多节点推理服务架构重构.md) | 多节点推理架构 |
| [系统流程总结.md](../系统流程总结.md) | 端到端流程总结 |

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

## API 契约

- 路径前缀：`/api/v1`
- 统一响应：`{ code: number, message: string, data: T }`
- 分页请求：`?page=1&size=20`，响应：`{ items: [], total: number }`
- 所有主键：UUID（VARCHAR 36）
- 时间格式：ISO 8601（`yyyy-MM-dd'T'HH:mm:ss`）
- 状态枚举：英文小写（`online`/`offline`/`loaded`/`unloaded`/`enabled`/`disabled`）

> 各模块详细技术栈与编码规范见 `.github/instructions/` 下的 scoped 指令文件（按需加载，不重复注入）。

## 模块间通信

```
前端 ──HTTP──→ vision-admin（Java）──HTTP──→ vision-inference（Python） × N 节点
                    │                              │
                    ├── PostgreSQL                  ├── YOLO 模型文件
                    ├── StorageService              ├── RTSP 流抓帧
                    │   (本地磁盘/MinIO)            └── 自动注册 + 心跳
                    └── 推理节点管理
                        (注册/心跳/负载)

Python → Java 回调：POST /api/v1/inference/callback（流式推理结果）
Python → Java 注册：POST /api/v1/nodes/register + PUT /nodes/{id}/heartbeat
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
| 节点注册模块 | [vision-inference/registration.py](../vision-inference/registration.py) |
| 节点状态持久化 | [vision-inference/state_store.py](../vision-inference/state_store.py) |
| Docker 完整部署 | [docker-compose.yml](../docker-compose.yml) |
| Docker 轻量部署 | [docker-compose.lite.yml](../docker-compose.lite.yml) |

## 注意事项

- **修改 API 接口时**：前后端类型定义必须同步更新（`frontend/src/types/index.ts` ↔ Java DTO/VO）
- **新增数据库表/列时**：必须编写 Flyway 迁移脚本，不要手动改库
- **模型相关操作**：加载/卸载指令从 Java 发起，Python 只执行
- **存储文件路径**：`{类型}/{日期}/{文件名}`，如 `images/2026-03-29/cam001_abc123.jpg`
- **已加载模型不允许删除**：需先卸载再删
- **规则发布后即时生效**：无需重启服务
- **推理节点**：Python 服务启动后自动注册到 Java 服务，通过心跳保活；Java 通过 `inference_node` 表管理节点状态
- **端口约定**：Java 26330，Python 26331，前端 5173（开发）
- **当前无单元测试**：`src/test/` 和前端均无测试文件，添加测试时 Java 用 JUnit 5 + MockMvc，前端用 Vitest
- 查看 [bugs.md](../bugs.md) 了解当前已知问题
