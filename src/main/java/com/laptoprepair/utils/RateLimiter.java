package com.laptoprepair.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimiter {

    private record Bucket(Instant windowStart, AtomicInteger count) {}

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    
    @Value("${app.rate-limiter.max-requests-per-minute:10}")
    private int maxRequestsPerMinute;
    
    @Value("${app.rate-limiter.window-size-seconds:60}")
    private int windowSizeSeconds;

    public boolean isAllowed(HttpServletRequest request) {
        // Skip rate limiting for staff endpoints
        String requestPath = request.getRequestURI();
        if (requestPath.startsWith("/staff/")) {
            return true;
        }
        
        String clientKey = getClientKey(request);
        Instant now = Instant.now();
        
        buckets.compute(clientKey, (key, bucket) -> {
            if (bucket == null || now.isAfter(bucket.windowStart.plusSeconds(windowSizeSeconds))) {
                return new Bucket(now, new AtomicInteger(1));
            } else {
                bucket.count.incrementAndGet();
                return bucket;
            }
        });
        
        return buckets.get(clientKey).count.get() <= maxRequestsPerMinute;
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
        Instant cutoff = Instant.now().minusSeconds(windowSizeSeconds);
        buckets.entrySet().removeIf(entry -> 
            entry.getValue().windowStart.isBefore(cutoff));
    }
}