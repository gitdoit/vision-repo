---
description: "Use when working on Java backend code in vision-admin: Spring Boot controllers, services, MyBatis mappers, entities, DTOs, database migrations, or application configuration."
applyTo: "vision-admin/**"
---
# Java 后端开发规范（vision-admin）

## 分包规则

按业务模块分包，**不按技术层分包**：
```
com.vision.{module}/
├── controller/    # REST API — 只做参数校验和转发，不写业务逻辑
├── service/       # 业务逻辑 — 不写 SQL
├── mapper/        # MyBatis 接口 — SQL 在 XML 或注解中
├── entity/        # 数据库实体 — 一个文件一个类
└── dto/           # 请求/响应 DTO、VO — 一个文件一个类
```

模块列表：`camera`, `model`, `rule`, `inference`, `alert`, `dashboard`, `capture`, `storage`, `config`, `common`

## 命名约定

| 类型 | 模式 | 示例 |
|------|------|------|
| Controller | `{Module}Controller` | `CameraController` |
| Service | `{Module}Service` | `CameraService` |
| Mapper | `{Module}Mapper` | `CameraMapper` |
| Entity | 单数名词 | `Camera` |
| 请求 DTO | `{Module}CreateDTO` / `{Module}UpdateDTO` | `CameraCreateDTO` |
| 响应 VO | `{Module}VO` | `CameraVO` |

## MyBatis-Plus 要点

- 主键策略：`@TableId(type = IdType.ASSIGN_UUID)`
- 软删除：`@TableLogic private Integer deleted;`
- 驼峰映射已开启，entity 字段用 camelCase，表列用 snake_case
- 复杂 SQL 写 mapper XML（`resources/mapper/{module}/`），简单查询用注解
- 分页必须传 `Page<T>` 参数，不要用 `@Select` + `LIMIT/OFFSET` 手写分页

## 统一响应格式

所有 Controller 返回 `R<T>`：
```java
@GetMapping("/{id}")
public R<CameraVO> getCamera(@PathVariable String id) {
    return R.ok(cameraService.getById(id));
}
```

分页返回 `R<PageResult<T>>`，`PageResult` 包含 `items` 和 `total` 字段。

## 数据库变更

- **必须**通过 Flyway 迁移脚本：`resources/db/migration/V{n}__{description}.sql`
- 不要手动改库结构
- JSONB 类型用于灵活字段（`actions`, `evidence`, `raw_json`）

## 存储服务

文件操作统一调用 `StorageService` 接口，不要直接操作文件系统或 MinIO SDK。
路径约定：`{类型}/{日期}/{文件名}`

## 调用推理服务

通过 `InferenceClient`（HTTP）调用 Python 服务，不要在 Java 中引入 Python 依赖。
推理服务地址配置在 `vision.inference.service-url`。
