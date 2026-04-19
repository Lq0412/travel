package com.lq.travel.util;

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
