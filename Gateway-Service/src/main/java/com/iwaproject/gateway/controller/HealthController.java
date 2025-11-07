package com.iwaproject.gateway.controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health check controller for Gateway Service.
 */
@Slf4j
@RestController
public final class HealthController {

    /**
     * Health check endpoint.
     *
     * @return Map containing health status information
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        log.debug("Health check requested");
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Gateway Service");
        response.put("timestamp", LocalDateTime.now());
        response.put("version", "1.0.0");
        response.put("message", "Gateway is running successfully!");
        log.info("Health check completed - Status: UP");
        return response;
    }

    /**
     * Test deployment endpoint.
     *
     * @return Map containing test deployment information
     */
    @GetMapping("/test")
    public Map<String, Object> testdecon() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Hello World !!!!");
        response.put("timestamp", LocalDateTime.now());
        response.put("deployment", "Success");
        return response;
    }
}
