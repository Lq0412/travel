package com.lq.travel.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置类
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")           // 1. 配置路径
                .allowCredentials(true)      // 2. 允许凭证
                .allowedOriginPatterns("*")  // 3. 允许的源
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 4. 允许的方法
                .allowedHeaders("*")         // 5. 允许的请求头
                .exposedHeaders("*");        // 6. 暴露的响应头
    }
}
