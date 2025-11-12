package com.iwaproject.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for Chat Service.
 */
@SpringBootApplication
@EnableScheduling
public class ChatServiceApplication {

    /**
     * Protected constructor for Spring Boot.
     */
    protected ChatServiceApplication() {
        // Spring Boot needs to instantiate this class
    }

    /**
     * Main method to start the Chat Service application.
     *
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        SpringApplication.run(ChatServiceApplication.class, args);
    }

}
