package com.succeshub.coreinfra.exception_handler.domain;

import com.succeshub.coreinfra.exception_handler.base.AppException;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Thrown when business/domain validation fails.
 * Optionally carries a field→message map for detailed feedback.
 */
@Getter
public class ValidationException extends AppException {

    private final Map<String, String> fieldErrors;

    /** Single-message validation error */
    public ValidationException(String message) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR");
        this.fieldErrors = Map.of();
    }

    /** Multi-field validation error — pass a map of { "email": "already in use", ... } */
    public ValidationException(String message, Map<String, String> fieldErrors) {
        super(message, HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR");
        this.fieldErrors = fieldErrors;
    }

}