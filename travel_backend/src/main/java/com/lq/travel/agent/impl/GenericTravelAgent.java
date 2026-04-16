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
import com.lq.travel.util.TravelIntentPromptFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 通用旅行智能助手
 * 面向多目的地场景提供行程规划和旅游咨询服务
 */
@Slf4j
public class GenericTravelAgent extends BaseAgent {

    private static final String ERROR_ACTION = "error";
    private static final Pattern TRAVEL_FOLLOW_UP_PATTERN = Pattern.compile(
            "(\\d+\\s*天|[一二三四五六七八九十两]+\\s*天|几天|预算|两三百|两百|三百|元|块|民宿|酒店|住宿|市区|市中心|景区附近|附近|位置|打车|地铁|公交|自驾|包车|高铁|飞机|明天|后天|周末|行程|规划|旅游|旅行|景点|路线|出行|出发|美食|拍照|打卡|夜景|夜市|餐厅|天气|穿搭)"
    );
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
            IntentType intent = resolveIntent(request);
            log.info("通用旅行代理意图识别: {} - {}", intent, intent.getDescription());

            String systemPrompt = getSystemPromptByIntent(intent);
            String destinationHint = multiAgentCoordinator.inferDestinationHint(request);
            String ragContext = travelRagService.buildRagContext(request.getTask(), destinationHint, intent);
            String enhancedPrompt = multiAgentCoordinator.buildCoordinatedPrompt(request, intent, ragContext, destinationHint);

            // 把所有的规则和 RAG 上下文拼接到系统提示词中，保持 message 为纯净的用户输入
            String combinedSystemPrompt = systemPrompt + "\n\n" + enhancedPrompt;

            AIRequest aiRequest = AIRequest.builder()
                    .message(StringUtils.hasText(request.getTask()) ? request.getTask() : "你好")
                    .systemPrompt(combinedSystemPrompt)
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
            String enhancedPrompt = multiAgentCoordinator.buildCoordinatedPrompt(
                request,
                IntentType.GENERAL_CHAT,
                ragContext,
                destinationHint
            );
            
            String combinedSystemPrompt = getSystemPrompt() + "\n\n" + enhancedPrompt;

            AIRequest aiRequest = AIRequest.builder()
                .message(StringUtils.hasText(request.getTask()) ? request.getTask() : "你好")
                .systemPrompt(combinedSystemPrompt)
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
        return TravelIntentPromptFactory.generalPrompt();
    }

    @Override
    public String[] getCapabilities() {
        return new String[]{"多目的地行程规划", "景点咨询", "预算建议", "路线建议"};
    }

    private String getSystemPromptByIntent(IntentType intent) {
        return TravelIntentPromptFactory.byIntent(intent);
    }

    private IntentType resolveIntent(AgentRequest request) {
        IntentType intent = IntentAnalyzer.analyze(request.getTask());
        if (intent != IntentType.GENERAL_CHAT) {
            return intent;
        }

        if (isConversationReady(request) && isTravelFollowUp(request.getTask())) {
            log.info("会话记忆已满足行程生成条件，自动升级为行程规划意图: {}", request.getTask());
            return IntentType.ITINERARY_GENERATION;
        }

        return intent;
    }

    private boolean isConversationReady(AgentRequest request) {
        if (request == null || request.getParameters() == null) {
            return false;
        }

        Object ready = request.getParameters().get("conversationReady");
        if (ready instanceof Boolean booleanReady) {
            return booleanReady;
        }

        return ready != null && Boolean.parseBoolean(String.valueOf(ready));
    }

    private boolean isTravelFollowUp(String task) {
        if (!StringUtils.hasText(task)) {
            return false;
        }
        return TRAVEL_FOLLOW_UP_PATTERN.matcher(task).find();
    }
}
