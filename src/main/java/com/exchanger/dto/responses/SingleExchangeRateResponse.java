package com.exchanger.dto.responses;

import java.math.BigDecimal;

public record SingleExchangeRateResponse(
        String sourceCurrency,
        String targetCurrency,
        BigDecimal exchangeRate
) {}
