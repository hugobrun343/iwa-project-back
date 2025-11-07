package com.iwaproject.gateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Web MVC configuration for Gateway (DEPRECATED).
 * This class is disabled in favor of reactive WebFlux configuration.
 * Only active with 'mvc' profile (not used anymore).
 */
@Configuration
@Profile("mvc")
@Deprecated
public class WebMvcConfig {
    // This class is no longer used with Spring Cloud Gateway
    // Spring Cloud Gateway uses WebFlux, not MVC
}
