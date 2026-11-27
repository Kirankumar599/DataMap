package com.datamap.exception;

import com.datamap.enums.ErrorCode;
import org.springframework.http.HttpStatus;

/**
 * Base domain exception for DataMap application errors.
 * Ensures raw sensitive cell values are never leaked in error messages.
 */
public class DataMapException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus status;

    public DataMapException(ErrorCode errorCode, String message) {
        this(errorCode, message, HttpStatus.BAD_REQUEST);
    }

    public DataMapException(ErrorCode errorCode, String message, HttpStatus status) {
        super(message);
        this.errorCode = errorCode;
        this.status = status;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
