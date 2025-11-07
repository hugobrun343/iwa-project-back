package com.iwaproject.gateway.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Test security configuration that disables authentication.
 * Only active with 'test' profile.
 */
@TestConfiguration
@EnableWebFluxSecurity
@Profile("test")
public class TestSecurityConfig {

  /**
   * Configure security to permit all requests in test environment.
   *
   * @param http the ServerHttpSecurity to modify
   * @return the configured SecurityWebFilterChain
   */
  @Bean
  public SecurityWebFilterChain testSecurityWebFilterChain(
      final ServerHttpSecurity http) {

    http
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .authorizeExchange(auth -> auth
            .anyExchange().permitAll()
        );

    return http.build();
  }
}

