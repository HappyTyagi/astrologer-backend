package com.astro.backend.Exception;

import com.astro.backend.Services.ErrorLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.Method;
import java.util.Map;

@ControllerAdvice
@RequiredArgsConstructor
public class ApiErrorResponseLoggingAdvice implements ResponseBodyAdvice<Object> {

    private final ErrorLogService errorLogService;

    @Override
    public boolean supports(
            MethodParameter returnType,
            Class<? extends HttpMessageConverter<?>> converterType
    ) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response
    ) {
        boolean shouldLog = false;
        String message = "Request failed.";
        String payload = body == null ? null : body.toString();
        Long userId = null;

        if (body instanceof Map<?, ?> map) {
            shouldLog = Boolean.FALSE.equals(map.get("status"));
            if (map.get("message") != null && !String.valueOf(map.get("message")).isBlank()) {
                message = String.valueOf(map.get("message"));
            }
            Object userIdValue = map.get("userId");
            if (userIdValue instanceof Number number) {
                userId = number.longValue();
            }
        } else if (body != null) {
            shouldLog = hasFalseStatus(body);
            String extractedMessage = extractMessage(body);
            if (extractedMessage != null && !extractedMessage.isBlank()) {
                message = extractedMessage;
            }
        }

        if (!shouldLog) {
            return body;
        }

        if (request instanceof ServletServerHttpRequest servletRequest) {
            Object alreadyLogged = servletRequest.getServletRequest()
                    .getAttribute(GlobalExceptionHandler.ERROR_LOGGED_ATTR);
            if (Boolean.TRUE.equals(alreadyLogged)) {
                return body;
            }
        }

        String endpoint = request.getURI() == null ? null : request.getURI().getPath();

        errorLogService.log(
                "API_RESPONSE",
                endpoint,
                new RuntimeException(message),
                payload,
                userId
        );
        return body;
    }

    private boolean hasFalseStatus(Object body) {
        try {
            Method method = body.getClass().getMethod("getStatus");
            Object status = method.invoke(body);
            return Boolean.FALSE.equals(status);
        } catch (Exception ignored) {
            return false;
        }
    }

    private String extractMessage(Object body) {
        try {
            Method method = body.getClass().getMethod("getMessage");
            Object message = method.invoke(body);
            return message == null ? null : String.valueOf(message);
        } catch (Exception ignored) {
            return null;
        }
    }
}
