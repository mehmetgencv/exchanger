package com.exchanger.service;

import com.exchanger.dto.requests.CurrencyConversionHistoryRequest;
import com.exchanger.dto.requests.CurrencyConversionRequest;
import com.exchanger.dto.requests.ExchangeRateRequest;
import com.exchanger.dto.responses.CurrencyConversionHistoryResponse;
import com.exchanger.dto.responses.CurrencyConversionResponse;
import com.exchanger.dto.responses.ExchangeRateResponse;
import com.exchanger.dto.responses.SingleExchangeRateResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface  CurrencyConversionService {
    SingleExchangeRateResponse getSingleExchangeRate(ExchangeRateRequest request);
    ExchangeRateResponse getExchangeRates(ExchangeRateRequest request);
    CurrencyConversionResponse convert(CurrencyConversionRequest request);
    Page<CurrencyConversionHistoryResponse> getHistory(CurrencyConversionHistoryRequest request, Pageable pageable);
}
