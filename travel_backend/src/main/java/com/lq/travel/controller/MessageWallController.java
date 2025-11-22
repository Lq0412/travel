package com.lq.travel.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lq.travel.annotation.AuthCheck;
import com.lq.travel.AI.core.model.dto.ResponseDTO;
import com.lq.travel.common.ResponseUtils;
import com.lq.travel.model.dto.message.MessageWallDTO;
import com.lq.travel.model.dto.message.MessageWallQueryRequest;
import com.lq.travel.model.dto.message.ScenicMessageWallDTO;
import com.lq.travel.model.entity.User;
import com.lq.travel.model.vo.MessageWallVO;
import com.lq.travel.model.vo.ScenicMessageWallVO;
import com.lq.travel.service.MessageWallService;
import com.lq.travel.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/message/wall")
public class MessageWallController {

    @Resource
    private MessageWallService messageWallService;

    @Resource
    private UserService userService;

    @PostMapping("/add")
    public ResponseDTO<MessageWallVO> addMessage(@RequestBody MessageWallDTO messageWallDTO,
                                                  HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        MessageWallVO messageWallVO = messageWallService.addMessage(messageWallDTO, loginUser);
        return ResponseUtils.success(messageWallVO);
    }

    @PostMapping("/list")
    public ResponseDTO<Page<MessageWallVO>> listMessages(@RequestBody MessageWallQueryRequest request,
                                                          HttpServletRequest httpRequest) {
        User loginUser = getLoginUser(httpRequest);
        Page<MessageWallVO> result = messageWallService.getMessageWallPage(request, loginUser);
        return ResponseUtils.success(result);
    }

    @PostMapping("/like/{messageId}")
    public ResponseDTO<Boolean> likeMessage(@PathVariable Long messageId,
                                             HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        messageWallService.likeMessage(messageId, loginUser);
        return ResponseUtils.success(true);
    }

    @PostMapping("/cancelLike/{messageId}")
    public ResponseDTO<Boolean> cancelLikeMessage(@PathVariable Long messageId,
                                                   HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        messageWallService.cancelLikeMessage(messageId, loginUser);
        return ResponseUtils.success(true);
    }

    @PostMapping("/delete/{messageId}")
    public ResponseDTO<Boolean> deleteMessage(@PathVariable Long messageId,
                                               HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        messageWallService.deleteMessage(messageId, loginUser);
        return ResponseUtils.success(true);
    }

    @AuthCheck(mustRole = "admin")
    @PostMapping("/review/{messageId}")
    public ResponseDTO<Boolean> reviewMessage(@PathVariable Long messageId,
                                               @RequestParam Integer status,
                                               HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        messageWallService.reviewMessage(messageId, status, loginUser);
        return ResponseUtils.success(true);
    }

    /**
     * 获取所有景点留言墙配置列表（含留言数量统计）
     */
    @AuthCheck(mustRole = "admin")
    @GetMapping("/list/all")
    public ResponseDTO<List<ScenicMessageWallVO>> getAllScenicMessageWalls(HttpServletRequest request) {
        try {
            User loginUser = getLoginUser(request);
            log.info("获取所有景点留言墙配置列表，用户：{}", loginUser.getUserName());
            List<ScenicMessageWallVO> result = messageWallService.getAllScenicMessageWalls();
            log.info("成功获取留言墙列表，共{}条", result != null ? result.size() : 0);
            return ResponseUtils.success(result != null ? result : List.of());
        } catch (Exception e) {
            log.error("获取景点留言墙配置列表失败", e);
            return ResponseUtils.error(500, "获取留言墙列表失败：" + e.getMessage());
        }
    }

    /**
     * 创建景点留言墙配置
     */
    @AuthCheck(mustRole = "admin")
    @PostMapping("/scenic/create")
    public ResponseDTO<ScenicMessageWallVO> createScenicMessageWall(@RequestBody ScenicMessageWallDTO dto,
                                                                     HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        
        // 参数验证
        if (dto.getScenicSpotId() == null) {
            return ResponseUtils.error(400, "景点ID不能为空");
        }
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            return ResponseUtils.error(400, "标题不能为空");
        }
        
        ScenicMessageWallVO result = messageWallService.createOrUpdateScenicMessageWall(dto, loginUser);
        return ResponseUtils.success(result);
    }

    /**
     * 更新景点留言墙配置
     */
    @AuthCheck(mustRole = "admin")
    @PutMapping("/scenic/update")
    public ResponseDTO<ScenicMessageWallVO> updateScenicMessageWall(@RequestBody ScenicMessageWallDTO dto,
                                                                    HttpServletRequest request) {
        User loginUser = getLoginUser(request);
        
        // 参数验证
        if (dto.getScenicSpotId() == null) {
            return ResponseUtils.error(400, "景点ID不能为空");
        }
        if (dto.getTitle() == null || dto.getTitle().trim().isEmpty()) {
            return ResponseUtils.error(400, "标题不能为空");
        }
        
        ScenicMessageWallVO result = messageWallService.createOrUpdateScenicMessageWall(dto, loginUser);
        return ResponseUtils.success(result);
    }

    private User getLoginUser(HttpServletRequest request) {
        return userService.getLoginUser(request);
    }
}