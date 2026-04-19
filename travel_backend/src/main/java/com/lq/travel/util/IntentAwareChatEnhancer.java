package com.lq.travel.util;

import com.lq.travel.model.dto.ai.AIRequest;
import com.lq.travel.model.dto.ai.AIResponse;
import com.lq.travel.model.enums.IntentType;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 同步聊天接口的意图增强器
 * 负责注入意图提示词并补充结构化元数据。
 */
@Slf4j
public final class IntentAwareChatEnhancer {

    private IntentAwareChatEnhancer() {
    }

    public static IntentType resolveIntent(AIRequest request) {
        if (request == null) {
            return IntentType.GENERAL_CHAT;
        }
        // TODO: Task 3 will inject IntentAnalyzer bean and use instance method
        return new IntentAnalyzer(null).analyze(request.getMessage());
    }

    public static void applySystemPromptIfMissing(AIRequest request, IntentType intent) {
        if (request == null) {
            return;
        }
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
            return;
        }
        request.setSystemPrompt(TravelIntentPromptFactory.byIntent(intent));
    }

    public static void enrichResponseMetadata(AIResponse response, IntentType intent) {
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