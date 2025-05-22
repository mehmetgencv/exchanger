package com.exchanger.exception;

public record CurrencyLayerError(
        int code,
        String type,
        String info
) {}
