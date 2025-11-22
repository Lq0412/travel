package com.lq.travel.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lq.travel.constant.UserConstant;
import com.lq.travel.exception.BusinessException;
import com.lq.travel.exception.ErrorCode;
import com.lq.travel.mapper.UserMapper;
import com.lq.travel.model.dto.user.UserQueryRequest;
import com.lq.travel.model.entity.User;
import com.lq.travel.model.enums.UserRoleEnum;
import com.lq.travel.model.vo.LoginUserVO;
import com.lq.travel.model.vo.UserVO;
import com.lq.travel.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 * 提供用户注册、登录、注销、查询等核心功能，包含密码加密和会话管理
 *
 * @author Lq304
 * @createDate 2025-08-20
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {

    /**
     * 用户注册
     * 验证用户输入参数，检查账号唯一性，加密密码并保存用户信息
     *
     * @param userAccount   用户账号
     * @param userPassword  用户密码
     * @param checkPassword 确认密码
     * @return 新创建的用户ID
     */
    @Override
    public Long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 参数校验
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }

        // 2. 检查用户账号是否重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_account", userAccount);
        long count = this.baseMapper.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号已存在");
        }
        
        // 3. 加密密码
        String encryptPassword = getEncryptPassword(userPassword);
        
        // 4. 创建并保存用户
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("无名");
        user.setUserRole(UserRoleEnum.USER.getValue());
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败");
        }
        return user.getId();
    }

    /**
     * 用户登录
     * 验证用户账号密码，成功后创建登录会话
     *
     * @param userAccount  用户账号
     * @param userPassword 用户密码
     * @param request      HTTP请求对象
     * @return 登录用户信息
     */
    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 参数校验
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        
        // 2. 加密密码
        String encryptPassword = getEncryptPassword(userPassword);
        
        // 3. 查询用户信息
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_account", userAccount);
        queryWrapper.eq("user_password", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        
        // 4. 验证用户是否存在
        if (user == null) {
            log.info("用户不存在");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        
        // 5. 保存用户登录状态到会话
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);

        return this.getLoginUserVO(user);
    }

    /**
     * 获取加密后的密码
     * 使用MD5加盐加密用户密码
     *
     * @param userPassword 原始密码
     * @return 加密后的密码
     */
    @Override
    public String getEncryptPassword(String userPassword) {
        final String SALT = "lqtravel";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

    /**
     * 获取脱敏后的用户信息
     * 将用户实体对象转换为VO对象，去除敏感信息
     *
     * @param user 用户实体对象
     * @return 脱敏后的用户信息
     */
    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    /**
     * 获取脱敏后的用户信息列表
     * 批量转换用户实体列表为VO列表
     *
     * @param userList 用户实体列表
     * @return 用户VO列表
     */
    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    /**
     * 获取登录用户信息（脱敏）
     * 将用户实体对象转换为登录用户VO对象
     *
     * @param user 用户实体对象
     * @return 登录用户信息
     */
    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * 获取当前登录用户
     * 从会话中获取当前登录用户，并验证用户状态
     *
     * @param request HTTP请求对象
     * @return 当前登录用户
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 1. 从会话中获取当前登录用户
        Object user = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) user;
        if (currentUser == null || currentUser.getId() == null) {
            return null;
        }
        
        // 2. 重新查询用户信息（确保数据最新）
        currentUser = this.getById(currentUser.getId());
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    /**
     * 用户注销
     * 清除当前用户的登录会话
     *
     * @param request HTTP请求对象
     * @return 注销操作结果
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 1. 检查是否已登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        
        // 2. 清除登录状态
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    /**
     * 构建用户查询条件
     * 根据查询请求参数构建MyBatis-Plus查询条件
     *
     * @param userQueryRequest 用户查询请求参数
     * @return 查询条件包装器
     */
    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        // 1. 参数校验
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        
        // 2. 提取查询参数
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        
        // 3. 构建查询条件
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "user_role", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "user_account", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "user_name", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "user_profile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 判断用户是否为管理员
     * 检查用户角色是否为管理员
     *
     * @param user 用户对象
     * @return 是否为管理员
     */
    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }
}




