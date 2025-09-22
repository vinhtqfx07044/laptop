package com.laptoprepair.interceptor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implements a simple rate limiting mechanism based on client IP address and
 * request type.
 * It uses a sliding window approach to limit the number of requests within a
 * specified time frame.
 */
@Component
public class RateLimiter {

    private record Bucket(Instant windowStart, AtomicInteger count) {
    }

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Value("${app.rate-limiter.public.max-requests-per-minute:10}")
    private int publicMaxRequestsPerMinute;

    @Value("${app.rate-limiter.chat.max-requests-per-minute:20}")
    private int chatMaxRequestsPerMinute;

    private static final int WINDOW_SIZE_SECONDS = 60;

    /**
     * Checks if a request is allowed based on a specific rate limit key prefix.
     * Requests to staff endpoints and static resources are always allowed.
     * 
     * @param request   The HttpServletRequest to check.
     * @param keyPrefix A prefix to categorize the rate limit (e.g., "public",
     *                  "chat").
     * @return true if the request is allowed, false otherwise.
     */
    public boolean isAllowed(HttpServletRequest request, String keyPrefix) {
        String requestPath = request.getRequestURI();

        // Skip rate limiting for staff endpoints and static resources
        if (requestPath.startsWith("/staff/") ||
                requestPath.startsWith("/css/") ||
                requestPath.startsWith("/js/") ||
                requestPath.startsWith("/images/") ||
                requestPath.startsWith("/favicon.ico")) {
            return true;
        }

        // Apply rate limiting to specified endpoints
        if (shouldRateLimit(requestPath, keyPrefix)) {
            String clientKey = keyPrefix + ":" + getClientKey(request);
            Instant now = Instant.now();

            buckets.compute(clientKey, (key, bucket) -> {
                if (bucket == null || now.isAfter(bucket.windowStart.plusSeconds(WINDOW_SIZE_SECONDS))) {
                    return new Bucket(now, new AtomicInteger(1));
                } else {
                    bucket.count.incrementAndGet();
                    return bucket;
                }
            });

            int maxRequests = getMaxRequestsForPrefix(keyPrefix);
            return buckets.get(clientKey).count.get() <= maxRequests;
        }

        return true;
    }

    private boolean shouldRateLimit(String requestPath, String keyPrefix) {
        if ("chat".equals(keyPrefix)) {
            return requestPath.startsWith("/api/chat/");
        }

        if ("public".equals(keyPrefix)) {
            // Rate limit sensitive public endpoints (excluding chat)
            return requestPath.equals("/submit") ||
                    requestPath.equals("/lookup") ||
                    requestPath.equals("/recover") ||
                    requestPath.equals("/login");
        }

        return false;
    }

    private int getMaxRequestsForPrefix(String keyPrefix) {
        return switch (keyPrefix) {
            case "chat" -> chatMaxRequestsPerMinute;
            case "public" -> publicMaxRequestsPerMinute;
            default -> publicMaxRequestsPerMinute; // Default to public limit
        };
    }

    private String getClientKey(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    public void cleanupExpiredBuckets() {
        Instant cutoff = Instant.now().minusSeconds(WINDOW_SIZE_SECONDS);
        buckets.entrySet().removeIf(entry -> entry.getValue().windowStart.isBefore(cutoff));
    }
}