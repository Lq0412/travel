package com.lq.travel.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("knowledge_experience")
public class KnowledgeExperience {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String platform;
    private String title;
    private String content;
    private String tags;
    private String url;
    private Integer syncStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
