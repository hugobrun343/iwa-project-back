package com.iwaproject.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for real-time chat messaging.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Configure message broker.
     *
     * @param config message broker registry
     */
    @Override
    public void configureMessageBroker(final MessageBrokerRegistry config) {
        // Enable a simple in-memory message broker to carry messages
        // back to the client on destinations prefixed with "/topic"
        config.enableSimpleBroker("/topic");
        // Prefix for messages bound to methods annotated with @MessageMapping
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * Register STOMP endpoints.
     *
     * @param registry STOMP endpoint registry
     */
    @Override
    public void registerStompEndpoints(final StompEndpointRegistry registry) {
        // Register "/ws" endpoint for WebSocket connections
        // The discussion ID will be passed in the destination path when sending messages
        // CORS is handled by the Gateway - do not configure origins here
        // to avoid duplicate CORS headers
        registry.addEndpoint("/ws")
                .withSockJS();
    }
}

