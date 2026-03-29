package com.lq.travel.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API限流注解
 * 
 * 使用示例：
 * <pre>
 * {@code
 * @ApiRateLimit(name = "aiChat")
 * public ResponseEntity<?> chat() {
 *     ...
 * }
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiRateLimit {
    
    /**
     * 限流器名称
     * 对应配置中的限流器
     */
    String name() default "default";
}


