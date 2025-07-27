package com.laptoprepair.interceptor;

import com.laptoprepair.exception.RateLimitExceededException;
import com.laptoprepair.utils.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiter rateLimiter;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestPath = request.getRequestURI();
        
        // Skip interceptor for chat endpoints (handled separately in ChatController)
        if (requestPath.startsWith("/api/chat/")) {
            return true;
        }
        
        if (!rateLimiter.isAllowed(request, "public")) {
            throw new RateLimitExceededException("Quá nhiều yêu cầu. Vui lòng đợi một chút trước khi thử lại.");
        }
        return true;
    }
}