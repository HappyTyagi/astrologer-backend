package com.astro.backend.Services;

import com.astro.backend.Entity.ErrorLog;
import com.astro.backend.Repositry.ErrorLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.PrintWriter;
import java.io.StringWriter;

@Service
@RequiredArgsConstructor
public class ErrorLogService {

    private static final int MAX_MESSAGE_LENGTH = 4000;
    private static final int MAX_PAYLOAD_LENGTH = 16000;
    private static final int MAX_STACK_LENGTH = 32000;

    private final ErrorLogRepository errorLogRepository;

    public Long log(String module, String endpoint, Throwable throwable, String payload, Long userId) {
        try {
            ErrorLog row = ErrorLog.builder()
                    .module(trim(module, 120))
                    .endpoint(trim(endpoint, 255))
                    .errorType(throwable == null ? "UNKNOWN" : trim(throwable.getClass().getSimpleName(), 120))
                    .errorMessage(trim(throwable == null ? null : throwable.getMessage(), MAX_MESSAGE_LENGTH))
                    .stackTrace(trim(stackTraceOf(throwable), MAX_STACK_LENGTH))
                    .requestPayload(trim(payload, MAX_PAYLOAD_LENGTH))
                    .userId(userId)
                    .build();
            return errorLogRepository.save(row).getId();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String stackTraceOf(Throwable throwable) {
        if (throwable == null) return null;
        try {
            StringWriter sw = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sw));
            return sw.toString();
        } catch (Exception e) {
            return throwable.toString();
        }
    }

    private String trim(String value, int max) {
        if (value == null) return null;
        String v = value.trim();
        if (v.length() <= max) return v;
        return v.substring(0, max);
    }
}
