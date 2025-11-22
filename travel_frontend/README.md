# 🌍 智能 AI 旅游推荐平台 - 前端（travel_frontend）

基于 **Vue 3 + TypeScript + Ant Design Vue + Vite** 的单页前端应用，提供智能行程规划、AI 对话、数字人交互、留言墙、电商购物等完整功能界面，对接后端 `travel_backend`。

> 本目录为智能 AI 旅游推荐平台的前端应用，位于仓库根目录下的 `travel_frontend`。

## ✨ 核心特性

### 用户端

- 🔐 **账号体系**：注册 / 登录 / 注销、个人中心、资料修改。
- 🤖 **AI 旅游助手**：接入通义千问，支持流式对话、打字机效果、上下文记忆。
- 🧭 **AI 行程规划**：意图识别、结构化行程卡片、历史恢复、一键保存到「我的行程」。
- 📅 **行程管理**：行程列表、详情、每日亮点编辑、完成状态管理。
- 🧑‍💻 **数字人服务**：通过 iframe 集成外部数字人服务，实现实时语音交互。
- 🏞 **景点浏览**：景点详情页、图文展示、打卡及留言。
- 💬 **留言墙 / 弹幕**：景点留言、弹幕效果、点赞互动。
- 🛒 **电商购物**：商品浏览、购物车、下单、订单查询与评价。
- 📝 **社区论坛**：发帖、评论、收藏、标签分类等。

### 管理端 & 商家端

- 📊 **用户管理**：用户列表、角色与权限配置。
- 🏢 **商家管理**：商家审核、店铺管理、资质维护。
- 🎯 **景点管理**：景点信息、图片与推荐配置。
- 💬 **留言管理**：留言墙内容审核与批量操作。
- 📦 **订单管理**：订单处理、售后及异常订单跟踪。
- 🏪 **商家商品管理**：商品发布 / 上下架 / 库存编辑、订单处理。
- ⚙️ **店铺设置与数据看板**：商家店铺信息、销售统计等。

## 🏗 技术栈

```text
框架       ：Vue 3 (Composition API)
语言       ：TypeScript 5.x
构建工具   ：Vite 6.x
UI 组件库  ：Ant Design Vue 4.x
路由       ：Vue Router 4.x
状态管理   ：Pinia
HTTP 客户端：Axios（统一请求封装）
样式       ：SCSS / Less
规范       ：ESLint + Prettier + vue-tsc
```

- 统一的接口模块（`src/api`），部分接口通过 `openapi.config.js` 从后端 OpenAPI 文档生成。
- 可复用业务逻辑封装在 `src/composables`。
- 统一的日志与错误处理（`src/utils/logger`、`src/request.ts`）。
- 响应式适配 PC / 平板 / 移动端（布局在 `src/layouts`，页面在 `src/pages`）。

## 📂 目录结构（节选）

```text
travel_frontend
├── src
│   ├── api/                # OpenAPI 生成的接口定义 & 手写 API
│   ├── assets/             # 静态资源
│   ├── components/         # 通用组件
│   ├── composables/        # 可复用业务逻辑
│   ├── constants/          # 常量与配置
│   ├── layouts/            # 布局组件（含 TopNavLayout 等）
│   ├── pages/              # 业务页面（用户端 / 管理端 / 商家端）
│   ├── router/             # 路由配置 & 守卫
│   ├── stores/             # Pinia 状态管理（登录用户、权限等）
│   ├── types/              # TypeScript 类型定义
│   ├── utils/              # 工具方法、日志等
│   ├── access.ts           # 路由访问控制
│   ├── request.ts          # Axios 实例与拦截器
│   ├── App.vue
│   └── main.ts
├── public/                 # 静态公共资源
├── docs/                   # 前端开发文档（如有）
├── .env.example            # 环境变量示例
├── vite.config.ts          # Vite 配置（含 /api 代理到后端）
├── package.json
└── tsconfig*.json
```

## 🚀 快速开始

### 1. 环境要求

- Node.js **>= 18.x**（推荐 20.x）
- npm **>= 9.x** 或 pnpm **>= 8.x**
- 后端服务 `travel_backend` 已在本地启动（默认 `http://127.0.0.1:8080/api`）

### 2. 克隆仓库并进入前端目录

```bash
git clone https://github.com/Lq0412/travel.git
cd travel/travel_frontend
```

### 3. 安装依赖

```bash
# 任选一种包管理器
npm install
# 或
pnpm install
```

### 4. 本地开发运行

```bash
npm run dev
# 或 pnpm dev
```

默认访问地址：`http://127.0.0.1:5173`

Vite 已在 `vite.config.ts` 中配置了代理：

- 前端请求 `/api/**` 将被代理到 `http://127.0.0.1:8080/api/**`。
- 请确保后端 `travel_backend` 已启动，否则部分页面会出现接口请求失败。

### 5. 生产构建与预览

```bash
# 构建产物（输出到 dist/）
npm run build

# 本地预览构建结果
npm run preview
```

## ⚙️ 环境配置

### API 生成（OpenAPI）

`openapi.config.js` 使用后端 `http://127.0.0.1:8080/api/v3/api-docs` 生成前端类型安全的 API 封装：

```bash
npm run openapi
```

建议在后端接口更新后重新运行该命令，以保持前后端接口定义一致。

### 本地环境变量

- 参考 `.env.example` 新建 `.env.local` 或 `.env.development` 并根据需要调整配置（如标题、接口前缀等）。
- Vite 环境变量以 `VITE_` 前缀注入，例如 `VITE_APP_TITLE`。

## 🧪 质量保障

常用脚本：

```bash
npm run lint        # 代码规范检查（ESLint）
npm run format      # 使用 Prettier 格式化 src/
npm run type-check  # TypeScript 类型检查
```

推荐在提交代码前至少执行一次 `npm run lint` 与 `npm run type-check`，保证提交质量。

## 🔗 与后端及数字人服务

- 后端服务：同仓库下 `travel_backend` 目录。  
  - 默认地址：`http://127.0.0.1:8080/api`  
  - 前端通过 `src/request.ts` 统一调用接口。
- 数字人服务：推荐接入开源项目 [DH_live](https://github.com/kleinlee/DH_live)，在前端通过 iframe 嵌入数字人页面（具体地址在配置中统一管理）。

## 📦 仓库结构（跨项目视角）

```text
travel
├── travel_backend    # Spring Boot 后端服务
└── travel_frontend   # Vue 3 前端应用（本目录）
```

> 本仓库统一管理前后端代码，方便联调与部署。

## 🤝 贡献与规范

欢迎通过 Issue 或 Pull Request 提出意见与改进建议：

- 保持组件职责单一，重用逻辑优先放入 `composables`。
- 遵循 TypeScript 最佳实践，避免使用 `any`。
- 提交前建议运行：`npm run lint && npm run type-check`。
- Commit 信息建议采用简洁清晰的中文或英文描述，例如：`feat: 新增行程回忆图入口`。

## 📮 联系方式

- 作者：**Lq0412**
- GitHub：[https://github.com/Lq0412](https://github.com/Lq0412)
- 邮箱：`15919508513@163.com`

