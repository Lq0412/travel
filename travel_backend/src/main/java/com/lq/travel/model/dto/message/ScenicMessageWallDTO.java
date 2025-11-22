package com.lq.travel.model.dto.message;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 景点留言墙配置DTO
 */
@Data
public class ScenicMessageWallDTO {
    
    @NotNull(message = "景点ID不能为空")
    private Long scenicSpotId;
    
    @NotBlank(message = "标题不能为空")
    private String title;
    
    private String description;
}

