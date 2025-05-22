package com.exchanger.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<ApiErrorResponse> handleExternalApiException(ExternalApiException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(new ApiErrorResponse(
                        502,
                        ex.getMessage(),
                        "EXTERNAL_API_ERROR",
                        LocalDateTime.now()
                ));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiErrorResponse> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse(
                        500,
                        "An unexpected runtime error occurred: " + ex.getMessage(),
                        "RUNTIME_EXCEPTION",
                        LocalDateTime.now()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse(
                        500,
                        "An unexpected error occurred: " + ex.getMessage(),
                        "GENERIC_EXCEPTION",
                        LocalDateTime.now()
                ));
    }
}
