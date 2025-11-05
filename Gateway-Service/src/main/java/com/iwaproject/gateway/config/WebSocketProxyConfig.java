package com.iwaproject.gateway.config;

import com.iwaproject.gateway.websocket.ChatWebSocketProxyHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket proxy configuration for Gateway.
 * Proxies WebSocket connections to Chat-Service.
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketProxyConfig implements WebSocketConfigurer {

    @Value("${CHAT_SERVICE_URL:http://localhost:8085}")
    private String chatServiceUrl;

    private final ChatWebSocketProxyHandler chatWebSocketProxyHandler;

    /**
     * Register WebSocket handlers.
     *
     * @param registry WebSocket handler registry
     */
    @Override
    public void registerWebSocketHandlers(final WebSocketHandlerRegistry registry) {
        // Proxy WebSocket connections from /ws to Chat-Service
        registry.addHandler(chatWebSocketProxyHandler, "/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}

