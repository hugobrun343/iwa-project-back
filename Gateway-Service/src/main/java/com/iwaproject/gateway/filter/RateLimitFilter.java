package com.iwaproject.gateway.filter;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Rate limiting filter for Spring Cloud Gateway.
 * Reactive version using simple token bucket algorithm.
 */
@Slf4j
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final int MAX_REQUESTS_PER_MINUTE = 100;
    private static final Duration WINDOW_DURATION = Duration.ofMinutes(1);

    private final Map<String, RequestCounter> cache = new ConcurrentHashMap<>();

    /**
     * Filter incoming requests to apply rate limiting.
     *
     * @param exchange the current server exchange
     * @param chain    the gateway filter chain
     * @return a Mono signaling completion
     */
    @Override
    public Mono<Void> filter(final ServerWebExchange exchange,
                             final GatewayFilterChain chain) {

        // Get client identifier (IP address)
        String clientId = getClientId(exchange);

        // Clean up old entries periodically
        cleanupOldEntries();

        // Get or create counter for this client
        RequestCounter counter = cache.computeIfAbsent(
                clientId, k -> new RequestCounter());

        // Check if rate limit exceeded
        if (counter.increment() > MAX_REQUESTS_PER_MINUTE) {
            log.warn("Rate limit exceeded for client: {}", clientId);
            exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
            exchange.getResponse().getHeaders()
                    .add("X-Rate-Limit-Retry-After-Seconds", "60");
            return exchange.getResponse().setComplete();
        }

        log.debug("Request allowed for client: {}", clientId);
        return chain.filter(exchange);
    }

    /**
     * Get client identifier from request.
     *
     * @param exchange the server exchange
     * @return the client identifier
     */
    private String getClientId(final ServerWebExchange exchange) {
        // Try to get from forwarded header first
        String forwarded = exchange.getRequest().getHeaders()
                .getFirst("X-Forwarded-For");

        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }

        // Fallback to remote address
        if (exchange.getRequest().getRemoteAddress() != null) {
            return exchange.getRequest().getRemoteAddress()
                    .getAddress().getHostAddress();
        }

        return "unknown";
    }

    /**
     * Clean up old entries from cache.
     */
    private void cleanupOldEntries() {
        Instant cutoff = Instant.now().minus(WINDOW_DURATION);
        cache.entrySet().removeIf(entry ->
                entry.getValue().getLastReset().isBefore(cutoff));
    }

    /**
     * Set filter order (run after JWT filter).
     *
     * @return the order value
     */
    @Override
    public int getOrder() {
        return -50; // After JWT filter (-100)
    }

    /**
     * Request counter with automatic reset.
     */
    private static class RequestCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private volatile Instant lastReset = Instant.now();

        /**
         * Increment counter and reset if window expired.
         *
         * @return current count after increment
         */
        public int increment() {
            Instant now = Instant.now();
            if (Duration.between(lastReset, now).compareTo(WINDOW_DURATION) > 0) {
                count.set(0);
                lastReset = now;
            }
            return count.incrementAndGet();
        }

        /**
         * Get last reset time.
         *
         * @return last reset instant
         */
        public Instant getLastReset() {
            return lastReset;
        }
    }
}
