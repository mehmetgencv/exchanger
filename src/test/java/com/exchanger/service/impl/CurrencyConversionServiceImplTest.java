package com.exchanger.service.impl;

import com.exchanger.client.ExchangeRateClient;
import com.exchanger.dto.requests.CurrencyConversionHistoryRequest;
import com.exchanger.dto.requests.CurrencyConversionRequest;
import com.exchanger.dto.requests.ExchangeRateRequest;
import com.exchanger.dto.responses.*;
import com.exchanger.entity.CurrencyConversion;
import com.exchanger.exception.ExternalApiException;
import com.exchanger.repository.CurrencyConversionRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class CurrencyConversionServiceImplTest {

    @Mock
    private ExchangeRateClient exchangeRateClient;

    @Mock
    private CurrencyConversionRepository currencyConversionRepository;

    @InjectMocks
    private CurrencyConversionServiceImpl currencyConversionService;

    @Captor
    private ArgumentCaptor<CurrencyConversion> currencyConversionCaptor;

    @Captor
    private ArgumentCaptor<List<CurrencyConversion>> currencyConversionListCaptor;

    @Captor
    private ArgumentCaptor<ExchangeRateRequest> exchangeRateRequestCaptor;


    @BeforeEach
    void setUp() {

    }

    @Test
    void givenValidRequest_whenGetExchangeRates_thenReturnsExchangeRateResponse() {
        // Given
        ExchangeRateRequest request = new ExchangeRateRequest("USD", List.of("EUR", "GBP"));
        Map<String, BigDecimal> ratesMap = new HashMap<>();
        ratesMap.put("USD_EUR", new BigDecimal("0.92"));
        ratesMap.put("USD_GBP", new BigDecimal("0.79"));
        ExchangeRateResponse mockResponse = new ExchangeRateResponse("USD", ratesMap);

        when(exchangeRateClient.getExchangeRates(request)).thenReturn(mockResponse);

        // When
        ExchangeRateResponse actualResponse = currencyConversionService.getExchangeRates(request);

        // Then
        assertNotNull(actualResponse);
        assertEquals("USD", actualResponse.sourceCurrency());
        assertEquals(2, actualResponse.rates().size());
        assertEquals(0, new BigDecimal("0.92").compareTo(actualResponse.rates().get("USD_EUR")));
        assertEquals(0, new BigDecimal("0.79").compareTo(actualResponse.rates().get("USD_GBP")));
        verify(exchangeRateClient).getExchangeRates(request);
    }

    @Test
    void givenClientFailure_whenGetExchangeRates_thenThrowsExternalApiException() {
        // Given
        ExchangeRateRequest request = new ExchangeRateRequest("USD", List.of("EUR"));
        when(exchangeRateClient.getExchangeRates(request)).thenThrow(new ExternalApiException("Service Unavailable"));

        // When & Then
        ExternalApiException exception = assertThrows(ExternalApiException.class, () -> {
            currencyConversionService.getExchangeRates(request);
        });
        assertEquals("Service Unavailable", exception.getMessage());
        verify(exchangeRateClient).getExchangeRates(request);
    }


    @Test
    void givenValidRequest_whenGetSingleExchangeRate_thenReturnsSingleExchangeRateResponse() {
        // Given
        ExchangeRateRequest request = new ExchangeRateRequest("USD", List.of("EUR"));
        Map<String, BigDecimal> ratesMap = Map.of("USD_EUR", new BigDecimal("0.92"));
        ExchangeRateResponse mockClientResponse = new ExchangeRateResponse("USD", ratesMap);

        when(exchangeRateClient.getExchangeRates(request)).thenReturn(mockClientResponse);

        // When
        SingleExchangeRateResponse actualResponse = currencyConversionService.getSingleExchangeRate(request);

        // Then
        assertNotNull(actualResponse);
        assertEquals("USD", actualResponse.sourceCurrency());
        assertEquals("EUR", actualResponse.targetCurrency());
        assertEquals(0, new BigDecimal("0.92").compareTo(actualResponse.exchangeRate()));
        verify(exchangeRateClient).getExchangeRates(request);
    }

    @Test
    void givenRateNotFoundForTarget_whenGetSingleExchangeRate_thenReturnsResponseWithNullRateOrThrows() {
        // Given
        ExchangeRateRequest request = new ExchangeRateRequest("USD", List.of("XYZ"));
        // Client might return an empty map or a map without the key
        ExchangeRateResponse mockClientResponse = new ExchangeRateResponse("USD", Collections.emptyMap());
        when(exchangeRateClient.getExchangeRates(request)).thenReturn(mockClientResponse);

        // When
        SingleExchangeRateResponse actualResponse = currencyConversionService.getSingleExchangeRate(request);

        // Then
        assertNotNull(actualResponse);
        assertEquals("USD", actualResponse.sourceCurrency());
        assertEquals("XYZ", actualResponse.targetCurrency());
        assertNull(actualResponse.exchangeRate());
        verify(exchangeRateClient).getExchangeRates(request);
    }


    // --- convert ---
    @Test
    void givenValidConversionRequest_whenConvert_thenReturnsResponseAndSavesConversion() {
        // Given
        CurrencyConversionRequest request = new CurrencyConversionRequest(
                new BigDecimal("100.00"), "USD", "EUR");
        UUID transactionId = UUID.randomUUID();

        // Mock client response for the specific internal ExchangeRateRequest
        Map<String, BigDecimal> ratesMap = Map.of("USD_EUR", new BigDecimal("0.92"));
        ExchangeRateResponse mockRateResponse = new ExchangeRateResponse("USD", ratesMap);

        // Capture the ExchangeRateRequest made by the service
        when(exchangeRateClient.getExchangeRates(exchangeRateRequestCaptor.capture())).thenReturn(mockRateResponse);


        CurrencyConversion savedConversion = new CurrencyConversion(
                "USD", "EUR", new BigDecimal("100.00"),
                new BigDecimal("92.00"), new BigDecimal("0.92")
        );
        savedConversion.setId(transactionId); // ID is set by repository
        savedConversion.setTransactionDate(LocalDateTime.now()); // Date is set by entity/service

        when(currencyConversionRepository.save(any(CurrencyConversion.class))).thenReturn(savedConversion);

        // When
        CurrencyConversionResponse actualResponse = currencyConversionService.convert(request);

        // Then
        assertNotNull(actualResponse);
        assertEquals(transactionId, actualResponse.transactionId());
        assertEquals(0, new BigDecimal("92.00").compareTo(actualResponse.convertedAmount()));

        // Verify the captured ExchangeRateRequest
        ExchangeRateRequest capturedErRequest = exchangeRateRequestCaptor.getValue();
        assertEquals("USD", capturedErRequest.sourceCurrency());
        assertTrue(capturedErRequest.targetCurrencies().contains("EUR"));
        assertEquals(1, capturedErRequest.targetCurrencies().size());


        verify(currencyConversionRepository).save(currencyConversionCaptor.capture());
        CurrencyConversion capturedConversion = currencyConversionCaptor.getValue();
        assertEquals("USD", capturedConversion.getSourceCurrency());
        assertEquals("EUR", capturedConversion.getTargetCurrency());
        assertEquals(0, new BigDecimal("100.00").compareTo(capturedConversion.getSourceAmount()));
        assertEquals(0, new BigDecimal("0.92").compareTo(capturedConversion.getExchangeRate()));
        assertEquals(0, new BigDecimal("92.00").compareTo(capturedConversion.getConvertedAmount()));
        assertNotNull(capturedConversion.getTransactionDate());
    }

    @Test
    void givenRateNotFoundForPair_whenConvert_thenThrowsExternalApiExceptionAndDoesNotSave() {
        // Given
        CurrencyConversionRequest request = new CurrencyConversionRequest(
                new BigDecimal("100.00"), "USD", "XYZ");

        ExchangeRateResponse mockRateResponse = new ExchangeRateResponse("USD", Collections.emptyMap());
        when(exchangeRateClient.getExchangeRates(any(ExchangeRateRequest.class))).thenReturn(mockRateResponse);

        // When & Then
        ExternalApiException exception = assertThrows(ExternalApiException.class, () -> {
            currencyConversionService.convert(request);
        });
        assertEquals("No exchange rate found for USD_XYZ", exception.getMessage());
        verify(currencyConversionRepository, never()).save(any(CurrencyConversion.class));
    }

    @Test
    void givenRepositoryFailure_whenConvert_thenThrowsRepositoryException() {
        // Given
        CurrencyConversionRequest request = new CurrencyConversionRequest(
                new BigDecimal("100.00"), "USD", "EUR");

        Map<String, BigDecimal> ratesMap = Map.of("USD_EUR", new BigDecimal("0.92"));
        ExchangeRateResponse mockRateResponse = new ExchangeRateResponse("USD", ratesMap);
        when(exchangeRateClient.getExchangeRates(any(ExchangeRateRequest.class))).thenReturn(mockRateResponse);

        when(currencyConversionRepository.save(any(CurrencyConversion.class)))
                .thenThrow(new RuntimeException("Database save failed"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            currencyConversionService.convert(request);
        });
        assertEquals("Database save failed", exception.getMessage());
    }


    // --- getHistory ---
    @Test
    void givenHistoryRequestByTransactionIdAndConversionExists_whenGetHistory_thenReturnsPageWithOneItem() {
        // Given
        UUID transactionId = UUID.randomUUID();
        CurrencyConversionHistoryRequest request = new CurrencyConversionHistoryRequest(transactionId, null);
        Pageable pageable = PageRequest.of(0, 10);

        CurrencyConversion conversion = new CurrencyConversion("USD", "EUR", BigDecimal.TEN, new BigDecimal("9.20"), new BigDecimal("0.92"));
        conversion.setId(transactionId);
        conversion.setTransactionDate(LocalDateTime.now());
        when(currencyConversionRepository.findById(transactionId)).thenReturn(Optional.of(conversion));

        // When
        Page<CurrencyConversionHistoryResponse> resultPage = currencyConversionService.getHistory(request, pageable);

        // Then
        assertNotNull(resultPage);
        assertEquals(1, resultPage.getTotalElements());
        assertEquals(1, resultPage.getContent().size());
        CurrencyConversionHistoryResponse responseItem = resultPage.getContent().getFirst();
        assertEquals(transactionId, responseItem.transactionId());
        assertEquals("USD", responseItem.sourceCurrency());
        verify(currencyConversionRepository).findById(transactionId);
        verify(currencyConversionRepository, never()).findAllByTransactionDateBetween(any(), any(), any());
    }

    @Test
    void givenHistoryRequestByTransactionIdAndConversionDoesNotExist_whenGetHistory_thenReturnsEmptyPage() {
        // Given
        UUID transactionId = UUID.randomUUID();
        CurrencyConversionHistoryRequest request = new CurrencyConversionHistoryRequest(transactionId, null);
        Pageable pageable = PageRequest.of(0, 10);
        when(currencyConversionRepository.findById(transactionId)).thenReturn(Optional.empty());

        // When
        Page<CurrencyConversionHistoryResponse> resultPage = currencyConversionService.getHistory(request, pageable);

        // Then
        assertNotNull(resultPage);
        assertTrue(resultPage.isEmpty());
        verify(currencyConversionRepository).findById(transactionId);
    }

    @Test
    void givenHistoryRequestByDateAndConversionsExist_whenGetHistory_thenReturnsPageWithMultipleItems() {
        // Given
        LocalDate date = LocalDate.of(2023, 10, 26);
        CurrencyConversionHistoryRequest request = new CurrencyConversionHistoryRequest(null, date);
        Pageable pageable = PageRequest.of(0, 10);

        CurrencyConversion c1 = new CurrencyConversion("USD", "EUR", BigDecimal.TEN, new BigDecimal("9.20"), new BigDecimal("0.92"));
        c1.setId(UUID.randomUUID());
        c1.setTransactionDate(date.atTime(10,0));
        CurrencyConversion c2 = new CurrencyConversion("GBP", "JPY", BigDecimal.ONE, new BigDecimal("150"), new BigDecimal("150"));
        c2.setId(UUID.randomUUID());
        c2.setTransactionDate(date.atTime(14,0));
        List<CurrencyConversion> conversions = List.of(c1, c2);
        Page<CurrencyConversion> mockPage = new PageImpl<>(conversions, pageable, conversions.size());

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        when(currencyConversionRepository.findAllByTransactionDateBetween(eq(start), eq(end), eq(pageable)))
                .thenReturn(mockPage);

        // When
        Page<CurrencyConversionHistoryResponse> resultPage = currencyConversionService.getHistory(request, pageable);

        // Then
        assertNotNull(resultPage);
        assertEquals(2, resultPage.getTotalElements());
        assertEquals(2, resultPage.getContent().size());
        assertEquals(c1.getId(), resultPage.getContent().get(0).transactionId());
        assertEquals(c2.getId(), resultPage.getContent().get(1).transactionId());
        verify(currencyConversionRepository).findAllByTransactionDateBetween(eq(start), eq(end), eq(pageable));
        verify(currencyConversionRepository, never()).findById(any());
    }

    // --- processCsvFile ---

    private MultipartFile createMockCsvFile(String content) {
        return new MockMultipartFile("file.csv", "file.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    private String createCsvContent(String[] headers, List<String[]> data) throws IOException {
        StringWriter sw = new StringWriter();
        try (CSVPrinter printer = new CSVPrinter(sw, CSVFormat.DEFAULT.withHeader(headers))) {
            for (String[] record : data) {
                printer.printRecord((Object[]) record);
            }
        }
        return sw.toString();
    }

    @Test
    void givenValidCsvFile_whenProcessCsvFile_thenReturnsSuccessfulResponsesAndSavesAll() throws IOException {
        // Given
        String csvContent = createCsvContent(
                new String[]{"amount", "sourceCurrency", "targetCurrency"},
                List.of(
                        new String[]{"100", "USD", "EUR"},
                        new String[]{"200", "USD", "GBP"},
                        new String[]{"50", "EUR", "USD"}
                )
        );
        MultipartFile csvFile = createMockCsvFile(csvContent);

        // Mock ExchangeRateClient behavior for USD group
        ExchangeRateRequest usdRequest = new ExchangeRateRequest("USD", List.of("EUR", "GBP"));
        Map<String, BigDecimal> usdRates = Map.of("USD_EUR", new BigDecimal("0.92"), "USD_GBP", new BigDecimal("0.79"));
        when(exchangeRateClient.getExchangeRates(eq(usdRequest)))
                .thenReturn(new ExchangeRateResponse("USD", usdRates));

        // Mock ExchangeRateClient behavior for EUR group
        ExchangeRateRequest eurRequest = new ExchangeRateRequest("EUR", List.of("USD"));
        Map<String, BigDecimal> eurRates = Map.of("EUR_USD", new BigDecimal("1.08"));
        when(exchangeRateClient.getExchangeRates(eq(eurRequest)))
                .thenReturn(new ExchangeRateResponse("EUR", eurRates));

        // Mock repository saveAll
        when(currencyConversionRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<CurrencyConversion> toSave = invocation.getArgument(0);
            // Simulate saving by assigning IDs
            toSave.forEach(c -> c.setId(UUID.randomUUID()));
            return toSave;
        });

        // When
        List<BulkConversionResponse> responses = currencyConversionService.processCsvFile(csvFile);

        // Then
        assertEquals(3, responses.size());

        // Response 1
        BulkConversionResponse r1 = responses.getFirst();
        assertEquals("EUR", r1.sourceCurrency());
        assertEquals("USD", r1.targetCurrency());
        assertEquals(new BigDecimal("50"), r1.originalAmount());
        assertNotNull(r1.transactionId());
        assertEquals(0, new BigDecimal("54.00").compareTo(r1.convertedAmount()));
        assertNull(r1.errorMessage());

        // Response 2
        BulkConversionResponse r2 = responses.get(1);
        assertEquals("USD", r2.sourceCurrency());
        assertEquals("EUR", r2.targetCurrency());
        assertEquals(new BigDecimal("100"), r2.originalAmount());
        assertNotNull(r2.transactionId());
        assertEquals(0, new BigDecimal("92.00").compareTo(r2.convertedAmount()));
        assertNull(r2.errorMessage());

        // Response 3
        BulkConversionResponse r3 = responses.get(2);
        assertEquals("USD", r3.sourceCurrency());
        assertEquals("GBP", r3.targetCurrency());
        assertEquals(new BigDecimal("200"), r3.originalAmount());
        assertNotNull(r3.transactionId());
        assertEquals(0, new BigDecimal("158.00").compareTo(r3.convertedAmount()));
        assertNull(r3.errorMessage());

        verify(currencyConversionRepository).saveAll(currencyConversionListCaptor.capture());
        List<CurrencyConversion> savedEntities = currencyConversionListCaptor.getValue();
        assertEquals(3, savedEntities.size());
    }

    @Test
    void givenCsvFileWithMissingRates_whenProcessCsvFile_thenReturnsMixedResponsesAndSavesSuccessfulOnes() throws IOException {
        // Given
        String csvContent = createCsvContent(
                new String[]{"amount", "sourceCurrency", "targetCurrency"},
                List.of(
                        new String[]{"100", "USD", "EUR"}, // Success
                        new String[]{"50", "USD", "XYZ"},  // Rate not found
                        new String[]{"75", "CAD", "USD"}   // Success
                )
        );
        MultipartFile csvFile = createMockCsvFile(csvContent);

        // Mock client for USD (EUR found, XYZ not)
        ExchangeRateRequest usdRequest = new ExchangeRateRequest("USD", List.of("EUR", "XYZ"));
        Map<String, BigDecimal> usdRates = Map.of("USD_EUR", new BigDecimal("0.92")); // No USD_XYZ
        when(exchangeRateClient.getExchangeRates(eq(usdRequest)))
                .thenReturn(new ExchangeRateResponse("USD", usdRates));

        // Mock client for CAD
        ExchangeRateRequest cadRequest = new ExchangeRateRequest("CAD", List.of("USD"));
        Map<String, BigDecimal> cadRates = Map.of("CAD_USD", new BigDecimal("0.75"));
        when(exchangeRateClient.getExchangeRates(eq(cadRequest)))
                .thenReturn(new ExchangeRateResponse("CAD", cadRates));

        when(currencyConversionRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<CurrencyConversion> toSave = invocation.getArgument(0);
            toSave.forEach(c -> c.setId(UUID.randomUUID()));
            return toSave;
        });

        // When
        List<BulkConversionResponse> responses = currencyConversionService.processCsvFile(csvFile);

        // Then
        assertEquals(3, responses.size());

        // USD -> EUR (Success)
        assertTrue(responses.stream().anyMatch(r -> r.convertedAmount() != null && r.convertedAmount().compareTo(new BigDecimal("92.00")) == 0 && r.transactionId() != null));
        // USD -> XYZ (Fail)
        assertTrue(responses.stream().anyMatch(r -> "Rate not found for USD_XYZ".equals(r.errorMessage()) && r.transactionId() == null));
        // CAD -> USD (Success)
        assertTrue(responses.stream().anyMatch(r -> r.convertedAmount() != null && r.convertedAmount().compareTo(new BigDecimal("56.25")) == 0 && r.transactionId() != null));


        verify(currencyConversionRepository).saveAll(currencyConversionListCaptor.capture());
        List<CurrencyConversion> savedEntities = currencyConversionListCaptor.getValue();
        assertEquals(2, savedEntities.size()); // Only successful ones
    }

    @Test
    void givenCsvFileWithMalformedRows_whenProcessCsvFile_thenSkipsMalformedAndProcessesValid() throws IOException {
        // Given
        String csvContent = createCsvContent(
                new String[]{"amount", "sourceCurrency", "targetCurrency"},
                List.of(
                        new String[]{"100", "USD", "EUR"},      // Valid
                        new String[]{"INVALID", "USD", "GBP"}, // Malformed amount
                        new String[]{"50", "EUR", "USD"}       // Valid
                )
        );
        MultipartFile csvFile = createMockCsvFile(csvContent);

        ExchangeRateRequest usdRequest = new ExchangeRateRequest("USD", List.of("EUR")); // GBP row is skipped
        Map<String, BigDecimal> usdRates = Map.of("USD_EUR", new BigDecimal("0.92"));
        when(exchangeRateClient.getExchangeRates(eq(usdRequest)))
                .thenReturn(new ExchangeRateResponse("USD", usdRates));

        ExchangeRateRequest eurRequest = new ExchangeRateRequest("EUR", List.of("USD"));
        Map<String, BigDecimal> eurRates = Map.of("EUR_USD", new BigDecimal("1.08"));
        when(exchangeRateClient.getExchangeRates(eq(eurRequest)))
                .thenReturn(new ExchangeRateResponse("EUR", eurRates));

        when(currencyConversionRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<CurrencyConversion> toSave = invocation.getArgument(0);
            toSave.forEach(c -> c.setId(UUID.randomUUID()));
            return toSave;
        });

        // When
        List<BulkConversionResponse> responses = currencyConversionService.processCsvFile(csvFile);

        // Then
        assertEquals(2, responses.size()); // Malformed row is skipped, so 2 results for 2 valid conversions
        assertTrue(responses.stream().anyMatch(r -> r.convertedAmount() != null && r.convertedAmount().compareTo(new BigDecimal("92.00")) == 0));
        assertTrue(responses.stream().anyMatch(r -> r.convertedAmount() != null && r.convertedAmount().compareTo(new BigDecimal("54.00")) == 0));

        verify(currencyConversionRepository).saveAll(currencyConversionListCaptor.capture());
        assertEquals(2, currencyConversionListCaptor.getValue().size());

    }


    @Test
    void givenCsvFileAndOneGroupFails_whenProcessCsvFile_thenSuccessfulOnesSavedAndFailuresReported() throws IOException {
        // Given
        String csvContent = createCsvContent(
                new String[]{"amount", "sourceCurrency", "targetCurrency"},
                List.of(
                        new String[]{"100", "USD", "EUR"},
                        new String[]{"200", "JPY", "USD"}
                )
        );
        MultipartFile csvFile = createMockCsvFile(csvContent);

        ExchangeRateRequest usdRequest = new ExchangeRateRequest("USD", List.of("EUR"));
        Map<String, BigDecimal> usdRates = Map.of("USD_EUR", new BigDecimal("0.92"));
        when(exchangeRateClient.getExchangeRates(eq(usdRequest)))
                .thenReturn(new ExchangeRateResponse("USD", usdRates));

        ExchangeRateRequest jpyRequest = new ExchangeRateRequest("JPY", List.of("USD"));
        when(exchangeRateClient.getExchangeRates(eq(jpyRequest)))
                .thenThrow(new ExternalApiException("JPY Service Error"));

        when(currencyConversionRepository.saveAll(anyList()))
                .thenAnswer(invocation -> {
                    List<CurrencyConversion> toSave = invocation.getArgument(0);
                    toSave.forEach(e -> e.setId(UUID.randomUUID()));
                    return toSave;
                });

        // When
        List<BulkConversionResponse> responses = currencyConversionService.processCsvFile(csvFile);

        // Then
        assertEquals(2, responses.size());

        BulkConversionResponse jpyResponse = responses.get(0);
        assertNull(jpyResponse.transactionId());
        assertNotNull(jpyResponse.errorMessage());
        assertTrue(jpyResponse.errorMessage().contains("JPY"));

        BulkConversionResponse usdResponse = responses.get(1);
        assertNotNull(usdResponse.transactionId());
        assertNull(usdResponse.errorMessage());
        assertEquals(new BigDecimal("92.00"), usdResponse.convertedAmount());


        verify(currencyConversionRepository).saveAll(anyList());
    }


    @Test
    void givenMalformedCsvFileStructure_whenProcessCsvFile_thenThrowsRuntimeException() {
        // Given
        // CSV that would cause CSVParser to throw, e.g., unclosed quote
        String malformedCsvContent = "amount,sourceCurrency,targetCurrency\n100,USD,\"EUR";
        MultipartFile csvFile = createMockCsvFile(malformedCsvContent);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            currencyConversionService.processCsvFile(csvFile);
        });
        assertTrue(exception.getMessage().startsWith("Failed to parse CSV:"));
    }

    @Test
    void givenEmptyCsvFile_whenProcessCsvFile_thenReturnsEmptyListAndSavesNothing() throws IOException {
        // Given
        String emptyCsvContent = createCsvContent(
                new String[]{"amount", "sourceCurrency", "targetCurrency"},
                Collections.emptyList()
        );
        MultipartFile csvFile = createMockCsvFile(emptyCsvContent); // File with only headers

        // When
        List<BulkConversionResponse> responses = currencyConversionService.processCsvFile(csvFile);

        // Then
        assertTrue(responses.isEmpty());
        verify(currencyConversionRepository, never()).saveAll(anyList());
        verify(exchangeRateClient, never()).getExchangeRates(any());
    }
}