package com.exchanger.dto.responses;

import com.exchanger.exception.CurrencyLayerError;

import java.math.BigDecimal;
import java.util.Map;

public record CurrencyLayerApiResponse(
        boolean success,
        String source,
        long timestamp,
        Map<String, BigDecimal> quotes,
        CurrencyLayerError error
) {}

