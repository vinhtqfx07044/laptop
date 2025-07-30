package com.laptoprepair.interceptor;

import com.laptoprepair.exception.RateLimitExceededException;
import com.laptoprepair.utils.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.lang.NonNull;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimiter rateLimiter;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull Object handler) throws Exception {
        if (!rateLimiter.isAllowed(request, "public")) {
            throw new RateLimitExceededException("Quá nhiều yêu cầu. Vui lòng đợi một chút trước khi thử lại.");
        }
        return true;
    }
}