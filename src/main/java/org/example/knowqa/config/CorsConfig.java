package org.example.knowqa.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 全局跨域配置
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 拦截所有的请求
                // Spring Boot 3.x 中，如果 allowCredentials 为 true，不能直接使用 "*"
                // 必须使用 allowedOriginPatterns
                .allowedOriginPatterns("*") 
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD")
                .allowedHeaders("*")
                .allowCredentials(true) // 是否允许携带 Cookie/Authorization 等凭证
                .maxAge(3600); // 预检请求（OPTIONS）的缓存时间，单位秒
    }
}