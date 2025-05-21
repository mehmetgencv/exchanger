package com.exchanger.client;

import com.exchanger.dto.requests.ExchangeRateRequest;
import com.exchanger.dto.responses.ExchangeRateResponse;

import java.math.BigDecimal;
import java.util.Map;

public interface ExchangeRateClient {
    /**
     * Returns a map of currency pairs (e.g., USD_EUR) to exchange rates
     */
    ExchangeRateResponse getExchangeRates(ExchangeRateRequest request);

}
