# 🌍 智能 AI 旅游推荐平台

本仓库 `travel` 是一个前后端分离的智能旅游平台 **Monorepo**，包含：

- `travel_backend`：基于 **Spring Boot 3.5.5 + Java 21** 的后端服务  
- `travel_frontend`：基于 **Vue 3 + TypeScript + Vite** 的前端单页应用  

核心功能围绕「AI + 旅游」，提供行程规划、行程管理、回忆图、数字人对话、电商购物、社区互动等能力。

---

## ✨ 功能总览

- 🤖 **AI 智能助手**
  - 接入通义千问（DashScope），支持文本对话、SSE 流式输出、上下文记忆。
  - ReAct Agent 架构，自动识别行程规划 / 景点查询 / 普通聊天等意图。
  - 支持配额控制、限流与基础风控能力。

- 🧭 **AI 行程规划与管理**
  - 输入目的地、天数、预算、主题，生成 1–2 套行程方案。
  - 结构化返回 JSON（如 `structured_data`），方便前端卡片式展示。
  - 支持保存行程、查看列表与详情、标记完成、删除、发布为论坛帖子等。

- 🖼 **行程照片与回忆图**
  - 行程照片上传（单张 / 批量），文件接入腾讯云 COS。
  - 基于 DashScope 图像模型生成「回忆卡片」，支持任务轮询、重试与历史版本。

- 👨‍💻 **数字人实时对话**
  - 对接外部数字人服务（如 [DH_live](https://github.com/kleinlee/DH_live)），支持实时语音互动。
  - 前端通过 iframe 嵌入数字人界面。

- 💬 **社区与留言**
  - 景点留言墙、弹幕展示、点赞互动。
  - 社区论坛：发帖、评论、收藏、标签分类。

- 🛒 **电商与权限体系**
  - 商家 / 商品管理、购物车、下单、订单管理。
  - 用户 / 商家 / 管理员多角色权限控制界面和接口。

---

## 🧱 仓库结构

```text
travel
├── travel_backend     # 后端：Spring Boot 服务，提供 REST API
└── travel_frontend    # 前端：Vue 3 单页应用，调用后端接口
```

各子目录内都有独立的 `README.md`，介绍详细功能、依赖与配置：

- 后端说明：`travel_backend/README.md`
- 前端说明：`travel_frontend/README.md`

---

## 🚀 快速上手（本地开发）

> 以下命令假设你已经安装了 **Git、JDK 21、Maven 3.8+、MySQL 8、Redis 6/7、Node.js 18+**。

### 1. 克隆仓库

```bash
git clone https://github.com/Lq0412/travel.git
cd travel
```

### 2. 初始化数据库

进入后端目录，创建数据库并导入脚本：

```bash
cd travel_backend
```

在 MySQL 中执行：

```sql
CREATE DATABASE travel CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
SOURCE src/main/resources/sql/travel.sql;
```

> 如仅是从旧版本升级，可按 `travel_backend/README.md` 中的说明，仅执行涉及 AI 行程、回忆图的新表和字段。

### 3. 配置后端（本地环境）

后端默认配置文件：

- `src/main/resources/application.yml`：公共配置（端口、上下文路径 `server.servlet.context-path=/api` 等）
- `src/main/resources/application-local.yml`：本地开发配置（**包含敏感信息，应放在本地，不提交到 Git**）

在本机上：

1. 根据示例配置填写数据库、Redis、DashScope、COS 等信息到 `application-local.yml`。  
2. 确认 `.gitignore` 已忽略该文件（仓库已默认配置）。

### 4. 启动后端

在 `travel_backend` 目录：

```bash
mvn clean install
mvn spring-boot:run
```

默认：

- 后端基础地址：`http://127.0.0.1:8080/api`
- 接口文档（开发环境）：`http://127.0.0.1:8080/api/doc.html`

### 5. 启动前端

新开一个终端，进入前端目录：

```bash
cd travel_frontend
npm install           # 或 pnpm install
npm run dev           # 或 pnpm dev
```

默认前端访问地址：`http://127.0.0.1:5173`

Vite 已在 `vite.config.ts` 中配置代理：

- `/api/**` → `http://127.0.0.1:8080/api/**`

保证后端已启动后，即可在浏览器里完整体验从登录、AI 对话到行程规划、电商购物等功能。

---

## 🧪 常用命令汇总

### 后端（在 `travel_backend` 目录）

```bash
mvn clean install       # 编译 & 单元测试
mvn spring-boot:run     # 以开发配置启动
```

### 前端（在 `travel_frontend` 目录）

```bash
npm run dev             # 本地开发
npm run build           # 生产构建（输出到 dist/）
npm run preview         # 预览构建结果
npm run lint            # 代码检查
npm run type-check      # TS 类型检查
npm run openapi         # 从后端 OpenAPI 生成前端 API 封装
```

---

## 🔐 安全与配置注意事项

- 不要在公开仓库中提交真实的 **数据库密码、Redis 密码、DashScope API Key、COS 密钥** 等敏感信息。
- 本地敏感配置请放在 `application-local.yml` 或 `.env.local` 等文件中，并确保被 `.gitignore` 忽略。
- 生产环境请根据自身需求调整：
  - API 限流与配额策略（Resilience4j / 自定义注解）
  - Knife4j 文档开关（生产环境建议关闭）
  - 日志级别与持久化方案

---

## 🤝 贡献与协作

欢迎通过 Issue 或 Pull Request 参与改进本项目：

- 保持良好的提交信息（推荐简洁中文或英文，如：`feat: 新增行程回忆图生成接口`）。
- 在提交前建议执行：
  - 后端：`mvn clean test`（视测试情况而定）
  - 前端：`npm run lint && npm run type-check`

---

## 👨‍💻 作者与联系方式

- 作者：**Lq0412**
- GitHub：<https://github.com/Lq0412>
- 邮箱：`15919508513@163.com`

如果这个项目对你有帮助，欢迎在 GitHub 上点一颗 ⭐ Star 支持一下。

