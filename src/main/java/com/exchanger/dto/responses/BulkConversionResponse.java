package com.exchanger.dto.responses;

import java.math.BigDecimal;
import java.util.UUID;

public record BulkConversionResponse(
        UUID transactionId,
        BigDecimal convertedAmount,
        String errorMessage
) {}