package com.datamap.exception;

import com.datamap.dto.ErrorResponseDto;
import com.datamap.enums.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Global exception handler ensuring all errors produce uniform JSON responses
 * and never leak raw sensitive data in error messages or logs.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DataMapException.class)
    public ResponseEntity<ErrorResponseDto> handleDataMapException(DataMapException ex) {
        log.warn("Handled application error: [{}]: {}", ex.getErrorCode(), ex.getMessage());
        ErrorResponseDto errorDto = new ErrorResponseDto(
            ex.getErrorCode().name(),
            ex.getMessage()
        );
        return ResponseEntity.status(ex.getStatus()).body(errorDto);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponseDto> handleMaxSizeException(MaxUploadSizeExceededException ex) {
        log.warn("File upload size exceeded limit");
        ErrorResponseDto errorDto = new ErrorResponseDto(
            ErrorCode.FILE_TOO_LARGE.name(),
            "File exceeds the 10 MB limit."
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDto);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponseDto> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Invalid argument: {}", ex.getMessage());
        ErrorResponseDto errorDto = new ErrorResponseDto(
            ErrorCode.PARSE_FAILED.name(),
            ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorDto);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGeneralException(Exception ex) {
        log.error("Internal unhandled error occurred", ex);
        ErrorResponseDto errorDto = new ErrorResponseDto(
            ErrorCode.INTERNAL_ERROR.name(),
            "An unexpected internal error occurred."
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorDto);
    }
}
