package com.exchanger.dto.responses;

import java.math.BigDecimal;
import java.util.Map;

public record CurrencyLayerApiResponse(
        boolean success,
        String source,
        long timestamp,
        Map<String, BigDecimal> quotes
) {}
