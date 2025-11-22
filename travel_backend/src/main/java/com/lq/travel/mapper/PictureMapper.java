package com.lq.travel.mapper;

import com.lq.travel.model.entity.Picture;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

/**
 * @author Lq304
 * @description 针对表【picture(图片)】的数据库操作Mapper
 * @createDate 2025-08-25 17:15:05
 * @Entity com.lq.common.model.entity.Picture
 */
@Mapper
public interface PictureMapper {

    /**
     * 插入图片
     */
    @Insert("INSERT INTO picture (id, url, thumbnail_url, name, introduction, category, tags, pic_size, pic_width, pic_height, pic_scale, pic_format, user_id, create_time, edit_time, update_time, review_status, review_message, reviewer_id, review_time, is_delete) " +
            "VALUES (#{id}, #{url}, #{thumbnailUrl}, #{name}, #{introduction}, #{category}, #{tags}, #{picSize}, #{picWidth}, #{picHeight}, #{picScale}, #{picFormat}, #{userId}, #{createTime}, #{editTime}, #{updateTime}, #{reviewStatus}, #{reviewMessage}, #{reviewerId}, #{reviewTime}, #{isDelete})")
    int insert(Picture picture);

    /**
     * 根据ID查询图片
     */
    @Select("SELECT * FROM picture WHERE id = #{id} AND is_delete = 0")
    Picture selectById(Long id);

    /**
     * 更新图片
     */
    @Update("UPDATE picture SET url = #{url}, thumbnail_url = #{thumbnailUrl}, name = #{name}, introduction = #{introduction}, category = #{category}, tags = #{tags}, pic_size = #{picSize}, pic_width = #{picWidth}, pic_height = #{picHeight}, pic_scale = #{picScale}, pic_format = #{picFormat}, user_id = #{userId}, edit_time = #{editTime}, update_time = #{updateTime}, review_status = #{reviewStatus}, review_message = #{reviewMessage}, reviewer_id = #{reviewerId}, review_time = #{reviewTime} WHERE id = #{id}")
    int updateById(Picture picture);

    /**
     * 根据ID删除图片（逻辑删除）
     */
    @Update("UPDATE picture SET is_delete = 1 WHERE id = #{id}")
    int deleteById(Long id);

    /**
     * 根据ID列表查询图片
     */
    @Select("<script>" +
            "SELECT * FROM picture WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            " AND is_delete = 0" +
            "</script>")
    List<Picture> selectByIds(@Param("ids") List<Long> ids);

    /**
     * 根据用户ID查询图片
     */
    @Select("SELECT * FROM picture WHERE user_id = #{userId} AND is_delete = 0")
    List<Picture> selectByUserId(Long userId);

    /**
     * 根据URL查询图片数量
     */
    @Select("SELECT COUNT(*) FROM picture WHERE url = #{url} AND is_delete = 0")
    long countByUrl(String url);

    /**
     * 分页查询图片
     */
    @Select("SELECT * FROM picture WHERE is_delete = 0 LIMIT #{offset}, #{limit}")
    List<Picture> selectPage(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 查询总数
     */
    @Select("SELECT COUNT(*) FROM picture WHERE is_delete = 0")
    long selectCount();

    /**
     * 根据条件查询图片（支持动态SQL）
     */
    List<Picture> selectByCondition(Map<String, Object> params);

    /**
     * 根据条件统计数量
     */
    long countByCondition(Map<String, Object> params);
}




