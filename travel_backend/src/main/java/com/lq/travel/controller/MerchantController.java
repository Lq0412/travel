package com.lq.travel.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lq.travel.AI.core.model.dto.ResponseDTO;
import com.lq.travel.common.DeleteRequest;
import com.lq.travel.common.ResponseUtils;
import com.lq.travel.constant.UserConstant;
import com.lq.travel.annotation.AuthCheck;
import com.lq.travel.model.dto.merchant.MerchantQueryRequest;
import com.lq.travel.model.entity.Merchant;
import com.lq.travel.model.entity.Product;
import com.lq.travel.service.MerchantService;
import com.lq.travel.service.UserService;
import com.lq.travel.exception.BusinessException;
import com.lq.travel.exception.ErrorCode;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import javax.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/merchant")
public class MerchantController {

    @Resource
    private MerchantService merchantService;

    @Resource
    private UserService userService;

    // 1. 分页获取商家列表
    @GetMapping("/list")
    public ResponseDTO<IPage<Merchant>> getMerchantList(
            @Valid MerchantQueryRequest queryRequest) {
        return ResponseUtils.success(merchantService.getMerchantList(queryRequest));
    }

    // 2. 获取商家详情
    @GetMapping("/detail/{merchantId}")
    public ResponseDTO<Merchant> getMerchantDetail(
            @PathVariable Long merchantId) {
        return ResponseUtils.success(merchantService.getMerchantDetail(merchantId));
    }

    // 3. 获取商家商品列表
    @GetMapping("/{merchantId}/products")
    public ResponseDTO<IPage<Product>> getMerchantProducts(
            @PathVariable Long merchantId,
            @Valid MerchantQueryRequest queryRequest) {
        return ResponseUtils.success(merchantService.getMerchantProducts(queryRequest, merchantId));
    }

    // 4. 商家推荐接口
    @GetMapping("/recommended")
    public ResponseDTO<List<Merchant>> getRecommendedMerchants(
            @RequestParam(defaultValue = "10") Integer limit) {
        return ResponseUtils.success(merchantService.getRecommendedMerchants(limit));
    }

    // 5. 商家搜索
    @PostMapping("/search")
    public ResponseDTO<IPage<Merchant>> searchMerchants(
            @RequestBody @Valid MerchantQueryRequest queryRequest) {
        return ResponseUtils.success(merchantService.searchMerchants(queryRequest));
    }

    // ================= 管理端 CRUD =================

    // A1. 新增商家（管理员）
    @PostMapping("/admin/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public ResponseDTO<Long> adminAddMerchant(@RequestBody Merchant merchant, HttpServletRequest request) {
        log.info("接收到新增商家请求，原始userId: {}, name: {}", merchant.getUserId(), merchant.getName());
        log.info("userId类型: {}", merchant.getUserId() != null ? merchant.getUserId().getClass().getName() : "null");
        
        // 填充创建者用户ID，避免 user_id 为空
        // 注意：前端可能传递字符串格式的 userId（避免 JavaScript 大整数精度丢失）
        // Spring Boot 的 Jackson 会自动将字符串转换为 Long
        if (merchant.getUserId() == null || merchant.getUserId() == 0) {
            Long loginUserId = userService.getLoginUser(request).getId();
            merchant.setUserId(loginUserId);
            log.info("userId 为空或0，使用当前登录管理员ID: {}", loginUserId);
        }
        
        // 无论 userId 是管理员选择的还是自动填充的，都需要更新用户角色
        Long targetUserId = merchant.getUserId();
        if (targetUserId != null && targetUserId > 0) {
            com.lq.travel.model.entity.User user = userService.getById(targetUserId);
            if (user != null) {
                // 检查用户是否已经是商家角色（避免重复更新）
                if (!UserConstant.MERCHANT_ROLE.equals(user.getUserRole())) {
                    user.setUserRole(UserConstant.MERCHANT_ROLE);
                    boolean updated = userService.updateById(user);
                    if (updated) {
                        log.info("成功更新用户角色为商家，用户ID: {}, 用户名: {}", targetUserId, user.getUserName());
                    } else {
                        log.warn("更新用户角色失败，用户ID: {}", targetUserId);
                    }
                } else {
                    log.info("用户已经是商家角色，用户ID: {}", targetUserId);
                }
            } else {
                log.error("用户不存在，无法更新角色，用户ID: {}", targetUserId);
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "关联的用户不存在，用户ID: " + targetUserId);
            }
        } else {
            log.warn("userId 无效: {}", targetUserId);
        }
        
        // 确保 type 字段有值（如果为 null，设置默认值为 1-美食）
        if (merchant.getType() == null) {
            merchant.setType(1);
            log.info("商家类型为空，设置为默认值 1（美食）");
        }
        
        boolean saved = merchantService.save(merchant);
        if (saved) {
            log.info("商家创建成功，商家ID: {}, userId: {}, name: {}", merchant.getId(), merchant.getUserId(), merchant.getName());
        } else {
            log.error("商家创建失败，name: {}", merchant.getName());
        }
        return ResponseUtils.success(saved ? merchant.getId() : 0L);
    }

    // A2. 更新商家（管理员）
    @PostMapping("/admin/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public ResponseDTO<Boolean> adminUpdateMerchant(@RequestBody Merchant merchant) {
        // 如果更新了 userId，需要同步更新用户角色
        if (merchant.getId() != null && merchant.getUserId() != null) {
            // 获取旧的商家记录
            Merchant oldMerchant = merchantService.getById(merchant.getId());
            if (oldMerchant != null && !oldMerchant.getUserId().equals(merchant.getUserId())) {
                // userId 发生了变化，需要更新用户角色
                // 1. 将新 userId 的用户角色更新为 merchant
                com.lq.travel.model.entity.User newUser = userService.getById(merchant.getUserId());
                if (newUser != null) {
                    newUser.setUserRole(UserConstant.MERCHANT_ROLE);
                    userService.updateById(newUser);
                    log.info("更新用户角色为商家，新用户ID: {}", merchant.getUserId());
                }
                
                // 2. 可选：将旧 userId 的用户角色恢复（如果该用户没有其他商家记录）
                // 注意：这里暂时不恢复旧用户角色，因为可能有其他商家记录关联
                log.info("商家关联用户已更改，从用户ID: {} 更改为: {}", oldMerchant.getUserId(), merchant.getUserId());
            }
        }
        
        return ResponseUtils.success(merchantService.updateById(merchant));
    }

    // A3. 删除商家（管理员）
    @PostMapping("/admin/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public ResponseDTO<Boolean> adminDeleteMerchant(@RequestBody DeleteRequest deleteRequest) {
        return ResponseUtils.success(merchantService.removeById(deleteRequest.getId()));
    }

    // A4. 获取商家详情（管理员）
    @GetMapping("/admin/get/{id}")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public ResponseDTO<Merchant> adminGetMerchant(@PathVariable Long id) {
        return ResponseUtils.success(merchantService.getById(id));
    }

    // A5. 分页列表（管理员）
    @PostMapping("/admin/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public ResponseDTO<IPage<Merchant>> adminListMerchants(@RequestBody @Valid MerchantQueryRequest queryRequest) {
        return ResponseUtils.success(merchantService.adminListMerchants(queryRequest));
    }

    // ================= 商家端接口 =================

    // M1. 获取当前商家的商家信息
    @GetMapping("/get/my")
    @AuthCheck(mustRole = UserConstant.MERCHANT_ROLE)
    public ResponseDTO<Merchant> getMyMerchant(HttpServletRequest request) {
        com.lq.travel.model.entity.User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new com.lq.travel.exception.BusinessException(
                com.lq.travel.exception.ErrorCode.NOT_LOGIN_ERROR, "用户未登录"
            );
        }
        
        Merchant merchant = merchantService.getCurrentMerchantByUserId(loginUser.getId());
        return ResponseUtils.success(merchant);
    }

    // M2. 更新当前商家的商家信息
    @PostMapping("/update/my")
    @AuthCheck(mustRole = UserConstant.MERCHANT_ROLE)
    public ResponseDTO<Boolean> updateMyMerchant(@RequestBody Merchant merchant, HttpServletRequest request) {
        com.lq.travel.model.entity.User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            throw new com.lq.travel.exception.BusinessException(
                com.lq.travel.exception.ErrorCode.NOT_LOGIN_ERROR, "用户未登录"
            );
        }
        
        // 获取当前商家的商家记录
        Merchant existingMerchant = merchantService.getCurrentMerchantByUserId(loginUser.getId());
        
        // 确保只能更新自己的商家信息
        merchant.setId(existingMerchant.getId());
        merchant.setUserId(loginUser.getId()); // 防止修改userId
        
        return ResponseUtils.success(merchantService.updateById(merchant));
    }
}