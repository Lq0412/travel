package com.lq.travel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lq.travel.model.entity.MessageWall;
import com.lq.travel.model.dto.message.MessageWallQueryRequest;
import org.apache.ibatis.annotations.Param;

public interface MessageWallMapper extends BaseMapper<MessageWall> {

    Page<MessageWall> selectMessageWallPage(Page<MessageWall> page,
                                            @Param("params") MessageWallQueryRequest request);

    Integer updateLikes(@Param("messageId") Long messageId,
                        @Param("increment") Integer increment);

    /**
     * 统计每个景点的已审核通过留言数量
     */
    Long countApprovedMessagesByScenicSpotId(@Param("scenicSpotId") Long scenicSpotId);
}