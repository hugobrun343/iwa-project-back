package com.iwaproject.gateway.router;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.Map;

/**
 * Service Router for IWA Project Gateway.
 * Routes requests to backend microservices using a clean, maintainable approach.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ServiceRouter {

    private final RestTemplate restTemplate;

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

    private Map<String, String> services;

    /**
     * Initialize the services map with service names and URLs.
     * Similar to RouteLocatorBuilder pattern from reactive gateway.
     */
    @PostConstruct
    public void initializeRoutes() {
        services = Map.ofEntries(
                Map.entry("users", userServiceUrl),
                Map.entry("announcements", announcementServiceUrl),
                Map.entry("applications", applicationServiceUrl),
                Map.entry("favorites", favoriteServiceUrl),
                Map.entry("discussions", chatServiceUrl),
                Map.entry("ratings", ratingServiceUrl),
                Map.entry("payments", paymentServiceUrl)
        );
        
        log.info("Initialized gateway routes for {} services", services.size());
        services.forEach((name, url) -> 
            log.debug("  {} -> {}", name, url)
        );
    }

    /**
     * Universal route handler for all API requests.
     * Dynamically routes to appropriate microservice based on path.
     */
    @RequestMapping({
        "/api/users/**",
        "/api/languages",
        "/api/specialisations",
        "/api/announcements/**",
        "/api/applications",
        "/api/applications/**",
        "/api/favorites/**",
        "/api/discussions/**",
        "/api/me/discussions",
        "/api/ratings/**",
        "/api/payments/**",
        // SockJS HTTP fallback requests (for WebSocket proxy)
        "/ws/info",
        "/ws/**"
    })
    public ResponseEntity<byte[]> routeRequest(
            final HttpServletRequest request,
            @RequestBody(required = false) final byte[] body) {
        
        String path = request.getRequestURI();
        String serviceName = extractServiceName(path);
        String serviceUrl = services.get(serviceName);
        
        if (serviceUrl == null) {
            log.warn("No service found for path: {}", path);
            return ResponseEntity.notFound().build();
        }
        
        return proxyRequest(request, body, serviceUrl);
    }

    /**
     * Extract service name from request path.
     * Maps API paths to service names in the services map.
     */
    private String extractServiceName(final String path) {
        if (path.startsWith("/api/users/") || path.equals("/api/users")
                || path.equals("/api/languages") 
                || path.equals("/api/specialisations")) {
            return "users";
        } else if (path.startsWith("/api/announcements/") 
                || path.equals("/api/announcements")) {
            return "announcements";
        } else if (path.startsWith("/api/applications/") 
                || path.equals("/api/applications")) {
            return "applications";
        } else if (path.startsWith("/api/favorites/") 
                || path.equals("/api/favorites")) {
            return "favorites";
        } else if (path.startsWith("/api/discussions/") 
                || path.equals("/api/discussions")
                || path.equals("/api/me/discussions")) {
            return "discussions";
        } else if (path.startsWith("/api/ratings/") 
                || path.equals("/api/ratings")) {
            return "ratings";
        } else if (path.startsWith("/api/payments/") 
                || path.equals("/api/payments")) {
            return "payments";
        } else if (path.startsWith("/ws")) {
            // SockJS HTTP fallback requests go to Chat-Service
            return "discussions";
        }
        return null;
    }

    /**
     * Generic proxy method that forwards requests to target service.
     *
     * @param request the HTTP servlet request
     * @param body the request body (if any)
     * @param serviceUrl the base URL of the target service
     * @return response from the target service
     */
    private ResponseEntity<byte[]> proxyRequest(
            final HttpServletRequest request,
            final byte[] body,
            final String serviceUrl) {

        String path = request.getRequestURI();
        String targetUrl = serviceUrl + path;
        
        String queryString = request.getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            targetUrl = targetUrl + "?" + queryString;
        }

        HttpHeaders headers = copyHeaders(request);
        HttpEntity<byte[]> entity = new HttpEntity<>(body, headers);
        HttpMethod method = HttpMethod.valueOf(request.getMethod());

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    targetUrl, method, entity, byte[].class);
            return buildResponse(response);
        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            return buildErrorResponse(ex);
        }
    }

    /**
     * Copy headers from request, excluding host and content-length.
     */
    private HttpHeaders copyHeaders(final HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (!headerName.equalsIgnoreCase("host")
                    && !headerName.equalsIgnoreCase("content-length")) {
                headers.add(headerName, request.getHeader(headerName));
            }
        }
        
        return headers;
    }

    /**
     * Build response entity, excluding transfer-encoding
     * and content-length headers.
     */
    private ResponseEntity<byte[]> buildResponse(
            final ResponseEntity<byte[]> serviceResponse) {
        HttpHeaders outHeaders = new HttpHeaders();
        
        serviceResponse.getHeaders().forEach((name, values) -> {
            if (!name.equalsIgnoreCase("transfer-encoding")
                    && !name.equalsIgnoreCase("content-length")) {
                values.forEach(v -> outHeaders.add(name, v));
            }
        });

        return new ResponseEntity<>(
                serviceResponse.getBody(),
                outHeaders,
                serviceResponse.getStatusCode());
    }

    /**
     * Build error response from exception.
     */
    private ResponseEntity<byte[]> buildErrorResponse(
            final org.springframework.web.client.HttpStatusCodeException ex) {
        HttpHeaders outHeaders = new HttpHeaders();
        HttpHeaders exHeaders = ex.getResponseHeaders();
        
        if (exHeaders != null) {
            exHeaders.forEach((name, values) -> {
                if (!name.equalsIgnoreCase("transfer-encoding")
                        && !name.equalsIgnoreCase("content-length")) {
                    values.forEach(v -> outHeaders.add(name, v));
                }
            });
        }

        return new ResponseEntity<>(
                ex.getResponseBodyAsByteArray(),
                outHeaders,
                ex.getStatusCode());
    }
}
