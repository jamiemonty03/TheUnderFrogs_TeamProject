package com.neueda.tradeexecutor.services;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.neueda.tradeexecutor.exceptions.ServiceTokenUnavailableException;

@Service
public class ServiceTokenProvider {

    static final Duration REFRESH_BEFORE_EXPIRY = Duration.ofSeconds(30);
    static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final RestTemplate tokenClient;
    private final String tokenUrl;
    private final String clientId;
    private final String clientSecret;
    private final Clock clock;

    private String cachedToken;
    private Instant expiresAt = Instant.EPOCH;

    @Autowired
    public ServiceTokenProvider(RestTemplateBuilder builder,
                                @Value("${auth.token-url}") String tokenUrl,
                                @Value("${auth.client-id}") String clientId,
                                @Value("${auth.client-secret}") String clientSecret) {
        this(builder.setConnectTimeout(REQUEST_TIMEOUT).setReadTimeout(REQUEST_TIMEOUT).build(),
                tokenUrl, clientId, clientSecret, Clock.systemUTC());
    }

    ServiceTokenProvider(RestTemplate tokenClient, String tokenUrl, String clientId, String clientSecret, Clock clock) {
        this.tokenClient = tokenClient;
        this.tokenUrl = tokenUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.clock = clock;
    }

    public synchronized String getToken() {
        Instant now = clock.instant();
        if (cachedToken == null || !now.isBefore(expiresAt.minus(REFRESH_BEFORE_EXPIRY))) {
            TokenResponse response = requestToken();
            cachedToken = response.accessToken();
            expiresAt = now.plusSeconds(response.expiresIn());
        }
        return cachedToken;
    }

    private TokenResponse requestToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        TokenResponse response;
        try {
            response = tokenClient.postForObject(tokenUrl, new HttpEntity<>(form, headers), TokenResponse.class);
        } catch (RestClientException e) {
            throw new ServiceTokenUnavailableException(
                    "Could not get a service token from auth-service for client " + clientId, e);
        }
        if (response == null || response.accessToken() == null || response.accessToken().isBlank()
                || response.expiresIn() <= 0) {
            throw new ServiceTokenUnavailableException("auth-service returned an unusable token response");
        }
        return response;
    }

    record TokenResponse(@JsonProperty("access_token") String accessToken,
                         @JsonProperty("token_type") String tokenType,
                         @JsonProperty("expires_in") long expiresIn) {
    }
}
