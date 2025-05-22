package com.exchanger.service;

import com.exchanger.dto.requests.ExchangeRateRequest;
import com.exchanger.dto.responses.ExchangeRateResponse;
import com.exchanger.dto.responses.SingleExchangeRateResponse;

public interface  CurrencyConversionService {
    SingleExchangeRateResponse getSingleExchangeRate(ExchangeRateRequest request);
    ExchangeRateResponse getExchangeRates(ExchangeRateRequest request);
}
