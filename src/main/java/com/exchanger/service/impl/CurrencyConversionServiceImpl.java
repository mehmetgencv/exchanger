package com.exchanger.service.impl;

import com.exchanger.client.ExchangeRateClient;
import com.exchanger.dto.requests.CurrencyConversionHistoryRequest;
import com.exchanger.dto.requests.CurrencyConversionRequest;
import com.exchanger.dto.requests.ExchangeRateRequest;
import com.exchanger.dto.responses.*;
import com.exchanger.entity.CurrencyConversion;
import com.exchanger.exception.ExternalApiException;
import com.exchanger.mapper.CurrencyConversionMapper;
import com.exchanger.repository.CurrencyConversionRepository;
import com.exchanger.service.CurrencyConversionService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CurrencyConversionServiceImpl implements CurrencyConversionService {

    private final ExchangeRateClient exchangeRateClient;
    private final CurrencyConversionRepository currencyConversionRepository;
    private static final Logger log = LoggerFactory.getLogger(CurrencyConversionServiceImpl.class);


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

    @Override
    public List<BulkConversionResponse> processCsvFile(MultipartFile file) {
        List<BulkConversionResponse> results = new ArrayList<>();
        List<CurrencyConversion> conversionsToPersist = new ArrayList<>();

        List<CurrencyConversionRequest> requests = parseCsvToRequests(file);

        Map<String, List<CurrencyConversionRequest>> grouped = requests.stream()
                .collect(Collectors.groupingBy(CurrencyConversionRequest::sourceCurrency));

        for (var entry : grouped.entrySet()) {
            String source = entry.getKey();
            List<CurrencyConversionRequest> groupRequests = entry.getValue();

            Set<String> targets = groupRequests.stream()
                    .map(CurrencyConversionRequest::targetCurrency)
                    .collect(Collectors.toSet());

            ExchangeRateResponse rateResponse;
            try {
                Thread.sleep(1000);
                rateResponse = fetchRates(new ExchangeRateRequest(source, new ArrayList<>(targets)));
            } catch (ExternalApiException e) {
                for (CurrencyConversionRequest req : groupRequests) {
                    results.add(new BulkConversionResponse(
                            null,
                            req.sourceCurrency(),
                            req.targetCurrency(),
                            req.amount(),
                            null,
                            null,
                            "Rate fetch failed for " + source + ": " + e.getMessage()));
                }
                continue;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Thread interrupted", e);
            }

            for (CurrencyConversionRequest req : groupRequests) {
                String key = source + "_" + req.targetCurrency();
                BigDecimal rate = rateResponse.rates().get(key);

                if (rate == null) {
                    results.add(new BulkConversionResponse(
                            null,
                            req.sourceCurrency(),
                            req.targetCurrency(),
                            req.amount(),
                            null,
                            null,
                            "Rate not found for " + key));
                    continue;
                }

                CurrencyConversion entity = CurrencyConversionMapper.INSTANCE.toEntity(req, rate);
                conversionsToPersist.add(entity);
                results.add(new BulkConversionResponse(
                        null,
                        entity.getSourceCurrency(),
                        entity.getTargetCurrency(),
                        entity.getExchangeRate(),
                        entity.getSourceAmount(),
                        entity.getConvertedAmount(),
                        null));
            }
        }

        List<CurrencyConversion> saved;

        if (!conversionsToPersist.isEmpty()) {
            saved = currencyConversionRepository.saveAll(conversionsToPersist);

            int i = 0;
            for (BulkConversionResponse result : results) {
                if (result.transactionId() == null && result.errorMessage() == null) {
                    UUID id = saved.get(i).getId();
                    results.set(results.indexOf(result), new BulkConversionResponse(
                            id,
                            result.sourceCurrency(),
                            result.targetCurrency(),
                            result.rate(),
                            result.originalAmount(),
                            result.convertedAmount(),
                            null));
                    i++;
                }
            }
        }
        return results;
    }


    private List<CurrencyConversionRequest> parseCsvToRequests(MultipartFile file) {
        List<CurrencyConversionRequest> requests = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader)) {

            for (CSVRecord record : parser) {
                try {
                    BigDecimal amount = new BigDecimal(record.get("amount"));
                    String source = record.get("sourceCurrency").toUpperCase();
                    String target = record.get("targetCurrency").toUpperCase();

                    requests.add(new CurrencyConversionRequest(amount, source, target));
                } catch (Exception e) {
                    log.warn("Failed to parse CSV record: {}", record, e);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse CSV: " + e.getMessage());
        }

        return requests;
    }



    private ExchangeRateResponse fetchRates(ExchangeRateRequest request) {
        return exchangeRateClient.getExchangeRates(request);
    }
}