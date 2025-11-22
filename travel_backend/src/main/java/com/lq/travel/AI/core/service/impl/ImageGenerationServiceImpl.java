package com.lq.travel.AI.core.service.impl;

import com.alibaba.dashscope.utils.Constants;
import com.lq.travel.AI.core.service.ImageGenerationService;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * AI图像生成服务实现（接入通义千问 Qwen-Image 异步接口）
 */
@Slf4j
@Service
public class ImageGenerationServiceImpl implements ImageGenerationService {
    
    @Value("${spring.ai.dashscope.api-key:}")
    private String apiKey;

    /**
     * 可选：自定义DashScope地域/网关
     * 例如： https://dashscope.aliyuncs.com 或 https://dashscope-intl.aliyuncs.com
     */
    @Value("${app.image.base-url:https://dashscope.aliyuncs.com}")
    private String baseUrl;

    /**
     * 通义万相/千问图像模型，如 qwen-image-plus 或 qwen-image
     */
    @Value("${app.image.model:qwen-image-plus}")
    private String model;

    /**
     * 文生图异步创建任务接口（北京地域默认）
     * 示例：/api/v1/services/aigc/text2image/image-synthesis
     */
    @Value("${app.image.endpoint:/api/v1/services/aigc/text2image/image-synthesis}")
    private String imageSynthesisPath;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @PostConstruct
    public void init() {
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            Constants.apiKey = apiKey;
            log.info("DashScope API密钥已设置（长度={}）", apiKey.length());
        } else {
            log.warn("DashScope API密钥未配置，图像生成功能可能不可用");
        }
    }
    
    @Override
    public String generateImageAsync(ImageGenerationRequest request) {
        // 参数校验
        if (request.getPrompt() == null || request.getPrompt().trim().isEmpty()) {
            throw new IllegalArgumentException("提示词不能为空");
        }
        
        if (request.getReferenceImageUrls() != null && request.getReferenceImageUrls().size() > 6) {
            throw new IllegalArgumentException("参考图片最多6张");
        }

        // 调用DashScope异步创建任务，直接返回远端task_id
        String remoteTaskId = createAsyncTaskOnDashScope(request);
        log.info("已创建Qwen-Image异步任务，taskId={}", remoteTaskId);
        return remoteTaskId;
    }
    
    // === DashScope 接入实现 ===

    private String createAsyncTaskOnDashScope(ImageGenerationRequest request) {
        String url = concatUrl(baseUrl, imageSynthesisPath);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        headers.add("X-DashScope-Async", "enable"); // 异步必填

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);

        Map<String, Object> input = new HashMap<>();
        input.put("prompt", request.getPrompt());
        // 可选：反向提示词，如有需要可从请求扩展
        body.put("input", input);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("size", normalizeSize(request.getSize()));
        parameters.put("n", 1);
        parameters.put("prompt_extend", true);
        parameters.put("watermark", true);
        body.put("parameters", parameters);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<String> resp = restTemplate.postForEntity(url, entity, String.class);
            String json = resp.getBody();
            JsonNode root = objectMapper.readTree(json);
            JsonNode output = root.path("output");
            String taskId = output.path("task_id").asText(null);
            if (taskId == null || taskId.isEmpty()) {
                throw new IllegalStateException("DashScope返回异常：未获得task_id，响应=" + json);
            }
            return taskId;
        } catch (HttpStatusCodeException e) {
            String bodyStr = e.getResponseBodyAsString();
            log.error("创建异步任务失败，status={}, body={}", e.getStatusCode(), bodyStr);
            throw new RuntimeException("创建异步任务失败：" + e.getStatusCode() + " " + bodyStr, e);
        } catch (Exception e) {
            throw new RuntimeException("创建异步任务异常：" + e.getMessage(), e);
        }
    }

    private String normalizeSize(String size) {
        if (size == null || size.isEmpty()) {
            return "1328*1328";
        }
        // 将 1024x1024 这样的格式转为 1024*1024
        return size.toLowerCase().replace('x', '*');
    }
    
    @Override
    public ImageGenerationResult getTaskStatus(String taskId) {
        ImageGenerationResult result = new ImageGenerationResult();
        result.setTaskId(taskId);

        String url = concatUrl(baseUrl, "/api/v1/tasks/" + taskId);
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> resp = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String json = resp.getBody();
            JsonNode root = objectMapper.readTree(json);
            JsonNode output = root.path("output");
            String status = output.path("task_status").asText("UNKNOWN");

            switch (status) {
                case "PENDING":
                    result.setStatus("pending");
                    result.setProgress(5);
                    break;
                case "RUNNING":
                    result.setStatus("processing");
                    result.setProgress(60);
                    break;
                case "SUCCEEDED":
                    result.setStatus("success");
                    result.setProgress(100);
                    // 解析图片URL
                    JsonNode results = output.path("results");
                    if (results.isArray() && results.size() > 0) {
                        JsonNode first = results.get(0);
                        String imageUrl = first.path("url").asText(null);
                        result.setImageUrl(imageUrl);
                    }
                    break;
                case "FAILED":
                    result.setStatus("failed");
                    // 解析更详细的失败原因（不同版本字段名可能不同，尽量兼容）
                    StringBuilder reason = new StringBuilder("DashScope任务失败");
                    // 常见字段：message / error / code / reason / task_error
                    String msg = safeText(output, "message");
                    String err = safeText(output, "error");
                    String code = safeText(output, "code");
                    String reasonText = safeText(output, "reason");
                    String taskError = safeText(output, "task_error");
                    if (!msg.isEmpty()) reason.append("：").append(msg);
                    if (!err.isEmpty()) reason.append("；错误：").append(err);
                    if (!code.isEmpty()) reason.append("；代码：").append(code);
                    if (!reasonText.isEmpty()) reason.append("；原因：").append(reasonText);
                    if (!taskError.isEmpty()) reason.append("；详情：").append(taskError);
                    result.setErrorMessage(reason.toString());
                    result.setProgress(0);
                    break;
                case "CANCELED":
                    result.setStatus("failed");
                    result.setErrorMessage("DashScope任务被取消");
                    result.setProgress(0);
                    break;
                default:
                    result.setStatus("failed");
                    result.setErrorMessage("DashScope任务状态未知: " + status);
                    result.setProgress(0);
                    break;
            }
            return result;
        } catch (HttpStatusCodeException e) {
            String bodyStr = e.getResponseBodyAsString();
            log.error("查询任务失败，status={}, body={}", e.getStatusCode(), bodyStr);
            result.setStatus("failed");
            result.setErrorMessage("查询失败：" + e.getStatusCode() + " " + bodyStr);
            result.setProgress(0);
            return result;
        } catch (Exception e) {
            result.setStatus("failed");
            result.setErrorMessage("查询异常：" + e.getMessage());
            result.setProgress(0);
            return result;
        }
    }

    /**
     * 从JSON节点安全读取一个文本字段，为空返回""
     */
    private String safeText(JsonNode node, String field) {
        if (node == null) return "";
        JsonNode v = node.path(field);
        if (v == null || v.isMissingNode() || v.isNull()) return "";
        String s = v.asText("");
        return s == null ? "" : s.trim();
    }
    
    private String concatUrl(String base, String path) {
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return base + path;
    }
}

