package com.lq.travel.util;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StructuredItineraryExtractorTest {

    @Test
    void shouldExtractAndNormalizeFromMarkdownJsonBlock() {
        String modelText = """
                观察: 已为你生成 2 天游行程，重点覆盖经典景点和夜游体验。

                ```json
                {
                  "destination": "东京",
                  "days": 2,
                  "budget": 2400,
                  "theme": "城市漫游",
                  "dailyPlans": [
                    {
                      "day": 1,
                      "date": "2026-04-06",
                      "activities": [
                        {
                          "time": "09:00-11:00",
                          "name": "浅草寺",
                          "type": "scenic",
                          "description": "老城文化体验",
                          "address": "东京都台东区浅草2-3-1",
                          "estimatedCost": 30
                        }
                      ]
                    }
                  ],
                  "tips": ["穿轻便鞋"]
                }
                ```
                """;

        Optional<String> normalized = StructuredItineraryExtractor.extractAndNormalize(modelText);

        assertTrue(normalized.isPresent());
        assertTrue(normalized.get().contains("\"destination\":\"东京\""));
        assertTrue(normalized.get().contains("\"time\":\"morning\""));
        assertTrue(normalized.get().contains("\"type\":\"attraction\""));
        assertTrue(normalized.get().contains("\"totalEstimatedCost\":30"));
        assertTrue(normalized.get().contains("\"location\":{\"address\":\"东京都台东区浅草2-3-1\"}"));
    }

    @Test
    void shouldReturnEmptyWhenNoDailyPlans() {
        String invalid = """
                观察: 先确认你的目的地。
                {"destination":"广州","days":2}
                """;

        Optional<String> normalized = StructuredItineraryExtractor.extractAndNormalize(invalid);
        assertTrue(normalized.isEmpty());
    }
}
