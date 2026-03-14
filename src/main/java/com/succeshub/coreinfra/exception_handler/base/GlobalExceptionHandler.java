package com.succeshub.coreinfra.exception_handler.base;

import com.succeshub.coreinfra.exception_handler.domain.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler — catches all exceptions thrown from controllers
 * and returns a consistent {@link ErrorResponse} JSON body.
 *
 * Priority order:
 *   1. ValidationException  (domain validation with optional field errors)
 *   2. AppException         (any other custom domain error)
 *   3. MethodArgumentNotValidException  (Bean Validation / @Valid)
 *   4. Exception            (unexpected fallback — never leak internals)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── 1. Domain: Validation ─────────────────────────────────────────────────

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        log.warn("Validation error: {}", ex.getMessage());

        ErrorResponse body = ErrorResponse.builder()
                .status(ex.getStatus().value())
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .fieldErrors(ex.getFieldErrors())
                .build();

        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    // ── 2. Domain: All other custom exceptions ────────────────────────────────

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex) {
        log.warn("Application error [{}]: {}", ex.getErrorCode(), ex.getMessage());

        ErrorResponse body = ErrorResponse.builder()
                .status(ex.getStatus().value())
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    // ── 3. Spring: @Valid / @Validated failures ───────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBeanValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        fe -> fe.getField(),
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "invalid value",
                        (first, second) -> first   // keep first message per field
                ));

        log.warn("Bean validation failed: {}", fieldErrors);

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode("VALIDATION_ERROR")
                .message("Request validation failed")
                .fieldErrors(fieldErrors)
                .build();

        return ResponseEntity.badRequest().body(body);
    }

    // ── 4. Fallback: unexpected errors ────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);   // full stack trace in logs only

        ErrorResponse body = ErrorResponse.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .errorCode("INTERNAL_ERROR")
                .message("An unexpected error occurred. Please try again later.")
                .build();

        return ResponseEntity.internalServerError().body(body);
    }
}