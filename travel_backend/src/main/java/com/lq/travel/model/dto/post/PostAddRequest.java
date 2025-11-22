package com.lq.travel.model.dto.post;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PostAddRequest implements Serializable {
    private String title;
    private String content;
    private String coverUrl;
    private Integer categoryId;
    private List<String> tags;
}