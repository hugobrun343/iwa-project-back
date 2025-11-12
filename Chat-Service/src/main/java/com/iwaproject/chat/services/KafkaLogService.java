package com.iwaproject.chat.services;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service to send structured log messages to Kafka.
 */
@Service
public class KafkaLogService {

    /**
     * Kafka template.
     */
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Object mapper.
     */
    private final ObjectMapper objectMapper;

    /**
     * Logs topic name.
     */
    private final String logsTopic;

    /**
     * Service name.
     */
    private final String serviceName;

    /**
     * Constructor.
     *
     * @param template kafka template
     * @param mapper object mapper
     * @param topic logs topic name
     * @param name service name
     */
    public KafkaLogService(
            final KafkaTemplate<String, Object> template,
            final ObjectMapper mapper,
            @Value("${kafka.logs.topic:microservices-logs}")
            final String topic,
            @Value("${spring.application.name:Chat-Service}")
            final String name) {
        this.kafkaTemplate = template;
        this.objectMapper = mapper;
        this.logsTopic = topic;
        this.serviceName = name;
    }

    /**
     * Send info log.
     *
     * @param logger logger name
     * @param message log message
     */
    public void info(final String logger, final String message) {
        sendLog("INFO", logger, message, null);
    }

    /**
     * Send error log.
     *
     * @param logger logger name
     * @param message log message
     */
    public void error(final String logger, final String message) {
        sendLog("ERROR", logger, message, null);
    }

    /**
     * Send error log with throwable.
     *
     * @param logger logger name
     * @param message log message
     * @param throwable exception
     */
    public void error(final String logger, final String message,
                      final Throwable throwable) {
        sendLog("ERROR", logger, message, throwable);
    }

    /**
     * Send debug log.
     *
     * @param logger logger name
     * @param message log message
     */
    public void debug(final String logger, final String message) {
        sendLog("DEBUG", logger, message, null);
    }

    /**
     * Send warn log.
     *
     * @param logger logger name
     * @param message log message
     */
    public void warn(final String logger, final String message) {
        sendLog("WARN", logger, message, null);
    }

    /**
     * Send log to Kafka.
     *
     * @param level log level
     * @param logger logger name
     * @param message log message
     * @param throwable exception if any
     */
    private void sendLog(final String level, final String logger,
                         final String message,
                         final Throwable throwable) {
        try {
            Map<String, Object> logEntry = new HashMap<>();
            logEntry.put("timestamp", Instant.now().toString());
            logEntry.put("level", level);
            logEntry.put("service", serviceName);
            logEntry.put("logger", logger);
            logEntry.put("message", message);

            if (throwable != null) {
                logEntry.put("exception", throwable.getClass().getName());
                logEntry.put("stackTrace", getStackTrace(throwable));
            }

            String json = objectMapper.writeValueAsString(logEntry);
            kafkaTemplate.send(logsTopic, json);
        } catch (Exception e) {
            // Fallback to console if Kafka fails
            System.err.println("[KafkaLogService] "
                    + "Failed to send log to Kafka: " + e.getMessage());
            System.err.println("Original log: [" + level + "] "
                    + logger + " - " + message);
        }
    }

    /**
     * Get stack trace as string.
     *
     * @param throwable the throwable
     * @return stack trace string
     */
    private String getStackTrace(final Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.toString()).append("\n");
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}

