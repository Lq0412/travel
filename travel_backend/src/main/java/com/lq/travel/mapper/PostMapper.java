package com.lq.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lq.travel.model.entity.Post;
import org.apache.ibatis.annotations.Select;

public interface PostMapper extends BaseMapper<Post> {
    @Select("UPDATE post SET view_count = view_count + 1 WHERE id = #{id}")
    void incrementViewCount(Long id);
}