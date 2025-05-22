package com.exchanger.controller;


import com.exchanger.dto.requests.ExchangeRateRequest;
import com.exchanger.dto.responses.ExchangeRateResponse;
import com.exchanger.dto.responses.SingleExchangeRateResponse;
import com.exchanger.service.CurrencyConversionService;
import jakarta.validation.constraints.NotBlank;
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

    @GetMapping("/exchange-rate")
    public ResponseEntity<SingleExchangeRateResponse> getExchangeRate(
            @RequestParam @NotBlank String source,
            @RequestParam @NotBlank String target
    ) {

        ExchangeRateRequest request = new ExchangeRateRequest(source.toUpperCase(), List.of(target.toUpperCase()) );
        SingleExchangeRateResponse response = currencyConversionService.getSingleExchangeRate(request);

        return ResponseEntity.ok(response);
    }
}
