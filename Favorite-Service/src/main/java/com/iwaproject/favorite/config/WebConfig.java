package com.iwaproject.favorite.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for Favorite Service.
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
}
