package com.neueda.accountservice.services;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

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
        return generateToken(username, null, null);
    }

    public String generateToken(String username, String accountId, List<String> roles) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .issuer("accounts-service")
                .subject(username)
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofMillis(jwtExpirationMs)));

        if (accountId != null && !accountId.isEmpty()) {
            claimsBuilder.claim("accountId", accountId);
        }

        if (roles != null && !roles.isEmpty()) {
            claimsBuilder.claim("roles", roles);
        }

        JwtClaimsSet claims = claimsBuilder.build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
