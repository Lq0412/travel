/*
 * File: .\src\main\java\com\lq\common\annotation\AuthCheck.java
 * Description: Auto-generated file header comment.
 * Created: 2025-10-17
 */

package com.lq.travel.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限校验
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)

public @interface AuthCheck {

    /**
     * 必须有某个角色
     */
    String mustRole() default "";

}
