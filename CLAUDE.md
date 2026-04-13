# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# gstack

使用 gstack 的 `/browse` 技能进行所有网页浏览操作。不要使用 `mcp__claude-in-chrome__*` 工具。

可用的 gstack 技能：
`/office-hours`、`/plan-ceo-review`、`/plan-eng-review`、`/plan-design-review`、`/design-consultation`、`/design-shotgun`、`/design-html`、`/review`、`/ship`、`/land-and-deploy`、`/canary`、`/benchmark`、`/browse`、`/connect-chrome`、`/qa`、`/qa-only`、`/design-review`、`/setup-browser-cookies`、`/setup-deploy`、`/retro`、`/investigate`、`/document-release`、`/codex`、`/cso`、`/autoplan`、`/plan-devex-review`、`/devex-review`、`/careful`、`/freeze`、`/guard`、`/unfreeze`、`/gstack-upgrade`、`/learn`

# 项目概述

AI 智能记账系统 — 基于 Spring Boot 3 + Spring AI + Vue 3 的智能记账应用。用户通过自然语言录入账单，AI 自动识别金额、分类、时间。

# 常用命令

## 后端

```bash
cd backend

# 启动依赖服务（MySQL + Redis）
docker-compose up -d

# 构建（跳过测试）
mvn clean package -DskipTests

# 运行
java -jar target/ai-bookkeeping-1.0.0-SNAPSHOT.jar

# 运行全部测试
mvn test

# 运行单个测试类
mvn test -Dtest=JwtUtilTest

# 运行单个测试方法
mvn test -Dtest=JwtUtilTest#testGenerateToken
```

后端启动后访问：`http://localhost:8086`，API 文档：`http://localhost:8086/doc.html`

## 前端

```bash
cd frontend

# 安装依赖
npm install

# 开发模式（端口 5173）
npm run dev

# 生产构建（含 TypeScript 类型检查）
npm run build

# 预览生产构建
npm run preview
```

前端开发服务器通过 Vite 代理将 `/api` 请求转发到 `localhost:8086`。

## 全栈 Docker 部署

```bash
docker-compose up -d
```

服务：MySQL(3307)、Redis(6380)、Backend(8082)、Nginx 前端(8090)。

# 架构

## 后端（Java 17 / Spring Boot 3.2.5）

包路径：`com.aibookkeeping`，位于 `backend/src/main/java/`。

分层结构：
- **controller** — REST 控制器，路径前缀 `/api/`，统一返回 `Result<T>` 包装（code/message/data）
- **service** — 接口 + Impl 分离，按领域分包：`auth`、`bill`、`budget`、`category`、`stat`、`user`
- **mapper** — MyBatis Plus Mapper 接口
- **entity** — 数据库实体（`@TableName`），`user` 和 `bill` 使用软删除（`@TableLogic`）
- **dto** / **vo** — 请求 DTO 和响应 VO

跨切面：
- **认证**：Spring Security + JWT 无状态认证，双 token（Access 2h / Refresh 7d），Redis 管理 token 黑名单
- **AI 集成**：Spring AI（OpenAI 兼容协议），DeepSeek 为主模型，智谱 GLM-4 为降级备选。两步式 AI 记账：解析预览 → 用户确认
- **审计日志**：AOP 切面 `AuditLogAspect`，拦截所有 POST/PUT/DELETE
- **安全**：XSS 过滤器、Redis 令牌桶限流（30 req/s）、AI 接口独立限流（20 req/min）
- **错误码**：`ErrorCode` 枚举集中管理，分段编码（1xxx 通用、10xx 认证、20xx 账单、30xx 分类、40xx AI、50xx 预算、60xx 用户）

配置文件：
- `application.yml` — 主配置（端口 8086、MySQL、Redis、DeepSeek、JWT、GLM、AI 缓存/限流）
- `application-dev.yml` — 开发环境覆盖
- `application-docker.yml` — Docker 环境覆盖

环境变量（敏感配置）：`MYSQL_PASSWORD`、`REDIS_HOST`/`REDIS_PASSWORD`、`DEEPSEEK_API_KEY`/`DEEPSEEK_BASE_URL`/`DEEPSEEK_MODEL`、`JWT_SECRET`、`GLM_API_KEY`/`GLM_BASE_URL`/`GLM_MODEL`

## 前端（Vue 3 + TypeScript + Vite）

位于 `frontend/src/`。

- **api/** — 按领域拆分的 API 模块（auth、bill、budget、category、dashboard、stat、user）
- **views/** — 页面组件，按功能分目录（auth、bill、budget、category、dashboard、profile、stat）
- **stores/** — Pinia 状态管理（`user` 管理 token，`dashboard`）
- **utils/request.ts** — Axios 封装：自动注入 Bearer token、过期前 5 分钟主动刷新、401 透明重试、网络错误自动重试（2 次，1s 退避）
- **components/MainLayout.vue** — 认证后页面的共享布局
- **router/** — 路由守卫鉴权，懒加载路由
- **types/index.ts** — TypeScript 类型定义，对应后端 DTO/VO

## 数据库（MySQL 8）

5 张表：`user`、`bill`、`category`、`budget`、`audit_log`。初始化脚本：`backend/sql/init.sql`，含 22 个系统预设分类（15 支出 + 7 收入）。MyBatis Plus 配置：自增主键、驼峰映射、软删除字段 `deleted`。