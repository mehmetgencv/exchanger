package com.exchanger.dto.responses;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CurrencyConversionHistoryResponse(
        UUID transactionId,
        String sourceCurrency,
        String targetCurrency,
        BigDecimal sourceAmount,
        BigDecimal convertedAmount,
        BigDecimal exchangeRate,
        LocalDateTime transactionDate
) {}