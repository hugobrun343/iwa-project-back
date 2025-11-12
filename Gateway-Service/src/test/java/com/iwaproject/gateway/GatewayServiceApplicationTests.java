package com.iwaproject.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.reactive.ReactiveOAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Gateway Service Application Tests.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.cloud.gateway.server.webflux.enabled=true"
    }
)
@ImportAutoConfiguration(exclude = {ReactiveOAuth2ResourceServerAutoConfiguration.class})
@ActiveProfiles("test")
class GatewayServiceApplicationTests {

  @Test
  void contextLoads() {
    // Test that the application context loads successfully
  }

}
