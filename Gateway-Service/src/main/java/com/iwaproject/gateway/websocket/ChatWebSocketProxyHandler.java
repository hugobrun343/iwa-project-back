package com.iwaproject.gateway.websocket;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

/**
 * WebSocket proxy handler that forwards connections to Chat-Service.
 */
@Slf4j
@Component
public class ChatWebSocketProxyHandler implements WebSocketHandler {

    @Value("${CHAT_SERVICE_URL:http://localhost:8085}")
    private String chatServiceUrl;

    private final WebSocketClient webSocketClient = new StandardWebSocketClient();
    private final ConcurrentHashMap<String, WebSocketSession> clientSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, WebSocketSession> backendSessions = new ConcurrentHashMap<>();

    /**
     * Handle WebSocket connection opened.
     *
     * @param session WebSocket session
     */
    @Override
    public void afterConnectionEstablished(final WebSocketSession session) {
        log.info("WebSocket connection established: {}", session.getId());
        clientSessions.put(session.getId(), session);

        try {
            // Convert HTTP URL to WebSocket URL
            String wsUrl = chatServiceUrl
                    .replace("http://", "ws://")
                    .replace("https://", "wss://")
                    + "/ws";

            // Create backend WebSocket connection using Spring WebSocket Client
            WebSocketSession backendSession = webSocketClient.doHandshake(
                    new BackendWebSocketHandler(session),
                    null,
                    URI.create(wsUrl)
            ).get();

            backendSessions.put(session.getId(), backendSession);
            log.info("Backend WebSocket connection established for session: {}", session.getId());

        } catch (InterruptedException | ExecutionException e) {
            log.error("Error establishing backend WebSocket connection: {}", e.getMessage(), e);
            try {
                session.close(CloseStatus.SERVER_ERROR);
            } catch (IOException ex) {
                log.error("Error closing client session: {}", ex.getMessage(), ex);
            }
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Handle WebSocket message received from client.
     *
     * @param session WebSocket session
     * @param message WebSocket message
     */
    @Override
    public void handleMessage(final WebSocketSession session, final WebSocketMessage<?> message) {
        WebSocketSession backendSession = backendSessions.get(session.getId());
        if (backendSession != null && backendSession.isOpen()) {
            try {
                String payload = message.getPayload().toString();
                backendSession.sendMessage(new TextMessage(payload));
            } catch (IOException e) {
                log.error("Error forwarding message to backend: {}", e.getMessage(), e);
            }
        } else {
            log.warn("Backend WebSocket not available for session: {}", session.getId());
        }
    }

    /**
     * Handle transport error.
     *
     * @param session WebSocket session
     * @param exception exception
     */
    @Override
    public void handleTransportError(final WebSocketSession session, final Throwable exception) {
        log.error("WebSocket transport error for session {}: {}", 
                session.getId(), exception.getMessage(), exception);
        closeBackendConnection(session.getId());
    }

    /**
     * Handle WebSocket connection closed.
     *
     * @param session WebSocket session
     * @param closeStatus close status
     */
    @Override
    public void afterConnectionClosed(final WebSocketSession session, final CloseStatus closeStatus) {
        log.info("WebSocket connection closed: {} - {}", session.getId(), closeStatus);
        clientSessions.remove(session.getId());
        closeBackendConnection(session.getId());
    }

    /**
     * Check if handler supports partial messages.
     *
     * @return false
     */
    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    /**
     * Close backend WebSocket connection.
     *
     * @param sessionId session ID
     */
    private void closeBackendConnection(final String sessionId) {
        WebSocketSession backendSession = backendSessions.remove(sessionId);
        if (backendSession != null && backendSession.isOpen()) {
            try {
                backendSession.close(CloseStatus.NORMAL);
            } catch (IOException e) {
                log.error("Error closing backend session: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Backend WebSocket handler that forwards messages from backend to client.
     */
    private class BackendWebSocketHandler implements WebSocketHandler {
        private final WebSocketSession clientSession;

        BackendWebSocketHandler(final WebSocketSession clientSession) {
            this.clientSession = clientSession;
        }

        @Override
        public void afterConnectionEstablished(final WebSocketSession session) {
            // Connection already established
        }

        @Override
        public void handleMessage(final WebSocketSession session, final WebSocketMessage<?> message) {
            try {
                if (clientSession.isOpen()) {
                    clientSession.sendMessage(message);
                }
            } catch (IOException e) {
                log.error("Error forwarding message to client: {}", e.getMessage(), e);
            }
        }

        @Override
        public void handleTransportError(final WebSocketSession session, final Throwable exception) {
            log.error("Backend WebSocket transport error: {}", exception.getMessage(), exception);
            try {
                if (clientSession.isOpen()) {
                    clientSession.close(CloseStatus.SERVER_ERROR);
                }
            } catch (IOException e) {
                log.error("Error closing client session: {}", e.getMessage(), e);
            }
        }

        @Override
        public void afterConnectionClosed(final WebSocketSession session, final CloseStatus closeStatus) {
            log.info("Backend WebSocket connection closed: {}", closeStatus);
            try {
                if (clientSession.isOpen()) {
                    clientSession.close(CloseStatus.NORMAL);
                }
            } catch (IOException e) {
                log.error("Error closing client session: {}", e.getMessage(), e);
            }
        }

        @Override
        public boolean supportsPartialMessages() {
            return false;
        }
    }
}

