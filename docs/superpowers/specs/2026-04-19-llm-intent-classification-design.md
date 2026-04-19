# LLM 意图分类与信息提取替换正则方案

## 背景

当前 AI 旅游对话的意图识别（`IntentAnalyzer`）和会话信息提取（`TravelConversationMemory`）均基于正则表达式。正则匹配无法理解自然语言语义，导致：

1. **意图误判**：用户发送旅游相关消息，但被识别为"普通聊天"（`GENERAL_CHAT`），不会触发行程生成流程。
2. **信息漏提取**：用户已提供目的地、日期等信息，但正则未匹配到，系统认为信息不完整，继续追问。
3. **裸 JSON 输出**：系统认为信息未收集完，LLM 在 `GENERAL_PROMPT` 下擅自输出未格式化的 JSON。

## 方案

用 LLM 调用替换两处正则逻辑，使用项目现有的 `AIService.chat(AIRequest)` 接口和默认模型（`tongyi-xiaomi-analysis-pro`）。LLM 失败时回退到原正则逻辑作为兜底。

### 改动 1：`IntentAnalyzer` — LLM 意图分类

**文件**：`travel_backend/src/main/java/com/lq/travel/util/IntentAnalyzer.java`

**变更**：
- `analyze(String userInput)` 方法改为 Spring Bean（需要注入 `AIService`），从 static 工具类改为 `@Component`。
- 内部调用 `AIService.chat()` 进行意图分类，system prompt 要求 LLM 返回 JSON `{"intent": "ITINERARY_GENERATION|ATTRACTION_QUERY|GENERAL_CHAT"}`。
- LLM 调用失败或 JSON 解析失败时，回退到现有正则逻辑。
- 原正则方法保留为 `analyzeByRegex(String)` 作为 fallback。
- `temperature=0.0`，`maxTokens=50`。

**分类 Prompt**：
```
你是一个旅游对话意图分类器。根据用户消息判断意图，只返回 JSON：{"intent": "ITINERARY_GENERATION"} 或 {"intent": "ATTRACTION_QUERY"} 或 {"intent": "GENERAL_CHAT"}

规则：
- ITINERARY_GENERATION：用户想规划行程、安排旅游、制定攻略，或提到天数/目的地+出行意图
- ATTRACTION_QUERY：用户询问某个具体景点的介绍、门票、推荐
- GENERAL_CHAT：其他闲聊、知识问答、与旅游规划无关的内容

关注语义而非关键词。例如"帮我安排下周末出去玩"是行程规划，不是普通聊天。
```

### 改动 2：`TravelConversationMemory` — LLM 信息提取

**文件**：`travel_backend/src/main/java/com/lq/travel/util/TravelConversationMemory.java`

**变更**：
- `analyze(List<AIMessage> messages)` 方法改为 Spring Bean（需要注入 `AIService`），从 static 工具类改为 `@Component`。
- 将对话历史拼接为文本，交给 LLM 一次性提取七个字段并判断是否收集完整。
- LLM 返回 JSON 包含所有字段 + `isReadyForItinerary` 布尔值。
- LLM 失败时回退到现有正则逻辑。
- 原正则方法保留为 `analyzeByRegex(List<AIMessage>)` 作为 fallback。
- `temperature=0.0`，`maxTokens=300`。

**提取 Prompt**：
```
你是旅行信息提取器。从对话历史中提取用户已确认的旅行信息，返回 JSON：
{
  "destination": "目的地（空字符串表示未确认）",
  "travelDate": "出行时间",
  "days": "天数",
  "budget": "预算",
  "accommodation": "住宿偏好",
  "transport": "交通方式",
  "companions": "同行人",
  "isReadyForItinerary": true/false
}

规则：
- 只提取用户明确说出的信息，不猜测。
- isReadyForItinerary 为 true 需要满足：目的地、出行时间、天数已确认。
- 住宿偏好和交通方式缺失不影响 isReadyForItinerary。
- 值为空字符串表示该字段未确认。
```

### 不改动的部分

- `IntentType` 枚举保持不变。
- `MemorySnapshot` record 结构不变（仍包含 7 个字段 + `isReadyForItinerary()` 方法）。
- `TravelIntentPromptFactory` 的 prompt 路由不变。
- `GenericTravelAgent` 的 auto-promotion 逻辑保留。
- `GenericTravelAgent` 中调用 `IntentAnalyzer` 和 `TravelConversationMemory` 的方式需适配（从 static 改为注入 Bean）。
- `AIController` 中调用 `IntentAnalyzer` 和 `TravelConversationMemory` 的方式需适配。
- `StructuredItineraryExtractor` 不变。
- 前端不变。

### 调用方适配

因为 `IntentAnalyzer` 和 `TravelConversationMemory` 从 static 工具类变为 Spring Bean，所有调用方需要从：
```java
IntentAnalyzer.analyze(text)
TravelConversationMemory.analyze(messages)
```
改为注入后调用：
```java
@Autowired IntentAnalyzer intentAnalyzer;
intentAnalyzer.analyze(text)

@Autowired TravelConversationMemory conversationMemory;
conversationMemory.analyze(messages)
```

受影响的调用方：
- `GenericTravelAgent`（通过构造器注入）
- `AIController`（通过 `@Autowired`）
- `IntentAwareChatEnhancer`（通过 `@Autowired`）

### 容错设计

```
LLM 调用
  ├─ 成功 → JSON 解析
  │    ├─ 解析成功 → 使用 LLM 结果
  │    └─ 解析失败 → 回退正则
  └─ 失败（异常/超时）→ 回退正则
```

- AIService 已内置缓存，相同输入不会重复调用 LLM。
- 不额外设置超时，复用 AIService 默认超时配置。

### `isReadyForItinerary` 条件放宽

当前正则版本要求 5 个字段全部确认（目的地 + 日期 + 天数 + 住宿 + 交通）。LLM 提取版本放宽为：**目的地 + 出行时间 + 天数**即可。住宿和交通作为可选信息，缺失时由 LLM 在行程中给出推荐。
