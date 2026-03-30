package com.lq.travel.util;

import com.lq.travel.model.dto.ai.AIRequest;
import com.lq.travel.model.dto.ai.AIResponse;
import com.lq.travel.model.enums.IntentType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntentAwareChatEnhancerTest {

    @Test
    void shouldApplyItineraryPromptWhenSystemPromptMissing() {
        AIRequest request = AIRequest.builder()
                .message("帮我规划一份广州两日游行程")
                .build();

        IntentType intent = IntentAwareChatEnhancer.resolveIntent(request);
        IntentAwareChatEnhancer.applySystemPromptIfMissing(request, intent);

        assertEquals(IntentType.ITINERARY_GENERATION, intent);
        assertNotNull(request.getSystemPrompt());
        assertTrue(request.getSystemPrompt().contains("dailyPlans"));
    }

    @Test
    void shouldKeepCustomSystemPrompt() {
        AIRequest request = AIRequest.builder()
                .message("介绍一下广州从化温泉景点")
                .systemPrompt("custom-system-prompt")
                .build();

        IntentType intent = IntentAwareChatEnhancer.resolveIntent(request);
        IntentAwareChatEnhancer.applySystemPromptIfMissing(request, intent);

        assertEquals("custom-system-prompt", request.getSystemPrompt());
    }

    @Test
    void shouldAttachStructuredItineraryMetadataWhenJsonExists() {
        String content = """
                观察: 已为你生成 1 天游玩计划。
                ```json
                {
                  "destination": "东京",
                  "days": 1,
                  "dailyPlans": [
                    {
                      "day": 1,
                      "activities": [
                        {
                          "time": "09:00-10:30",
                          "name": "浅草寺",
                          "type": "scenic",
                          "description": "体验老城文化",
                          "address": "东京都台东区",
                          "estimatedCost": 30
                        }
                      ]
                    }
                  ]
                }
                ```
                """;

        AIResponse response = AIResponse.success(content, "model-x", "provider-x");
        IntentAwareChatEnhancer.enrichResponseMetadata(response, IntentType.ITINERARY_GENERATION);

        assertEquals("ITINERARY_GENERATION", response.getMetadata().get("intentType"));
        assertEquals(Boolean.TRUE, response.getMetadata().get("structuredItineraryAvailable"));
        assertTrue(String.valueOf(response.getMetadata().get("structuredItineraryJson")).contains("\"destination\":\"东京\""));
    }

    @Test
    void shouldMarkStructuredUnavailableWhenIntentIsAttractionQuery() {
        AIResponse response = AIResponse.success("广州从化有很多温泉景点", "model-x", "provider-x");

        IntentAwareChatEnhancer.enrichResponseMetadata(response, IntentType.ATTRACTION_QUERY);

        assertEquals("ATTRACTION_QUERY", response.getMetadata().get("intentType"));
        assertEquals(Boolean.FALSE, response.getMetadata().get("structuredItineraryAvailable"));
        assertFalse(response.getMetadata().containsKey("structuredItineraryJson"));
    }
}