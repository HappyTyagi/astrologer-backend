package com.astro.backend.Exception;

import com.astro.backend.enumData.ErrorCodeEnum;
import lombok.Getter;

import java.util.Map;

@Getter
public class SDDException extends RuntimeException {

    private final String field;
    private final boolean status;           // remove default assignment
    private final Map<String, Object> response;

    // ================= Existing Constructors =================

    public SDDException(String field, boolean status, String message) {
        super(message);
        this.field = field;
        this.status = status;
        this.response = null;
    }

    public SDDException(boolean status, String message) {
        super(message);
        this.field = null;
        this.status = status;
        this.response = null;
    }

    public SDDException(boolean status, String message, Throwable cause) {
        super(message, cause);
        this.field = null;
        this.status = status;
        this.response = null;
    }

    public SDDException(boolean status, String message, Map<String, Object> response) {
        super(message);
        this.field = null;
        this.status = status;
        this.response = response;
    }

    // ================= New Constructors for ErrorCode =================

    // Default status = 400
    public SDDException(ErrorCodeEnum errorCode) {
        super(errorCode.getMessage());
        this.field = null;
        this.status = true;  // defaulting to true
        this.response = Map.of("errorCode", errorCode.getCode());
    }

    // Custom status + errorCode
    public SDDException(boolean status, ErrorCodeEnum errorCode) {
        super(errorCode.getMessage());
        this.field = null;
        this.status = status;
        this.response = Map.of("errorCode", errorCode.getCode());
    }

    // Field + status + errorCode
    public SDDException(String field, boolean status, ErrorCodeEnum errorCode) {
        super(errorCode.getMessage());
        this.field = field;
        this.status = status;
        this.response = Map.of("errorCode", errorCode.getCode());
    }
}
