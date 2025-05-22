package com.exchanger.controller;


import com.exchanger.dto.requests.CurrencyConversionHistoryRequest;
import com.exchanger.dto.requests.CurrencyConversionRequest;
import com.exchanger.dto.requests.ExchangeRateRequest;
import com.exchanger.dto.responses.CurrencyConversionHistoryResponse;
import com.exchanger.dto.responses.CurrencyConversionResponse;
import com.exchanger.dto.responses.SingleExchangeRateResponse;
import com.exchanger.service.CurrencyConversionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/conversion")
public class CurrencyConversionController {

    private final CurrencyConversionService currencyConversionService;

    public CurrencyConversionController(CurrencyConversionService currencyConversionService) {
        this.currencyConversionService = currencyConversionService;
    }

    @Operation(
            summary = "Get exchange rate between two currencies",
            description = "Returns the current exchange rate for a given source and target currency pair."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful exchange rate retrieval"),
            @ApiResponse(responseCode = "400", description = "Invalid input parameters"),
            @ApiResponse(responseCode = "502", description = "External API error")
    })
    @GetMapping("/exchange-rate")
    public ResponseEntity<SingleExchangeRateResponse> getExchangeRate(
            @Parameter(description = "Source currency code (e.g. USD)", example = "USD")
            @RequestParam @NotBlank String source,

            @Parameter(description = "Target currency code (e.g. EUR)", example = "EUR")
            @RequestParam @NotBlank String target
    ) {

        ExchangeRateRequest request = new ExchangeRateRequest(source.toUpperCase(), List.of(target.toUpperCase()) );
        SingleExchangeRateResponse response = currencyConversionService.getSingleExchangeRate(request);

        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Convert currency amount",
            description = "Converts a given amount from source currency to target currency and returns the converted amount and transaction ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Conversion successful",
                    content = @Content(schema = @Schema(implementation = CurrencyConversionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data", content = @Content),
            @ApiResponse(responseCode = "502", description = "Failed to fetch exchange rate from external API", content = @Content)
    })
    @PostMapping
    public ResponseEntity<CurrencyConversionResponse> convertCurrency(
            @RequestBody @Valid CurrencyConversionRequest request
    ) {
        CurrencyConversionResponse response = currencyConversionService.convert(request);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get currency conversion history",
            description = "Returns paginated conversion history filtered by transactionId or date. At least one must be provided."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "History retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed for request"),
    })
    @GetMapping("/history")
    public Page<CurrencyConversionHistoryResponse> getConversionHistory(
            @Valid CurrencyConversionHistoryRequest request,
            @Parameter(hidden = true) Pageable pageable
    ) {
        return currencyConversionService.getHistory(request, pageable);
    }
}
