package com.neueda.tradeexecutor.config;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.neueda.tradeexecutor.services.ServiceTokenProvider;

class ServiceTokenInterceptorTest {

    private static final String URL = "http://orders-service:8081/api/orders/ORD-1";

    private ServiceTokenProvider serviceTokenProvider;
    private RestTemplate restTemplate;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        serviceTokenProvider = mock(ServiceTokenProvider.class);
        when(serviceTokenProvider.getToken()).thenReturn("service-token");
        restTemplate = new RestTemplateConfig().restTemplate(new RestTemplateBuilder(), serviceTokenProvider);
        server = MockRestServiceServer.bindTo(restTemplate).build();
    }

    @Test
    @DisplayName("Every outgoing call carries the executor's service token")
    void addsServiceToken() {
        server.expect(requestTo(URL))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer service-token"))
                .andRespond(withSuccess());

        restTemplate.getForObject(URL, String.class);

        server.verify();
    }

    @Test
    @DisplayName("A request that already has an Authorization header is left unchanged")
    void keepsExistingAuthorizationHeader() {
        server.expect(requestTo(URL))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer existing-token"))
                .andRespond(withSuccess());

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("existing-token");
        restTemplate.exchange(URL, HttpMethod.GET, new HttpEntity<>(headers), String.class);

        server.verify();
        verify(serviceTokenProvider, never()).getToken();
    }
}
