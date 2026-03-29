package com.lq.travel.service;

import lombok.Data;

import java.util.List;

/**
 * AI图像生成服务接口
 */
public interface ImageGenerationService {
    
    /**
     * 生成图像（异步任务）
     * @param request 图像生成请求
     * @return 任务ID，用于后续轮询状态
     */
    String generateImageAsync(ImageGenerationRequest request);
    
    /**
     * 查询生成任务状态
     * @param taskId 任务ID
     * @return 任务状态和结果
     */
    ImageGenerationResult getTaskStatus(String taskId);
    
    /**
     * 图像生成请求
     */
    @Data
    class ImageGenerationRequest {
        /**
         * 提示词
         */
        private String prompt;
        
        /**
         * 参考图片URL列表（最多6张）
         */
        private List<String> referenceImageUrls;
        
        /**
         * 尺寸：1024x1024（MVP固定）
         */
        private String size = "1024x1024";
        
        /**
         * 风格：fresh（清新）或 documentary（纪实）
         */ 
        private String style = "fresh";
        
        /**
         * 质量：standard（标准）
         */
        private String quality = "standard";
    }
    
    /**
     * 图像生成结果
     */
    @Data
    class ImageGenerationResult {
        /**
         * 任务ID
         */
        private String taskId;
        
        /**
         * 状态：pending、processing、success、failed
         */
        private String status;
        
        /**
         * 生成的图片URL（成功时返回）
         */
        private String imageUrl;
        
        /**
         * 错误信息（失败时返回）
         */
        private String errorMessage;
        
        /**
         * 进度（0-100）
         */
        private Integer progress;
    }
}

