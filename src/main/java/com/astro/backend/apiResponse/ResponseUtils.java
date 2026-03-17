package com.astro.backend.apiResponse;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ResponseUtils {

    public static <T> ApiResponse<T> createSuccessResponse(T data, Boolean status, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setResponse(data);
        response.setStatus(status);
        response.setMessage(message);
        return response;
    }

    public static <T> ApiResponse<T> createSuccessResponse(T data, TypeReference<T> tClass, Boolean status, String message) {
        return createSuccessResponse(data, status, message);
    }


    public <T> ApiResponse<T> createFailureResponse(String code, Boolean status) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setStatus(status);
//        response.setError_code(code);
        response.setMessage(code);
        return response;
    }

    public <T> ApiResponse<T> createFailureResponse(String msg, Boolean status,String code) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setStatus(status);
        response.setError_code(code);
        response.setMessage(msg);
        return response;
    }



}