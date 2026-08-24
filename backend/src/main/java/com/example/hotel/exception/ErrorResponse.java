package com.example.hotel.exception;

import java.util.List;

public record ErrorResponse(
        String code, String message, List<FieldError> fieldErrors, String traceId) {
    public record FieldError(String field, String message) {}
}
