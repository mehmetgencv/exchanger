package com.exchanger.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> Objects.toString(fieldError.getDefaultMessage(), "Validation failed"))
                .findFirst()
                .orElse("Validation failed");

        return ResponseEntity
                .badRequest()
                .body(new ApiErrorResponse(
                        400,
                        message,
                        "VALIDATION_ERROR",
                        LocalDateTime.now()
                ));
    }

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
