package com.neueda.tradeexecutor.services;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class ServiceTokenProvider {

    static final String SERVICE_NAME = "trade-executor";
    static final List<String> ROLES = List.of("SERVICE");
    static final Duration REFRESH_BEFORE_EXPIRY = Duration.ofSeconds(30);

    private final JwtEncoder jwtEncoder;
    private final Duration tokenTtl;
    private final Clock clock;

    private String cachedToken;
    private Instant expiresAt = Instant.EPOCH;

    @Autowired
    public ServiceTokenProvider(JwtEncoder jwtEncoder, @Value("${service-token.ttl}") Duration tokenTtl) {
        this(jwtEncoder, tokenTtl, Clock.systemUTC());
    }

    ServiceTokenProvider(JwtEncoder jwtEncoder, Duration tokenTtl, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.tokenTtl = tokenTtl;
        this.clock = clock;
    }

    public synchronized String getToken() {
        Instant now = clock.instant();
        if (cachedToken == null || !now.isBefore(expiresAt.minus(REFRESH_BEFORE_EXPIRY))) {
            expiresAt = now.plus(tokenTtl);
            cachedToken = generateToken(now, expiresAt);
        }
        return cachedToken;
    }

    private String generateToken(Instant now, Instant expiry) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(SERVICE_NAME)
                .subject(SERVICE_NAME)
                .issuedAt(now)
                .expiresAt(expiry)
                .claim("roles", ROLES)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
