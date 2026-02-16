package com.astro.backend.Exception;

import com.astro.backend.Auth.JwtAuthFilter;
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

    public static final String ERROR_LOGGED_ATTR = "ERROR_LOGGED";
    private final ErrorLogService errorLogService;

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        Long userId = extractUserId(request);
        Long errorId = errorLogService.log(
                "GLOBAL",
                request == null ? null : request.getRequestURI(),
                ex,
                null,
                userId
        );
        if (request != null) {
            request.setAttribute(ERROR_LOGGED_ATTR, Boolean.TRUE);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", false);
        body.put("message", ex.getMessage() == null || ex.getMessage().isBlank()
                ? "Request failed."
                : ex.getMessage());
        if (errorId != null) {
            body.put("errorId", errorId);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex, HttpServletRequest request) {
        Long userId = extractUserId(request);
        Long errorId = errorLogService.log(
                "GLOBAL",
                request == null ? null : request.getRequestURI(),
                ex,
                null,
                userId
        );
        if (request != null) {
            request.setAttribute(ERROR_LOGGED_ATTR, Boolean.TRUE);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", false);
        body.put("message", "Something went wrong. Please try again.");
        if (errorId != null) {
            body.put("errorId", errorId);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private Long extractUserId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object userIdAttr = request.getAttribute(JwtAuthFilter.AUTH_USER_ID_ATTR);
        if (userIdAttr instanceof Number number) {
            return number.longValue();
        }
        if (userIdAttr != null) {
            try {
                return Long.parseLong(String.valueOf(userIdAttr).trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
