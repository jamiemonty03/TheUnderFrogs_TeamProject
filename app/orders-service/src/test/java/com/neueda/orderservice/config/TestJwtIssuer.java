package com.neueda.orderservice.config;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.test.context.DynamicPropertyRegistry;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;

/**
 * Stands in for auth-service in tests: signs RS256 tokens with a test key and serves the
 * matching public key as a JWKS, so the service's real decoder config is exercised.
 *
 * Register it in a test with:
 * <pre>
 * &#64;DynamicPropertySource
 * static void auth(DynamicPropertyRegistry registry) { TestJwtIssuer.registerProperties(registry); }
 * </pre>
 */
public final class TestJwtIssuer {

    public static final String ISSUER = "auth-service";
    public static final String AUDIENCE = "trading-platform";

    private static final RSAKey KEY = generateKey("test-key");
    private static final RSAKey UNKNOWN_KEY = generateKey("unknown-key");
    private static final HttpServer JWKS_SERVER = startJwksServer();

    private TestJwtIssuer() {}

    public static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", TestJwtIssuer::jwksUri);
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> ISSUER);
        registry.add("spring.security.oauth2.resourceserver.jwt.audiences", () -> AUDIENCE);
    }

    public static String jwksUri() {
        return "http://localhost:" + JWKS_SERVER.getAddress().getPort() + "/.well-known/jwks.json";
    }

    /** A valid user token for alice with no roles. */
    public static String userToken() {
        return token().build();
    }

    /** A valid token for the trade-executor with role SERVICE. */
    public static String serviceToken() {
        return token().subject("trade-executor").roles("SERVICE").build();
    }

    public static Builder token() {
        return new Builder();
    }

    /** An HS256 token signed with a shared secret, the way tokens were made before auth-service. */
    public static String sharedSecretToken() {
        try {
            SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), token().claims());
            jwt.sign(new MACSigner("old-shared-secret-that-is-at-least-32-bytes".getBytes(StandardCharsets.UTF_8)));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    public static final class Builder {
        private String subject = "alice";
        private List<String> roles;
        private String issuer = ISSUER;
        private String audience = AUDIENCE;
        private Instant expiresAt = Instant.now().plus(Duration.ofMinutes(5));
        private RSAKey key = KEY;

        public Builder subject(String subject) { this.subject = subject; return this; }
        public Builder roles(String... roles) { this.roles = List.of(roles); return this; }
        public Builder issuer(String issuer) { this.issuer = issuer; return this; }
        public Builder audience(String audience) { this.audience = audience; return this; }
        public Builder expiresAt(Instant expiresAt) { this.expiresAt = expiresAt; return this; }

        /** Signs with a key that auth-service's JWKS does not publish. */
        public Builder signedWithUnknownKey() { this.key = UNKNOWN_KEY; return this; }

        public String build() {
            try {
                SignedJWT jwt = new SignedJWT(
                        new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(key.getKeyID()).build(), claims());
                jwt.sign(new RSASSASigner(key));
                return jwt.serialize();
            } catch (JOSEException e) {
                throw new IllegalStateException(e);
            }
        }

        private JWTClaimsSet claims() {
            JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                    .jwtID(UUID.randomUUID().toString())
                    .subject(subject)
                    .issuer(issuer)
                    .audience(audience)
                    .issueTime(Date.from(expiresAt.minus(Duration.ofMinutes(5))))
                    .expirationTime(Date.from(expiresAt));
            if (roles != null) {
                claims.claim("roles", roles);
            }
            return claims.build();
        }
    }

    private static RSAKey generateKey(String keyId) {
        try {
            return new RSAKeyGenerator(2048).keyID(keyId).generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    private static HttpServer startJwksServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            byte[] body = new JWKSet(KEY.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
            server.createContext("/.well-known/jwks.json", exchange -> {
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream out = exchange.getResponseBody()) {
                    out.write(body);
                }
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException("Could not start the test JWKS server", e);
        }
    }
}
