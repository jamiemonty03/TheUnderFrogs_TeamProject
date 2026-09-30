package com.neueda.orderservice.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.OctetSequenceKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import com.neueda.orderservice.controllers.OrderController;
import com.neueda.orderservice.repositories.OrderRepository;
import com.neueda.orderservice.services.orderServices.OrderProcessor;
import com.neueda.orderservice.services.OrderCancellationService;

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "jwt.secret=" + ServiceTokenSecurityTest.SECRET)
class ServiceTokenSecurityTest {

    static final String SECRET = "test-secret-that-is-at-least-32-bytes-long!!";
    private static final String WRONG_SECRET = "a-different-secret-that-is-also-32-bytes-long";
    private static final String ORDERS = "/orders";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderProcessor orderProcessor;

    @MockBean
    private OrderCancellationService orderCancellationService;

    @MockBean
    private OrderRepository orderRepository;

    @MockBean
    private RestTemplate restTemplate;

    @Test
    @DisplayName("Service token is accepted")
    void acceptsServiceToken() throws Exception {
        Mockito.when(orderRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        mockMvc.perform(get(ORDERS).header(HttpHeaders.AUTHORIZATION, bearer(serviceToken(SECRET))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Missing token is rejected with 401")
    void rejectsMissingToken() throws Exception {
        mockMvc.perform(get(ORDERS)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Garbage token is rejected with 401")
    void rejectsGarbageToken() throws Exception {
        mockMvc.perform(get(ORDERS).header(HttpHeaders.AUTHORIZATION, bearer("not-a-jwt")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token signed with the wrong secret is rejected with 401")
    void rejectsWrongSecret() throws Exception {
        mockMvc.perform(get(ORDERS).header(HttpHeaders.AUTHORIZATION, bearer(serviceToken(WRONG_SECRET))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Expired service token is rejected with 401")
    void rejectsExpiredToken() throws Exception {
        String expired = token(SECRET, List.of("SERVICE"), Instant.now().minus(Duration.ofMinutes(5)));
        mockMvc.perform(get(ORDERS).header(HttpHeaders.AUTHORIZATION, bearer(expired)))
                .andExpect(status().isUnauthorized());
    }

    private static String serviceToken(String secret) {
        return token(secret, List.of("SERVICE"), Instant.now().plus(Duration.ofMinutes(5)));
    }

    private static String token(String secret, List<String> roles, Instant expiresAt) {
        OctetSequenceKey key = new OctetSequenceKey.Builder(secret.getBytes(StandardCharsets.UTF_8))
                .algorithm(JWSAlgorithm.HS256)
                .build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("trade-executor")
                .issuedAt(expiresAt.minus(Duration.ofMinutes(5)))
                .expiresAt(expiresAt)
                .claim("roles", roles)
                .build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)))
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
