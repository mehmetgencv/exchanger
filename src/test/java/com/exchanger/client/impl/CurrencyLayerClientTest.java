package com.exchanger.client.impl;

import com.exchanger.dto.requests.ExchangeRateRequest;
import com.exchanger.dto.responses.CurrencyLayerApiResponse;
import com.exchanger.dto.responses.ExchangeRateResponse;
import com.exchanger.exception.ExternalApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
class CurrencyLayerClientTest {

    private MockWebServer mockWebServer;
    private CurrencyLayerClient currencyLayerClient;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        WebClient.Builder webClientBuilder = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString());
        currencyLayerClient =  new CurrencyLayerClient(webClientBuilder, mockWebServer.url("/").toString(), "test-access-key");
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void givenValidRequest_whenGetExchangeRates_thenReturnsExchangeRateResponse() throws Exception {
        ExchangeRateRequest request = new ExchangeRateRequest("USD", List.of("EUR", "GBP"));
        Map<String, BigDecimal> quotes = new HashMap<>();
        quotes.put("USDEUR", new BigDecimal("0.85"));
        quotes.put("USDGBP", new BigDecimal("0.73"));
        CurrencyLayerApiResponse apiResponse = new CurrencyLayerApiResponse(true, "USD", 1697059200L, quotes);

        String jsonResponse = objectMapper.writeValueAsString(apiResponse);

        mockWebServer.enqueue(new MockResponse()
                .setBody(jsonResponse)
                .addHeader("Content-Type", "application/json")
                .setResponseCode(200));

        ExchangeRateResponse response = currencyLayerClient.getExchangeRates(request);

        assertNotNull(response, "Response should not be null");
        assertEquals("USD", response.sourceCurrency(), "Source currency should be USD");
        assertEquals(new BigDecimal("0.85"), response.rates().get("USD_EUR"), "USD to EUR rate should be 0.85");
        assertEquals(new BigDecimal("0.73"), response.rates().get("USD_GBP"), "USD to GBP rate should be 0.73");
    }

    @Test
    void givenFailedApiResponse_whenGetExchangeRates_thenThrowsExternalApiException() throws Exception {
        ExchangeRateRequest request = new ExchangeRateRequest("USD", List.of("EUR"));
        CurrencyLayerApiResponse apiResponse = new CurrencyLayerApiResponse(false, null, 0L, null);

        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(apiResponse))
                .addHeader("Content-Type", "application/json")
                .setResponseCode(200));

        ExternalApiException exception = assertThrows(ExternalApiException.class,
                () -> currencyLayerClient.getExchangeRates(request),
                "Should throw ExternalApiException for failed API response");
        assertEquals("Failed to fetch exchange rates.", exception.getMessage(), "Exception message should match");
    }

    @Test
    void givenNullApiResponse_whenGetExchangeRates_thenThrowsExternalApiException() {
        ExchangeRateRequest request = new ExchangeRateRequest("USD", List.of("EUR"));
        mockWebServer.enqueue(new MockResponse()
                .setBody("")
                .addHeader("Content-Type", "application/json")
                .setResponseCode(200));

        ExternalApiException exception = assertThrows(ExternalApiException.class,
                () -> currencyLayerClient.getExchangeRates(request),
                "Should throw ExternalApiException for null response");
        assertEquals("Failed to fetch exchange rates.", exception.getMessage(), "Exception message should match");
    }
}