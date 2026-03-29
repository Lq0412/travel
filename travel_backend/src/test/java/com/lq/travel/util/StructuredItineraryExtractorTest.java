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
        assertTrue(normalized.get().contains("\"location\":{\"address\":\"东京都台东区浅草2-3-1\""));
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

    @Test
    void shouldHandleLenientJsonWithSingleQuotesAndTrailingCommas() {
        String modelText = """
                观察: 我先给你一版可落地草案，后续可以继续微调。

                ```json
                {
                  destination: '成都',
                  days: 2,
                  budget: '1800',
                  theme: '休闲逛吃',
                  dailyPlans: [
                    {
                      day: 1,
                      activities: [
                        {
                          time: '10:00-12:00',
                          title: '宽窄巷子',
                          type: 'scenic',
                          description: '老成都慢生活体验',
                          address: '成都市青羊区',
                          estimatedCost: '50',
                        },
                      ],
                    },
                  ],
                  tips: '建议错峰出行，准备轻便鞋',
                }
                ```
                """;

        Optional<String> normalized = StructuredItineraryExtractor.extractAndNormalize(modelText);

        assertTrue(normalized.isPresent());
        assertTrue(normalized.get().contains("\"destination\":\"成都\""));
        assertTrue(normalized.get().contains("\"name\":\"宽窄巷子\""));
        assertTrue(normalized.get().contains("\"type\":\"attraction\""));
        assertTrue(normalized.get().contains("\"time\":\"morning\""));
        assertTrue(normalized.get().contains("\"tips\":[\"建议错峰出行\",\"准备轻便鞋\"]"));
    }

    @Test
    void shouldExtractNestedItineraryObject() {
        String modelText = """
                观察: 这是你要的精简版规划。

                {
                  "result": {
                    "itinerary": {
                      "destination": "西安",
                      "days": 1,
                      "dailyPlans": [
                        {
                          "day": 1,
                          "activities": [
                            {
                              "time": "evening",
                              "name": "大唐不夜城",
                              "type": "attraction",
                              "description": "夜游体验",
                              "address": "雁塔区",
                              "estimatedCost": 0
                            }
                          ]
                        }
                      ],
                      "tips": ["建议夜间保暖"]
                    }
                  }
                }
                """;

        Optional<String> normalized = StructuredItineraryExtractor.extractAndNormalize(modelText);

        assertTrue(normalized.isPresent());
        assertTrue(normalized.get().contains("\"destination\":\"西安\""));
        assertTrue(normalized.get().contains("\"dailyPlans\""));
        assertTrue(normalized.get().contains("\"name\":\"大唐不夜城\""));
        assertTrue(normalized.get().contains("\"tips\":[\"建议夜间保暖\"]"));
    }

    @Test
    void shouldPreserveLongitudeLatitudeInLocation() {
        String modelText = """
                {
                  "destination": "上海",
                  "days": 1,
                  "dailyPlans": [
                    {
                      "day": 1,
                      "activities": [
                        {
                          "time": "morning",
                          "name": "外滩",
                          "type": "attraction",
                          "description": "黄浦江沿岸经典景观",
                          "location": {
                            "address": "中山东一路",
                            "longitude": "121.4903",
                            "latitude": "31.2417"
                          },
                          "estimatedCost": 0
                        }
                      ]
                    }
                  ]
                }
                """;

        Optional<String> normalized = StructuredItineraryExtractor.extractAndNormalize(modelText);

        assertTrue(normalized.isPresent());
        assertTrue(normalized.get().contains("\"longitude\":121.4903"));
        assertTrue(normalized.get().contains("\"latitude\":31.2417"));
        assertTrue(normalized.get().contains("\"coordinates\":[121.4903,31.2417]"));
    }
}
