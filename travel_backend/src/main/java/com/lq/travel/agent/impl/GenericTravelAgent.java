package com.lq.travel.agent.impl;

import com.lq.travel.agent.BaseAgent;
import com.lq.travel.callback.EnhancedStreamCallbackAdapter;
import com.lq.travel.callback.StreamCallback;
import com.lq.travel.model.dto.ai.AIRequest;
import com.lq.travel.model.dto.ai.AIResponse;
import com.lq.travel.model.dto.ai.AgentRequest;
import com.lq.travel.model.dto.ai.AgentResponse;
import com.lq.travel.model.enums.IntentType;
import com.lq.travel.service.AIService;
import com.lq.travel.service.TravelRagService;
import com.lq.travel.service.impl.TravelMultiAgentCoordinator;
import com.lq.travel.util.IntentAnalyzer;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 通用旅行智能助手
 * 面向多目的地场景提供行程规划和旅游咨询服务
 */
@Slf4j
public class GenericTravelAgent extends BaseAgent {

    private static final String ERROR_ACTION = "error";
    private final TravelRagService travelRagService;
    private final TravelMultiAgentCoordinator multiAgentCoordinator;

    public GenericTravelAgent(String name,
                              AIService aiService,
                              TravelRagService travelRagService,
                              TravelMultiAgentCoordinator multiAgentCoordinator) {
        super(name, "通用旅行智能助手 - 提供多目的地旅行规划和咨询", aiService);
        this.travelRagService = travelRagService;
        this.multiAgentCoordinator = multiAgentCoordinator;
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
            String destinationHint = multiAgentCoordinator.inferDestinationHint(request);
            String ragContext = travelRagService.buildRagContext(request.getTask(), destinationHint, intent);
            String enhancedPrompt = multiAgentCoordinator.buildCoordinatedPrompt(request, intent, ragContext, destinationHint);

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
            String destinationHint = multiAgentCoordinator.inferDestinationHint(request);
            String ragContext = travelRagService.buildRagContext(request.getTask(), destinationHint, IntentType.GENERAL_CHAT);

            AIRequest aiRequest = AIRequest.builder()
                .message(multiAgentCoordinator.buildCoordinatedPrompt(
                    request,
                    IntentType.GENERAL_CHAT,
                    ragContext,
                    destinationHint
                ))
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
                    - location.longitude: number (景点的经度数值)
                    - location.latitude: number (景点的纬度数值)
                    - estimatedCost: number
                    - tips: string[]
                - totalEstimatedCost: number
                - tips: string[]

                约束：
                - dailyPlans 的天数必须与 days 一致。
                - 每天 activities 至少 1 条。
                - 信息不足时，也要先生成可执行草案，并在 tips 中标注待确认项。
                - JSON 必须是合法 JSON：所有 key/字符串使用双引号，不允许注释，不允许尾随逗号。
                - 整个回复中只允许出现一个 JSON 对象，不要再输出第二个 JSON。
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
}
