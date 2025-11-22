package com.lq.travel.digitalhuman.controller;

import com.lq.travel.AI.core.constants.TimeoutConfig;
import com.lq.travel.AI.core.model.dto.ResponseDTO;
import com.lq.travel.common.ResponseUtils;
import com.lq.travel.digitalhuman.service.DigitalHumanService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * 数字人控制器
 * 
 * @author Lq
 * @date 2025-01-XX
 */
@RestController
@RequestMapping("/digital-human")
@Slf4j
public class DigitalHumanController {

    @Autowired(required = false)
    private DigitalHumanService digitalHumanService;

    /**
     * 测试接口
     */
    @GetMapping("/test")
    public ResponseDTO<Map<String, Object>> test() {
        log.info("数字人服务测试 - 收到请求");
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("status", "ok");
        result.put("message", "数字人服务运行正常");
        result.put("timestamp", System.currentTimeMillis());
        result.put("controller", "DigitalHumanController loaded successfully");
        result.put("service", digitalHumanService != null ? "Service已加载" : "Service未加载");
        return ResponseUtils.success(result);
    }

    /**
     * 文本对话接口 - SSE流式响应
     * 
     * @param message 用户消息
     * @param voiceId 语音ID（可选，默认为0）
     * @return SSE流式响应
     */
    @PostMapping("/chat")
    public SseEmitter chat(
            @RequestParam String message,
            @RequestParam(required = false, defaultValue = "0") Integer voiceId,
            @RequestParam(required = false, defaultValue = "1.0") Double voiceSpeed) {
        
        log.info("收到数字人对话请求: message={}, voiceId={}, voiceSpeed={}", message, voiceId, voiceSpeed);
        
        // 创建SSE发射器，30秒超时
        SseEmitter emitter = new SseEmitter(TimeoutConfig.SSE_TIMEOUT_DIGITAL_HUMAN);
        
        // 异步处理对话
        digitalHumanService.processChat(message, voiceId, voiceSpeed, emitter);
        
        return emitter;
    }

    /**
     * 获取可用语音列表
     */
    @GetMapping("/voices")
    public ResponseDTO<Map<String, Object>> getVoices() {
        log.info("获取语音列表");
        
        // 暂时返回固定的语音列表
        Map<String, Object> voices = Map.of(
            "voices", new Integer[]{0, 1, 2, 3},
            "voiceNames", new String[]{"默认", "温柔", "活泼", "成熟"}
        );
        
        return ResponseUtils.success(voices);
    }
    
    /**
     * 简单的对话测试接口（用于调试）
     */
    @PostMapping(value = "/chat-simple", produces = "application/json")
    public ResponseDTO<Map<String, Object>> chatSimple(@RequestBody Map<String, String> request) {
        log.info("收到简单对话请求: {}", request);
        
        String message = request.getOrDefault("message", "你好");
        
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("status", "ok");
        result.put("message", message);
        result.put("reply", "这是一个模拟回复，测试成功！");
        result.put("timestamp", System.currentTimeMillis());
        result.put("hint", "SSE接口需要使用Params传递参数，不使用Body");
        
        return ResponseUtils.success(result);
    }
}

