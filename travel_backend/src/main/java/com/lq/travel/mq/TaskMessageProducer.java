package com.lq.travel.mq;

import com.lq.travel.constant.AiMqConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * AI 任务消息生产者
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "ai.mq", name = "enabled", havingValue = "true")
public class TaskMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    public void sendTripPlanTask(Map<String, Object> body) {
        send(AiMqConstants.ROUTING_TRIP_PLAN, body);
    }

    public void sendChatSummaryTask(Map<String, Object> body) {
        send(AiMqConstants.ROUTING_CHAT_SUMMARY, body);
    }

    public void sendKnowledgeIngestionTask(Map<String, Object> body) {
        send(AiMqConstants.ROUTING_KNOWLEDGE_INGESTION, body);
    }

    private void send(String routingKey, Map<String, Object> body) {
        rabbitTemplate.convertAndSend(AiMqConstants.EXCHANGE_AI_TASK, routingKey, body);
        log.info("Sent task message, routingKey={}, payloadKeys={}", routingKey, body.keySet());
    }
}
