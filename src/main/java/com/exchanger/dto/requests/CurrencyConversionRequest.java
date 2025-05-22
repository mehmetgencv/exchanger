package com.exchanger.dto.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record CurrencyConversionRequest(

        @DecimalMin(value = "0.0", inclusive = false, message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "Source currency must not be blank")
        String sourceCurrency,

        @NotBlank(message = "Target currency must not be blank")
        String targetCurrency

) {}
