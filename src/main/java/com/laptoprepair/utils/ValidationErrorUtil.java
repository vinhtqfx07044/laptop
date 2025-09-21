package com.laptoprepair.utils;

import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class for extracting and formatting validation errors from BindingResult.
 * Used to centralize Jakarta validation error handling and display.
 */
@Component
public class ValidationErrorUtil {

    /**
     * Extracts field-specific error messages from BindingResult.
     * Format: "fieldName: error message"
     *
     * @param bindingResult the Spring validation result
     * @return list of formatted error messages for header display
     */
    public List<String> extractErrorMessages(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.toList());
    }

    /**
     * Creates a map of field names to error status.
     * Used for applying CSS classes to form fields with errors.
     *
     * @param bindingResult the Spring validation result
     * @return map where key is field name and value is true if field has error
     */
    public Map<String, Boolean> getFieldErrorStatus(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                error -> true,
                (existing, replacement) -> existing
            ));
    }

    /**
     * Creates a map of field names to their specific error messages.
     * Used for displaying server-side error messages in invalid-feedback divs.
     *
     * @param bindingResult the Spring validation result
     * @return map where key is field name and value is the error message
     */
    public Map<String, String> getFieldErrorMessages(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
            .collect(Collectors.toMap(
                FieldError::getField,
                FieldError::getDefaultMessage,
                (existing, replacement) -> existing
            ));
    }

    /**
     * Checks if there are any validation errors.
     *
     * @param bindingResult the Spring validation result
     * @return true if there are field errors, false otherwise
     */
    public boolean hasErrors(BindingResult bindingResult) {
        return bindingResult.hasErrors();
    }
}