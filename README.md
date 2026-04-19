# 智能 AI 旅游推荐平台

基于 Spring Boot + Vue 3 的 AI 旅游规划与推荐平台，集成大模型对话、RAG 知识检索、智能行程生成等功能。

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Spring Boot 3.5.5, Java 21, MyBatis-Plus, MySQL 8.4, Redis 7.4, RabbitMQ 4 |
| AI | Spring AI 1.0.0-SNAPSHOT, 阿里云 DashScope (通义千问), Milvus 2.4 (RAG), Tavily 搜索 |
| 前端 | Vue 3 (Composition API), TypeScript, Vite 6, Ant Design Vue 4, 高德地图 |
| 部署 | Docker Compose, 阿里云 ACR 镜像仓库, 腾讯云 COS 对象存储 |

## 项目结构

```
travel/
├── .env.example                    # 环境变量模板
├── docker-compose.yml              # 本地开发编排（从源码构建）
├── docker-compose.prod.yml         # 生产部署编排（从 ACR 拉取镜像）
├── docs/
│   └── deployment-aliyun-ecs.md    # 阿里云 ECS 部署指南
│
├── travel_backend/                 # ===== 后端 (Spring Boot) =====
│   ├── Dockerfile                  # 多阶段构建 (Maven → JRE)
│   ├── pom.xml
│   └── src/main/java/com/lq/travel/
│       ├── TravelApplication.java  # 启动类
│       │
│       ├── agent/                  # AI Agent 体系
│       │   ├── Agent.java              # Agent 接口
│       │   ├── BaseAgent.java          # Agent 基类
│       │   └── impl/
│       │       └── GenericTravelAgent.java  # 通用旅行助手
│       │
│       ├── annotation/             # 自定义注解
│       │   ├── ApiRateLimit.java       # API 限流
│       │   └── AuthCheck.java          # 权限校验
│       │
│       ├── aop/                    # AOP 切面
│       │   ├── ApiRateLimitAspect.java # 限流切面
│       │   └── AuthInterceptor.java    # 鉴权拦截器
│       │
│       ├── callback/               # SSE 流式回调
│       │   ├── StreamCallback.java          # 回调接口
│       │   ├── StreamCallbackAdapter.java   # 回调适配器
│       │   ├── EnhancedStreamCallbackAdapter.java
│       │   └── CleaningStreamCallbackAdapter.java
│       │
│       ├── common/                 # 通用响应类
│       │   ├── BaseResponse.java, ResultUtils.java
│       │   ├── ResponseDTO.java, ResponseUtils.java
│       │   ├── PageRequest.java, DeleteRequest.java
│       │
│       ├── config/                 # 配置类
│       │   ├── AIConfiguration.java       # AI 模块初始化
│       │   ├── AIConfigurationValidator.java
│       │   ├── AIRateLimiterConfig.java   # AI 限流配置
│       │   ├── RabbitMQConfig.java        # 消息队列配置
│       │   ├── RedisConfig.java           # Redis 配置
│       │   ├── CosClientConfig.java       # 腾讯云 COS
│       │   ├── MybatisPlusConfig.java
│       │   ├── ProductSchemaInitializer.java  # 商品表自动初始化
│       │   └── ...Cors, Jackson, Async, Knife4j
│       │
│       ├── constant/               # 常量
│       │   ├── AIModelConfig.java         # AI 模型参数
│       │   ├── TimeoutConfig.java         # 超时配置
│       │   ├── AiMqConstants.java         # MQ 常量
│       │   └── UserConstant.java, RegexPatterns.java
│       │
│       ├── controller/             # REST 控制器 (10个)
│       │   ├── AIController.java          # AI 对话 / 流式接口
│       │   ├── AiTaskController.java      # AI 任务管理
│       │   ├── ConversationController.java # 会话管理
│       │   ├── MonitoringController.java  # AI 监控
│       │   ├── TripController.java        # 行程 CRUD
│       │   ├── TripPhotoController.java   # 行程照片
│       │   ├── ProductController.java     # 商品
│       │   ├── UserController.java        # 用户登录注册
│       │   ├── FileController.java        # 文件上传
│       │   └── ContentImageController.java # AI 图片生成
│       │
│       ├── exception/              # 异常处理
│       │   ├── BusinessException.java, ErrorCode.java
│       │   ├── GlobalExceptionHandler.java, ThrowUtils.java
│       │
│       ├── manager/                # 上传管理
│       │   ├── CosManager.java            # 腾讯云 COS 操作
│       │   ├── FileManager.java
│       │   └── upload/                    # 图片上传模板
│       │       ├── FilePictureUpload.java, UrlPictureUpload.java
│       │       └── PictureUploadTemplate.java
│       │
│       ├── mapper/                 # MyBatis Mapper (10个)
│       │   ├── UserMapper, TripMapper, TripPhotoMapper
│       │   ├── ProductMapper
│       │   ├── AIConversationMapper, AIMessageMapper, AiTaskMapper
│       │   └── KnowledgeAttraction/Experience/FoodMapper
│       │
│       ├── model/                  # 数据模型
│       │   ├── entity/             # 数据库实体 (11个)
│       │   │   ├── User, Trip, TripPhoto, Product
│       │   │   ├── AIConversation, AIMessage, AiTask, AgentTask
│       │   │   └── KnowledgeAttraction/Experience/Food
│       │   ├── dto/                # 请求 DTO
│       │   │   ├── ai/             # AI 相关 (AIRequest, AgentRequest, RagChunk...)
│       │   │   ├── user/           # 用户 (Login, Register, Update...)
│       │   │   ├── trip/           # 行程 (Generate, Save, ForumPublish)
│       │   │   ├── product/        # 商品 (ProductSave)
│       │   │   ├── order/          # 订单 (CreateOrder)
│       │   │   ├── message/        # 留言墙
│       │   │   └── merchant/, file/
│       │   ├── vo/                 # 视图对象 (8个)
│       │   │   ├── LoginUserVO, UserVO, TripVO, TripPhotoVO
│       │   │   ├── ProductVO, AIConversationVO, AgentInfoVO
│       │   │   └── AiTaskStatusVO, ContentImageVO
│       │   └── enums/             # 枚举
│       │       ├── UserRoleEnum, IntentType
│       │       └── PictureReviewStatusEnum
│       │
│       ├── mq/                     # 消息队列
│       │   ├── TaskMessageProducer.java          # 任务消息生产者
│       │   └── KnowledgeIngestionListener.java   # 知识入库消费者
│       │
│       ├── provider/               # AI 模型提供商
│       │   ├── AIProvider.java             # 提供商接口
│       │   └── DashScopeProvider.java      # 阿里云 DashScope
│       │
│       ├── service/                # 服务接口 (15个)
│       │   └── impl/              # 服务实现
│       │       ├── AIServiceImpl.java              # AI 核心服务
│       │       ├── AgentServiceImpl.java           # Agent 管理
│       │       ├── TravelRagServiceImpl.java       # RAG 检索增强
│       │       ├── MilvusRagClient.java            # Milvus 客户端
│       │       ├── MilvusKnowledgeSyncService.java # 知识同步
│       │       ├── DashScopeEmbeddingService.java  # 向量嵌入
│       │       ├── TravelMultiAgentCoordinator.java # 多 Agent 协调
│       │       ├── TripServiceImpl.java            # 行程服务
│       │       ├── ProductServiceImpl.java         # 商品服务
│       │       ├── UserServiceImpl.java            # 用户服务
│       │       ├── ImageGenerationServiceImpl.java # AI 图片生成
│       │       ├── AmapServiceImpl.java            # 高德地图服务
│       │       └── ...
│       │
│       └── util/                   # 工具类
│           ├── IntentAwareChatEnhancer.java    # 意图感知对话增强
│           ├── IntentAnalyzer.java             # 意图分析
│           ├── TravelConversationMemory.java   # 对话记忆
│           ├── StructuredItineraryExtractor.java # 行程解析
│           ├── ResponseParser.java             # AI 响应解析
│           ├── AICacheHandler.java             # AI 缓存
│           └── ...Season, Greeting, TimeFormat, etc.
│
├── travel_backend/src/main/resources/
│   ├── application.yml             # 通用配置
│   ├── application-local.yml       # 本地开发配置
│   ├── application-prod.yml        # 生产环境配置
│   ├── mapper/                     # MyBatis XML
│   │   ├── AIConversationMapper.xml
│   │   ├── AIMessageMapper.xml
│   │   └── UserMapper.xml
│   └── sql/
│       ├── travel.sql              # 建表 SQL（Docker 初始化用）
│       ├── travel-data-backup.sql  # 数据备份
│       ├── knowledge_seed.sql      # 知识库种子数据
│       └── knowledge_data_template.sql
│
├── travel_frontend/                # ===== 前端 (Vue 3) =====
│   ├── Dockerfile                  # 多阶段构建 (Node → Nginx)
│   ├── nginx.conf                  # Nginx 配置
│   ├── package.json
│   ├── vite.config.ts
│   ├── index.html
│   └── src/
│       ├── App.vue                 # 根组件
│       ├── main.ts                 # 入口
│       ├── request.ts              # Axios 封装
│       ├── access.ts               # 路由守卫
│       │
│       ├── api/                    # API 接口层 (12个模块)
│       │   ├── aiController.ts         # AI 对话
│       │   ├── chatConversationClient.ts
│       │   ├── conversationController.ts
│       │   ├── monitoringController.ts  # AI 监控
│       │   ├── tripController.ts        # 行程
│       │   ├── tripPhotoController.ts   # 照片
│       │   ├── productController.ts     # 商品
│       │   ├── userController.ts        # 用户
│       │   ├── fileController.ts        # 文件
│       │   └── contentImageController.ts
│       │
│       ├── components/             # 公共组件
│       │   ├── content/            # 内容组件
│       │   ├── home/               # 首页组件
│       │   ├── icons/              # 图标组件
│       │   ├── mall/               # 商城组件
│       │   └── trips/              # 行程组件
│       │
│       ├── composables/            # 组合式函数 (8个)
│       │   ├── useChatStream.ts         # SSE 流式对话
│       │   ├── useWorkspaceConversations.ts # 工作区会话
│       │   ├── useAutoScroll.ts         # 自动滚动
│       │   ├── useVisualContent.ts      # 可视化内容
│       │   ├── useHomeDestinations.ts   # 首页目的地
│       │   └── useRecentTripRecommendations.ts
│       │
│       ├── layouts/                # 布局
│       │   ├── LandingLayout.vue       # 落地页布局
│       │   └── TopNavLayout.vue        # 顶部导航布局
│       │
│       ├── pages/                  # 页面
│       │   ├── HomePage.vue            # 首页
│       │   ├── workspace/              # AI 工作区
│       │   │   └── WorkspacePage.vue
│       │   ├── trips/                  # 行程管理
│       │   │   ├── TripsPage.vue
│       │   │   └── TripDetailPage.vue
│       │   ├── mall/                   # 商城
│       │   │   ├── MallPage.vue
│       │   │   ├── ProductDetailPage.vue
│       │   │   └── PaymentSuccess.vue
│       │   ├── ai/                     # AI 监控（管理员）
│       │   │   └── AIAdminMonitorPage.vue
│       │   └── user/                   # 用户
│       │       ├── LoginPage.vue
│       │       ├── RegisterPage.vue
│       │       ├── ProfilePage.vue
│       │       └── ChatInput.vue
│       │
│       ├── router/                 # 路由
│       │   └── index.ts               # 14 条路由
│       │
│       ├── stores/                 # Pinia 状态
│       │   └── useLoginUserStore.ts
│       │
│       ├── types/                  # TypeScript 类型
│       │   ├── chat.ts
│       │   ├── itinerary.ts
│       │   └── product.ts
│       │
│       └── utils/                  # 工具函数
│           ├── chatStreamParser.ts     # SSE 流解析
│           ├── chatRecommendations.ts  # 聊天推荐
│           ├── productRecommendation.ts # 商品推荐
│           ├── productCatalog.ts       # 商品目录
│           ├── productOrder.ts         # 订单处理
│           ├── tripDetail.ts           # 行程详情
│           ├── tripPhoto.ts            # 照片管理
│           ├── tripWorkflow.ts         # 行程工作流
│           ├── workspaceSession.ts     # 工作区会话
│           ├── layoutResolver.ts       # 布局解析
│           ├── errorHandler.ts         # 错误处理
│           ├── logger.ts              # 日志
│           ├── monitoring.ts          # 监控
│           └── ...test files
│
└── docker-compose.yml / docker-compose.prod.yml   # Docker 编排
```

## 页面路由

| 页面 | 路由 | 说明 |
|------|------|------|
| 首页 | `/` | 目的地展示、AI 推荐入口 |
| AI 工作区 | `/workspace` | 流式对话、行程生成 |
| 我的行程 | `/trips` | 行程列表 |
| 行程详情 | `/trips/:id` | 时间线 + 地图 + 照片 |
| 商城 | `/mall` | 旅游商品 |
| 商品详情 | `/products/:id` | 商品详情 + 购买 |
| 登录/注册 | `/user/login`, `/user/register` | 用户认证 |
| 个人中心 | `/profile` | 用户信息 |
| AI 监控 | `/admin/ai-monitor` | 管理员监控（Milvus 状态等） |

## 快速开始

### 本地开发

```bash
# 1. 启动依赖服务
docker compose up -d mysql redis rabbitmq etcd minio milvus

# 2. 后端（IDEA 中运行 TravelApplication，profile=local）
# 3. 前端
cd travel_frontend && npm install && npm run dev
```

### Docker 全量部署

```bash
cp .env.example .env  # 编辑填入密码和 API Key
docker compose up -d
```

### 生产部署（阿里云 ECS）

详见 [docs/deployment-aliyun-ecs.md](docs/deployment-aliyun-ecs.md)

```bash
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
```
