package com.lq.travel.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lq.travel.model.dto.message.MessageWallDTO;
import com.lq.travel.model.dto.message.MessageWallQueryRequest;
import com.lq.travel.model.dto.message.ScenicMessageWallDTO;
import com.lq.travel.model.entity.MessageWall;
import com.lq.travel.model.entity.User;
import com.lq.travel.model.vo.MessageWallVO;
import com.lq.travel.model.vo.ScenicMessageWallVO;

import java.util.List;

public interface MessageWallService extends IService<MessageWall> {

    MessageWallVO addMessage(MessageWallDTO messageWallDTO, User loginUser);

    Page<MessageWallVO> getMessageWallPage(MessageWallQueryRequest request, User loginUser);

    void likeMessage(Long messageId, User loginUser);

    void cancelLikeMessage(Long messageId, User loginUser);

    void deleteMessage(Long messageId, User loginUser);

    void reviewMessage(Long messageId, Integer status, User loginUser);

    /**
     * 获取所有景点留言墙配置列表（含留言数量统计）
     */
    List<ScenicMessageWallVO> getAllScenicMessageWalls();

    /**
     * 创建或更新景点留言墙配置
     */
    ScenicMessageWallVO createOrUpdateScenicMessageWall(ScenicMessageWallDTO dto, User loginUser);
}