package com.neueda.tradeexecutor.config;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Clock;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.neueda.tradeexecutor.services.ServiceTokenProvider;
import com.neueda.tradeexecutor.services.ServiceTokenProviderFactory;

class ServiceTokenFlowTest {

    private static final String TOKEN_URL = "http://auth-service:8081/auth/token";
    private static final List<String> SERVICE_URLS = List.of(
            "http://orders-service:8081/api/orders/ORD-1",
            "http://instruments-service:8081/api/instruments/AAPL",
            "http://accounts-service:8081/api/accounts/ACC0001",
            "http://positions-service:8081/api/positions/ACC0001");

    @Test
    @DisplayName("One token from auth-service is sent to all four services")
    void sameAuthServiceTokenGoesToAllFourServices() {
        RestTemplate tokenClient = new RestTemplate();
        MockRestServiceServer authService = MockRestServiceServer.bindTo(tokenClient).build();
        authService.expect(once(), requestTo(TOKEN_URL)).andRespond(withSuccess(
                "{\"access_token\":\"token-from-auth-service\",\"token_type\":\"Bearer\",\"expires_in\":300}",
                MediaType.APPLICATION_JSON));
        ServiceTokenProvider provider = ServiceTokenProviderFactory.create(tokenClient, TOKEN_URL, Clock.systemUTC());

        RestTemplate apiClient = new RestTemplateConfig().restTemplate(new RestTemplateBuilder(), provider);
        MockRestServiceServer services = MockRestServiceServer.bindTo(apiClient).build();
        for (String url : SERVICE_URLS) {
            services.expect(requestTo(url))
                    .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer token-from-auth-service"))
                    .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        }

        SERVICE_URLS.forEach(url -> apiClient.getForObject(url, String.class));

        services.verify();
        authService.verify();
    }

    @Test
    @DisplayName("The executor has no way to sign tokens itself")
    void noSigningLibraryOnTheClasspath() {
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("org.springframework.security.oauth2.jwt.JwtEncoder"));
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("com.nimbusds.jose.JWSSigner"));
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("com.neueda.tradeexecutor.config.JwtEncoderConfig"));
    }
}
