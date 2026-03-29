package com.lq.travel.callback;

import com.lq.travel.callback.StreamCallback;
import com.lq.travel.model.enums.IntentType;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

class EnhancedStreamCallbackAdapterOutputTest {

    @Test
    void shouldPrintFinalStreamPayloadForManualInspection() {
        StringBuilder emitted = new StringBuilder();
        AtomicBoolean completed = new AtomicBoolean(false);

        StreamCallback collector = new StreamCallback() {
            @Override
            public void onData(String data) {
                emitted.append(data);
            }

            @Override
            public void onComplete() {
                completed.set(true);
            }

            @Override
            public void onError(Exception error) {
                throw new AssertionError("Unexpected stream error", error);
            }
        };

        EnhancedStreamCallbackAdapter adapter = new EnhancedStreamCallbackAdapter(
                collector,
                IntentType.ITINERARY_GENERATION
        );

        String chunk1 = "Plan ready. Here is the draft.\\n\\n```json\\n{\"destination\":\"Tokyo\",\"days\":2,\"dailyPlans\":[{\"day\":1,\"activities\":[{";
        String chunk2 = "\"time\":\"09:00-11:00\",\"name\":\"Sensoji Temple\",\"type\":\"scenic\",\"description\":\"Old town culture walk\",\"address\":\"2-3-1 Asakusa, Taito City, Tokyo\",\"estimatedCost\":30}]}],\"tips\":[\"Wear comfortable shoes\"]}\\n```";

        adapter.onData(chunk1);
        adapter.onData(chunk2);
        adapter.onComplete();

        String finalPayload = emitted.toString();

        System.out.println("=== FINAL_STREAM_PAYLOAD_START ===");
        System.out.println(finalPayload);
        System.out.println("=== FINAL_STREAM_PAYLOAD_END ===");

        assertTrue(completed.get());
        assertTrue(finalPayload.contains("__STRUCTURED_DATA_START__"));
        assertTrue(finalPayload.contains("__STRUCTURED_DATA_END__"));
        assertTrue(finalPayload.contains("\"destination\":\"Tokyo\""));
        assertTrue(finalPayload.contains("\"time\":\"morning\""));
        assertTrue(finalPayload.contains("\"type\":\"attraction\""));
    }

    @Test
    void shouldAlsoSendStructuredDataForGeneralChatWhenJsonExists() {
        StringBuilder emitted = new StringBuilder();
        AtomicBoolean completed = new AtomicBoolean(false);

        StreamCallback collector = new StreamCallback() {
            @Override
            public void onData(String data) {
                emitted.append(data);
            }

            @Override
            public void onComplete() {
                completed.set(true);
            }

            @Override
            public void onError(Exception error) {
                throw new AssertionError("Unexpected stream error", error);
            }
        };

        EnhancedStreamCallbackAdapter adapter = new EnhancedStreamCallbackAdapter(
                collector,
                IntentType.GENERAL_CHAT
        );

        String chunk1 = "观察: 这是可保存草案。\\n\\n```json\\n{\"destination\":\"广州从化\",\"days\":2,\"dailyPlans\":[{\"day\":1,\"activities\":[{";
        String chunk2 = "\"time\":\"09:00\",\"name\":\"石门国家森林公园\",\"type\":\"scenic\",\"description\":\"赏花徒步\",\"address\":\"广州市从化区\",\"estimatedCost\":80}]}],\"tips\":[\"建议带防晒\"]}\\n```";

        adapter.onData(chunk1);
        adapter.onData(chunk2);
        adapter.onComplete();

        String finalPayload = emitted.toString();

        assertTrue(completed.get());
        assertTrue(finalPayload.contains("__STRUCTURED_DATA_START__"));
        assertTrue(finalPayload.contains("__STRUCTURED_DATA_END__"));
        assertTrue(finalPayload.contains("\"destination\":\"广州从化\""));
        assertTrue(finalPayload.contains("\"time\":\"morning\""));
    }
}
