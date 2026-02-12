package com.astro.backend.Exception;

import com.astro.backend.Services.ErrorLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ErrorLogService errorLogService;

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex, HttpServletRequest request) {
        Long errorId = errorLogService.log(
                "GLOBAL",
                request == null ? null : request.getRequestURI(),
                ex,
                null,
                null
        );

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", false);
        body.put("message", "Something went wrong. Please try again.");
        if (errorId != null) {
            body.put("errorId", errorId);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
