package com.lq.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lq.travel.model.entity.Tag;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface TagMapper extends BaseMapper<Tag> {

    @Select("SELECT t.* FROM tag t " +
            "INNER JOIN post_tag pt ON t.id = pt.tag_id " +
            "WHERE pt.post_id = #{postId} AND t.is_delete = 0")
    List<Tag> selectTagsByPostId(Long postId);

    @Select("SELECT t.* FROM tag t " +
            "WHERE t.name LIKE CONCAT('%', #{keyword}, '%') AND t.is_delete = 0 " +
            "ORDER BY t.create_time DESC")
    List<Tag> searchTags(String keyword);
}