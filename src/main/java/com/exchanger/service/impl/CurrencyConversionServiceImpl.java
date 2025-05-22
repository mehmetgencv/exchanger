package com.exchanger.service.impl;

import com.exchanger.client.ExchangeRateClient;
import com.exchanger.dto.requests.CurrencyConversionHistoryRequest;
import com.exchanger.dto.requests.CurrencyConversionRequest;
import com.exchanger.dto.requests.ExchangeRateRequest;
import com.exchanger.dto.responses.CurrencyConversionHistoryResponse;
import com.exchanger.dto.responses.CurrencyConversionResponse;
import com.exchanger.dto.responses.ExchangeRateResponse;
import com.exchanger.dto.responses.SingleExchangeRateResponse;
import com.exchanger.entity.CurrencyConversion;
import com.exchanger.exception.ExternalApiException;
import com.exchanger.repository.CurrencyConversionRepository;
import com.exchanger.service.CurrencyConversionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CurrencyConversionServiceImpl implements CurrencyConversionService {

    private final ExchangeRateClient exchangeRateClient;
    private final CurrencyConversionRepository currencyConversionRepository;


    public CurrencyConversionServiceImpl(ExchangeRateClient exchangeRateClient, CurrencyConversionRepository currencyConversionRepository) {
        this.exchangeRateClient = exchangeRateClient;
        this.currencyConversionRepository = currencyConversionRepository;
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


    @Override
    public CurrencyConversionResponse convert(CurrencyConversionRequest request) {

        ExchangeRateResponse rateResponse = fetchRates(
                new ExchangeRateRequest(
                        request.sourceCurrency(),
                        List.of(request.targetCurrency())
                )
        );


        String key = request.sourceCurrency() + "_" + request.targetCurrency();
        BigDecimal rate = rateResponse.rates().get(key);

        if (rate == null) {
            throw new ExternalApiException("No exchange rate found for " + key);
        }

        BigDecimal convertedAmount = request.amount().multiply(rate);

        CurrencyConversion conversion = new CurrencyConversion(
                request.sourceCurrency(),
                request.targetCurrency(),
                request.amount(),
                convertedAmount,
                rate
        );

        CurrencyConversion saved = currencyConversionRepository.save(conversion);


        return new CurrencyConversionResponse(saved.getId(), saved.getConvertedAmount());
    }

    @Override
    public Page<CurrencyConversionHistoryResponse> getHistory(CurrencyConversionHistoryRequest request, Pageable pageable) {
        List<CurrencyConversion> results;

        if (request.transactionId() != null) {
            results = currencyConversionRepository.findById(request.transactionId())
                    .map(List::of)
                    .orElse(List.of());
        } else {
            var start = request.date().atStartOfDay();
            var end = start.plusDays(1);
            results = currencyConversionRepository
                    .findAllByTransactionDateBetween(start, end, pageable)
                    .getContent();
        }

        var responseList = results.stream()
                .map(c -> new CurrencyConversionHistoryResponse(
                        c.getId(),
                        c.getSourceCurrency(),
                        c.getTargetCurrency(),
                        c.getSourceAmount(),
                        c.getConvertedAmount(),
                        c.getExchangeRate(),
                        c.getTransactionDate()
                ))
                .toList();

        return new PageImpl<>(responseList, pageable, responseList.size());
    }


    private ExchangeRateResponse fetchRates(ExchangeRateRequest request) {
        return exchangeRateClient.getExchangeRates(request);
    }
}