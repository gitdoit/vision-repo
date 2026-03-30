---
description: "Use when working on Docker deployment, docker-compose configuration, deploy scripts, CI/CD, or production environment setup."
applyTo: "docker-compose*.yml, deploy.sh, **/Dockerfile"
---
# 部署规范

## 部署模式

| 模式 | 命令 | 存储 | 说明 |
|------|------|------|------|
| lite | `./deploy.sh --mode lite --device cpu` | 本地磁盘 | 最小依赖，适合小型项目 |
| full | `./deploy.sh --mode full --device gpu` | MinIO | 含对象存储，适合生产环境 |

## 服务编排

```
postgres:16  →  vision-admin:8080  →  vision-inference:5000
                      ↓
              minio:9000 (full 模式可选)
```

- `vision-admin` 依赖 postgres（健康检查）和 vision-inference
- GPU 推理需要 nvidia-docker runtime

## 配置要点

- 环境变量通过 `.env` 文件注入（模板：`.env.example`）
- 存储类型通过 `VISION_STORAGE_TYPE` 切换：`local` 或 `minio`
- 推理设备通过 `DEVICE` 切换：`cpu` 或 `cuda`
- 数据库连接：`DB_HOST`, `DB_PORT`, `DB_USER`, `DB_PASSWORD`, `DB_NAME`

## Volume 映射

- `pg_data` — PostgreSQL 数据持久化
- `file_data:/data/vision/files` — 图片/文件存储
- `model_data:/data/vision/models` — YOLO 模型文件
