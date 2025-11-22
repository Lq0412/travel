package com.lq.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lq.travel.model.entity.PostTag;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface PostTagMapper extends BaseMapper<PostTag> {

    @Delete("DELETE FROM post_tag WHERE post_id = #{postId}")
    void deleteByPostId(Long postId);

    @Select("SELECT post_id FROM post_tag WHERE tag_id = #{tagId}")
    List<Long> selectPostIdsByTagId(Long tagId);

    @Select("<script>" +
            "SELECT DISTINCT post_id FROM post_tag WHERE tag_id IN " +
            "<foreach collection='tagIds' item='tagId' open='(' separator=',' close=')'>" +
            "#{tagId}" +
            "</foreach>" +
            "</script>")
    List<Long> selectPostIdsByTagIds(List<Long> tagIds);
}