# 智能 AI 旅游推荐平台 - 后端（travel_backend）

本目录是「智能 AI 旅游推荐平台」的后端服务，基于 Spring Boot 3.5.5 + Java 21 构建，提供 AI 行程规划、行程管理、回忆图生成、数字人对话、电商与社区等 REST API，供前端 `travel_frontend` 调用。

## 功能概览

- **AI 智能助手**
  - 接入通义千问（DashScope），支持文本对话、SSE 流式输出、上下文记忆。
  - ReAct Agent 架构，内置意图识别（行程规划 / 景点查询 / 普通聊天）。
  - 支持配额控制及基于 Resilience4j 的限流注解（例如 `@ApiRateLimit`）。

- **AI 行程规划与管理**
  - 根据目的地、天数、预算、主题生成 1–2 个候选行程方案。
  - 返回结构化 JSON 字段 `structured_data`，便于前端展示与后续扩展。
  - 支持保存行程、查看列表及详情、标记完成、删除、发布论坛帖子等操作。

- **行程媒体与回忆图**
  - 支持行程照片上传（单张 / 批量），文件存储接入腾讯云 COS。
  - 使用 DashScope 图像模型生成「回忆卡片」，支持异步任务、轮询、失败重试。
  - 提供回忆图历史版本查询与回退能力。

- **数字人实时对话**
  - 对接外部数字人服务（如 `DH_live`），提供纯文本流接口。
  - 支持在前端以 iframe 方式嵌入数字人交互界面。

- **业务与系统功能**
  - 用户、商家、管理员多角色权限体系。
  - 商家及商品管理、购物车、订单管理等电商能力。
  - 留言墙、社区论坛等社区互动模块。
  - 统一的错误处理、日志及基础响应模型。

## 技术栈

- **基础框架**：Spring Boot 3.5.5、Spring Web
- **数据访问**：MyBatis-Plus 3.5.8、MySQL 8.x
- **缓存与会话**：Redis 6/7、Spring Session、Caffeine
- **AI 能力**：Spring AI、DashScope（`qwen-plus`、`qwen-image-plus` 等）
- **稳定性与观测**：Resilience4j 限流与熔断、统一日志
- **接口文档**：Knife4j（基于 OpenAPI 3）
- **对象存储**：腾讯云 COS

## 项目结构（节选）

```text
travel_backend
├── pom.xml
├── src
│   ├── main
│   │   ├── java/com/lq/travel
│   │   │   ├── ai/...            # AI 对话、Agent、DashScope 封装
│   │   │   ├── controller/...    # 行程、回忆图、留言墙、电商等 REST 接口
│   │   │   ├── model
│   │   │   │   ├── entity/...    # Trip、TripPhoto、MemoryCard 等实体
│   │   │   │   ├── dto/...       # 各类请求 / 响应 DTO
│   │   │   │   └── vo/...        # 视图对象（含 dailyHighlights、photos 等）
│   │   │   ├── service/...       # 业务服务与实现
│   │   │   └── common/...        # 统一返回、异常、工具类等
│   │   └── resources
│   │       ├── application.yml           # 通用配置（端口、上下文路径等）
│   │       ├── application-local.yml     # 本地开发配置（已在 .gitignore 中忽略）
│   │       ├── mapper/*.xml              # MyBatis-Plus 映射文件
│   │       └── sql/travel.sql            # 初始化数据库脚本
│   └── test/...                          # 单元测试
└── tools/...                             # 辅助脚本（如数字人对接示例等）
```

> 说明：具体包名与模块可能随重构略有调整，以实际代码为准。

## 快速开始

### 1. 环境准备

- JDK **21**
- Maven **3.8+**
- MySQL **8.0+**
- Redis **6/7**
- （可选）Node.js 18+：用于启动前端 `travel_frontend`

### 2. 克隆仓库并进入后端目录

```bash
git clone https://github.com/Lq0412/travel.git
cd travel/travel_backend
```

### 3. 初始化数据库

在 MySQL 中创建数据库并导入 SQL：

```sql
CREATE DATABASE travel CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- 导入完整表结构和基础数据
SOURCE src/main/resources/sql/travel.sql;
```

如从旧版本迁移，仅关注新功能所需表结构，可参考以下核心字段与表（简要示意，具体以 `travel.sql` 为准）：

- 在行程表中新增 `structured_data` 字段，用于存储 AI 生成的完整行程 JSON。
- 新增 `trip_photo`、`memory_card`、`memory_card_history` 三张表，用于行程照片与回忆图管理。

### 4. 配置本地环境

系统默认启用 `local` 配置：

```yaml
# src/main/resources/application.yml（节选）
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  profiles:
    active: local
```

在本地开发时，建议复制示例并填写自己的敏感配置到 `application-local.yml`（该文件已经在 `.gitignore` 中忽略，不会提交到 Git）：

- 数据库连接：`spring.datasource.url` / `username` / `password`
- Redis 连接：`spring.data.redis.*`
- 通义千问 API Key：`spring.ai.dashscope.api-key` 或自定义 `ai.default.provider`
- 腾讯云 COS：`cos.client.bucket`、`region`、`secretId`、`secretKey`
- 数字人服务：`digital-human.python-service-url`

请不要在公共仓库中提交真实密钥和密码。

### 5. 启动项目

```bash
mvn clean install
mvn spring-boot:run
```

启动后：

- 后端基础地址：`http://127.0.0.1:8080/api`
- Knife4j 文档：`http://127.0.0.1:8080/api/doc.html`（开发环境可开启）

前端 `travel_frontend` 已通过 Vite 代理将 `/api` 转发到 `http://127.0.0.1:8080/api`，按默认配置即可直接联调。

## 常用 API（概览）

仅列出与 AI 模块相关的核心接口，完整定义请以实际代码和接口文档为准。

- **AI 对话**
  - `POST /api/ai/chat`：流式对话接口，支持通义千问大模型。

- **AI 监控与 Milvus 运维（管理员）**
  - `POST /api/ai/monitor/rag/milvus/sync`：触发 Milvus 全量知识同步，可选重建集合。
  - `GET  /api/ai/monitor/rag/milvus/query-count`：基于 `entities/query` 返回当前可查询条数（兼容 `entities/count` 不可用场景）。

- **AI 行程规划**
  - `POST /api/ai/trips/generate`：根据目的地 / 天数 / 预算 / 主题生成行程方案。
  - `POST /api/ai/trips/save`：保存行程与 `structured_data`。
  - `GET  /api/ai/trips/my`：查询当前用户的所有行程。
  - `GET  /api/ai/trips/{id}`：查看行程详情。
  - `POST /api/ai/trips/{id}/complete`：标记行程完成。
  - `DELETE /api/ai/trips/{id}`：删除行程。
  - `POST /api/ai/trips/{id}/publish-forum`：将行程发布到论坛。

- **行程照片与回忆图**
  - `POST /api/trips/{tripId}/photos`：上传单张行程照片。
  - `POST /api/trips/{tripId}/photos/batch`：批量上传行程照片。
  - `GET  /api/trips/{tripId}/photos`：查询行程照片。
  - `DELETE /api/trips/photos/{photoId}`：删除照片。
  - `POST /api/ai/images/memory-card`：基于选定照片创建回忆图任务。
  - `GET  /api/ai/images/memory-card/status/{taskId}`：轮询任务状态。
  - `GET  /api/ai/images/memory-card/trip/{tripId}`：查询当前生效的回忆图。
  - `GET  /api/ai/images/memory-card/history/{tripId}`：查询回忆图历史。
  - `POST /api/ai/images/memory-card/{tripId}/regenerate`：重新生成回忆图。
  - `POST /api/ai/images/memory-card/history/{historyId}/set-current`：将历史版本设置为当前。

## 与前端协同

- 前端代码位于同一仓库的 `travel_frontend` 目录。
- 开发时建议先启动后端，再在 `travel_frontend` 中执行 `npm run dev` 启动前端开发服务器。
- 数据接口约定和字段命名已在代码和接口文档中保持一致，新增字段（如 `structured_data`、回忆图相关字段）均已在前端对接。

## 维护者

- 作者：**Lq0412**
- 联系方式：`15919508513@163.com`
