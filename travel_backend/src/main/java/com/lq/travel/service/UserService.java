package com.lq.travel.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.lq.travel.model.dto.user.UserQueryRequest;
import com.lq.travel.model.entity.User;
import com.lq.travel.model.vo.LoginUserVO;
import com.lq.travel.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * @author Lq304
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2025-08-20 15:55:36
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     */
    Long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取加密后的密码
     */
    String getEncryptPassword(String userPassword);

    /**
     * 获得脱敏后的用户信息
     */
    UserVO getUserVO(User user);

    /**
     * 获得脱敏后的用户信息列表
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 获取脱敏的登录用户信息
     *
     * @param user 用户对象
     * @return 脱敏后的登录用户信息
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取当前的登录用户信息
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 注销登录
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取查询条件
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 是否为管理员
     *
     * @param user
     * @return
     */
    boolean isAdmin(User user);

}
