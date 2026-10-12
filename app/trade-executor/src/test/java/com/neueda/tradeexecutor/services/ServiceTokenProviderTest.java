package com.neueda.tradeexecutor.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.neueda.tradeexecutor.exceptions.ServiceTokenUnavailableException;

class ServiceTokenProviderTest {

    private static final String TOKEN_URL = "http://auth-service:8081/auth/token";
    private static final String CLIENT_SECRET = "the-trade-executor-client-secret";
    private static final Duration TTL = Duration.ofSeconds(300);

    private MockRestServiceServer authService;
    private MutableClock clock;
    private ServiceTokenProvider provider;

    @BeforeEach
    void setUp() {
        RestTemplate tokenClient = new RestTemplate();
        authService = MockRestServiceServer.bindTo(tokenClient).build();
        clock = new MutableClock(Instant.parse("2026-10-12T10:00:00Z"));
        provider = new ServiceTokenProvider(tokenClient, TOKEN_URL, "trade-executor", CLIENT_SECRET, clock);
    }

    @Test
    @DisplayName("Asks auth-service for a token with the client credentials grant, form encoded")
    void requestsTokenWithClientCredentials() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", "trade-executor");
        form.add("client_secret", CLIENT_SECRET);
        authService.expect(once(), requestTo(TOKEN_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(content().formData(form))
                .andRespond(tokenResponse("token-1"));

        assertEquals("token-1", provider.getToken());
        authService.verify();
    }

    @Test
    @DisplayName("Reuses the same token until 30 seconds before it expires")
    void reusesTokenBeforeRefreshWindow() {
        authService.expect(once(), requestTo(TOKEN_URL)).andRespond(tokenResponse("token-1"));

        String first = provider.getToken();
        clock.advance(TTL.minus(ServiceTokenProvider.REFRESH_BEFORE_EXPIRY).minusSeconds(1));

        assertEquals(first, provider.getToken());
        authService.verify();
    }

    @Test
    @DisplayName("Gets a new token automatically 30 seconds before the current one expires")
    void refreshesTokenNearExpiry() {
        authService.expect(once(), requestTo(TOKEN_URL)).andRespond(tokenResponse("token-1"));
        authService.expect(once(), requestTo(TOKEN_URL)).andRespond(tokenResponse("token-2"));

        assertEquals("token-1", provider.getToken());
        clock.advance(TTL.minus(ServiceTokenProvider.REFRESH_BEFORE_EXPIRY));

        assertEquals("token-2", provider.getToken());
        authService.verify();
    }

    @Test
    @DisplayName("Gets a new token once the current one has expired")
    void refreshesTokenAfterExpiry() {
        authService.expect(once(), requestTo(TOKEN_URL)).andRespond(tokenResponse("token-1"));
        authService.expect(once(), requestTo(TOKEN_URL)).andRespond(tokenResponse("token-2"));

        provider.getToken();
        clock.advance(TTL.plusMinutes(1));

        assertEquals("token-2", provider.getToken());
        authService.verify();
    }

    @Test
    @DisplayName("Uses the lifetime auth-service reports rather than a local setting")
    void followsExpiresInFromAuthService() {
        authService.expect(once(), requestTo(TOKEN_URL)).andRespond(tokenResponse("short", 60));
        authService.expect(once(), requestTo(TOKEN_URL)).andRespond(tokenResponse("next", 60));

        provider.getToken();
        clock.advance(Duration.ofSeconds(30));

        assertEquals("next", provider.getToken());
        authService.verify();
    }

    @Test
    @DisplayName("A rejected client secret fails with a clear error that does not contain the secret")
    void wrongSecretFails() {
        authService.expect(once(), requestTo(TOKEN_URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"errorCode\":\"AUTH-401\",\"message\":\"Invalid client credentials\"}"));

        ServiceTokenUnavailableException ex = assertThrows(ServiceTokenUnavailableException.class, provider::getToken);

        assertFalse(ex.getMessage().contains(CLIENT_SECRET));
    }

    @Test
    @DisplayName("Tries again on the next call after a failure instead of caching it")
    void retriesAfterFailure() {
        authService.expect(once(), requestTo(TOKEN_URL)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        authService.expect(once(), requestTo(TOKEN_URL)).andRespond(tokenResponse("token-1"));

        assertThrows(ServiceTokenUnavailableException.class, provider::getToken);

        assertEquals("token-1", provider.getToken());
        authService.verify();
    }

    @Test
    @DisplayName("Rejects a response without a usable access token")
    void rejectsUnusableResponse() {
        authService.expect(once(), requestTo(TOKEN_URL))
                .andRespond(withSuccess("{\"token_type\":\"Bearer\",\"expires_in\":300}", MediaType.APPLICATION_JSON));

        assertThrows(ServiceTokenUnavailableException.class, provider::getToken);
    }

    private static org.springframework.test.web.client.ResponseCreator tokenResponse(String token) {
        return tokenResponse(token, TTL.toSeconds());
    }

    private static org.springframework.test.web.client.ResponseCreator tokenResponse(String token, long expiresIn) {
        return withSuccess("{\"access_token\":\"" + token + "\",\"token_type\":\"Bearer\",\"expires_in\":" + expiresIn + "}",
                MediaType.APPLICATION_JSON);
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
