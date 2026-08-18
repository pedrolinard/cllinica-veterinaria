package com.vetclinic.api.common.exception;

import java.time.Instant;
import java.util.List;

/**
 * Formato padrão de resposta de erro retornado pela API.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) {
    }

    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(Instant.now(), status, error, message, null);
    }

    public static ErrorResponse of(int status, String error, String message, List<FieldError> fieldErrors) {
        return new ErrorResponse(Instant.now(), status, error, message, fieldErrors);
    }
}
