package com.exchanger.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ExchangeRateClientFactory {

    private final Map<String, ExchangeRateClient> clients;
    private final String provider;

    public ExchangeRateClientFactory(
            Map<String, ExchangeRateClient> clients,
            @Value("${exchange.provider:currencyLayerClient}") String provider
    ) {
        this.clients = clients;
        this.provider = provider;
    }

    public ExchangeRateClient getClient() {
        return clients.getOrDefault(provider, clients.get("currencyLayerClient"));
    }
}
