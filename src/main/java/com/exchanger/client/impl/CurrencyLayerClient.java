package com.exchanger.client.impl;

import com.exchanger.client.ExchangeRateClient;
import com.exchanger.dto.requests.ExchangeRateRequest;
import com.exchanger.dto.responses.CurrencyLayerApiResponse;
import com.exchanger.dto.responses.ExchangeRateResponse;
import com.exchanger.exception.ExternalApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component("currencyLayerClient")
public class CurrencyLayerClient implements ExchangeRateClient {

    private final WebClient webClient;
    private final String accessKey;

    public CurrencyLayerClient(
            WebClient.Builder builder,
            @Value("${exchange.api.url}") String baseUrl,
            @Value("${exchange.api.key}") String accessKey
    ) {
        this.webClient = builder.baseUrl(baseUrl).build();
        this.accessKey = accessKey;
    }

    @Override
    public ExchangeRateResponse getExchangeRates(ExchangeRateRequest request) {
        String source = request.sourceCurrency();
        String currencies = String.join(",", request.targetCurrencies());

        CurrencyLayerApiResponse rawResponse = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/live")
                        .queryParam("access_key", accessKey)
                        .queryParam("source", source)
                        .queryParam("currencies", currencies)
                        .build())
                .retrieve()
                .bodyToMono(CurrencyLayerApiResponse.class)
                .block();

        if (rawResponse == null || !rawResponse.success() || rawResponse.quotes() == null) {
            throw new ExternalApiException("Failed to fetch exchange rates.");
        }

        Map<String, BigDecimal> rates = new HashMap<>();
        rawResponse.quotes().forEach((key, value) -> {
            String formattedKey = key.substring(0, 3) + "_" + key.substring(3); // USD_EUR
            rates.put(formattedKey, value);
        });

        return new ExchangeRateResponse(source, rates);
    }

}
