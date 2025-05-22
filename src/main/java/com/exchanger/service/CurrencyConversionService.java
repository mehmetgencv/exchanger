package com.exchanger.service;

import com.exchanger.dto.requests.CurrencyConversionHistoryRequest;
import com.exchanger.dto.requests.CurrencyConversionRequest;
import com.exchanger.dto.requests.ExchangeRateRequest;
import com.exchanger.dto.responses.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


public interface  CurrencyConversionService {
    SingleExchangeRateResponse getSingleExchangeRate(ExchangeRateRequest request);
    ExchangeRateResponse getExchangeRates(ExchangeRateRequest request);
    CurrencyConversionResponse convert(CurrencyConversionRequest request);
    Page<CurrencyConversionHistoryResponse> getHistory(CurrencyConversionHistoryRequest request, Pageable pageable);
    List<BulkConversionResponse> processCsvFile(MultipartFile file);
}
