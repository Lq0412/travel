package com.lq.travel.service.impl;

import com.lq.travel.callback.EnhancedStreamCallbackAdapter;
import com.lq.travel.callback.StreamCallback;
import com.lq.travel.model.dto.ai.AIRequest;
import com.lq.travel.model.entity.AIMessage;
import com.lq.travel.model.enums.IntentType;
import com.lq.travel.provider.AIProvider;
import com.lq.travel.service.AIMessageService;
import com.lq.travel.util.AICacheHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AIServiceImplStreamPersistenceTest {

    @Test
    void shouldPersistStructuredMarkersWhenEnhancedStreamCallbackAddsStructuredPayload() {
        AIMessageService messageService = mock(AIMessageService.class);
        AICacheHandler cacheHandler = mock(AICacheHandler.class);
        AIProvider provider = mock(AIProvider.class);

        when(provider.getProviderName()).thenReturn("mock");
        when(provider.isAvailable()).thenReturn(true);
        when(provider.getDefaultModel()).thenReturn("mock-model");
        when(cacheHandler.generateCacheKey(any(AIRequest.class), eq(99L))).thenReturn("ai:cache:test");
        when(messageService.getRecentMessages(99L, 1)).thenReturn(Collections.emptyList());
        when(messageService.convertToAIRequestHistory(99L)).thenReturn(Collections.emptyList());

        AIServiceImpl service = new AIServiceImpl();
        ReflectionTestUtils.setField(service, "providers", List.of(provider));
        ReflectionTestUtils.setField(service, "messageService", messageService);
        ReflectionTestUtils.setField(service, "cacheHandler", cacheHandler);
        ReflectionTestUtils.setField(service, "defaultProviderName", "mock");
        service.initProviders();

        StreamCallback collector = new StreamCallback() {
            @Override
            public void onData(String data) {
                // no-op
            }

            @Override
            public void onComplete() {
                // no-op
            }

            @Override
            public void onError(Exception error) {
                throw new AssertionError("Unexpected error", error);
            }
        };

        EnhancedStreamCallbackAdapter callback = new EnhancedStreamCallbackAdapter(
                collector,
                IntentType.ITINERARY_GENERATION
        );

        when(provider.getAvailableModels()).thenReturn(new String[]{"mock-model"});
        doAnswer(invocation -> {
            StreamCallback delegated = invocation.getArgument(1);
            delegated.onData("行程草案如下：\n```json\n{\"destination\":\"杭州\",\"days\":2,\"dailyPlans\":[{\"day\":1,\"activities\":[{");
            delegated.onData("\"time\":\"09:00\",\"name\":\"西湖\",\"type\":\"scenic\",\"description\":\"湖畔漫步\",\"address\":\"杭州市西湖区\",\"estimatedCost\":0}]}]}\n```");
            delegated.onComplete();
            return null;
        }).when(provider).chatStream(any(AIRequest.class), any(StreamCallback.class));

        AIRequest request = AIRequest.builder()
                .message("帮我规划杭州两日游")
                .model("mock-model")
                .provider("mock")
                .stream(true)
                .build();

        service.chatStream(request, 99L, callback);

        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(messageService, times(1)).saveAIMessage(eq(99L), contentCaptor.capture(), eq(null), anyLong());

        String persistedContent = contentCaptor.getValue();
        assertTrue(persistedContent.contains("__STRUCTURED_DATA_START__"));
        assertTrue(persistedContent.contains("__STRUCTURED_DATA_END__"));
        assertTrue(persistedContent.contains("\"destination\":\"杭州\""));
    }
}
