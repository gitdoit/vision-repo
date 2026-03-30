---
description: "Use when working on Vue 3 frontend code: pages, components, Pinia stores, API modules, TypeScript types, Tailwind styling, or MSW mocks."
applyTo: "frontend/src/**"
---
# 前端开发规范（frontend）

## 技术栈

Vue 3.5 + TypeScript 5.6 + Vite 5.4 + Naive UI + Tailwind CSS 4 + Pinia + ECharts

## 代码结构

```
src/
├── api/
│   ├── client.ts       # Axios 实例，baseURL: /api/v1，响应拦截器解包 R<T>
│   └── modules/        # 按模块拆分 API 调用（camera.ts, model.ts, ...）
├── components/layout/  # 全局布局组件
├── pages/{module}/     # 每个业务模块一个文件夹
├── stores/{module}.ts  # Pinia store，一个模块一个文件
├── types/index.ts      # 所有 TypeScript 接口定义
├── mocks/              # MSW mock handlers + data
└── router/index.ts     # 路由配置
```

## 关键约定

- **API 响应格式**：后端返回 `{ code, message, data }`，Axios 拦截器应解包为 `data`
- **分页**：请求 `?page=1&size=20`，响应 `{ items: T[], total: number }`
- **状态枚举**：使用英文小写，与后端保持一致（`online`/`offline`/`loaded`/`unloaded`）
- **UI 库**：统一使用 Naive UI 组件，不引入其他 UI 库
- **样式**：Tailwind CSS utility-first，避免自定义 CSS（除非 Tailwind 无法实现）

## 前后端同步

修改接口时，`frontend/src/types/index.ts` 中的类型定义必须与 Java DTO/VO 字段名完全一致。
字段不匹配会导致页面数据显示异常。

## Mock 模式

MSW 拦截浏览器请求提供 mock 数据，禁用 MSW 即切换到真实 API。
Mock handlers 在 `src/mocks/handlers.ts`，mock 数据在 `src/mocks/data.ts`。
