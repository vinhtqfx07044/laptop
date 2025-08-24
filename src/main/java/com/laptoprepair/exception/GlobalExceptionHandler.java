package com.laptoprepair.exception;

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

/**
 * Global exception handler for the application.
 * This class provides centralized exception handling for various types of exceptions
 * and returns appropriate responses or redirects.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ValidationException.class)
    public RedirectView handleValidation(ValidationException ex,
            HttpServletRequest request,
            RedirectAttributes attrs) {
        log.warn("Validation error not handled locally: {} at {}", ex.getMessage(), request.getRequestURI());
        return createRedirectWithError(ex.getMessage(), request, attrs);
    }

    @ExceptionHandler(NotFoundException.class)
    public RedirectView handleNotFound(NotFoundException ex,
            HttpServletRequest request,
            RedirectAttributes attrs) {
        log.warn("Not found error not handled locally: {} at {}", ex.getMessage(), request.getRequestURI());
        return createRedirectWithError(ex.getMessage(), request, attrs);
    }

    @ExceptionHandler(CSVImportException.class)
    public RedirectView handleCSV(CSVImportException ex,
            HttpServletRequest request,
            RedirectAttributes attrs) {
        log.warn("CSV import error not handled locally: {} at {}", ex.getMessage(), request.getRequestURI());
        String message = ex.getMessage();
        if (ex.getRowNumber() > 0) {
            message += " (dòng " + ex.getRowNumber() + ")";
        }
        return createRedirectWithError(message, request, attrs);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public RedirectView handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpServletRequest request,
            RedirectAttributes attrs) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .toList();

        if (errors.size() == 1) {
            attrs.addFlashAttribute("errorMessage", errors.get(0));
        } else {
            attrs.addFlashAttribute("errorMessages", errors);
        }
        return createRedirectView(request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public RedirectView handleIllegalArgument(IllegalArgumentException ex,
            HttpServletRequest request,
            RedirectAttributes attrs) {
        return createRedirectWithError("ID yêu cầu không hợp lệ. Vui lòng kiểm tra lại.", request, attrs);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ModelAndView handleRateLimitExceeded(RateLimitExceededException ex) {
        return createErrorModelAndView(429, ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView handleAccessDenied(AccessDeniedException ex) {
        return createErrorModelAndView(403, "Bạn không có quyền truy cập tài nguyên này");
    }

    @ExceptionHandler(Exception.class)
    public RedirectView handleGeneral(Exception ex,
            HttpServletRequest request,
            RedirectAttributes attrs) {
        log.error("System error: {}", ex.getMessage(), ex);
        return createRedirectWithError("Lỗi hệ thống, vui lòng thử lại sau", request, attrs);
    }

    private RedirectView createRedirectWithError(String errorMessage,
            HttpServletRequest request,
            RedirectAttributes attrs) {
        attrs.addFlashAttribute("errorMessage", errorMessage);
        return createRedirectView(request);
    }

    private ModelAndView createErrorModelAndView(int status, String errorMessage) {
        ModelAndView modelAndView = new ModelAndView("error");
        modelAndView.addObject("status", status);
        modelAndView.addObject("errorMsg", errorMessage);
        return modelAndView;
    }

    private RedirectView createRedirectView(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String redirectUrl = "/";
        
        // FIX: Smart redirect strategy thay vì dùng referer để tránh mất flash message
        if (requestURI.contains("/staff/requests")) {
            redirectUrl = "/staff/requests/list";
        } else if (requestURI.contains("/staff/service-items")) {
            redirectUrl = "/staff/service-items";
        } else if (requestURI.contains("/staff/")) {
            redirectUrl = "/staff/requests/list";
        } else if (requestURI.contains("/public/")) {
            redirectUrl = "/";
        }
        
        log.info("Global error redirect: {} -> {}", requestURI, redirectUrl);
        return new RedirectView(redirectUrl);
    }
}