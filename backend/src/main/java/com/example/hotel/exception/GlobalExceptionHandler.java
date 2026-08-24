package com.example.hotel.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.slf4j.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ErrorResponse> business(BusinessException e, HttpServletRequest r) {
        return ResponseEntity.status(e.getStatus())
                .body(error(e.getCode(), e.getMessage(), null, r));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(
            MethodArgumentNotValidException e, HttpServletRequest r) {
        var fields =
                e.getBindingResult().getFieldErrors().stream()
                        .map(x -> new ErrorResponse.FieldError(x.getField(), x.getDefaultMessage()))
                        .toList();
        return ResponseEntity.badRequest().body(error("VALIDATION_ERROR", "请求参数不合法", fields, r));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> malformed(HttpServletRequest r) {
        return ResponseEntity.badRequest().body(error("MALFORMED_JSON", "请求内容无法解析", null, r));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> unexpected(Exception e, HttpServletRequest r) {
        String id = trace(r);
        log.error("Unexpected error traceId={}", id, e);
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse("INTERNAL_ERROR", "系统繁忙，请稍后重试", null, id));
    }

    private ErrorResponse error(
            String c, String m, List<ErrorResponse.FieldError> f, HttpServletRequest r) {
        return new ErrorResponse(c, m, f, trace(r));
    }

    private String trace(HttpServletRequest r) {
        Object v = r.getAttribute("traceId");
        return v == null ? UUID.randomUUID().toString() : v.toString();
    }
}
