package com.exchanger.dto.requests;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record CurrencyConversionRequest(

        @Schema(example = "100.00", description = "Amount to convert")
        @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than zero")
        BigDecimal amount,

        @Schema(example = "USD", description = "Source currency code")
        @NotBlank(message = "Source currency must not be blank")
        String sourceCurrency,

        @Schema(example = "TRY", description = "Target currency code")
        @NotBlank(message = "Target currency must not be blank")
        String targetCurrency

) {}
