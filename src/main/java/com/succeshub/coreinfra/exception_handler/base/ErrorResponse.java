package com.succeshub.coreinfra.exception_handler.base;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

/**
 * Unified error response returned by the global exception handler.
 *
 * Fields with null values are omitted from the JSON response.
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final int status;
    private final String errorCode;
    private final String message;
    private final Instant timestamp;
    private final Map<String, String> fieldErrors;  // only set for ValidationException

    private ErrorResponse(Builder builder) {
        this.status      = builder.status;
        this.errorCode   = builder.errorCode;
        this.message     = builder.message;
        this.timestamp   = Instant.now();
        this.fieldErrors = (builder.fieldErrors == null || builder.fieldErrors.isEmpty())
                ? null
                : builder.fieldErrors;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    // ── Builder ───────────────────────────────────────────────────────────────

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private int status;
        private String errorCode;
        private String message;
        private Map<String, String> fieldErrors;

        public Builder status(int status)                       { this.status = status;           return this; }
        public Builder errorCode(String errorCode)              { this.errorCode = errorCode;     return this; }
        public Builder message(String message)                  { this.message = message;         return this; }
        public Builder fieldErrors(Map<String, String> errors)  { this.fieldErrors = errors;      return this; }

        public ErrorResponse build() { return new ErrorResponse(this); }
    }
}
