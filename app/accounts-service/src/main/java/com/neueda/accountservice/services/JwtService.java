package com.neueda.accountservice.services;

import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final long jwtExpirationMs;

    public JwtService(JwtEncoder jwtEncoder, @Value("${jwt.expiration}") long jwtExpirationMs) {
        this.jwtEncoder = jwtEncoder;
        this.jwtExpirationMs = jwtExpirationMs;
    }

    public String generateToken(String username) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("accounts-service")
                .subject(username)
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofMillis(jwtExpirationMs)))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
