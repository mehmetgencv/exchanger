package com.exchanger.exception;

import java.time.LocalDateTime;

public record ApiErrorResponse(
        int status,
        String message,
        String errorCode,
        LocalDateTime timestamp
) {}