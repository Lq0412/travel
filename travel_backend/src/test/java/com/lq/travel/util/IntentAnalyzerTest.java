package com.lq.travel.util;

import com.lq.travel.model.enums.IntentType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IntentAnalyzerTest {

    @Test
    void shouldRecognizeItineraryByTravelPlanningExpression() {
        String input = "给我一个两天的广州从化旅游规划";

        IntentType result = IntentAnalyzer.analyze(input);

        assertEquals(IntentType.ITINERARY_GENERATION, result);
    }

    @Test
    void shouldRecognizeItineraryWhenStructuredJsonRequirementsPresent() {
        String input = "给我一份从化两日游计划，输出可保存结构化行程 JSON，字段包含 dailyPlans 和 totalEstimatedCost";

        IntentType result = IntentAnalyzer.analyze(input);

        assertEquals(IntentType.ITINERARY_GENERATION, result);
    }

    @Test
    void shouldKeepAttractionIntentForSpotQuery() {
        String input = "广州从化温泉景点门票和开放时间怎么样";

        IntentType result = IntentAnalyzer.analyze(input);

        assertEquals(IntentType.ATTRACTION_QUERY, result);
    }
}
