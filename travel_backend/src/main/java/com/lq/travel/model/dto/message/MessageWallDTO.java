package com.lq.travel.model.dto.message;

import lombok.Data;
import javax.validation.constraints.NotNull;

@Data
public class MessageWallDTO {
    @NotNull(message = "景点ID不能为空")
    private Long scenicSpotId;

    private String content;

    private String textColor = "#FFFFFF";
    private Integer fontSize = 20;
    private String backgroundColor = "rgba(0,0,0,0.3)";
    private Long backgroundId;
    private Boolean isBarrage = false;
    private Integer barrageSpeed = 3;
    private Integer barrageTrajectory = 3;
    private Boolean isAnonymous = false;
    private Integer messageType = 1;
}
