package com.lq.travel.model.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RAG 检索片段
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagChunk {

    /**
     * 片段标题
     */
    private String title;

    /**
     * 片段来源
     */
    private String source;

    /**
     * 片段正文
     */
    private String content;

    /**
     * 相关性得分
     */
    private Double score;
}
