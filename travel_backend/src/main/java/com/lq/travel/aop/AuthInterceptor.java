/*
 * File: .\src\main\java\com\lq\common\aop\AuthInterceptor.java
 * Description: Auto-generated file header comment.
 * Created: 2025-10-17
 */

package com.lq.travel.aop;


import com.lq.travel.annotation.AuthCheck;
import com.lq.travel.exception.BusinessException;
import com.lq.travel.exception.ErrorCode;
import com.lq.travel.model.entity.User;
import com.lq.travel.model.enums.UserRoleEnum;
import com.lq.travel.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 权限校验
 */
@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    /**
     * 执行拦截
     *
     * @param joinPoint 切入点
     * @param authCheck 权限校验注解
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();

        // 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 如果用户未登录，直接拒绝
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "用户未登录");
        }
        
        // 将登录用户放到 request attribute 中，供 Controller 使用
        request.setAttribute("loginUser", loginUser);

        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        // 不需要权限，放行
        if (mustRoleEnum == null) {
            return joinPoint.proceed();
        }

        // 获取当前用户具有的权限
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(loginUser.getUserRole());
        // 没有权限，拒绝
        if (userRoleEnum == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 1. 要求必须有管理员权限，但用户没有管理员权限，拒绝
        if (UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(userRoleEnum)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "需要管理员权限");
        }

        // 2. 如果要求商家，但用户角色不是商家或管理员，拒绝
        // (通常管理员拥有所有权限，所以这里允许管理员通过)
        if (UserRoleEnum.MERCHANT.equals(mustRoleEnum) &&
                !(UserRoleEnum.MERCHANT.equals(userRoleEnum) || UserRoleEnum.ADMIN.equals(userRoleEnum))) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "需要商家权限");
        }

        // 通过权限校验，放行
        return joinPoint.proceed();
    }
}
