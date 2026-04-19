# LLM Intent Classification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace regex-based intent analysis and conversation memory extraction with LLM calls, with regex fallback on failure.

**Architecture:** Convert `IntentAnalyzer` and `TravelConversationMemory` from static utility classes to Spring `@Component` beans that inject `AIService`. Each class keeps its original regex logic as a private fallback method. The public `analyze()` method tries LLM first, falls back to regex on any failure. Call sites (`AIController`, `GenericTravelAgent`, `IntentAwareChatEnhancer`) are updated from static calls to injected bean calls.

**Tech Stack:** Java 21, Spring Boot 3.5.5, existing `AIService.chat(AIRequest)` API

---

## File Structure

| Action | File | Responsibility |
|--------|------|----------------|
| Rewrite | `travel_backend/src/main/java/com/lq/travel/util/IntentAnalyzer.java` | LLM intent classification with regex fallback |
| Rewrite | `travel_backend/src/main/java/com/lq/travel/util/TravelConversationMemory.java` | LLM info extraction with regex fallback |
| Modify | `travel_backend/src/main/java/com/lq/travel/util/IntentAwareChatEnhancer.java` | Convert from static to `@Component`, inject `IntentAnalyzer` |
| Modify | `travel_backend/src/main/java/com/lq/travel/agent/impl/GenericTravelAgent.java` | Add `IntentAnalyzer` constructor parameter |
| Modify | `travel_backend/src/main/java/com/lq/travel/config/AIConfiguration.java` | Pass `IntentAnalyzer` bean to `GenericTravelAgent` constructor |
| Modify | `travel_backend/src/main/java/com/lq/travel/controller/AIController.java` | Inject `TravelConversationMemory` and `IntentAwareChatEnhancer` beans |
| Rewrite | `travel_backend/src/test/java/com/lq/travel/util/IntentAnalyzerTest.java` | Test LLM path, regex fallback, edge cases |
| Rewrite | `travel_backend/src/test/java/com/lq/travel/util/TravelConversationMemoryTest.java` | Test LLM path, regex fallback, edge cases |

---

### Task 1: Convert IntentAnalyzer to Spring Bean with LLM Classification

**Files:**
- Rewrite: `travel_backend/src/main/java/com/lq/travel/util/IntentAnalyzer.java`
- Test: `travel_backend/src/test/java/com/lq/travel/util/IntentAnalyzerTest.java`

- [ ] **Step 1: Write the test for LLM-based intent analysis**

Create test with a mocked `AIService` that returns known intent JSON. Test the instance method `analyze()`.

```java
package com.lq.travel.util;

import com.lq.travel.model.dto.ai.AIRequest;
import com.lq.travel.model.dto.ai.AIResponse;
import com.lq.travel.model.enums.IntentType;
import com.lq.travel.service.AIService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IntentAnalyzerTest {

    private IntentAnalyzer createAnalyzer(AIService aiService) {
        return new IntentAnalyzer(aiService);
    }

    private AIService mockAiServiceReturning(String content) {
        AIService aiService = mock(AIService.class);
        AIResponse response = AIResponse.builder()
                .success(true)
                .content(content)
                .build();
        when(aiService.chat(any(AIRequest.class))).thenReturn(response);
        return aiService;
    }

    @Test
    void shouldClassifyItineraryGenerationViaLLM() {
        AIService aiService = mockAiServiceReturning("{\"intent\": \"ITINERARY_GENERATION\"}");
        IntentAnalyzer analyzer = createAnalyzer(aiService);

        IntentType result = analyzer.analyze("帮我安排下周末去成都玩");

        assertEquals(IntentType.ITINERARY_GENERATION, result);
    }

    @Test
    void shouldClassifyAttractionQueryViaLLM() {
        AIService aiService = mockAiServiceReturning("{\"intent\": \"ATTRACTION_QUERY\"}");
        IntentAnalyzer analyzer = createAnalyzer(aiService);

        IntentType result = analyzer.analyze("都江堰门票多少钱");

        assertEquals(IntentType.ATTRACTION_QUERY, result);
    }

    @Test
    void shouldClassifyGeneralChatViaLLM() {
        AIService aiService = mockAiServiceReturning("{\"intent\": \"GENERAL_CHAT\"}");
        IntentAnalyzer analyzer = createAnalyzer(aiService);

        IntentType result = analyzer.analyze("今天天气怎么样");

        assertEquals(IntentType.GENERAL_CHAT, result);
    }

    @Test
    void shouldFallbackToRegexWhenLLMFails() {
        AIService aiService = mock(AIService.class);
        when(aiService.chat(any(AIRequest.class))).thenThrow(new RuntimeException("LLM unavailable"));
        IntentAnalyzer analyzer = createAnalyzer(aiService);

        IntentType result = analyzer.analyze("给我一个两天的广州从化旅游规划");

        assertEquals(IntentType.ITINERARY_GENERATION, result);
    }

    @Test
    void shouldFallbackToRegexWhenLLMReturnsInvalidJson() {
        AIService aiService = mockAiServiceReturning("not valid json at all");
        IntentAnalyzer analyzer = createAnalyzer(aiService);

        IntentType result = analyzer.analyze("广州从化温泉景点门票和开放时间怎么样");

        assertEquals(IntentType.ATTRACTION_QUERY, result);
    }

    @Test
    void shouldFallbackToGeneralChatWhenBothFail() {
        AIService aiService = mock(AIService.class);
        when(aiService.chat(any(AIRequest.class))).thenThrow(new RuntimeException("LLM unavailable"));
        IntentAnalyzer analyzer = createAnalyzer(aiService);

        IntentType result = analyzer.analyze("你好呀");

        assertEquals(IntentType.GENERAL_CHAT, result);
    }

    @Test
    void shouldHandleNullInput() {
        AIService aiService = mockAiServiceReturning("{\"intent\": \"GENERAL_CHAT\"}");
        IntentAnalyzer analyzer = createAnalyzer(aiService);

        IntentType result = analyzer.analyze(null);

        assertEquals(IntentType.GENERAL_CHAT, result);
    }

    @Test
    void shouldHandleEmptyInput() {
        AIService aiService = mockAiServiceReturning("{\"intent\": \"GENERAL_CHAT\"}");
        IntentAnalyzer analyzer = createAnalyzer(aiService);

        IntentType result = analyzer.analyze("   ");

        assertEquals(IntentType.GENERAL_CHAT, result);
    }

    @Test
    void shouldReturnGeneralChatWhenLLMReturnsUnknownIntent() {
        AIService aiService = mockAiServiceReturning("{\"intent\": \"SOMETHING_WEIRD\"}");
        IntentAnalyzer analyzer = createAnalyzer(aiService);

        IntentType result = analyzer.analyze("随便聊聊");

        assertEquals(IntentType.GENERAL_CHAT, result);
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd travel_backend && mvn test -pl . -Dtest=IntentAnalyzerTest -Dsurefire.useFile=false 2>&1 | tail -30`
Expected: FAIL — `IntentAnalyzer` is still a static class, not a Spring bean with constructor injection.

- [ ] **Step 3: Implement the new IntentAnalyzer**

Rewrite `travel_backend/src/main/java/com/lq/travel/util/IntentAnalyzer.java`:

```java
package com.lq.travel.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lq.travel.model.dto.ai.AIRequest;
import com.lq.travel.model.dto.ai.AIResponse;
import com.lq.travel.model.enums.IntentType;
import com.lq.travel.service.AIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Slf4j
@Component
public class IntentAnalyzer {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String CLASSIFICATION_PROMPT = """
            你是一个旅游对话意图分类器。根据用户消息判断意图，只返回JSON：{"intent": "ITINERARY_GENERATION"} 或 {"intent": "ATTRACTION_QUERY"} 或 {"intent": "GENERAL_CHAT"}

            规则：
            - ITINERARY_GENERATION：用户想规划行程、安排旅游、制定攻略，或提到天数/目的地+出行意图
            - ATTRACTION_QUERY：用户询问某个具体景点的介绍、门票、推荐
            - GENERAL_CHAT：其他闲聊、知识问答、与旅游规划无关的内容

            关注语义而非关键词。例如"帮我安排下周末出去玩"是行程规划，不是普通聊天。
            """;

    // --- Regex fallback patterns (unchanged from original) ---
    private static final Pattern DIRECT_ITINERARY_PATTERN = Pattern.compile(
            "(旅游规划|旅行规划|旅游计划|旅行计划|行程规划|路线规划|行程安排|旅游攻略|旅行攻略)"
    );
    private static final Pattern PLAN_VERB_PATTERN = Pattern.compile("(规划|安排|制定|设计|生成|做一份|出一份|做个|策划|定制)");
    private static final Pattern PLAN_NOUN_PATTERN = Pattern.compile("(行程|计划|路线|方案|攻略)");
    private static final Pattern TRAVEL_PATTERN = Pattern.compile("(游玩|旅游|旅行|出游|出行|度假|去哪玩|怎么玩)");
    private static final Pattern DAY_PATTERN = Pattern.compile("(几天|\\d+\\s*[天日]|[一二三四五六七八九十两]+\\s*[天日])");
    private static final Pattern STRUCTURED_JSON_PATTERN = Pattern.compile(
            "(结构化行程|结构化\\s*json|输出要求|dailyplans|totalestimatedcost|imageurl)"
    );
    private static final Pattern ATTRACTION_PATTERN = Pattern.compile(
            ".*(推荐|介绍|有什么|哪里|哪些).*(景点|地方|好玩)|" +
            ".*(景点|温泉|森林公园|古村|溪头村|石门).*(怎么样|如何|介绍)|" +
            ".*(门票|开放时间|交通).*"
    );

    private final AIService aiService;

    public IntentAnalyzer(AIService aiService) {
        this.aiService = aiService;
    }

    public IntentType analyze(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return IntentType.GENERAL_CHAT;
        }

        try {
            return analyzeByLLM(userInput);
        } catch (Exception e) {
            log.warn("LLM意图分类失败，回退到正则: {}", e.getMessage());
            return analyzeByRegex(userInput);
        }
    }

    private IntentType analyzeByLLM(String userInput) {
        AIRequest request = AIRequest.builder()
                .message(userInput)
                .systemPrompt(CLASSIFICATION_PROMPT)
                .temperature(0.0)
                .maxTokens(50)
                .build();

        AIResponse response = aiService.chat(request);
        if (!response.getSuccess() || response.getContent() == null) {
            throw new RuntimeException("LLM返回失败: " + response.getErrorMessage());
        }

        return parseIntentFromJson(response.getContent().trim());
    }

    private IntentType parseIntentFromJson(String json) {
        try {
            JsonNode node = OBJECT_MAPPER.readTree(json);
            String intentStr = node.has("intent") ? node.get("intent").asText() : "";
            return switch (intentStr) {
                case "ITINERARY_GENERATION" -> IntentType.ITINERARY_GENERATION;
                case "ATTRACTION_QUERY" -> IntentType.ATTRACTION_QUERY;
                case "GENERAL_CHAT" -> IntentType.GENERAL_CHAT;
                default -> {
                    log.warn("LLM返回未知意图: {}", intentStr);
                    yield null;
                }
            };
        } catch (Exception e) {
            log.warn("意图JSON解析失败: {}", json);
            return null;
        }
    }

    // --- Regex fallback (original logic, unchanged) ---
    IntentType analyzeByRegex(String userInput) {
        if (userInput == null || userInput.trim().isEmpty()) {
            return IntentType.GENERAL_CHAT;
        }

        String input = userInput.toLowerCase();
        String itineraryReason = matchItineraryReason(input);

        if (itineraryReason != null) {
            log.info("正则回退-识别为行程规划意图(规则: {}): {}", itineraryReason, userInput);
            return IntentType.ITINERARY_GENERATION;
        }

        if (ATTRACTION_PATTERN.matcher(input).find()) {
            log.info("正则回退-识别为景点查询意图: {}", userInput);
            return IntentType.ATTRACTION_QUERY;
        }

        log.info("正则回退-识别为通用聊天意图: {}", userInput);
        return IntentType.GENERAL_CHAT;
    }

    private String matchItineraryReason(String input) {
        if (DIRECT_ITINERARY_PATTERN.matcher(input).find()) {
            return "direct_itinerary_keywords";
        }
        boolean hasPlanVerb = PLAN_VERB_PATTERN.matcher(input).find();
        boolean hasPlanNoun = PLAN_NOUN_PATTERN.matcher(input).find();
        boolean hasTravelWord = TRAVEL_PATTERN.matcher(input).find();
        boolean hasDayExpression = DAY_PATTERN.matcher(input).find();
        boolean hasStructuredJsonHint = STRUCTURED_JSON_PATTERN.matcher(input).find();

        if (hasStructuredJsonHint && (hasTravelWord || hasPlanNoun || hasPlanVerb)) {
            return "structured_json_requirement";
        }
        if (hasPlanVerb && (hasPlanNoun || hasTravelWord)) {
            return "plan_verb_with_plan_or_travel";
        }
        if (hasDayExpression && (hasTravelWord || hasPlanNoun || hasPlanVerb)) {
            return "day_expression_with_travel_context";
        }
        if (input.contains("怎么玩")) {
            return "how_to_play";
        }
        return null;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd travel_backend && mvn test -pl . -Dtest=IntentAnalyzerTest -Dsurefire.useFile=false 2>&1 | tail -30`
Expected: All 9 tests PASS.

- [ ] **Step 5: Commit**

```bash
cd travel_backend
git add src/main/java/com/lq/travel/util/IntentAnalyzer.java src/test/java/com/lq/travel/util/IntentAnalyzerTest.java
git commit -m "refactor: convert IntentAnalyzer from static util to Spring bean with LLM classification and regex fallback"
```

---

### Task 2: Convert TravelConversationMemory to Spring Bean with LLM Extraction

**Files:**
- Rewrite: `travel_backend/src/main/java/com/lq/travel/util/TravelConversationMemory.java`
- Test: `travel_backend/src/test/java/com/lq/travel/util/TravelConversationMemoryTest.java`

- [ ] **Step 1: Write the test for LLM-based conversation memory extraction**

```java
package com.lq.travel.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lq.travel.model.dto.ai.AIRequest;
import com.lq.travel.model.dto.ai.AIResponse;
import com.lq.travel.model.entity.AIMessage;
import com.lq.travel.service.AIService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TravelConversationMemoryTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TravelConversationMemory createMemory(AIService aiService) {
        return new TravelConversationMemory(aiService);
    }

    private AIService mockAiServiceReturning(String json) {
        AIService aiService = mock(AIService.class);
        AIResponse response = AIResponse.builder().success(true).content(json).build();
        when(aiService.chat(any(AIRequest.class))).thenReturn(response);
        return aiService;
    }

    private static AIMessage userMessage(String content) {
        AIMessage message = new AIMessage();
        message.setRole("user");
        message.setContent(content);
        return message;
    }

    @Test
    void shouldExtractAllFieldsViaLLM() {
        String json = """
                {"destination":"成都","travelDate":"这周末","days":"2天","budget":"2000元","accommodation":"民宿","transport":"高铁","companions":"情侣","isReadyForItinerary":true}
                """;
        AIService aiService = mockAiServiceReturning(json);
        TravelConversationMemory memory = createMemory(aiService);

        TravelConversationMemory.MemorySnapshot snapshot = memory.analyze(List.of(
                userMessage("我想这周末和女朋友去成都玩两天，预算2000，住民宿坐高铁")
        ));

        assertEquals("成都", snapshot.destination());
        assertEquals("这周末", snapshot.travelDate());
        assertEquals("2天", snapshot.days());
        assertEquals("2000元", snapshot.budget());
        assertEquals("民宿", snapshot.accommodation());
        assertEquals("高铁", snapshot.transport());
        assertEquals("情侣", snapshot.companions());
        assertTrue(snapshot.isReadyForItinerary());
    }

    @Test
    void shouldNotBeReadyWhenOnlyDestinationProvided() {
        String json = """
                {"destination":"成都","travelDate":"","days":"","budget":"","accommodation":"","transport":"","companions":"","isReadyForItinerary":false}
                """;
        AIService aiService = mockAiServiceReturning(json);
        TravelConversationMemory memory = createMemory(aiService);

        TravelConversationMemory.MemorySnapshot snapshot = memory.analyze(List.of(
                userMessage("我想去成都")
        ));

        assertEquals("成都", snapshot.destination());
        assertFalse(snapshot.isReadyForItinerary());
    }

    @Test
    void shouldFallbackToRegexWhenLLMFails() {
        AIService aiService = mock(AIService.class);
        when(aiService.chat(any(AIRequest.class))).thenThrow(new RuntimeException("LLM unavailable"));
        TravelConversationMemory memory = createMemory(aiService);

        TravelConversationMemory.MemorySnapshot snapshot = memory.analyze(List.of(
                userMessage("我想去揭阳玩两天"),
                userMessage("4月25号，安静的"),
                userMessage("自驾")
        ));

        assertEquals("揭阳", snapshot.destination());
        assertEquals("4月25号", snapshot.travelDate());
        assertEquals("两天", snapshot.days());
        assertEquals("安静的", snapshot.accommodation());
        assertEquals("自驾", snapshot.transport());
        assertTrue(snapshot.isReadyForItinerary());
    }

    @Test
    void shouldFallbackToRegexWhenLLMReturnsInvalidJson() {
        AIService aiService = mockAiServiceReturning("garbage response");
        TravelConversationMemory memory = createMemory(aiService);

        TravelConversationMemory.MemorySnapshot snapshot = memory.analyze(List.of(
                userMessage("我打算去揭阳"),
                userMessage("明天，3天"),
                userMessage("市区，1000，打车")
        ));

        assertEquals("揭阳", snapshot.destination());
        assertEquals("明天", snapshot.travelDate());
        assertEquals("3天", snapshot.days());
        assertTrue(snapshot.isReadyForItinerary());
    }

    @Test
    void shouldHandleNullMessages() {
        AIService aiService = mockAiServiceReturning("{}");
        TravelConversationMemory memory = createMemory(aiService);

        TravelConversationMemory.MemorySnapshot snapshot = memory.analyze(null);

        assertFalse(snapshot.hasAnySignal());
        assertFalse(snapshot.isReadyForItinerary());
    }

    @Test
    void shouldHandleEmptyMessages() {
        AIService aiService = mockAiServiceReturning("{}");
        TravelConversationMemory memory = createMemory(aiService);

        TravelConversationMemory.MemorySnapshot snapshot = memory.analyze(List.of());

        assertFalse(snapshot.hasAnySignal());
    }

    @Test
    void shouldHandleLLMReturningEmptyJson() {
        AIService aiService = mockAiServiceReturning("{}");
        TravelConversationMemory memory = createMemory(aiService);

        TravelConversationMemory.MemorySnapshot snapshot = memory.analyze(List.of(
                userMessage("你好")
        ));

        assertFalse(snapshot.hasAnySignal());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd travel_backend && mvn test -pl . -Dtest=TravelConversationMemoryTest -Dsurefire.useFile=false 2>&1 | tail -30`
Expected: FAIL — `TravelConversationMemory` is still a static class.

- [ ] **Step 3: Implement the new TravelConversationMemory**

Rewrite `travel_backend/src/main/java/com/lq/travel/util/TravelConversationMemory.java`:

```java
package com.lq.travel.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lq.travel.model.dto.ai.AIRequest;
import com.lq.travel.model.dto.ai.AIResponse;
import com.lq.travel.model.entity.AIMessage;
import com.lq.travel.service.AIService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class TravelConversationMemory {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String EXTRACTION_PROMPT = """
            你是旅行信息提取器。从对话历史中提取用户已确认的旅行信息，返回JSON：
            {"destination":"目的地","travelDate":"出行时间","days":"天数","budget":"预算","accommodation":"住宿偏好","transport":"交通方式","companions":"同行人","isReadyForItinerary":true或false}

            规则：
            - 只提取用户明确说出的信息，不猜测。未确认的字段值为空字符串。
            - isReadyForItinerary为true需要满足：目的地、出行时间、天数已确认。
            - 住宿偏好和交通方式缺失不影响isReadyForItinerary。
            - 只返回JSON，不要其他内容。
            """;

    // --- Regex fallback patterns (unchanged from original) ---
    private static final Pattern DESTINATION_LABEL_PATTERN = Pattern.compile("(?:目的地|城市|地点)\\s*[:：]\\s*([\\p{IsHan}A-Za-z0-9·]{2,20})");
    private static final Pattern DESTINATION_VERB_PATTERN = Pattern.compile("(?:去|到|前往|想去|打算去|计划去)([\\p{IsHan}A-Za-z0-9·]{2,20})");
    private static final Pattern DESTINATION_ONLY_PATTERN = Pattern.compile("^[\\p{IsHan}A-Za-z·]{2,8}$");
    private static final Pattern DAYS_PATTERN = Pattern.compile("([0-9一二三四五六七八九十两]+)\\s*天");
    private static final Pattern DATE_PATTERN = Pattern.compile("(明天|后天|大后天|今天|下周[一二三四五六日天]?|周末|\\d{1,2}月\\d{1,2}[日号]|\\d{4}-\\d{1,2}-\\d{1,2})");
    private static final Pattern BUDGET_PATTERN = Pattern.compile("(?:预算|花费|预算大概|大概|约|每晚|一天|每人|总共)?[^。！？\\n]{0,12}?(?:\\d+\\s*[-~至]\\s*\\d+\\s*元?|\\d+\\s*元|[两三四五六七八九十]\\s*百(?:\\s*元)?|两三百|三四百|四五百)");
    private static final Pattern ACCOMMODATION_PATTERN = Pattern.compile("(民宿|酒店|青旅|客栈|公寓|度假村|别墅)");
    private static final Pattern ACCOMMODATION_PREFERENCE_PATTERN = Pattern.compile("(安静的?|安静点|市区(?:酒店|民宿|附近|便捷|方便)?|市中心|近地铁|景区附近|交通方便|方便的?)");
    private static final Pattern TRANSPORT_PATTERN = Pattern.compile("(打车|地铁|公交|自驾|包车|高铁|飞机|步行|骑行|出租车)");
    private static final Pattern COMPANION_PATTERN = Pattern.compile("(亲子|情侣|家人|朋友|同事|独自|自己|小孩|老人|孩子|闺蜜)");
    private static final Set<String> NON_DESTINATION_PHRASES = Set.of(
            "就这样", "可以", "好的", "好呀", "好哦", "好呢", "行", "行吧", "收到", "明白了", "继续", "下一步"
    );

    private final AIService aiService;

    public TravelConversationMemory(AIService aiService) {
        this.aiService = aiService;
    }

    public MemorySnapshot analyze(List<AIMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return new MemorySnapshot("", "", "", "", "", "", "");
        }

        try {
            return analyzeByLLM(messages);
        } catch (Exception e) {
            log.warn("LLM信息提取失败，回退到正则: {}", e.getMessage());
            return analyzeByRegex(messages);
        }
    }

    private MemorySnapshot analyzeByLLM(List<AIMessage> messages) {
        String conversationText = buildConversationText(messages);

        AIRequest request = AIRequest.builder()
                .message(conversationText)
                .systemPrompt(EXTRACTION_PROMPT)
                .temperature(0.0)
                .maxTokens(300)
                .build();

        AIResponse response = aiService.chat(request);
        if (!response.getSuccess() || response.getContent() == null) {
            throw new RuntimeException("LLM返回失败");
        }

        return parseSnapshotFromJson(response.getContent().trim());
    }

    private String buildConversationText(List<AIMessage> messages) {
        StringBuilder sb = new StringBuilder("对话历史：\n");
        for (AIMessage msg : messages) {
            if (msg != null && StringUtils.hasText(msg.getContent())) {
                sb.append(msg.getRole()).append("：").append(msg.getContent()).append("\n");
            }
        }
        return sb.toString();
    }

    private MemorySnapshot parseSnapshotFromJson(String json) {
        try {
            JsonNode node = OBJECT_MAPPER.readTree(json);
            String destination = getTextField(node, "destination");
            String travelDate = getTextField(node, "travelDate");
            String days = getTextField(node, "days");
            String budget = getTextField(node, "budget");
            String accommodation = getTextField(node, "accommodation");
            String transport = getTextField(node, "transport");
            String companions = getTextField(node, "companions");

            boolean llmReady = node.has("isReadyForItinerary") && node.get("isReadyForItinerary").asBoolean(false);

            return new LLMEnhancedSnapshot(destination, travelDate, days, budget, accommodation, transport, companions, llmReady);
        } catch (Exception e) {
            log.warn("旅行信息JSON解析失败: {}", json);
            throw new RuntimeException("JSON解析失败", e);
        }
    }

    private String getTextField(JsonNode node, String field) {
        return node.has(field) ? node.get(field).asText("") : "";
    }

    // --- Regex fallback (original logic, unchanged) ---
    MemorySnapshot analyzeByRegex(List<AIMessage> messages) {
        String destination = "";
        String travelDate = "";
        String days = "";
        String budget = "";
        String accommodation = "";
        String transport = "";
        String companions = "";

        if (messages != null) {
            for (AIMessage message : messages) {
                if (message == null || !isUserMessage(message.getRole())) {
                    continue;
                }
                String content = message.getContent();
                if (!StringUtils.hasText(content)) {
                    continue;
                }
                String normalizedContent = content.trim();

                String detectedDestination = detectDestination(normalizedContent);
                if (StringUtils.hasText(detectedDestination)) {
                    destination = detectedDestination;
                }
                String detectedDate = detectTravelDate(normalizedContent);
                if (StringUtils.hasText(detectedDate)) {
                    travelDate = detectedDate;
                }
                String detectedDays = detectDays(normalizedContent);
                if (StringUtils.hasText(detectedDays)) {
                    days = detectedDays;
                }
                String detectedBudget = detectBudget(normalizedContent);
                if (StringUtils.hasText(detectedBudget)) {
                    budget = detectedBudget;
                }
                String detectedAccommodation = detectAccommodation(normalizedContent);
                if (StringUtils.hasText(detectedAccommodation)) {
                    accommodation = detectedAccommodation;
                }
                String detectedTransport = detectTransport(normalizedContent);
                if (StringUtils.hasText(detectedTransport)) {
                    transport = detectedTransport;
                }
                String detectedCompanions = detectCompanions(normalizedContent);
                if (StringUtils.hasText(detectedCompanions)) {
                    companions = detectedCompanions;
                }
            }
        }
        return new MemorySnapshot(destination, travelDate, days, budget, accommodation, transport, companions);
    }

    // --- All original regex helper methods preserved below ---
    private static boolean isUserMessage(String role) {
        return "user".equalsIgnoreCase(role);
    }

    private static String detectDestination(String content) {
        Matcher labelMatcher = DESTINATION_LABEL_PATTERN.matcher(content);
        if (labelMatcher.find()) return cleanDestination(labelMatcher.group(1));
        Matcher verbMatcher = DESTINATION_VERB_PATTERN.matcher(content);
        if (verbMatcher.find()) return cleanDestination(verbMatcher.group(1));
        if (looksLikeStandaloneDestination(content)) return cleanDestination(content);
        return "";
    }

    private static boolean looksLikeStandaloneDestination(String content) {
        if (!StringUtils.hasText(content)) return false;
        String normalized = content.trim();
        if (!DESTINATION_ONLY_PATTERN.matcher(normalized).matches()) return false;
        if (NON_DESTINATION_PHRASES.contains(normalized)) return false;
        return !DATE_PATTERN.matcher(normalized).find()
                && !DAYS_PATTERN.matcher(normalized).find()
                && !BUDGET_PATTERN.matcher(normalized).find()
                && !ACCOMMODATION_PATTERN.matcher(normalized).find()
                && !TRANSPORT_PATTERN.matcher(normalized).find()
                && !COMPANION_PATTERN.matcher(normalized).find();
    }

    private static String detectTravelDate(String content) {
        Matcher matcher = DATE_PATTERN.matcher(content);
        if (matcher.find()) return matcher.group(1).trim();
        return "";
    }

    private static String detectDays(String content) {
        Matcher matcher = DAYS_PATTERN.matcher(content);
        if (matcher.find()) return matcher.group(1).trim() + "天";
        return "";
    }

    private static String detectBudget(String content) {
        Matcher matcher = BUDGET_PATTERN.matcher(content);
        if (matcher.find()) return matcher.group().trim();
        return "";
    }

    private static String detectAccommodation(String content) {
        Matcher matcher = ACCOMMODATION_PATTERN.matcher(content);
        if (matcher.find()) return matcher.group(1).trim();
        Matcher preferenceMatcher = ACCOMMODATION_PREFERENCE_PATTERN.matcher(content);
        if (preferenceMatcher.find()) return preferenceMatcher.group(1).trim();
        return "";
    }

    private static String detectTransport(String content) {
        Matcher matcher = TRANSPORT_PATTERN.matcher(content);
        if (matcher.find()) return matcher.group(1).trim();
        return "";
    }

    private static String detectCompanions(String content) {
        Matcher matcher = COMPANION_PATTERN.matcher(content);
        if (matcher.find()) return matcher.group(1).trim();
        return "";
    }

    private static String cleanDestination(String raw) {
        if (!StringUtils.hasText(raw)) return "";
        String normalized = raw.trim();
        normalized = normalized.replaceAll("(?:\\d+|[一二三四五六七八九十两]+)\\s*天.*$", "");
        normalized = normalized.replaceAll("(?:周末|明天|后天|今天|大后天)$", "");
        String[] suffixes = {"怎么玩", "怎么游", "旅游", "旅行", "游玩", "出游", "出行", "攻略", "计划", "方案", "玩", "游", "吧", "啊", "呀", "呢", "哦", "咯", "啦"};
        for (String suffix : suffixes) {
            if (normalized.endsWith(suffix) && normalized.length() > suffix.length()) {
                normalized = normalized.substring(0, normalized.length() - suffix.length()).trim();
                break;
            }
        }
        String lower = normalized.toLowerCase();
        List<String> stopWords = List.of("旅游", "旅行", "行程", "攻略", "计划", "方案");
        if (stopWords.contains(lower)) return "";
        return normalized;
    }

    // --- Inner classes ---

    /**
     * Extended snapshot that uses LLM-provided readiness check.
     * Falls back to original 5-field check if LLM didn't provide isReadyForItinerary.
     */
    static class LLMEnhancedSnapshot extends MemorySnapshot {
        private final boolean llmReady;

        LLMEnhancedSnapshot(String destination, String travelDate, String days,
                            String budget, String accommodation, String transport,
                            String companions, boolean llmReady) {
            super(destination, travelDate, days, budget, accommodation, transport, companions);
            this.llmReady = llmReady;
        }

        @Override
        public boolean isReadyForItinerary() {
            if (llmReady) return true;
            return super.isReadyForItinerary();
        }
    }

    public static class MemorySnapshot {
        private final String destination;
        private final String travelDate;
        private final String days;
        private final String budget;
        private final String accommodation;
        private final String transport;
        private final String companions;

        public MemorySnapshot(String destination, String travelDate, String days,
                              String budget, String accommodation, String transport,
                              String companions) {
            this.destination = destination;
            this.travelDate = travelDate;
            this.days = days;
            this.budget = budget;
            this.accommodation = accommodation;
            this.transport = transport;
            this.companions = companions;
        }

        public String destination() { return destination; }
        public String travelDate() { return travelDate; }
        public String days() { return days; }
        public String budget() { return budget; }
        public String accommodation() { return accommodation; }
        public String transport() { return transport; }
        public String companions() { return companions; }

        public boolean hasAnySignal() {
            return StringUtils.hasText(destination)
                    || StringUtils.hasText(travelDate)
                    || StringUtils.hasText(days)
                    || StringUtils.hasText(budget)
                    || StringUtils.hasText(accommodation)
                    || StringUtils.hasText(transport)
                    || StringUtils.hasText(companions);
        }

        public boolean isReadyForItinerary() {
            return StringUtils.hasText(destination)
                    && StringUtils.hasText(travelDate)
                    && StringUtils.hasText(days)
                    && StringUtils.hasText(accommodation)
                    && StringUtils.hasText(transport);
        }

        public List<String> getMissingFields() {
            List<String> missing = new ArrayList<>();
            if (!StringUtils.hasText(destination)) missing.add("目的地");
            if (!StringUtils.hasText(travelDate)) missing.add("出行时间");
            if (!StringUtils.hasText(days)) missing.add("游玩天数");
            if (!StringUtils.hasText(accommodation)) missing.add("住宿偏好");
            if (!StringUtils.hasText(transport)) missing.add("通勤方式");
            return missing;
        }

        public String toPromptBlock() {
            if (!hasAnySignal()) return "";
            StringBuilder builder = new StringBuilder();
            builder.append("【会话记忆】\n");
            appendField(builder, "目的地", destination);
            appendField(builder, "出行时间", travelDate);
            appendField(builder, "游玩天数", days);
            appendField(builder, "住宿偏好", accommodation);
            appendField(builder, "预算", budget);
            appendField(builder, "通勤方式", transport);
            appendField(builder, "同行人群", companions);
            List<String> missingFields = getMissingFields();
            if (!missingFields.isEmpty()) {
                builder.append("【尚未确认】\n");
                for (String field : missingFields) {
                    builder.append("- ").append(field).append("\n");
                }
            }
            builder.append("【记忆规则】\n")
                    .append("- 上述已确认信息优先级高于单轮输入，除非用户明确更改，不要重复追问。\n")
                    .append("- 仅追问【尚未确认】中的内容，每轮只问 1-2 个最关键问题。\n");
            if (isReadyForItinerary()) {
                builder.append("- 若已满足行程生成条件，直接切换到行程规划，不要继续闲聊。\n");
            }
            return builder.toString().trim();
        }

        private void appendField(StringBuilder builder, String label, String value) {
            if (StringUtils.hasText(value)) {
                builder.append("- ").append(label).append("：").append(value.trim()).append("\n");
            }
        }
    }
}
```

Note: The `MemorySnapshot` changes from a `record` to a regular `class` with accessor methods (same names: `destination()`, `travelDate()`, etc.) so call sites don't break. The `LLMEnhancedSnapshot` subclass overrides `isReadyForItinerary()` to use the LLM's readiness verdict.

- [ ] **Step 4: Run test to verify it passes**

Run: `cd travel_backend && mvn test -pl . -Dtest=TravelConversationMemoryTest -Dsurefire.useFile=false 2>&1 | tail -30`
Expected: All 7 tests PASS.

- [ ] **Step 5: Commit**

```bash
cd travel_backend
git add src/main/java/com/lq/travel/util/TravelConversationMemory.java src/test/java/com/lq/travel/util/TravelConversationMemoryTest.java
git commit -m "refactor: convert TravelConversationMemory from static util to Spring bean with LLM extraction and regex fallback"
```

---

### Task 3: Convert IntentAwareChatEnhancer to Spring Bean

**Files:**
- Modify: `travel_backend/src/main/java/com/lq/travel/util/IntentAwareChatEnhancer.java`

- [ ] **Step 1: Rewrite IntentAwareChatEnhancer as a Spring bean**

Replace the entire file at `travel_backend/src/main/java/com/lq/travel/util/IntentAwareChatEnhancer.java`:

```java
package com.lq.travel.util;

import com.lq.travel.model.dto.ai.AIRequest;
import com.lq.travel.model.dto.ai.AIResponse;
import com.lq.travel.model.enums.IntentType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
public class IntentAwareChatEnhancer {

    private final IntentAnalyzer intentAnalyzer;

    public IntentAwareChatEnhancer(IntentAnalyzer intentAnalyzer) {
        this.intentAnalyzer = intentAnalyzer;
    }

    public IntentType resolveIntent(AIRequest request) {
        if (request == null) {
            return IntentType.GENERAL_CHAT;
        }
        return intentAnalyzer.analyze(request.getMessage());
    }

    public void applySystemPromptIfMissing(AIRequest request, IntentType intent) {
        if (request == null) {
            return;
        }
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
            return;
        }
        request.setSystemPrompt(TravelIntentPromptFactory.byIntent(intent));
    }

    public void enrichResponseMetadata(AIResponse response, IntentType intent) {
        if (response == null) {
            return;
        }

        Map<String, Object> metadata = response.getMetadata();
        if (metadata == null) {
            metadata = new HashMap<>();
            response.setMetadata(metadata);
        }

        IntentType safeIntent = intent == null ? IntentType.GENERAL_CHAT : intent;
        metadata.put("intentType", safeIntent.name());
        metadata.put("intentDescription", safeIntent.getDescription());

        if (!Boolean.TRUE.equals(response.getSuccess())) {
            metadata.put("structuredItineraryAvailable", false);
            return;
        }

        if (safeIntent != IntentType.ITINERARY_GENERATION && safeIntent != IntentType.GENERAL_CHAT) {
            metadata.put("structuredItineraryAvailable", false);
            return;
        }

        Optional<String> normalizedJson = StructuredItineraryExtractor.extractAndNormalize(response.getContent());
        metadata.put("structuredItineraryAvailable", normalizedJson.isPresent());
        if (normalizedJson.isPresent()) {
            metadata.put("structuredItineraryJson", normalizedJson.get());
        }

        if (normalizedJson.isPresent()) {
            log.info("同步聊天已附加结构化行程JSON，长度: {}, 意图: {}", normalizedJson.get().length(), safeIntent);
        }
    }
}
```

Changes: removed `final` class, `private` constructor, and all `static` keywords. Injected `IntentAnalyzer` via constructor. All method signatures unchanged (just removed `static`).

- [ ] **Step 2: Update AIController to inject beans instead of static calls**

In `travel_backend/src/main/java/com/lq/travel/controller/AIController.java`, make these changes:

Add field injections (after existing `@Resource` fields around line 60):

```java
@Resource
private IntentAwareChatEnhancer intentAwareChatEnhancer;

@Resource
private TravelConversationMemory travelConversationMemory;
```

Remove the static import (line 38):
```java
// REMOVE this line:
import com.lq.travel.util.TravelConversationMemory;
```
(Keep `import com.lq.travel.util.IntentAwareChatEnhancer;` — it's still needed.)

Replace all 3 static calls in the `chat()` method (lines 89-94):

```java
        // BEFORE:
        IntentType intent = IntentAwareChatEnhancer.resolveIntent(request);
        IntentAwareChatEnhancer.applySystemPromptIfMissing(request, intent);
        ...
        IntentAwareChatEnhancer.enrichResponseMetadata(response, intent);

        // AFTER:
        IntentType intent = intentAwareChatEnhancer.resolveIntent(request);
        intentAwareChatEnhancer.applySystemPromptIfMissing(request, intent);
        ...
        intentAwareChatEnhancer.enrichResponseMetadata(response, intent);
```

Replace the static call in `tourismStream()` (line 193):

```java
        // BEFORE:
        TravelConversationMemory.MemorySnapshot memorySnapshot = TravelConversationMemory.analyze(conversationMessages);

        // AFTER:
        TravelConversationMemory.MemorySnapshot memorySnapshot = travelConversationMemory.analyze(conversationMessages);
```

- [ ] **Step 3: Update GenericTravelAgent constructor to accept IntentAnalyzer**

In `travel_backend/src/main/java/com/lq/travel/agent/impl/GenericTravelAgent.java`:

Add `IntentAnalyzer` as a constructor parameter and field:

```java
import com.lq.travel.util.IntentAnalyzer;

public class GenericTravelAgent extends BaseAgent {
    // ... existing fields ...
    private final IntentAnalyzer intentAnalyzer;

    public GenericTravelAgent(String name,
                              AIService aiService,
                              TravelRagService travelRagService,
                              TravelMultiAgentCoordinator multiAgentCoordinator,
                              IntentAnalyzer intentAnalyzer) {
        super(name, "通用旅行智能助手 - 提供多目的地旅行规划和咨询", aiService);
        this.travelRagService = travelRagService;
        this.multiAgentCoordinator = multiAgentCoordinator;
        this.intentAnalyzer = intentAnalyzer;
    }
```

Replace the static call in `resolveIntent()` (line 162):

```java
    // BEFORE:
    IntentType intent = IntentAnalyzer.analyze(request.getTask());

    // AFTER:
    IntentType intent = intentAnalyzer.analyze(request.getTask());
```

Remove the static import:
```java
// REMOVE:
import com.lq.travel.util.IntentAnalyzer;
```
(It's still imported via the field declaration in the constructor parameter.)

- [ ] **Step 4: Update AIConfiguration to pass IntentAnalyzer bean**

In `travel_backend/src/main/java/com/lq/travel/config/AIConfiguration.java`:

Add the `IntentAnalyzer` injection:

```java
@Autowired
private IntentAnalyzer intentAnalyzer;
```

Add import:
```java
import com.lq.travel.util.IntentAnalyzer;
```

Update the constructor call in `registerDefaultAgents()` (lines 57-62):

```java
    GenericTravelAgent genericTravelAgent = new GenericTravelAgent(
            "generic-travel",
            aiService,
            travelRagService,
            travelMultiAgentCoordinator,
            intentAnalyzer
    );
```

- [ ] **Step 5: Build and run all tests**

Run: `cd travel_backend && mvn test -Dsurefire.useFile=false 2>&1 | tail -40`
Expected: All tests PASS. Pay attention to `IntentAwareChatEnhancerTest` — if it calls static methods, it may need updating too. Check output for compilation errors first.

- [ ] **Step 6: Commit**

```bash
cd travel_backend
git add src/main/java/com/lq/travel/util/IntentAwareChatEnhancer.java \
        src/main/java/com/lq/travel/controller/AIController.java \
        src/main/java/com/lq/travel/agent/impl/GenericTravelAgent.java \
        src/main/java/com/lq/travel/config/AIConfiguration.java
git commit -m "refactor: adapt all callers from static calls to injected beans for IntentAnalyzer and TravelConversationMemory"
```

---

### Task 4: Fix Any Remaining Compilation and Test Issues

**Files:**
- Check: `travel_backend/src/test/java/com/lq/travel/util/IntentAwareChatEnhancerTest.java`

- [ ] **Step 1: Check IntentAwareChatEnhancerTest**

Read `travel_backend/src/test/java/com/lq/travel/util/IntentAwareChatEnhancerTest.java`. If it calls `IntentAwareChatEnhancer.resolveIntent(...)` as a static method, it must be updated to create an instance with a mocked `IntentAnalyzer`.

- [ ] **Step 2: Fix if needed and run full test suite**

Run: `cd travel_backend && mvn compile test -Dsurefire.useFile=false 2>&1 | tail -50`
Expected: BUILD SUCCESS, all tests PASS.

- [ ] **Step 3: Commit if changes were made**

```bash
cd travel_backend
git add -A
git commit -m "fix: update IntentAwareChatEnhancerTest for bean injection"
```

---

## Self-Review Checklist

- [x] **Spec coverage:** IntentAnalyzer LLM conversion → Task 1. TravelConversationMemory LLM conversion → Task 2. Caller adaptation → Task 3. Test fixes → Task 4.
- [x] **Placeholder scan:** No TBD/TODO. All code blocks contain actual implementation code.
- [x] **Type consistency:** `MemorySnapshot` changes from `record` to `class` with same accessor method names (`destination()`, `isReadyForItinerary()`, etc.). `LLMEnhancedSnapshot` extends it and overrides `isReadyForItinerary()`. All callers use the same method signatures.
