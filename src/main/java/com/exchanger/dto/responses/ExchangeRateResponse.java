package com.exchanger.dto.responses;

import java.math.BigDecimal;
import java.util.Map;

public record ExchangeRateResponse(
        String sourceCurrency,
        Map<String, BigDecimal> rates
) {}
