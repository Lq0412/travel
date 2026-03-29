package com.lq.travel.AI.core.agent.impl;

import com.lq.travel.AI.core.agent.BaseAgent;
import com.lq.travel.AI.core.callback.EnhancedStreamCallbackAdapter;
import com.lq.travel.AI.core.interfaces.StreamCallback;
import com.lq.travel.AI.core.model.AIRequest;
import com.lq.travel.AI.core.model.AIResponse;
import com.lq.travel.AI.core.model.AgentRequest;
import com.lq.travel.AI.core.model.AgentResponse;
import com.lq.travel.AI.core.model.IntentType;
import com.lq.travel.AI.core.service.AIService;
import com.lq.travel.AI.core.util.IntentAnalyzer;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 通用旅行智能助手
 * 面向多目的地场景提供行程规划和旅游咨询服务
 */
@Slf4j
public class GenericTravelAgent extends BaseAgent {

    private static final String ERROR_ACTION = "error";

    public GenericTravelAgent(String name, AIService aiService) {
        super(name, "通用旅行智能助手 - 提供多目的地旅行规划和咨询", aiService);
    }

    @Override
    public void executeStream(AgentRequest request, StreamCallback callback) {
        executeStream(request, null, callback);
    }

    @Override
    public void executeStream(AgentRequest request, Long conversationId, StreamCallback callback) {
        executeStreamWithIntent(request, conversationId, callback);
    }

    /**
     * 根据用户意图进行流式执行
     */
    public void executeStreamWithIntent(AgentRequest request, Long conversationId, StreamCallback callback) {
        try {
            IntentType intent = IntentAnalyzer.analyze(request.getTask());
            log.info("通用旅行代理意图识别: {} - {}", intent, intent.getDescription());

            String systemPrompt = getSystemPromptByIntent(intent);
            String enhancedPrompt = buildEnhancedPrompt(request, intent);

            AIRequest aiRequest = AIRequest.builder()
                    .message(enhancedPrompt)
                    .systemPrompt(systemPrompt)
                    .temperature(0.7)
                    .maxTokens(2000)
                    .stream(true)
                    .build();

            EnhancedStreamCallbackAdapter adapter = new EnhancedStreamCallbackAdapter(callback, intent);

            if (conversationId != null) {
                aiService.chatStream(aiRequest, conversationId, adapter);
            } else {
                aiService.chatStream(aiRequest, adapter);
            }
        } catch (Exception e) {
            log.error("通用旅行代理流式执行失败", e);
            callback.onError(e);
        }
    }

    @Override
    protected AgentResponse.Step executeStep(int stepNumber, AgentRequest request, List<AgentResponse.Step> previousSteps) {
        try {
            AIRequest aiRequest = AIRequest.builder()
                    .message(buildEnhancedPrompt(request, IntentType.GENERAL_CHAT))
                    .systemPrompt(getSystemPrompt())
                    .temperature(0.6)
                    .maxTokens(1200)
                    .build();

            AIResponse response = aiService.chat(aiRequest);
            if (!response.getSuccess()) {
                return AgentResponse.Step.builder()
                        .stepNumber(stepNumber)
                        .action(ERROR_ACTION)
                        .observation(response.getErrorMessage())
                        .reasoning("模型调用失败")
                        .timestamp(System.currentTimeMillis())
                        .build();
            }

            return AgentResponse.Step.builder()
                    .stepNumber(stepNumber)
                    .action("travel_assist")
                    .observation(response.getContent())
                    .reasoning("已按通用旅行助手策略生成响应")
                    .timestamp(System.currentTimeMillis())
                    .build();
        } catch (Exception e) {
            return AgentResponse.Step.builder()
                    .stepNumber(stepNumber)
                    .action(ERROR_ACTION)
                    .observation("执行失败: " + e.getMessage())
                    .reasoning("异常中断")
                    .timestamp(System.currentTimeMillis())
                    .build();
        }
    }

    @Override
    protected boolean isTaskCompleted(AgentResponse.Step currentStep, AgentRequest request) {
        return true;
    }

    @Override
    protected String getSystemPrompt() {
        return """
            你是一个专业的旅行工作台助手，负责帮助用户完成旅行需求澄清、景点咨询和行程建议。

            输出规范：
            - 保持回复清晰、准确、可执行
            - 不要编造未确认的用户约束
            - 若用户提供信息不足，先追问关键字段（目的地、天数、预算、出行偏好）
            - 使用以下三段格式输出：
              思考: [你的分析]
              行动: [你的行动计划]
              观察: [给用户可直接理解和执行的内容]
            """;
    }

    @Override
    public String[] getCapabilities() {
        return new String[]{"多目的地行程规划", "景点咨询", "预算建议", "路线建议"};
    }

    private String getSystemPromptByIntent(IntentType intent) {
        return switch (intent) {
            case ITINERARY_GENERATION -> """
                你是专业的旅行规划师。

                当用户请求行程规划时，必须严格按以下格式输出：
                1. 先输出一段用户可读文案，必须以“观察:”开头。
                2. 紧接着输出一个 ```json 代码块，且只包含一个 JSON 对象。
                3. 不要输出“思考:”和“行动:”。

                JSON 字段必须完整且可直接给前端渲染：
                - destination: string
                - days: number
                - budget: number
                - theme: string
                - dailyPlans: array
                  - day: number
                  - date: string
                  - activities: array
                    - time: 仅允许 morning/noon/evening
                    - name: string
                    - type: 仅允许 attraction/transport/rest/meal
                    - description: string
                    - imageUrl: string
                    - location.address: string
                    - estimatedCost: number
                    - tips: string[]
                - totalEstimatedCost: number
                - tips: string[]

                约束：
                - dailyPlans 的天数必须与 days 一致。
                - 每天 activities 至少 1 条。
                - 信息不足时，也要先生成可执行草案，并在 tips 中标注待确认项。
                """;
            case ATTRACTION_QUERY -> """
                你是专业的旅行景点助手，擅长介绍各地景点信息。
                请提供详细、准确的景点介绍，包括：
                - 景点特色和亮点
                - 开放时间和门票价格
                - 交通方式和停车信息
                - 游玩建议和注意事项
                - 周边配套设施

                回答格式：观察: [你的详细介绍]
                """;
            case GENERAL_CHAT -> getSystemPrompt();
        };
    }

    private String buildEnhancedPrompt(AgentRequest request, IntentType intent) {
        StringBuilder prompt = new StringBuilder();

        if (request.getContext() != null && !request.getContext().isEmpty()) {
            prompt.append("【上下文信息】\n").append(request.getContext()).append("\n\n");
        }

        prompt.append("【用户需求】\n").append(request.getTask()).append("\n\n");

        if (intent == IntentType.ITINERARY_GENERATION) {
            prompt.append("【规划要求】\n");
            prompt.append("请为用户生成详细旅行行程，包括：\n");
            prompt.append("1. 每日按 morning/noon/evening 三个时段安排\n");
            prompt.append("2. 景点、餐饮、住宿推荐\n");
            prompt.append("3. 预算估算（门票、餐饮、住宿）\n");
            prompt.append("4. 实用旅游建议和注意事项\n");
            prompt.append("5. 结构化的JSON数据（必须包含）\n");
            prompt.append("6. JSON字段必须与前端结构保持一致，便于直接渲染和保存\n\n");
        }

        if (request.getConstraints() != null && !request.getConstraints().isEmpty()) {
            prompt.append("【约束条件】\n").append(request.getConstraints()).append("\n");
        }

        return prompt.toString();
    }
}
