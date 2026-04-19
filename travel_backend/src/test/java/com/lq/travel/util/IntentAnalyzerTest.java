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
