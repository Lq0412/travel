package com.lq.travel.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lq.travel.mapper.MessageWallMapper;
import com.lq.travel.model.dto.message.MessageWallDTO;
import com.lq.travel.model.dto.message.MessageWallQueryRequest;
import com.lq.travel.model.entity.MessageLike;
import com.lq.travel.model.entity.MessageWall;
import com.lq.travel.model.entity.User;
import com.lq.travel.model.vo.MessageWallVO;
import com.lq.travel.model.dto.message.ScenicMessageWallDTO;
import com.lq.travel.model.entity.ScenicSpot;
import com.lq.travel.model.vo.ScenicMessageWallVO;
import com.lq.travel.service.MessageLikeService;
import com.lq.travel.service.MessageWallService;
import com.lq.travel.service.ScenicSpotService;
import com.lq.travel.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MessageWallServiceImpl extends ServiceImpl<MessageWallMapper, MessageWall>
        implements MessageWallService {

    @Autowired
    private UserService userService;

    @Autowired
    private MessageLikeService messageLikeService;

    @Autowired
    private ScenicSpotService scenicSpotService;

    @Override
    @Transactional
    public MessageWallVO addMessage(MessageWallDTO messageWallDTO, User loginUser) {
        MessageWall messageWall = new MessageWall();
        BeanUtils.copyProperties(messageWallDTO, messageWall);

        // 设置用户信息
        messageWall.setUserId(loginUser.getId());
        if (Boolean.TRUE.equals(messageWallDTO.getIsAnonymous())) {
            messageWall.setUserName("匿名用户");
            messageWall.setUserAvatar(null);
        } else {
            messageWall.setUserName(loginUser.getUserName());
            messageWall.setUserAvatar(loginUser.getUserAvatar());
        }

        // 设置默认状态（需要审核）
        messageWall.setStatus(0);
        messageWall.setLikes(0);

        boolean saved = this.save(messageWall);
        if (!saved) {
            throw new RuntimeException("留言发布失败");
        }

        return getMessageWallVO(messageWall, loginUser);
    }

    @Override
    public Page<MessageWallVO> getMessageWallPage(MessageWallQueryRequest request, User loginUser) {
        Page<MessageWall> page = new Page<>(request.getCurrent(), request.getPageSize());
        Page<MessageWall> messagePage = this.baseMapper.selectMessageWallPage(page, request);

        Page<MessageWallVO> voPage = new Page<>();
        BeanUtils.copyProperties(messagePage, voPage);

        List<MessageWallVO> voList = messagePage.getRecords().stream()
                .map(message -> getMessageWallVO(message, loginUser))
                .collect(Collectors.toList());

        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional
    public void likeMessage(Long messageId, User loginUser) {
        MessageWall messageWall = this.getById(messageId);
        if (messageWall == null || messageWall.getIsDelete() == 1) {
            throw new RuntimeException("留言不存在");
        }

        // 检查是否已经点赞
        boolean hasLiked = messageLikeService.checkUserLike(loginUser.getId(), messageId);
        if (hasLiked) {
            throw new RuntimeException("已经点赞过该留言");
        }

        // 添加点赞记录
        MessageLike messageLike = new MessageLike();
        messageLike.setUserId(loginUser.getId());
        messageLike.setMessageId(messageId);
        messageLikeService.save(messageLike);

        // 更新点赞数
        this.baseMapper.updateLikes(messageId, 1);
    }

    @Override
    @Transactional
    public void cancelLikeMessage(Long messageId, User loginUser) {
        boolean removed = messageLikeService.removeUserLike(loginUser.getId(), messageId);
        if (removed) {
            this.baseMapper.updateLikes(messageId, -1);
        }
    }

    @Override
    public void deleteMessage(Long messageId, User loginUser) {
        MessageWall messageWall = this.getById(messageId);
        if (messageWall == null) {
            throw new RuntimeException("留言不存在");
        }

        // 检查权限：只能删除自己的留言或者是管理员
        if (!messageWall.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new RuntimeException("没有权限删除该留言");
        }

        MessageWall updateEntity = new MessageWall();
        updateEntity.setId(messageId);
        updateEntity.setIsDelete(1);
        this.updateById(updateEntity);
    }

    @Override
    public void reviewMessage(Long messageId, Integer status, User loginUser) {
        if (!userService.isAdmin(loginUser)) {
            throw new RuntimeException("没有审核权限");
        }

        MessageWall updateEntity = new MessageWall();
        updateEntity.setId(messageId);
        updateEntity.setStatus(status);
        this.updateById(updateEntity);
    }

    private MessageWallVO getMessageWallVO(MessageWall messageWall, User loginUser) {
        MessageWallVO vo = new MessageWallVO();
        BeanUtils.copyProperties(messageWall, vo);

        // 检查当前用户是否点赞
        if (loginUser != null) {
            boolean isLiked = messageLikeService.checkUserLike(loginUser.getId(), messageWall.getId());
            vo.setIsLiked(isLiked);
        }

        return vo;
    }

    @Override
    public List<ScenicMessageWallVO> getAllScenicMessageWalls() {
        try {
            // 获取所有景点
            List<ScenicSpot> spots = scenicSpotService.getAllSpots();
            log.info("获取到{}个景点", spots != null ? spots.size() : 0);
            
            if (spots == null || spots.isEmpty()) {
                return new ArrayList<>();
            }
            
            return spots.stream().map(spot -> {
                try {
                    ScenicMessageWallVO vo = new ScenicMessageWallVO();
                    vo.setScenicSpotId(spot.getId());
                    vo.setScenicSpotName(spot.getName());
                    // 注意：如果数据库字段不存在，这里可能为null，但不影响功能
                    vo.setTitle(spot.getMessageWallTitle());
                    vo.setDescription(spot.getMessageWallDescription());
                    vo.setCreateTime(spot.getCreateTime());
                    vo.setUpdateTime(spot.getUpdateTime());
                    
                    // 统计该景点已审核通过的留言数量
                    Long messageCount = this.baseMapper.countApprovedMessagesByScenicSpotId(spot.getId());
                    vo.setMessageCount(messageCount != null ? messageCount : 0L);
                    
                    return vo;
                } catch (Exception e) {
                    log.error("处理景点 {} 的留言墙配置时出错", spot.getId(), e);
                    // 返回一个基本的VO，避免整个列表失败
                    ScenicMessageWallVO vo = new ScenicMessageWallVO();
                    vo.setScenicSpotId(spot.getId());
                    vo.setScenicSpotName(spot.getName());
                    vo.setMessageCount(0L);
                    return vo;
                }
            }).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取所有景点留言墙配置列表失败", e);
            throw new RuntimeException("获取留言墙列表失败：" + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public ScenicMessageWallVO createOrUpdateScenicMessageWall(ScenicMessageWallDTO dto, User loginUser) {
        if (!userService.isAdmin(loginUser)) {
            throw new RuntimeException("没有权限管理留言墙配置");
        }

        ScenicSpot spot = scenicSpotService.getById(dto.getScenicSpotId());
        if (spot == null || spot.getIsDelete() == 1) {
            throw new RuntimeException("景点不存在");
        }

        // 更新景点留言墙配置
        ScenicSpot updateSpot = new ScenicSpot();
        updateSpot.setId(dto.getScenicSpotId());
        updateSpot.setMessageWallTitle(dto.getTitle());
        updateSpot.setMessageWallDescription(dto.getDescription());
        
        boolean updated = scenicSpotService.updateById(updateSpot);
        if (!updated) {
            throw new RuntimeException("更新留言墙配置失败");
        }

        // 重新获取更新后的景点信息
        spot = scenicSpotService.getById(dto.getScenicSpotId());
        
        // 构造返回VO
        ScenicMessageWallVO vo = new ScenicMessageWallVO();
        vo.setScenicSpotId(spot.getId());
        vo.setScenicSpotName(spot.getName());
        vo.setTitle(spot.getMessageWallTitle());
        vo.setDescription(spot.getMessageWallDescription());
        vo.setCreateTime(spot.getCreateTime());
        vo.setUpdateTime(spot.getUpdateTime());
        
        // 统计留言数量
        Long messageCount = this.baseMapper.countApprovedMessagesByScenicSpotId(spot.getId());
        vo.setMessageCount(messageCount != null ? messageCount : 0L);
        
        return vo;
    }
}