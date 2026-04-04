# 智能 AI 旅游推荐平台 - 后端（travel_backend）

本目录是「智能 AI 旅游推荐平台」的后端服务，基于 Spring Boot 3.5.5 + Java 21 构建，提供 AI 行程规划、行程管理、回忆图生成、数字人对话、电商与社区等 REST API，供前端 `travel_frontend` 调用。

## 当前实现重点

当前后端优先服务“课程 / 比赛展示型 AI 旅行产品”的主链路，核心是支撑前端可完整演示：

- **AI 旅行工作台**
  - `/api/ai/tourism/stream` 提供流式规划能力。
  - 内置意图识别、结构化结果提取、对话上下文与消息持久化。
  - 支持限流和用户配额控制。

- **行程管理**
  - `/api/ai/trips/*` 提供保存、我的行程、详情、完成、删除等能力。
  - 结构化行程以 JSON 形式沉淀，供前端时间轴与地图展示。

- **照片资产**
  - `/api/trips/{tripId}/photos*` 提供基于 URL 的照片关联、查询、删除。
  - 当前前端主版本使用这一套接口形成“行程详情 + 照片资产”的演示闭环。

- **AI 监控与知识库调度**
  - `/api/ai/monitor/*` 提供健康检查、Milvus 查询与同步。
  - `/api/ai/tasks/*` 提供知识补齐任务投递和状态查询。

## 当前不作为主版本承诺的范围

- 电商、论坛、留言墙、数字人等模块即使仓库中存在相关代码，也不作为当前主导航和主演示路径的一部分。
- 这些能力更适合作为后续扩展方向，而不是当前评审重点。

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

仅列出当前前端主版本真实依赖的核心接口，完整定义请以实际代码和接口文档为准。

- **AI 对话**
  - `POST /api/ai/chat`：流式对话接口，支持通义千问大模型。

- **AI 监控与 Milvus 运维（管理员）**
  - `POST /api/ai/monitor/rag/milvus/sync`：触发 Milvus 全量知识同步，可选重建集合。
  - `GET  /api/ai/monitor/rag/milvus/query-count`：基于 `entities/query` 返回当前可查询条数（兼容 `entities/count` 不可用场景）。

- **AI 行程规划与沉淀**
  - `POST /api/ai/trips/generate`：根据目的地 / 天数 / 预算 / 主题生成行程方案。
  - `POST /api/ai/trips/save`：保存行程与结构化 JSON。
  - `GET  /api/ai/trips/my`：查询当前用户的所有行程。
  - `GET  /api/ai/trips/{id}`：查看行程详情。
  - `POST /api/ai/trips/{id}/complete`：标记行程完成。
  - `DELETE /api/ai/trips/{id}`：删除行程。

- **行程照片**
  - `POST /api/trips/{tripId}/photos`：上传单张行程照片。
  - `POST /api/trips/{tripId}/photos/batch`：批量上传行程照片。
  - `GET  /api/trips/{tripId}/photos`：查询行程照片。
  - `DELETE /api/trips/photos/{photoId}`：删除照片。

## 与前端协同

- 前端代码位于同一仓库的 `travel_frontend` 目录。
- 开发时建议先启动后端，再在 `travel_frontend` 中执行 `npm run dev` 启动前端开发服务器。
- 当前主版本对齐的前端页面为：首页、工作台、我的行程、行程详情、登录注册、AI 监控台。
- 对外叙事建议明确区分：
  - AI 主规划链路：`/api/ai/tourism/stream`
  - 行程沉淀：`/api/ai/trips/*`
  - 照片资产：`/api/trips/{tripId}/photos*`
  - 后台能力证明：`/api/ai/monitor/*` 与 `/api/ai/tasks/*`

## 维护者

- 作者：**Lq0412**
- 联系方式：`15919508513@163.com`
