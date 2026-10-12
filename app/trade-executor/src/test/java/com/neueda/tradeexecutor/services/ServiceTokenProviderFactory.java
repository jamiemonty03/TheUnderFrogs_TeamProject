package com.neueda.tradeexecutor.services;

import java.time.Clock;

import org.springframework.web.client.RestTemplate;

public final class ServiceTokenProviderFactory {

    private ServiceTokenProviderFactory() {
    }

    public static ServiceTokenProvider create(RestTemplate tokenClient, String tokenUrl, Clock clock) {
        return new ServiceTokenProvider(tokenClient, tokenUrl, "trade-executor", "test-client-secret", clock);
    }
}
