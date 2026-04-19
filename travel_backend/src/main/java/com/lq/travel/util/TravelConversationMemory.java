package com.lq.travel.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lq.travel.model.dto.ai.AIRequest;
import com.lq.travel.model.dto.ai.AIResponse;
import com.lq.travel.model.entity.AIMessage;
import com.lq.travel.service.AIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从对话历史中提取旅行规划记忆摘要。
 * 用于把已确认信息注入后续轮次，避免模型重复追问。
 * 先尝试 LLM 提取，失败时回退到正则表达式。
 */
@Component
public class TravelConversationMemory {

    private static final Logger log = LoggerFactory.getLogger(TravelConversationMemory.class);

    private static final String EXTRACTION_SYSTEM_PROMPT = """
            你是旅行信息提取器。从对话历史中提取用户已确认的旅行信息，返回JSON：
            {"destination":"目的地","travelDate":"出行时间","days":"天数","budget":"预算","accommodation":"住宿偏好","transport":"交通方式","companions":"同行人","isReadyForItinerary":true或false}

            规则：
            - 只从user角色的消息中提取信息，忽略assistant角色的消息和系统提供的上下文信息。
            - 只提取用户自己明确说出的个人旅行计划，不提取推荐、咨询、美食等话题中提到的地名。
            - 例如"请推荐成都美食"不算用户要去成都，isReadyForItinerary应为false。
            - 未确认的字段值为空字符串。
            - isReadyForItinerary为true需要满足：用户明确表达了要出行，且目的地、出行时间、天数已确认。
            - 只返回JSON，不要其他内容。
            """;

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

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AIService aiService;

    public TravelConversationMemory(AIService aiService) {
        this.aiService = aiService;
    }

    /**
     * Instance method: tries LLM extraction first, falls back to regex.
     */
    public MemorySnapshot analyze(List<AIMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return new MemorySnapshot("", "", "", "", "", "", "");
        }

        // Try LLM extraction first
        try {
            MemorySnapshot llmResult = extractViaLLM(messages);
            if (llmResult != null) {
                return llmResult;
            }
        } catch (Exception e) {
            log.debug("LLM extraction failed, falling back to regex: {}", e.getMessage());
        }

        // Fallback to regex
        return extractViaRegex(messages);
    }

    private MemorySnapshot extractViaLLM(List<AIMessage> messages) {
        String conversationText = buildConversationText(messages);
        if (!StringUtils.hasText(conversationText)) {
            return null;
        }

        AIRequest request = AIRequest.builder()
                .message(conversationText)
                .systemPrompt(EXTRACTION_SYSTEM_PROMPT)
                .temperature(0.0)
                .maxTokens(300)
                .build();

        AIResponse response = aiService.chat(request);

        if (response == null || !Boolean.TRUE.equals(response.getSuccess()) || !StringUtils.hasText(response.getContent())) {
            return null;
        }

        return parseLLMResponse(response.getContent());
    }

    private MemorySnapshot parseLLMResponse(String content) {
        try {
            // Extract JSON from the response (may have markdown code blocks)
            String json = content.trim();
            if (!json.startsWith("{")) {
                int start = json.indexOf('{');
                int end = json.lastIndexOf('}');
                if (start >= 0 && end > start) {
                    json = json.substring(start, end + 1);
                }
            }

            JsonNode node = OBJECT_MAPPER.readTree(json);

            String destination = getTextValue(node, "destination");
            String travelDate = getTextValue(node, "travelDate");
            String days = getTextValue(node, "days");
            String budget = getTextValue(node, "budget");
            String accommodation = getTextValue(node, "accommodation");
            String transport = getTextValue(node, "transport");
            String companions = getTextValue(node, "companions");
            boolean llmReady = node.has("isReadyForItinerary") && node.get("isReadyForItinerary").asBoolean(false);

            return new LLMEnhancedSnapshot(destination, travelDate, days, budget, accommodation, transport, companions, llmReady);
        } catch (Exception e) {
            log.debug("Failed to parse LLM response as JSON: {}", e.getMessage());
            return null;
        }
    }

    private String getTextValue(JsonNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            String value = node.get(field).asText("");
            return StringUtils.hasText(value) ? value : "";
        }
        return "";
    }

    private String buildConversationText(List<AIMessage> messages) {
        StringBuilder sb = new StringBuilder();
        for (AIMessage message : messages) {
            if (message == null || !StringUtils.hasText(message.getContent())) {
                continue;
            }
            String role = message.getRole();
            if ("user".equalsIgnoreCase(role)) {
                sb.append("用户：").append(message.getContent().trim()).append("\n");
            } else if ("assistant".equalsIgnoreCase(role)) {
                sb.append("助手：").append(message.getContent().trim()).append("\n");
            }
        }
        return sb.toString().trim();
    }

    // ==================== Regex-based extraction (static, used as fallback) ====================

    private static MemorySnapshot extractViaRegex(List<AIMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return new MemorySnapshot("", "", "", "", "", "", "");
        }

        String destination = "";
        String travelDate = "";
        String days = "";
        String budget = "";
        String accommodation = "";
        String transport = "";
        String companions = "";

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

        return new MemorySnapshot(destination, travelDate, days, budget, accommodation, transport, companions);
    }

    private static boolean isUserMessage(String role) {
        return "user".equalsIgnoreCase(role);
    }

    private static String detectDestination(String content) {
        Matcher labelMatcher = DESTINATION_LABEL_PATTERN.matcher(content);
        if (labelMatcher.find()) {
            return cleanDestination(labelMatcher.group(1));
        }

        Matcher verbMatcher = DESTINATION_VERB_PATTERN.matcher(content);
        if (verbMatcher.find()) {
            return cleanDestination(verbMatcher.group(1));
        }

        if (looksLikeStandaloneDestination(content)) {
            return cleanDestination(content);
        }

        return "";
    }

    private static boolean looksLikeStandaloneDestination(String content) {
        if (!StringUtils.hasText(content)) {
            return false;
        }

        String normalized = content.trim();
        if (!DESTINATION_ONLY_PATTERN.matcher(normalized).matches()) {
            return false;
        }

        if (NON_DESTINATION_PHRASES.contains(normalized)) {
            return false;
        }

        return !DATE_PATTERN.matcher(normalized).find()
                && !DAYS_PATTERN.matcher(normalized).find()
                && !BUDGET_PATTERN.matcher(normalized).find()
                && !ACCOMMODATION_PATTERN.matcher(normalized).find()
                && !TRANSPORT_PATTERN.matcher(normalized).find()
                && !COMPANION_PATTERN.matcher(normalized).find();
    }

    private static String detectTravelDate(String content) {
        Matcher matcher = DATE_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    private static String detectDays(String content) {
        Matcher matcher = DAYS_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim() + "天";
        }
        return "";
    }

    private static String detectBudget(String content) {
        Matcher matcher = BUDGET_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group().trim();
        }
        return "";
    }

    private static String detectAccommodation(String content) {
        Matcher matcher = ACCOMMODATION_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        Matcher preferenceMatcher = ACCOMMODATION_PREFERENCE_PATTERN.matcher(content);
        if (preferenceMatcher.find()) {
            return preferenceMatcher.group(1).trim();
        }
        return "";
    }

    private static String detectTransport(String content) {
        Matcher matcher = TRANSPORT_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    private static String detectCompanions(String content) {
        Matcher matcher = COMPANION_PATTERN.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

    private static String cleanDestination(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }

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
        if (stopWords.contains(lower)) {
            return "";
        }

        return normalized;
    }

    // ==================== MemorySnapshot (converted from record to class) ====================

    public static class MemorySnapshot {
        private final String destination;
        private final String travelDate;
        private final String days;
        private final String budget;
        private final String accommodation;
        private final String transport;
        private final String companions;

        public MemorySnapshot(String destination, String travelDate, String days,
                              String budget, String accommodation, String transport, String companions) {
            this.destination = destination;
            this.travelDate = travelDate;
            this.days = days;
            this.budget = budget;
            this.accommodation = accommodation;
            this.transport = transport;
            this.companions = companions;
        }

        public String destination() {
            return destination;
        }

        public String travelDate() {
            return travelDate;
        }

        public String days() {
            return days;
        }

        public String budget() {
            return budget;
        }

        public String accommodation() {
            return accommodation;
        }

        public String transport() {
            return transport;
        }

        public String companions() {
            return companions;
        }

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
            if (!StringUtils.hasText(destination)) {
                missing.add("目的地");
            }
            if (!StringUtils.hasText(travelDate)) {
                missing.add("出行时间");
            }
            if (!StringUtils.hasText(days)) {
                missing.add("游玩天数");
            }
            if (!StringUtils.hasText(accommodation)) {
                missing.add("住宿偏好");
            }
            if (!StringUtils.hasText(transport)) {
                missing.add("通勤方式");
            }
            return missing;
        }

        public String toPromptBlock() {
            if (!hasAnySignal()) {
                return "";
            }

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

    // ==================== LLM-enhanced snapshot ====================

    public static class LLMEnhancedSnapshot extends MemorySnapshot {
        private final boolean llmReadyForItinerary;

        public LLMEnhancedSnapshot(String destination, String travelDate, String days,
                                   String budget, String accommodation, String transport,
                                   String companions, boolean llmReadyForItinerary) {
            super(destination, travelDate, days, budget, accommodation, transport, companions);
            this.llmReadyForItinerary = llmReadyForItinerary;
        }

        @Override
        public boolean isReadyForItinerary() {
            return llmReadyForItinerary;
        }
    }
}
