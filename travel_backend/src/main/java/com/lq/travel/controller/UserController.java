package com.lq.travel.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lq.travel.common.ResponseDTO;
import com.lq.travel.AI.core.service.QuotaService;
import com.lq.travel.common.DeleteRequest;
import com.lq.travel.common.ResponseUtils;
import com.lq.travel.constant.UserConstant;
import com.lq.travel.exception.BusinessException;
import com.lq.travel.exception.ErrorCode;
import com.lq.travel.exception.ThrowUtils;
import com.lq.travel.model.dto.user.*;
import com.lq.travel.model.entity.User;
import com.lq.travel.model.vo.LoginUserVO;
import com.lq.travel.model.vo.UserVO;
import com.lq.travel.service.UserService;
import com.lq.travel.annotation.AuthCheck;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 用户管理接口
 * 提供用户注册、登录、注销、增删改查等功能
 *
 * @author Lq304
 * @createDate 2025-08-20
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;
    
    @Resource
    private QuotaService quotaService;
    

    // 1. 用户注册
    @PostMapping("/register")
//    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public ResponseDTO<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        // 1. 参数校验
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        
        // 2. 执行注册
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResponseUtils.success(result);
    }

    // 2. 用户登录
    @PostMapping("/login")
    public ResponseDTO<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        // 1. 参数校验
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        
        // 2. 执行登录
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResponseUtils.success(loginUserVO);
    }

    // 3. 获取当前登录用户信息
    @GetMapping("/get/login")
    public ResponseDTO<LoginUserVO> getLoginUser(HttpServletRequest request) {
        // 1. 获取登录用户
        User loginUserVO = userService.getLoginUser(request);
        
        // 2. 转换为封装类并返回
        return ResponseUtils.success(userService.getLoginUserVO(loginUserVO));
    }

    // 4. 用户注销
    @PostMapping("/logout")
    public ResponseDTO<Boolean> userLogout(HttpServletRequest request) {
        // 1. 参数校验
        ThrowUtils.throwIf(request == null, ErrorCode.OPERATION_ERROR);
        
        // 2. 执行注销
        boolean result = userService.userLogout(request);
        return ResponseUtils.success(result);
    }

    // 5. 创建用户（仅管理员）
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public ResponseDTO<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        // 1. 参数校验
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        
        // 2. DTO转实体对象
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        
        // 3. 设置默认密码
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        
        // 4. 保存用户
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResponseUtils.success(user.getId());
    }

    // 6. 根据ID获取用户详情（仅管理员）
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public ResponseDTO<User> getUserById(long id) {
        // 1. 参数校验
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        
        // 2. 查询用户信息
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResponseUtils.success(user);
    }

    // 7. 根据ID获取用户详情（封装类）
    @GetMapping("/get/vo")
    public ResponseDTO<UserVO> getUserVOById(long id) {
        // 1. 获取用户信息
        ResponseDTO<User> response = getUserById(id);
        User user = response.getData();
        
        // 2. 转换为封装类并返回
        return ResponseUtils.success(userService.getUserVO(user));
    }

    // 8. 删除用户（仅管理员）
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public ResponseDTO<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        // 1. 参数校验
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        
        // 2. 执行删除
        boolean b = userService.removeById(deleteRequest.getId());
        return ResponseUtils.success(b);
    }

    // 9. 更新用户信息（仅管理员）
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public ResponseDTO<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        // 1. 参数校验
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        
        // 2. DTO转实体对象
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        
        // 3. 执行更新
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResponseUtils.success(true);
    }

    // 10. 分页获取用户列表（仅管理员）
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public ResponseDTO<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        // 1. 参数校验
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        
        // 2. 提取分页参数
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
        
        // 3. 执行分页查询
        Page<User> userPage = userService.page(new Page<>(current, pageSize),
                userService.getQueryWrapper(userQueryRequest));
        
        // 4. 转换为封装类
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResponseUtils.success(userVOPage);
    }

    /**
     * 更新当前登录用户的基础信息
     */
    @PostMapping("/update/my")
    public ResponseDTO<Boolean> updateMyUser(@RequestBody UserUpdateMyRequest updateRequest,
                                             HttpServletRequest request) {
        if (updateRequest == null) {
            return ResponseUtils.error(400, "请求参数为空");
        }

        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            return ResponseUtils.error(401, "用户未登录");
        }

        User updateUser = new User();
        updateUser.setId(loginUser.getId());

        if (StringUtils.isNotBlank(updateRequest.getUserName())) {
            updateUser.setUserName(updateRequest.getUserName().trim());
        }
        if (updateRequest.getUserAvatar() != null) {
            updateUser.setUserAvatar(updateRequest.getUserAvatar());
        }
        if (updateRequest.getUserProfile() != null) {
            updateUser.setUserProfile(updateRequest.getUserProfile());
        }

        boolean result = userService.updateById(updateUser);
        if (!result) {
            return ResponseUtils.error(500, "更新失败");
        }

        // 刷新会话中的用户信息
        User refreshedUser = userService.getById(loginUser.getId());
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, refreshedUser);

        return ResponseUtils.success(true);
    }
    
    /**
     * Upload avatar and persist to current user
     * 简化版本：直接保存文件到服务器
     */
    @PostMapping("/avatar/upload")
    public ResponseDTO<String> uploadAvatar(@RequestPart("file") MultipartFile file, HttpServletRequest request) {
        if (file == null || file.isEmpty()) {
            return ResponseUtils.error(ErrorCode.PARAMS_ERROR, "请上传头像图片");
        }
        
        try {
            User loginUser = userService.getLoginUser(request);
            ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

            // 保存文件到本地目录
            String fileName = "avatar_" + loginUser.getId() + "_" + System.currentTimeMillis() + 
                    getFileExtension(file.getOriginalFilename());
            java.io.File uploadDir = new java.io.File("uploads/avatars");
            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }
            
            java.io.File destFile = new java.io.File(uploadDir, fileName);
            file.transferTo(destFile);
            
            String avatarUrl = "/uploads/avatars/" + fileName;
            
            User updateUser = new User();
            updateUser.setId(loginUser.getId());
            updateUser.setUserAvatar(avatarUrl);
            boolean updateResult = userService.updateById(updateUser);
            ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新头像失败");

            User refreshedUser = userService.getById(loginUser.getId());
            request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, refreshedUser);

            return ResponseUtils.success(avatarUrl);
        } catch (Exception e) {
            return ResponseUtils.error(ErrorCode.SYSTEM_ERROR, "上传头像失败: " + e.getMessage());
        }
    }
    
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return ".jpg";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    /**
     * 管理员重置用户配额
     */
    @PostMapping("/admin/reset-quota")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public ResponseDTO<Boolean> resetUserQuota(
            @RequestParam Long userId,
            @RequestParam(required = false, defaultValue = "10000") Integer quota) {
        
        if (userId == null) {
            return ResponseUtils.error(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }
        
        if (quota == null || quota < 0) {
            return ResponseUtils.error(ErrorCode.PARAMS_ERROR, "配额值必须大于等于0");
        }
        
        quotaService.resetQuota(userId, quota);
        return ResponseUtils.success("配额重置成功", true);
    }
    
    /**
     * 管理员充值用户配额
     */
    @PostMapping("/admin/recharge-quota")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public ResponseDTO<Boolean> rechargeUserQuota(
            @RequestParam Long userId,
            @RequestParam Integer tokens) {
        
        if (userId == null) {
            return ResponseUtils.error(ErrorCode.PARAMS_ERROR, "用户ID不能为空");
        }
        
        if (tokens == null || tokens <= 0) {
            return ResponseUtils.error(ErrorCode.PARAMS_ERROR, "充值数量必须大于0");
        }
        
        quotaService.rechargeQuota(userId, tokens);
        return ResponseUtils.success("充值成功", true);
    }
    
    /**
     * 查询用户剩余配额
     */
    @GetMapping("/quota")
    public ResponseDTO<Integer> getUserQuota(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (loginUser == null) {
            return ResponseUtils.error(ErrorCode.NOT_LOGIN_ERROR);
        }
        
        Integer remaining = quotaService.getRemainingQuota(loginUser.getId());
        return ResponseUtils.success(remaining);
    }
}

