package com.lq.travel.controller;

import com.lq.travel.model.entity.AIConversation;
import com.lq.travel.model.entity.AIMessage;
import com.lq.travel.model.vo.AIConversationVO;
import com.lq.travel.common.ResponseDTO;
import com.lq.travel.service.AIConversationService;
import com.lq.travel.service.AIMessageService;
import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@RestController
@RequestMapping("/ai/conversations")
@Slf4j
@Validated
public class ConversationController {

    @Resource
    private AIConversationService conversationService;

    @Resource
    private AIMessageService messageService;

    /**
     * 1. 创建新会话
     */
    @PostMapping
    public ResponseEntity<ResponseDTO<AIConversation>> createConversation(
            @RequestParam @NotNull(message = "用户ID不能为空") Long userId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false, defaultValue = "dashscope") String provider,
            @RequestParam(required = false, defaultValue = "qwen-plus") String model) {
        try {
            log.info("用户 {} 创建新会话", userId);
            AIConversation conversation = conversationService.createConversation(userId, title, provider, model);
            return ResponseEntity.ok(ResponseDTO.success("会话创建成功", conversation));
        } catch (Exception e) {
            log.error("创建会话失败", e);
            return ResponseEntity.ok(ResponseDTO.error("创建会话失败: " + e.getMessage()));
        }
    }

    /**
     * 2. 查询用户所有会话列表
     */
    @GetMapping
    public ResponseEntity<ResponseDTO<List<AIConversationVO>>> getUserConversations(
            @RequestParam @NotNull(message = "用户ID不能为空") Long userId,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码不能小于1") int pageNum,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "页大小不能小于1") int pageSize) {
        try {
            log.info("查询用户 {} 的会话列表，页码: {}, 页大小: {}", userId, pageNum, pageSize);
            List<AIConversationVO> conversations = conversationService.getUserConversations(userId, pageNum, pageSize);
            return ResponseEntity.ok(ResponseDTO.success("查询会话列表成功", conversations));
        } catch (Exception e) {
            log.error("查询用户 {} 会话列表失败", userId, e);
            return ResponseEntity.ok(ResponseDTO.error("查询会话列表失败: " + e.getMessage()));
        }
    }

    /**
     * 3. 查询单个会话详情
     */
    @GetMapping("/{conversationId}")
    public ResponseEntity<ResponseDTO<AIConversation>> getConversation(
            @PathVariable @NotNull(message = "会话ID不能为空") Long conversationId,
            @RequestParam @NotNull(message = "用户ID不能为空") Long userId) {
        try {
            log.info("查询会话详情: conversationId={}, userId={}", conversationId, userId);
            if (!conversationService.isConversationOwner(conversationId, userId)) {
                return ResponseEntity.ok(ResponseDTO.error("无权访问该会话或会话不存在"));
            }
            AIConversation conversation = conversationService.getConversationById(conversationId);
            return ResponseEntity.ok(ResponseDTO.success("查询会话详情成功", conversation));
        } catch (Exception e) {
            log.error("查询会话详情失败: conversationId={}, userId={}", conversationId, userId, e);
            return ResponseEntity.ok(ResponseDTO.error("查询会话详情失败: " + e.getMessage()));
        }
    }

    /**
     * 4. 查询单个会话的消息历史
     */
    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<ResponseDTO<List<AIMessage>>> getConversationMessages(
            @PathVariable @NotNull(message = "会话ID不能为空") Long conversationId,
            @RequestParam @NotNull(message = "用户ID不能为空") Long userId) {
        try {
            log.info("查询会话消息历史: conversationId={}, userId={}", conversationId, userId);
            if (!conversationService.isConversationOwner(conversationId, userId)) {
                return ResponseEntity.ok(ResponseDTO.error("无权访问该会话或会话不存在"));
            }
            List<AIMessage> messages = messageService.getConversationMessages(conversationId);
            return ResponseEntity.ok(ResponseDTO.success("查询消息历史成功", messages));
        } catch (Exception e) {
            log.error("查询会话消息历史失败: conversationId={}, userId={}", conversationId, userId, e);
            return ResponseEntity.ok(ResponseDTO.error("查询消息历史失败: " + e.getMessage()));
        }
    }

    /**
     * 5. 更新会话标题
     */
    @PutMapping("/{conversationId}/title")
    public ResponseEntity<ResponseDTO<Boolean>> updateConversationTitle(
            @PathVariable @NotNull(message = "会话ID不能为空") Long conversationId,
            @RequestParam @NotNull(message = "用户ID不能为空") Long userId,
            @RequestParam @NotNull(message = "新标题不能为空") String title) {
        try {
            log.info("更新会话标题: conversationId={}, userId={}, newTitle={}", conversationId, userId, title);
            if (!conversationService.isConversationOwner(conversationId, userId)) {
                return ResponseEntity.ok(ResponseDTO.error("无权修改该会话或会话不存在"));
            }
            boolean success = conversationService.updateConversationTitle(conversationId, title);
            if (success) {
                return ResponseEntity.ok(ResponseDTO.success("会话标题更新成功", true));
            } else {
                return ResponseEntity.ok(ResponseDTO.error("会话标题更新失败，请检查会话ID"));
            }
        } catch (Exception e) {
            log.error("更新会话标题失败: conversationId={}, userId={}", conversationId, userId, e);
            return ResponseEntity.ok(ResponseDTO.error("更新会话标题失败: " + e.getMessage()));
        }
    }

    /**
     * 6. 删除会话及其所有消息
     */
    @DeleteMapping("/{conversationId}")
    public ResponseEntity<ResponseDTO<Boolean>> deleteConversation(
            @PathVariable @NotNull(message = "会话ID不能为空") Long conversationId,
            @RequestParam @NotNull(message = "用户ID不能为空") Long userId) {
        try {
            log.info("删除会话及其消息: conversationId={}, userId={}", conversationId, userId);
            if (!conversationService.isConversationOwner(conversationId, userId)) {
                return ResponseEntity.ok(ResponseDTO.error("无权删除该会话或会话不存在"));
            }
            boolean success = conversationService.deleteConversationWithMessages(conversationId);
            if (success) {
                return ResponseEntity.ok(ResponseDTO.success("会话及其消息删除成功", true));
            } else {
                return ResponseEntity.ok(ResponseDTO.error("会话及其消息删除失败，请检查会话ID"));
            }
        } catch (Exception e) {
            log.error("删除会话及其消息失败: conversationId={}, userId={}", conversationId, userId, e);
            return ResponseEntity.ok(ResponseDTO.error("删除会话及其消息失败: " + e.getMessage()));
        }
    }

    /**
     * 7. 查询会话的最新N条消息
     */
    @GetMapping("/{conversationId}/messages/recent")
    public ResponseEntity<ResponseDTO<List<AIMessage>>> getRecentMessages(
            @PathVariable @NotNull(message = "会话ID不能为空") Long conversationId,
            @RequestParam @NotNull(message = "用户ID不能为空") Long userId,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "限制数量不能小于1") int limit) {
        try {
            log.info("查询会话最新 {} 条消息: conversationId={}, userId={}", limit, conversationId, userId);
            if (!conversationService.isConversationOwner(conversationId, userId)) {
                return ResponseEntity.ok(ResponseDTO.error("无权访问该会话或会话不存在"));
            }
            List<AIMessage> messages = messageService.getRecentMessages(conversationId, limit);
            return ResponseEntity.ok(ResponseDTO.success("查询最新消息成功", messages));
        } catch (Exception e) {
            log.error("查询会话最新消息失败: conversationId={}, userId={}", conversationId, userId, e);
            return ResponseEntity.ok(ResponseDTO.error("查询最新消息失败: " + e.getMessage()));
        }
    }
}

