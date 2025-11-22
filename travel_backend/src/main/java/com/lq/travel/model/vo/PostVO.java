package com.lq.travel.model.vo;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class PostVO {
    private Long id;
    private String title;
    private String content;
    private String coverUrl;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private Date createTime;
    private String authorName;
    private String authorAvatar;
    private List<String> tags;
}