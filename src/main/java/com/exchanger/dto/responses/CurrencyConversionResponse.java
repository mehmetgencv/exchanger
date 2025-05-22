package com.exchanger.dto.responses;

import java.math.BigDecimal;
import java.util.UUID;

public record CurrencyConversionResponse(
        UUID transactionId,
        BigDecimal convertedAmount
) {}
