package com.iwaproject.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Cloud Gateway configuration.
 * Defines routing rules and filters for microservices.
 */
@Configuration
public class GatewayConfig {

  @Value("${USER_SERVICE_URL:http://localhost:8081}")
  private String userServiceUrl;

  @Value("${ANNOUNCEMENT_SERVICE_URL:http://localhost:8082}")
  private String announcementServiceUrl;

  @Value("${APPLICATION_SERVICE_URL:http://localhost:8083}")
  private String applicationServiceUrl;

  @Value("${FAVORITE_SERVICE_URL:http://localhost:8084}")
  private String favoriteServiceUrl;

  @Value("${CHAT_SERVICE_URL:http://localhost:8085}")
  private String chatServiceUrl;

  @Value("${RATING_SERVICE_URL:http://localhost:8086}")
  private String ratingServiceUrl;

  @Value("${PAYMENT_SERVICE_URL:http://localhost:8087}")
  private String paymentServiceUrl;

  /**
   * Configure gateway routes programmatically.
   *
   * @param builder the route locator builder
   * @return configured RouteLocator
   */
  @Bean
  public RouteLocator gatewayRoutes(final RouteLocatorBuilder builder) {
    return builder.routes()
            // User Service routes
            .route("user-service", r -> r
                    .path("/api/users/**", "/api/languages", "/api/specialisations")
                    .uri(userServiceUrl))

            // Announcement Service routes
            .route("announcement-service", r -> r
                    .path("/api/announcements/**")
                    .uri(announcementServiceUrl))

            // Application Service routes
            .route("application-service", r -> r
                    .path("/api/applications/**")
                    .uri(applicationServiceUrl))

            // Favorite Service routes
            .route("favorite-service", r -> r
                    .path("/api/favorites/**")
                    .uri(favoriteServiceUrl))

            // Chat/Discussion Service routes
            .route("chat-service", r -> r
                    .path("/api/discussions/**", "/api/me/discussions", "/api/messages/**")
                    .uri(chatServiceUrl))

            // Rating Service routes
            .route("rating-service", r -> r
                    .path("/api/ratings/**")
                    .uri(ratingServiceUrl))

            // Payment Service routes
            .route("payment-service", r -> r
                    .path("/api/payments/**")
                    .uri(paymentServiceUrl))

            .build();
  }
}

