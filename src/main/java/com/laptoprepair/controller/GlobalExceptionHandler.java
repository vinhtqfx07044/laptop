package com.laptoprepair.controller;

import com.laptoprepair.exception.NotFoundException;
import com.laptoprepair.exception.SystemException;
import com.laptoprepair.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

/**
 * Global exception handler for the application.
 * This class provides centralized exception handling for various types of
 * exceptions and returns appropriate responses or redirects.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final Map<String, String> REDIRECT_MAPPING = Map.of(
            "/staff/requests", "/staff/requests/list",
            "/staff/service-items", "/staff/service-items");

    @ExceptionHandler({ ValidationException.class, NotFoundException.class })
    public RedirectView handleRedirectExceptions(Exception ex, HttpServletRequest request, RedirectAttributes attrs) {
        return createRedirectWithError(ex.getMessage(), request, attrs);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public RedirectView handleValidationErrors(MethodArgumentNotValidException ex,
            HttpServletRequest request, RedirectAttributes attrs) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage()).toList();

        if (errors.size() == 1) {
            attrs.addFlashAttribute("errorMessage", errors.get(0));
        } else {
            attrs.addFlashAttribute("errorMessages", errors);
        }
        return createRedirectView(request);
    }

    @ExceptionHandler({ SystemException.class, AccessDeniedException.class })
    public ModelAndView handleErrorExceptions(Exception ex) {
        int status = ex instanceof SystemException ? 429 : 403;
        String message = ex instanceof SystemException ? ex.getMessage() : "Bạn không có quyền truy cập tài nguyên này";
        return createErrorModelAndView(status, message);
    }

    @ExceptionHandler({ IllegalArgumentException.class, Exception.class })
    public RedirectView handleGeneralExceptions(Exception ex, HttpServletRequest request, RedirectAttributes attrs) {
        String message = ex instanceof IllegalArgumentException ? "ID yêu cầu không hợp lệ. Vui lòng kiểm tra lại."
                : "Lỗi hệ thống, vui lòng thử lại sau";

        if (ex instanceof Exception && !(ex instanceof IllegalArgumentException)) {
            log.error("System error: {}", ex.getMessage(), ex);
        }
        return createRedirectWithError(message, request, attrs);
    }

    private RedirectView createRedirectWithError(String errorMessage,
            HttpServletRequest request,
            RedirectAttributes attrs) {
        attrs.addFlashAttribute("errorMessage", errorMessage);
        return createRedirectView(request);
    }

    private RedirectView createRedirectView(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String redirectUrl = REDIRECT_MAPPING.entrySet().stream()
                .filter(entry -> requestURI.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(requestURI.contains("/staff/") ? "/staff/requests/list" : "/");

        log.info("Global error redirect: {} -> {}", requestURI, redirectUrl);
        return new RedirectView(redirectUrl != null ? redirectUrl : "/");
    }

    private ModelAndView createErrorModelAndView(int status, String errorMessage) {
        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.addObject("status", status);
        modelAndView.addObject("errorMsg", errorMessage);
        return modelAndView;
    }
}