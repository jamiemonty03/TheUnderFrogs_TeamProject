package com.neueda.accountservice.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

class JwtServiceTest {

    @Test
    void generateTokenUsesHs256AndExpectedClaims() {
        JwtEncoder encoder = mock(JwtEncoder.class);
        Jwt encodedJwt = mock(Jwt.class);
        when(encodedJwt.getTokenValue()).thenReturn("encoded.jwt.token");
        when(encoder.encode(any(JwtEncoderParameters.class))).thenReturn(encodedJwt);
        long expirationMs = Duration.ofMinutes(15).toMillis();
        JwtService jwtService = new JwtService(encoder, expirationMs);

        String token = jwtService.generateToken("test-user");

        assertEquals("encoded.jwt.token", token);
        var parameters = org.mockito.ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(encoder).encode(parameters.capture());
        var header = parameters.getValue().getJwsHeader();
        var claims = parameters.getValue().getClaims();

        assertEquals(MacAlgorithm.HS256, header.getAlgorithm());
        assertEquals("accounts-service", claims.getClaimAsString("iss"));
        assertEquals("test-user", claims.getSubject());
        assertNotNull(claims.getIssuedAt());
        assertEquals(claims.getIssuedAt().plusMillis(expirationMs), claims.getExpiresAt());
    }
}
