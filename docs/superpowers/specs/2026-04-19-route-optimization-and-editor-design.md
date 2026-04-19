# 智能路线优化引擎 + 交互式行程编辑器 设计文档

> **目标：** 为旅行平台增加基于遗传算法的路线优化引擎和拖拽式行程编辑器，形成"AI 生成 → 算法优化 → 人工微调"的闭环，提升技术深度和用户体验。
>
> **竞赛亮点表述：** "基于遗传算法的多约束旅行路线优化引擎 + 所见即所得的人机协作行程编辑器"

---

## 1. 背景

当前行程生成流程：AI 对话 → 通义千问生成文本行程 → 前端展示。

问题：
- AI 生成的景点顺序未经优化，路线可能绕远
- 用户无法修改 AI 生成的行程，只能重新生成
- 缺乏算法层面的技术深度，"调 API"痕迹明显

本设计在 AI 生成和用户交互之间加入两个核心模块，解决上述问题。

---

## 2. 系统架构

```
用户输入旅行需求
      ↓
AI 对话生成行程（已有）
      ↓
StructuredItinerary（已有数据结构）
      ↓
┌─────────────────────────────────┐
│  路线优化引擎（后端新增）         │
│  RouteOptimizationService        │
│  ├── 距离矩阵构建（AMap API）    │
│  ├── 遗传算法求解器              │
│  └── 优化结果返回               │
└─────────────────────────────────┘
      ↓
┌─────────────────────────────────┐
│  交互式行程编辑器（前端改造）     │
│  ├── 拖拽排序 + 跨天调整        │
│  ├── 添加/删除/编辑景点          │
│  ├── 实时路线重算（AMap Driving）│
│  └── 保存修改到后端             │
└─────────────────────────────────┘
```

---

## 3. 路线优化引擎

### 3.1 问题定义

输入：一天内 N 个景点，每个景点有坐标 (lng, lat)、建议游玩时长、时间偏好（morning/noon/evening）。

目标：找到最优游览顺序，使总移动距离最短。

约束：
- 每个景点有建议游玩时长
- 时间窗偏好（如"故宫适合上午"应尽量满足）
- 总时间不超过一天可用时间

这是带时间窗约束的旅行商问题（TSPTW）的变体。

### 3.2 算法：遗传算法

**编码：** 排列编码（permutation），染色体 = 景点索引的排列。

**适应度函数：**
```
fitness = 1 / (totalDistance + penaltyWeight * timeWindowViolations)
```
- `totalDistance`：相邻景点间的距离之和（从预计算距离矩阵查表）
- `timeWindowViolations`：违反时间窗偏好次数 × 惩罚权重
- `penaltyWeight`：设为平均景点间距离的 2 倍，使时间窗违反的惩罚有实际意义

**遗传操作：**
- **选择：** 锦标赛选择（tournament size = 3）
- **交叉：** 顺序交叉 OX（Order Crossover），保留子序列相对顺序
- **变异：** 交换变异（Swap Mutation），随机交换两个位置

**参数：**
- 种群大小：80
- 最大迭代：200 代
- 交叉率：0.8
- 变异率：0.15
- 精英保留：前 2 个个体直接进入下一代
- 终止条件：200 代或连续 30 代最优解无改善

**性能：** 单城市内通常 10-20 个景点，GA 在毫秒级完成，无需异步处理。

### 3.3 距离矩阵构建

调用高德地图驾车路线规划 API（`AMap.Driving` 前端 / REST API 后端），获取景点间两两的实际驾车距离和时间。

矩阵大小：N×N，N ≤ 20 时 API 调用次数 ≤ 190 次（上三角）。

后端调用 `AmapService` 已有的 geocode 方法获取坐标，新增 distance matrix 方法调用高德驾车路线 REST API。

### 3.4 后端 API

**新增端点：** `POST /ai/trips/optimize`

请求体 `RouteOptimizationRequest`：
```json
{
  "activities": [
    {
      "name": "故宫博物院",
      "longitude": 116.397,
      "latitude": 39.918,
      "visitDurationMinutes": 180,
      "timeWindow": "morning",
      "estimatedCost": 60
    }
  ],
  "transportMode": "driving",
  "dayStartTime": "09:00",
  "dayEndTime": "21:00"
}
```

响应体 `RouteOptimizationResponse`：
```json
{
  "optimizedOrder": [2, 0, 3, 1],
  "totalDistance": 35.2,
  "totalDuration": 128,
  "totalCost": 480,
  "segments": [
    {
      "from": 2,
      "to": 0,
      "distance": 8.5,
      "duration": 25,
      "polyline": []
    }
  ],
  "fitnessHistory": [0.031, 0.042, 0.051, ...],
  "generationsUsed": 87
}
```

`fitnessHistory` 用于前端展示算法收敛动画。

### 3.5 后端新增文件

| 文件 | 职责 |
|------|------|
| `service/RouteOptimizationService.java` | 接口：优化入口方法 |
| `service/impl/RouteOptimizationServiceImpl.java` | 实现：构建距离矩阵 + 调用 GA |
| `optimizer/GeneticAlgorithmSolver.java` | GA 求解器：种群初始化、选择、交叉、变异、迭代 |
| `optimizer/Chromosome.java` | 染色体：排列编码 + 适应度计算 |
| `optimizer/DistanceMatrix.java` | 距离矩阵：调用 AMap API 构建 |
| `model/dto/route/RouteOptimizationRequest.java` | 请求 DTO |
| `model/dto/route/RouteOptimizationResponse.java` | 响应 DTO |
| `model/dto/route/RouteActivity.java` | 景点输入 DTO |
| `model/dto/route/RouteSegment.java` | 路线段 DTO |

### 3.6 AMap Distance API 集成

在现有 `AmapService` 中新增方法：

```java
DistanceMatrix getDistanceMatrix(List<GeoPoint> origins, List<GeoPoint> destinations, String mode);
```

调用高德 REST API `https://restapi.amap.com/v3/direction/driving`（驾车路线规划），传入起终点坐标，解析返回的距离和时间。

---

## 4. 交互式行程编辑器

### 4.1 功能清单

| 功能 | 描述 | 优先级 |
|------|------|--------|
| 拖拽重排 | 同一天内景点卡片拖拽换序 | P0 |
| 跨天调整 | 将景点从一天移到另一天 | P0 |
| 智能优化 | 一键调用 GA 引擎，动画展示优化过程 | P0 |
| 删除景点 | 点击删除按钮移除景点 | P0 |
| 添加景点 | AMap POI 搜索弹窗，选择后插入 | P1 |
| 编辑详情 | 弹出编辑面板修改名称/时间/描述/费用 | P1 |
| 保存修改 | 将编辑后的行程保存到后端 | P0 |
| 实时统计 | 距离/时间/费用随编辑实时更新 | P0 |

### 4.2 前端组件架构

```
TripDetailPage（改造）
├── RouteSummaryBar（新增）          ← 顶部距离/时间/费用统计栏
├── TripItineraryTimeline（改造）    ← 加入拖拽 + 编辑操作
│   ├── DayGroup（新增）             ← 按天分组，支持跨天拖入
│   │   ├── ActivityCard（改造）     ← 拖拽手柄 + 操作按钮
│   │   └── DropZone（新增）         ← 拖放目标区域
│   └── AddActivityButton（新增）    ← 打开 POI 搜索弹窗
├── ActivityEditDrawer（新增）        ← 编辑景点详情侧边抽屉
├── POISearchModal（新增）           ← AMap POI 搜索 + 选择弹窗
├── OptimizeButton（新增）           ← 触发 GA 优化 + 动画
└── DynamicMap（改造）               ← 路线变化动画模式
```

### 4.3 拖拽实现

使用 `vuedraggable`（基于 SortableJS）：

- 同天内拖拽：`vuedraggable` 组件包裹当天的 ActivityCard 列表
- 跨天拖拽：每个 DayGroup 是一个独立的 droppable 区域，景点可跨组拖拽
- 拖拽结束后触发 `onOrderChange` 事件

### 4.4 路线重算策略

每次编辑操作后：
1. 前端收集当前所有景点的坐标和顺序
2. 对每一天，依次调用 `AMap.Driving` 计算相邻景点间的路线
3. 汇总距离和时间，更新 `RouteSummaryBar`
4. 更新地图上的路线 polyline

不调用后端——前端的 AMap JS API 已支持驾车路线规划，响应更快。

### 4.5 "一键智能优化"流程

1. 用户点击"智能优化"按钮
2. 前端收集当天（或所有天）的景点列表
3. 调用后端 `POST /ai/trips/optimize`
4. 后端返回优化顺序 + `fitnessHistory`
5. 前端：
   - 时间线上卡片动画过渡到新位置
   - 地图上路线动画切换
   - 展示收敛曲线（小型折线图）

### 4.6 保存编辑

现有 `POST /ai/trips/save` 端点已支持保存行程数据。编辑后的 `StructuredItinerary` JSON 重新序列化后调用该端点即可。

新增端点用于增量更新（避免重新保存整个行程）：

`PUT /ai/trips/{id}/activities` — 更新行程的活动列表（顺序、增删、修改）。

### 4.7 前端新增/改造文件

| 文件 | 动作 | 职责 |
|------|------|------|
| `components/trips/detail/RouteSummaryBar.vue` | 新增 | 距离/时间/费用统计栏 |
| `components/trips/detail/DayGroup.vue` | 新增 | 按天分组的拖放容器 |
| `components/trips/detail/ActivityCard.vue` | 新增 | 可拖拽的活动卡片 |
| `components/trips/detail/ActivityEditDrawer.vue` | 新增 | 编辑景点详情的抽屉 |
| `components/trips/detail/POISearchModal.vue` | 新增 | POI 搜索弹窗 |
| `components/trips/detail/OptimizeButton.vue` | 新增 | 优化触发按钮 + 收敛动画 |
| `pages/trips/TripDetailPage.vue` | 改造 | 集成编辑器、状态管理 |
| `components/trips/detail/TripItineraryTimeline.vue` | 改造 | 替换为拖拽式实现 |
| `pages/workspace/DynamicMap.vue` | 改造 | 新增路线动画模式 |
| `utils/routeCalculation.ts` | 新增 | 前端路线计算工具函数 |
| `types/route.ts` | 新增 | 路线优化相关类型定义 |

---

## 5. 数据模型变更

### 后端 DTO 新增

`RouteOptimizationRequest` — 路线优化请求
`RouteOptimizationResponse` — 路线优化响应
`RouteActivity` — 优化输入景点
`RouteSegment` — 优化后路线段

### 数据库

无新增表。编辑后的行程通过 `trip.structured_data` 字段存储（JSON 序列化），与现有数据结构兼容。

### 前端类型

`RouteSummary` — 距离/时间/费用统计
`RouteSegmentResult` — 前端计算的路线段

---

## 6. 测试策略

### 后端测试

- `GeneticAlgorithmSolverTest`：测试 GA 求解器
  - 简单 4 景点问题，验证结果优于随机排列
  - 10 景点问题，验证收敛
  - 空输入、单景点、两景点等边界情况
  - 时间窗约束验证

- `RouteOptimizationServiceTest`：测试集成服务
  - mock AMap distance API，验证距离矩阵构建
  - 验证请求/响应转换

### 前端测试

- 组件渲染和交互测试（Vitest + Vue Test Utils）
- 拖拽排序后的数据更新验证
- 路线重算逻辑的单元测试

---

## 7. 实施优先级

1. **P0 — GA 求解器 + 优化 API**（后端核心，技术亮点）
2. **P0 — 拖拽重排 + 保存**（编辑器基础）
3. **P0 — 实时路线重算 + 统计栏**（交互完整性）
4. **P0 — 一键优化按钮 + 地图动画**（演示效果）
5. **P1 — 添加/删除景点**（编辑器增强）
6. **P1 — 编辑景点详情**（编辑器增强）
7. **P1 — 跨天调整**（编辑器增强）
