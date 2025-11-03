package com.iwaproject.user.services;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Kafka producer service.
 */
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    /**
     * Kafka template.
     */
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Send message to Kafka topic.
     *
     * @param topic the topic name
     * @param message the message to send
     */
    public void sendMessage(final String topic, final Object message) {
        kafkaTemplate.send(topic, message);
    }
}
