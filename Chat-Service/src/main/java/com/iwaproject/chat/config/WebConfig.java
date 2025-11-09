package com.iwaproject.chat.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for Chat Service.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    /**
     * Gateway security interceptor.
     */
    private final GatewaySecurityInterceptor gatewaySecurityInterceptor;

    /**
     * Add interceptors.
     *
     * @param registry interceptor registry
     */
    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(gatewaySecurityInterceptor)
                .addPathPatterns("/api/**");
    }

    /**
     * Allow CORS for direct SockJS/WebSocket requests to the chat service.
     * This supports local testing from file:// or other null origins.
     */
    @Override
    public void addCorsMappings(final CorsRegistry registry) {
        registry.addMapping("/ws/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}

