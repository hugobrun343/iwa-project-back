package com.iwaproject.announcement.configs;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor to verify requests come from the Gateway.
 * Prevents direct access to the service bypassing the gateway.
 */
@Component
public class GatewaySecurityInterceptor implements HandlerInterceptor {

    /**
     * Expected gateway secret.
     */
    @Value("${gateway.secret:}")
    private String expectedSecret;

    /**
     * Pre-handle method to verify gateway secret.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param handler the handler
     * @return true if request is from gateway, false otherwise
     */
    @Override
    public boolean preHandle(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final Object handler) throws Exception {

        // Skip check if no secret configured (for tests/local dev)
        if (expectedSecret == null || expectedSecret.isEmpty()) {
            return true;
        }

        // Check gateway secret header
        String gatewaySecret = request.getHeader("X-Gateway-Secret");

        if (gatewaySecret == null
                || !gatewaySecret.equals(expectedSecret)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Forbidden\","
                    + "\"message\":\"Access denied\"}"
            );
            return false;
        }

        return true;
    }
}
