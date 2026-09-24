package com.neueda.orderservice.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class BearerTokenForwardingInterceptorTest {

    private final BearerTokenForwardingInterceptor interceptor = new BearerTokenForwardingInterceptor();
    private final ClientHttpRequestExecution execution = mock(ClientHttpRequestExecution.class);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static Jwt jwt(String tokenValue) {
        return Jwt.withTokenValue(tokenValue)
                .header("alg", "HS256")
                .subject("alice")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }

    @Test
    @DisplayName("Copies the caller's JWT onto the outgoing request")
    void forwardsCallerToken() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt("caller-token")));
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, "/accounts/ACC0001");

        interceptor.intercept(request, new byte[0], execution);

        assertEquals("Bearer caller-token", request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
        verify(execution).execute(request, new byte[0]);
    }

    @Test
    @DisplayName("Leaves the request alone when there is no authenticated JWT")
    void noTokenWhenUnauthenticated() throws Exception {
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, "/accounts/ACC0001");

        interceptor.intercept(request, new byte[0], execution);

        assertNull(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    @DisplayName("Ignores non-JWT authentication")
    void ignoresNonJwtAuthentication() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("alice", "pw"));
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, "/accounts/ACC0001");

        interceptor.intercept(request, new byte[0], execution);

        assertNull(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    @DisplayName("Keeps an Authorization header that is already set")
    void keepsExistingAuthorizationHeader() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt("caller-token")));
        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, "/accounts/ACC0001");
        request.getHeaders().setBearerAuth("explicit-token");

        interceptor.intercept(request, new byte[0], execution);

        assertEquals("Bearer explicit-token", request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION));
    }
}
