package com.exchanger.service.impl;

import com.exchanger.client.ExchangeRateClient;
import com.exchanger.dto.requests.ExchangeRateRequest;
import com.exchanger.dto.responses.ExchangeRateResponse;
import com.exchanger.dto.responses.SingleExchangeRateResponse;
import com.exchanger.service.CurrencyConversionService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CurrencyConversionServiceImpl implements CurrencyConversionService {

    private final ExchangeRateClient exchangeRateClient;


    public CurrencyConversionServiceImpl(ExchangeRateClient exchangeRateClient) {
        this.exchangeRateClient = exchangeRateClient;
    }

    @Override
    public ExchangeRateResponse getExchangeRates(ExchangeRateRequest request) {
        return fetchRates(request);
    }

    @Override
    public SingleExchangeRateResponse getSingleExchangeRate(ExchangeRateRequest request) {
        ExchangeRateResponse response = fetchRates(request);
        String target = request.targetCurrencies().getFirst();
        String key = request.sourceCurrency() + "_" + target;

        BigDecimal rate = response.rates().get(key);

        return new SingleExchangeRateResponse(
                request.sourceCurrency(),
                target,
                rate
        );
    }

    private ExchangeRateResponse fetchRates(ExchangeRateRequest request) {
        return exchangeRateClient.getExchangeRates(request);
    }
}