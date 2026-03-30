package com.lq.travel.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lq.travel.constant.AiMqConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 拓扑与消息序列化配置
 */
@Configuration
@ConditionalOnProperty(prefix = "ai.mq", name = "enabled", havingValue = "true")
public class RabbitMQConfig {

    /**
     * 使用 Jackson 作为消息序列化转换器，保持 Producer/Consumer 一致性
     */
    @Bean
    public MessageConverter rabbitMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter rabbitMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(rabbitMessageConverter);
        return template;
    }

    // ========== 交换机 ==========
    @Bean
    public DirectExchange aiTaskExchange() {
        return new DirectExchange(AiMqConstants.EXCHANGE_AI_TASK, true, false);
    }

    @Bean
    public DirectExchange aiTaskDlx() {
        return new DirectExchange(AiMqConstants.EXCHANGE_AI_TASK_DLX, true, false);
    }

    // ========== 队列 ==========
    @Bean
    public Queue tripPlanQueue() {
        return QueueBuilder.durable(AiMqConstants.QUEUE_TRIP_PLAN)
                .withArgument("x-dead-letter-exchange", AiMqConstants.EXCHANGE_AI_TASK_DLX)
                .withArgument("x-dead-letter-routing-key", AiMqConstants.ROUTING_TRIP_PLAN)
                .build();
    }

    @Bean
    public Queue tripPlanDlq() {
        return QueueBuilder.durable(AiMqConstants.QUEUE_TRIP_PLAN_DLQ).build();
    }

    @Bean
    public Queue chatSummaryQueue() {
        return QueueBuilder.durable(AiMqConstants.QUEUE_CHAT_SUMMARY)
                .withArgument("x-dead-letter-exchange", AiMqConstants.EXCHANGE_AI_TASK_DLX)
                .withArgument("x-dead-letter-routing-key", AiMqConstants.ROUTING_CHAT_SUMMARY)
                .build();
    }

    @Bean
    public Queue chatSummaryDlq() {
        return QueueBuilder.durable(AiMqConstants.QUEUE_CHAT_SUMMARY_DLQ).build();
    }

    @Bean
    public Queue knowledgeIngestionQueue() {
        return QueueBuilder.durable(AiMqConstants.QUEUE_KNOWLEDGE_INGESTION)
                .withArgument("x-dead-letter-exchange", AiMqConstants.EXCHANGE_AI_TASK_DLX)
                .withArgument("x-dead-letter-routing-key", AiMqConstants.ROUTING_KNOWLEDGE_INGESTION)
                .build();
    }

    @Bean
    public Queue knowledgeIngestionDlq() {
        return QueueBuilder.durable(AiMqConstants.QUEUE_KNOWLEDGE_INGESTION_DLQ).build();
    }

    // ========== 绑定 ==========
    @Bean
    public Binding tripPlanBinding(Queue tripPlanQueue, DirectExchange aiTaskExchange) {
        return BindingBuilder.bind(tripPlanQueue)
                .to(aiTaskExchange)
                .with(AiMqConstants.ROUTING_TRIP_PLAN);
    }

    @Bean
    public Binding tripPlanDlqBinding(Queue tripPlanDlq, DirectExchange aiTaskDlx) {
        return BindingBuilder.bind(tripPlanDlq)
                .to(aiTaskDlx)
                .with(AiMqConstants.ROUTING_TRIP_PLAN);
    }

    @Bean
    public Binding chatSummaryBinding(Queue chatSummaryQueue, DirectExchange aiTaskExchange) {
        return BindingBuilder.bind(chatSummaryQueue)
                .to(aiTaskExchange)
                .with(AiMqConstants.ROUTING_CHAT_SUMMARY);
    }

    @Bean
    public Binding chatSummaryDlqBinding(Queue chatSummaryDlq, DirectExchange aiTaskDlx) {
        return BindingBuilder.bind(chatSummaryDlq)
                .to(aiTaskDlx)
                .with(AiMqConstants.ROUTING_CHAT_SUMMARY);
    }

    @Bean
    public Binding knowledgeIngestionBinding(Queue knowledgeIngestionQueue, DirectExchange aiTaskExchange) {
        return BindingBuilder.bind(knowledgeIngestionQueue)
                .to(aiTaskExchange)
                .with(AiMqConstants.ROUTING_KNOWLEDGE_INGESTION);
    }

    @Bean
    public Binding knowledgeIngestionDlqBinding(Queue knowledgeIngestionDlq, DirectExchange aiTaskDlx) {
        return BindingBuilder.bind(knowledgeIngestionDlq)
                .to(aiTaskDlx)
                .with(AiMqConstants.ROUTING_KNOWLEDGE_INGESTION);
    }
}
