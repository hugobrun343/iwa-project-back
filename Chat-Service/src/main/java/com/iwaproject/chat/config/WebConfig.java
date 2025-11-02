package com.iwaproject.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC configuration.
 * Registers security interceptors.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Gateway security interceptor.
     */
    private final GatewaySecurityInterceptor gatewaySecurityInterceptor;

    /**
     * Constructor for WebConfig.
     *
     * @param interceptor the gateway security interceptor
     */
    public WebConfig(final GatewaySecurityInterceptor interceptor) {
        this.gatewaySecurityInterceptor = interceptor;
    }

    /**
     * Add interceptors to the registry.
     *
     * @param registry the interceptor registry
     */
    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(gatewaySecurityInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/actuator/**");
    }
}

