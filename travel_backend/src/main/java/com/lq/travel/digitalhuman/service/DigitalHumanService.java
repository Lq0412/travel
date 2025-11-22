package com.lq.travel.digitalhuman.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

/**
 * 数字人服务
 * 
 * @author Lq
 * @date 2025-01-XX
 */
@Service
@Slf4j
public class DigitalHumanService {

    @Value("${digital-human.python-service-url:http://localhost:8888}")
    private String pythonServiceUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public DigitalHumanService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 处理数字人对话 - 流式响应
     * 
     * @param message 用户消息
     * @param voiceId 语音ID
     * @param voiceSpeed 语音速度
     * @param emitter SSE发射器
     */
    @Async
    public void processChat(String message, Integer voiceId, Double voiceSpeed, SseEmitter emitter) {
        try {
            log.info("开始处理对话: message={}, voiceId={}, voiceSpeed={}", message, voiceId, voiceSpeed);

            // 构造请求体
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("input_mode", "text");
            requestBody.put("prompt", message);
            requestBody.put("voice_id", voiceId);
            requestBody.put("voice_speed", voiceSpeed);

            // 调用Python服务
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            String url = pythonServiceUrl + "/eb_stream";
            log.info("调用Python服务: url={}", url);

            ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                request,
                String.class
            );

            // 处理响应
            String responseBody = response.getBody();
            log.debug("Python服务响应: {}", responseBody);

            // 解析多行JSON响应
            String[] lines = responseBody.split("\n");
            
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;

                try {
                    // 解析每一行JSON
                    Map<String, Object> data = objectMapper.readValue(line, Map.class);
                    
                    // 发送SSE事件
                    emitter.send(SseEmitter.event()
                        .name("message")
                        .data(data));
                    
                    // 如果到达终点，完成流
                    Boolean endpoint = (Boolean) data.get("endpoint");
                    if (endpoint != null && endpoint) {
                        log.info("对话流结束");
                        break;
                    }
                    
                } catch (Exception e) {
                    log.error("解析JSON失败: line={}", line, e);
                }
            }

            // 完成流
            emitter.complete();
            log.info("数字人对话处理完成");

        } catch (Exception e) {
            log.error("数字人对话处理失败", e);
            try {
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("error", true);
                errorData.put("message", "服务暂时不可用: " + e.getMessage());
                errorData.put("endpoint", true);
                
                emitter.send(SseEmitter.event()
                    .name("message")
                    .data(errorData));
                emitter.complete();
            } catch (Exception ex) {
                log.error("发送错误响应失败", ex);
                emitter.completeWithError(ex);
            }
        }
    }

    /**
     * 测试Python服务连接
     */
    public boolean testConnection() {
        try {
            String url = pythonServiceUrl + "/static/MiniLive.html";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("连接Python服务失败", e);
            return false;
        }
    }
}

