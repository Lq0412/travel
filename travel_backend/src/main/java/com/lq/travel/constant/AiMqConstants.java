package com.lq.travel.constant;

/**
 * RabbitMQ 交换机 / 队列 / 路由键常量
 */
public final class AiMqConstants {

    private AiMqConstants() {
    }

    public static final String EXCHANGE_AI_TASK = "ai.task.direct";
    public static final String EXCHANGE_AI_TASK_DLX = "ai.task.dlx";

    public static final String QUEUE_TRIP_PLAN = "ai.task.trip-plan";
    public static final String QUEUE_CHAT_SUMMARY = "ai.task.chat-summary";
    public static final String QUEUE_KNOWLEDGE_INGESTION = "ai.task.knowledge-ingestion";

    public static final String QUEUE_TRIP_PLAN_DLQ = "ai.task.trip-plan.dlq";
    public static final String QUEUE_CHAT_SUMMARY_DLQ = "ai.task.chat-summary.dlq";
    public static final String QUEUE_KNOWLEDGE_INGESTION_DLQ = "ai.task.knowledge-ingestion.dlq";

    public static final String ROUTING_TRIP_PLAN = "TRIP_PLAN";
    public static final String ROUTING_CHAT_SUMMARY = "CHAT_SUMMARY";
    public static final String ROUTING_KNOWLEDGE_INGESTION = "KNOWLEDGE_INGESTION";
}
