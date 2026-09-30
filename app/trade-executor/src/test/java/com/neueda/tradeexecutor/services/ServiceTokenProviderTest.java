package com.neueda.tradeexecutor.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.neueda.tradeexecutor.config.JwtEncoderConfig;

class ServiceTokenProviderTest {

    private static final String SECRET = "test-secret-that-is-at-least-32-bytes-long!!";
    private static final Duration TTL = Duration.ofMinutes(5);

    private MutableClock clock;
    private ServiceTokenProvider provider;

    @BeforeEach
    void setUp() {
        JwtEncoderConfig encoderConfig = new JwtEncoderConfig();
        ReflectionTestUtils.setField(encoderConfig, "sharedSecret", SECRET);
        clock = new MutableClock(Instant.now());
        provider = new ServiceTokenProvider(encoderConfig.jwtEncoder(), TTL, clock);
    }

    @Test
    @DisplayName("Token is signed with the shared secret and identifies the executor as a SERVICE")
    void tokenHasServiceClaims() {
        Jwt jwt = decode(provider.getToken());

        assertEquals("trade-executor", jwt.getSubject());
        assertEquals("trade-executor", jwt.getClaimAsString("iss"));
        assertEquals(List.of("SERVICE"), jwt.getClaimAsStringList("roles"));
        assertEquals(TTL, Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()));
    }

    @Test
    @DisplayName("The same token is reused until the refresh window")
    void reusesTokenBeforeRefreshWindow() {
        String first = provider.getToken();

        clock.advance(TTL.minus(ServiceTokenProvider.REFRESH_BEFORE_EXPIRY).minusSeconds(1));

        assertEquals(first, provider.getToken());
    }

    @Test
    @DisplayName("A new token is created shortly before the current one expires")
    void refreshesTokenNearExpiry() {
        String first = provider.getToken();
        Instant firstExpiry = decode(first).getExpiresAt();

        clock.advance(TTL.minus(ServiceTokenProvider.REFRESH_BEFORE_EXPIRY));
        String second = provider.getToken();

        assertNotEquals(first, second);
        assertEquals(firstExpiry.plus(TTL.minus(ServiceTokenProvider.REFRESH_BEFORE_EXPIRY)),
                decode(second).getExpiresAt());
    }

    @Test
    @DisplayName("A new token is created once the current one has expired")
    void refreshesTokenAfterExpiry() {
        String first = provider.getToken();

        clock.advance(TTL.plusMinutes(1));

        assertNotEquals(first, provider.getToken());
    }

    private static Jwt decode(String token) {
        SecretKeySpec key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).build().decode(token);
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
