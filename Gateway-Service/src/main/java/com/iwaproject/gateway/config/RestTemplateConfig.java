package com.iwaproject.gateway.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration for RestTemplate (DEPRECATED).
 * This class is disabled in favor of reactive WebClient.
 * Only active with 'mvc' profile (not used anymore).
 */
@Configuration
@Profile("mvc")
@Deprecated
public class RestTemplateConfig {
    // This class is no longer used with Spring Cloud Gateway
    // Spring Cloud Gateway uses WebClient instead of RestTemplate
}
