package com.exchanger.dto.responses;

import java.math.BigDecimal;
import java.util.UUID;

public record BulkConversionResponse(
        UUID transactionId,
        String sourceCurrency,
        String targetCurrency,
        BigDecimal rate,
        BigDecimal originalAmount,
        BigDecimal convertedAmount,
        String errorMessage
) {}