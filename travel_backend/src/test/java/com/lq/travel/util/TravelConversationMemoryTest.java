package com.lq.travel.util;

import com.lq.travel.model.entity.AIMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TravelConversationMemoryTest {

    @Test
    void shouldMarkConversationReadyWhenDateAccommodationAndTransportAreCollectedAcrossTurns() {
        TravelConversationMemory.MemorySnapshot snapshot = TravelConversationMemory.analyze(List.of(
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
    void shouldTreatDowntownAreaPreferenceAsAccommodationSignal() {
        TravelConversationMemory.MemorySnapshot snapshot = TravelConversationMemory.analyze(List.of(
                userMessage("我打算去揭阳"),
                userMessage("明天，3天"),
                userMessage("市区，1000，打车")
        ));

        assertEquals("揭阳", snapshot.destination());
        assertEquals("明天", snapshot.travelDate());
        assertEquals("3天", snapshot.days());
        assertEquals("市区", snapshot.accommodation());
        assertEquals("打车", snapshot.transport());
        assertTrue(snapshot.isReadyForItinerary());
    }

    private static AIMessage userMessage(String content) {
        AIMessage message = new AIMessage();
        message.setRole("user");
        message.setContent(content);
        return message;
    }
}
