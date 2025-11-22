# 智能 AI 旅游推荐平台 - 后端

基于 Spring Boot 3.2 + Java 17 的前后端分离后端，内置通义千问（文本、图像）能力，覆盖行程规划、行程存档、数字人对话、电商与社区等模块。当前版本已新增行程管理、AI 回忆图、意图识别等功能，并修复原有文档的编码问题。

## 核心功能
- **AI 智能助手**：SSE 流式对话，ReAct Agent 架构，通义千问接入，支持上下文记忆、配额和限流（`@ApiRateLimit`）。新增 IntentAnalyzer 自动识别行程规划 / 景点查询 / 普通聊天意图。
- **AI 行程规划与管理（新增）**：输入目的地、天数、预算、主题生成 1-2 个方案，返回按天亮点与结构化 JSON（`structured_data`）。可保存行程、查看列表/详情、标记完成/删除，并一键发布为论坛帖子。
- **行程媒体与回忆图（新增）**：行程照片上传（单个/批量），基于通义千问图像（DashScope Qwen-Image）生成“回忆卡片”，支持异步轮询、重试、历史版本存档、COS 持久化与回退。
- **数字人实时对话**：集成 DH_live，后端提供纯文本流，支持语音播报和 iframe 嵌入。
- **业务功能**：景点/留言墙、商品/订单/购物车、社区论坛、用户与商家权限体系等。

## 技术栈
Spring Boot 3.2 · MyBatis Plus 3.5.5 · MySQL 8 · Redis 6/7 · Caffeine · Resilience4j · Knife4j · DashScope（qwen-plus / qwen-image-plus）· 腾讯云 COS · Vue 3 前端（独立仓库）· DH_live

## 快速开始
1) **环境要求**：JDK 21，Maven 3.8+，MySQL 8.0+，Redis 6/7，Node.js 18+（前端）。

2) **拉取代码**
```bash
git clone https://github.com/Lq0412/travel-ai-backend.git
cd travel-ai-backend
```

3) **初始化数据库**
```sql
CREATE DATABASE travel CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- 导入主 SQL（包含行程、回忆图等新表）
SOURCE src/main/resources/sql/travel.sql;
```
如从旧版本升级，至少执行：
```sql
ALTER TABLE trip ADD COLUMN structured_data TEXT COMMENT 'AI 生成的完整行程 JSON';
CREATE TABLE IF NOT EXISTS trip_photo(
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  trip_id BIGINT NOT NULL,
  photo_url VARCHAR(1024) NOT NULL,
  shot_time DATETIME NULL,
  description VARCHAR(255) NULL,
  sort_order INT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_delete TINYINT DEFAULT 0
);
CREATE TABLE IF NOT EXISTS memory_card(
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  trip_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  template_name VARCHAR(50) DEFAULT 'default',
  image_url VARCHAR(1024),
  task_id VARCHAR(128),
  status VARCHAR(32),
  error_message VARCHAR(512),
  retry_count INT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_delete TINYINT DEFAULT 0
);
CREATE TABLE IF NOT EXISTS memory_card_history(
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  trip_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  template_name VARCHAR(50),
  image_url VARCHAR(1024),
  task_id VARCHAR(128),
  status VARCHAR(32),
  error_message VARCHAR(512),
  retry_count INT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  is_delete TINYINT DEFAULT 0
);
```

4) **配置文件**
编辑 `src/main/resources/application-local.yml`（或自建 `application-*.yml`），主要项：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/travel
    username: your_user
    password: your_pass
  data:
    redis:
      host: 127.0.0.1
      port: 6379
  ai:
    dashscope:
      api-key: <your_dashscope_key>
      chat.options.model: qwen-plus
      image.options.model: qwen-image-plus

cos:
  client:
    bucket: <your_bucket>
    region: ap-guangzhou
    secretId: <id>
    secretKey: <key>

app:
  image:
    base-url: https://dashscope.aliyuncs.com        # 可选，默认直连官方
    model: qwen-image-plus                          # 覆盖默认图生模型
    endpoint: /api/v1/services/aigc/text2image/image-synthesis

digital-human:
  python-service-url: http://localhost:8888
```

5) **启动**
```bash
mvn clean install
mvn spring-boot:run   # 默认端口 8080，context-path /api
```
API 文档：`http://localhost:8080/api/doc.html`（Knife4j 开发态开启）。

## API 速览（新增重点）
- 行程生成：`POST /api/ai/trips/generate`（auth，rate-limit），入参 destination/days/budget/theme，返回 1-2 个方案。
- 行程保存：`POST /api/ai/trips/save`，写入行程、按天亮点、`structured_data`。
- 我的行程：`GET /api/ai/trips/my`；行程详情：`GET /api/ai/trips/{id}`；完成：`POST /api/ai/trips/{id}/complete`；删除：`DELETE /api/ai/trips/{id}`。
- 行程发帖：`POST /api/ai/trips/{id}/publish-forum`，需已有成功的回忆图。
- 行程照片：`POST /api/trips/{tripId}/photos`（单张 URL），`POST /api/trips/{tripId}/photos/batch`，`GET /api/trips/{tripId}/photos`，`DELETE /api/trips/photos/{photoId}`。
- 回忆图：`POST /api/ai/images/memory-card`（>=3 张照片）生成任务，`GET /api/ai/images/memory-card/status/{taskId}` 轮询，`GET /api/ai/images/memory-card/trip/{tripId}` 查询当前，`POST /api/ai/images/memory-card/{tripId}/regenerate` 重生成，`GET /api/ai/images/memory-card/history/{tripId}` 历史，`POST /api/ai/images/memory-card/history/{historyId}/set-current` 切换。
- AI 对话：`/api/ai/chat`（流式）及 Agent 相关接口继续可用，支持意图识别和限流/配额。

## 项目结构（局部）
```
src/main/java/com/lq/travel
├── AI/core/...             # AI 对话、Agent、DashScope 接入、回调增强
├── controller/             # REST 控制器（含行程、回忆图、照片）
├── model/
│   ├── entity/             # Trip, TripPhoto, MemoryCard, MemoryCardHistory...
│   ├── dto/                # TripGenerate/Save、MemoryCardGenerate 等
│   └── vo/                 # TripVO（含 dailyHighlights、photos、memoryCard）
├── service/impl/           # TripServiceImpl、MemoryCardServiceImpl 等
└── resources/
    ├── application*.yml
    └── sql/travel.sql
```

## 部署与生产注意
- 推荐 `mvn clean package -DskipTests` 后用 Docker 或 `java -jar` 部署，JDK 21。
- 提前配置 COS/CND 与 DashScope 网络出口；image 生成任务依赖外网。
- 定时任务会轮询回忆图任务（30 秒），确保数据库和 COS 可用。

## 相关仓库
- 前端：`https://github.com/Lq0412/travel-ai-frontend`
- 数字人：`https://github.com/kleinlee/DH_live`

## 维护者
Lq0412 — 15919508513@163.com
