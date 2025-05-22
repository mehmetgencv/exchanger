package com.exchanger.dto.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;

import java.time.LocalDate;
import java.util.UUID;

public record CurrencyConversionHistoryRequest(
        @Schema(example = "123e4567-e89b-12d3-a456-426614174000")
        UUID transactionId,

        @Schema(example = "2025-05-18")
        LocalDate date
) {
    @AssertTrue(message = "At least one of transactionId or date must be provided.")
    public boolean isAtLeastOneProvided() {
        return transactionId != null || date != null;
    }
}
