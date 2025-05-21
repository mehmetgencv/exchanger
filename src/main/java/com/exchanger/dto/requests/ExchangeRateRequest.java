package com.exchanger.dto.requests;

import java.util.List;

public record ExchangeRateRequest(
        String sourceCurrency,
        List<String> targetCurrencies
) {}
