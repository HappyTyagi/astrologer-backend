package com.astro.backend.ResponseDTO.PurchaseApi;

public class ProxyErrorResponse {
    private boolean success;
    private String message;

    public ProxyErrorResponse() {
    }

    public ProxyErrorResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}