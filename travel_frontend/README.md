# 🌍 智能 AI 旅游推荐平台 - 前端系统

![Vue 3](https://img.shields.io/badge/Vue-3.5.0-42b983.svg)
![TypeScript](https://img.shields.io/badge/TypeScript-5.x-blue.svg)
![Ant Design Vue](https://img.shields.io/badge/Ant%20Design%20Vue-4.x-1890ff.svg)
![Vite](https://img.shields.io/badge/Vite-5.x-646CFF.svg)
![Code Quality](https://img.shields.io/badge/Code%20Quality-A+-success.svg)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)

> 基于 Vue 3 + TypeScript + Ant Design Vue 的现代化前后端分离智能旅游助手前端系统，集成 AI 对话、数字人交互、留言墙弹幕、电商购物等完整功能。

## 📋 项目简介

本项目是智能 AI 旅游推荐平台的前端系统，提供完整的用户界面和交互体验。核心特性包括：

- **🤖 AI 智能对话**：集成通义千问大模型，流式对话、打字机效果、上下文记忆
- **👨‍💼 数字人交互**：集成 [DH_live](https://github.com/kleinlee/DH_live) 数字人服务，支持实时语音对话
- **💬 留言墙系统**：景点留言、弹幕动画、实时刷新、点赞互动
- **🛒 电商购物**：商品浏览、购物车、在线下单、订单跟踪
- **📝 社区论坛**：帖子发布、评论互动、标签分类、收藏点赞
- **🎯 三端管理**：用户端、管理端、商家端完整界面
- **🏗️ 现代化架构**：Vue 3 Composition API、TypeScript、工业级代码质量

## ✨ 功能特性

### 👥 用户端功能
- 🔐 **用户系统**：注册、登录、个人中心、资料修改
- 🤖 **AI 对话助手**：智能旅游咨询、流式响应、上下文记忆
- 🎯 **AI 行程规划（NEW）**：意图识别、结构化卡片、历史恢复、一键保存
- 📅 **我的行程**：查看保存的行程、编辑每日亮点、管理行程
- 👨‍💼 **数字人服务**：实时语音交互、自然对话体验
- 🏞️ **景点浏览**：景点详情、图片展示、路线规划
- 💬 **留言墙**：发布留言、弹幕展示、点赞互动
- 🛒 **电商购物**：商品浏览、购物车管理、在线支付
- 📋 **订单管理**：订单查询、状态跟踪、评价晒单
- 📝 **社区论坛**：发布帖子、评论互动、内容收藏

### 👨‍💼 管理端功能
- 📊 **用户管理**：用户列表、权限管理、账号状态
- 🏢 **商家管理**：商家审核、资质管理、店铺监控
- 🎯 **景点管理**：景点信息、图片管理、推荐设置
- 💬 **留言墙管理**：内容审核、违规处理、批量操作
- 📢 **公告管理**：公告发布、编辑、置顶设置
- 📦 **订单管理**：订单查询、异常处理、数据统计
- 🗂️ **分类管理**：商品分类、标签管理

### 🏪 商家端功能
- 📦 **商品管理**：商品发布、编辑、上下架、库存管理
- 📋 **订单处理**：订单查询、发货管理、售后处理
- ⚙️ **店铺设置**：店铺信息、资质认证、运营设置
- 📊 **数据统计**：销售数据、订单分析、商品排行

## 🏗️ 技术架构

### 核心技术栈

```
前端框架：Vue 3.5.0 (Composition API)
开发语言：TypeScript 5.x
构建工具：Vite 5.x
UI 组件库：Ant Design Vue 4.x
路由管理：Vue Router 4.x
状态管理：Pinia
HTTP 客户端：Axios
样式处理：SCSS
代码规范：ESLint + Prettier
```

### 代码质量工具
- ✅ **统一 API 层**：集中的接口管理，避免代码重复
- ✅ **错误处理**：全局错误拦截、友好提示、自动重定向
- ✅ **日志系统**：分级 Logger（DEBUG/INFO/WARN/ERROR），环境自适应
- ✅ **类型系统**：完整的 TypeScript 类型定义，消除 `any`
- ✅ **Composable**：可复用的业务逻辑封装
- ✅ **常量管理**：集中式配置，消除魔法数字
- ✅ **组件化**：单一职责原则，组件拆分合理

### 系统架构

```
├── 用户界面层
│   ├── 用户端页面
│   ├── 管理端页面
│   └── 商家端页面
├── 业务逻辑层
│   ├── Composables（可复用逻辑）
│   ├── API 接口封装
│   ├── 状态管理
│   └── 路由守卫
├── 基础设施层
│   ├── HTTP 请求拦截
│   ├── 错误处理
│   ├── 日志记录
│   └── 工具函数
└── 数字人集成
    └── Iframe 嵌入式交互


## 🚀 快速开始

### 环境要求

- **Node.js >= 18.x** (推荐 20.x)
- **npm >= 9.x** 或 **pnpm >= 8.x**
- **后端服务**: 确保后端 API 服务已启动（默认 http://127.0.0.1:8080）

### 1. 克隆项目

```bash
git clone https://github.com/Lq0412/travel-ai-frontend.git
cd travel-ai-frontend
```

### 2. 安装依赖

```bash
npm install
# 或使用 pnpm（推荐，更快）
pnpm install
```

### 3. 配置环境变量

复制 `.env.example` 并重命名为 `.env.local`：

```bash
cp .env.example .env.local
```

编辑 `.env.local` 配置：

```env
# API基础URL（后端服务地址）
VITE_API_BASE_URL=/api

# 数字人服务URL（可选）
VITE_DIGITAL_HUMAN_URL=http://127.0.0.1:8888/static/MiniLive_RealTime.html
```

### 4. 启动开发服务器

```bash
npm run dev
```

访问：http://127.0.0.1:5173

### 5. 生产环境构建

```bash
npm run build
```

构建产物在 `dist/` 目录

### 6. 代码检查和格式化

```bash
# 代码检查
npm run lint

# 代码格式化
npm run format
```

## 🔧 配置说明

### 环境变量

创建 `.env.local` 文件配置环境变量：

```env
# API基础URL
VITE_API_BASE_URL=/api

# 数字人服务URL
VITE_DIGITAL_HUMAN_URL=http://127.0.0.1:8888/static/MiniLive_RealTime.html
```

### 后端接口

默认后端API地址：`/api`

> 确保后端服务已启动，详见后端项目README

## 💡 核心功能说明

### 1. AI 智能对话与行程规划系统

**核心对话功能**
- ✅ **流式响应**：SSE (Server-Sent Events) 实现打字机效果
- ✅ **上下文记忆**：多轮对话保持连贯性
- ✅ **智能推荐**：基于通义千问的专业旅游咨询
- ✅ **对话历史**：自动保存和查询历史对话

**🎯 智能行程规划（NEW）**
- ✅ **意图识别**：自动识别行程规划意图（如"从化3日游"）
- ✅ **结构化数据**：AI返回完整的JSON格式行程数据
- ✅ **行程卡片**：精美的渐变卡片展示完整行程
  - 目的地和天数
  - 每日详细计划（活动+餐饮）
  - 预算和费用估算
  - 旅行建议和tips
- ✅ **历史恢复**：重新进入对话时自动恢复行程卡片
- ✅ **一键保存**：保存到"我的行程"，支持后续管理
- ✅ **数据分层**：
  - `structuredData` - 完整数据（地点、坐标、费用等）
  - `dailyHighlights` - 每日亮点（便于编辑和调整）

**技术实现**
- `useChatStream` - 封装流式对话和结构化数据提取
- `ItineraryCard` - 行程卡片组件（紫色渐变主题）
- 自动提取 `__STRUCTURED_DATA_START__` 标记内的JSON
- 从历史消息恢复结构化数据
- 智能提取每日亮点到简化格式

### 2. 数字人实时交互

**集成 DH_live 服务**
- 使用 iframe 嵌入数字人服务页面
- 实时语音交互，低延迟体验
- 纯净对话文本，适配语音播报
- 支持自定义数字人形象

**组件封装**
- `DigitalHumanIframe` 组件统一管理
- 消息传递机制，前后端数据同步
- 错误处理和服务可用性检测

> **参考**：[DH_live 项目](https://github.com/kleinlee/DH_live)

### 3. 留言墙弹幕系统

**功能特性**
- **实时刷新**：10 秒自动刷新（可配置）
- **弹幕动画**：CSS 动画实现流畅滚动
- **点赞互动**：实时点赞，乐观更新
- **审核机制**：管理员可审核、删除留言
- **状态管理**：待审核、已通过、已拒绝

**技术亮点**
- `useMessageWall` Composable 封装核心逻辑
- 自动刷新、消息加载、发送留言
- 弹幕速度、颜色、字号可自定义
- 响应式设计，移动端适配

### 4. 电商购物系统

**完整流程**
- **商品浏览**：分类筛选、搜索、排序
- **购物车**：添加商品、数量调整、批量管理
- **订单结算**：地址选择、支付方式、优惠券
- **订单跟踪**：订单状态、物流信息、评价晒单

**用户体验**
- 流畅的购物流程
- 实时库存显示
- 友好的错误提示
- 移动端优化

## 📦 部署指南

### 生产环境构建

```bash
# 1. 安装依赖
npm install

# 2. 构建生产版本
npm run build

# 3. 构建产物在 dist/ 目录
```

### Nginx 部署

```nginx
server {
    listen 80;
    server_name your-domain.com;
    root /path/to/dist;
    index index.html;

    # 前端路由支持
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 代理
    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        
        # SSE 流式响应支持
        proxy_buffering off;
        proxy_cache off;
        proxy_set_header Connection '';
        chunked_transfer_encoding off;
    }

    # 静态资源缓存
    location ~* \.(jpg|jpeg|png|gif|ico|css|js)$ {
        expires 7d;
        add_header Cache-Control "public, immutable";
    }
}
```

### Docker 部署

创建 `Dockerfile`：

```dockerfile
FROM node:20-alpine as build
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
```

构建和运行：

```bash
# 构建镜像
docker build -t travel-frontend .

# 运行容器
docker run -d -p 80:80 --name travel-frontend travel-frontend
```

### 环境变量配置

不同环境使用不同的配置文件：

- `.env` - 默认配置
- `.env.local` - 本地开发（不提交到 Git）
- `.env.development` - 开发环境
- `.env.production` - 生产环境

## 🎨 特色亮点

- ✅ **现代化架构**：Vue 3 Composition API，拥抱最新前端技术
- ✅ **类型安全**：完整的 TypeScript 类型系统，IDE 智能提示
- ✅ **工业级代码**：统一的错误处理和日志系统
- ✅ **高度复用**：Composable 模式抽取可复用逻辑
- ✅ **规范化**：常量集中管理，消除魔法数字
- ✅ **性能优化**：路由懒加载、组件按需引入
- ✅ **用户体验**：流畅的动画、友好的交互反馈
- ✅ **响应式设计**：完美适配 PC、平板、手机
- ✅ **文档完善**：完整的开发文档和代码注释

## 🔗 相关项目

### 后端项目

**travel-ai-backend** - 基于 Spring Boot 3.x 的智能旅游推荐平台后端

- **项目地址**：https://github.com/Lq0412/travel-ai-backend
- **技术栈**：Spring Boot 3.2.0 + MyBatis Plus + MySQL + Redis
- **AI 集成**：通义千问 (DashScope)
- **核心功能**：AI 智能助手、Agent 架构、留言墙、电商系统、社区论坛

### 数字人服务

**DH_live** - 开源数字人项目，提供低延迟、高质量的实时数字人对话体验

- **项目地址**：https://github.com/kleinlee/DH_live
- **在线体验**：https://matesx.com
- **小程序**：搜索"MatesX数字生命"
- **特点**：最低算力、最小存储、无须训练、开箱即用

### 项目结构

```
.
├── travel-ai-frontend/    # 前端项目（本仓库）
├── travel-ai-backend/     # 后端项目
└── DH_live/              # 数字人服务（第三方）
```

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

### 开发规范

- 遵循 Vue 3 和 TypeScript 最佳实践
- 使用 ESLint 和 Prettier 保持代码风格统一
- 提交前运行 `npm run lint` 检查代码
- Commit 信息遵循约定式提交规范

### 提交流程

1. Fork 本仓库
2. 创建特性分支：`git checkout -b feature/AmazingFeature`
3. 提交更改：`git commit -m 'Add some AmazingFeature'`
4. 推送分支：`git push origin feature/AmazingFeature`
5. 提交 Pull Request

## 📄 开源协议

本项目采用 [MIT](LICENSE) 协议开源。

## 👨‍💻 作者

**Lq0412**

- GitHub: [@Lq0412](https://github.com/Lq0412)
- Email: 15919508513@163.com

## 🙏 致谢

感谢以下开源项目和技术社区，为本项目提供了强大的技术支持：

### 前端框架与工具

- [Vue.js](https://vuejs.org/) - 渐进式 JavaScript 框架
- [TypeScript](https://www.typescriptlang.org/) - JavaScript 的超集
- [Vite](https://vitejs.dev/) - 下一代前端构建工具
- [Ant Design Vue](https://antdv.com/) - 企业级 UI 组件库
- [Vue Router](https://router.vuejs.org/) - Vue.js 官方路由
- [Pinia](https://pinia.vuejs.org/) - Vue.js 状态管理

### HTTP 与工具库

- [Axios](https://axios-http.com/) - 基于 Promise 的 HTTP 客户端
- [Day.js](https://day.js.org/) - 轻量级日期处理库
- [VueUse](https://vueuse.org/) - Vue Composition API 工具集

### 后端与服务

- [Spring Boot](https://spring.io/projects/spring-boot) - 强大的 Java 应用框架
- [通义千问 (DashScope)](https://help.aliyun.com/zh/dashscope/) - 阿里云大语言模型
- [DH_live](https://github.com/kleinlee/DH_live) - 开源数字人项目

### 代码质量

- [ESLint](https://eslint.org/) - JavaScript 代码检查工具
- [Prettier](https://prettier.io/) - 代码格式化工具

### 特别鸣谢

- 感谢 **[@kleinlee](https://github.com/kleinlee)** 开源的 [DH_live](https://github.com/kleinlee/DH_live) 项目，为本项目提供了优秀的数字人解决方案。
- 感谢 **Vue.js 社区**和 **Ant Design 团队**，提供了优秀的开发工具和组件库。
- 感谢 **阿里云 DashScope** 提供的通义千问 AI 服务支持。

## 📮 联系方式

如有问题或建议，请通过以下方式联系：

- **Email**: 15919508513@163.com
- **GitHub Issues**: [提交 Issue](https://github.com/Lq0412/travel-ai-frontend/issues)
- **后端项目**: [travel-ai-backend](https://github.com/Lq0412/travel-ai-backend)

## 📊 项目状态

- ✅ 前端核心功能已完成
- ✅ AI 对话界面已集成
- ✅ **AI 行程规划系统已完成**（NEW）
  - ✅ 意图识别和结构化输出
  - ✅ 行程卡片展示
  - ✅ 历史消息恢复
  - ✅ 一键保存到"我的行程"
  - ✅ 每日亮点智能提取
- ✅ 数字人服务已集成
- ✅ 留言墙弹幕系统已完成
- ✅ 电商购物系统已完成
- ✅ 代码质量优化已完成
- 🚧 持续优化用户体验中

---

⭐ 如果这个项目对您有帮助，欢迎 Star！

**完整项目架构：**

- **前端仓库**：https://github.com/Lq0412/travel-ai-frontend
- **后端仓库**：https://github.com/Lq0412/travel-ai-backend
- **数字人服务**：https://github.com/kleinlee/DH_live
