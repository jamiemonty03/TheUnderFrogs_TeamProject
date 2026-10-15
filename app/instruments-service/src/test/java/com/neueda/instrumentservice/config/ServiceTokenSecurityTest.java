package com.neueda.instrumentservice.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import com.neueda.instrumentservice.controllers.InstrumentController;
import com.neueda.instrumentservice.services.InstrumentService;

@WebMvcTest(InstrumentController.class)
@Import(SecurityConfig.class)
class ServiceTokenSecurityTest {

    private static final String INSTRUMENTS = "/instruments";

    @DynamicPropertySource
    static void auth(DynamicPropertyRegistry registry) {
        TestJwtIssuer.registerProperties(registry);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @MockBean
    private InstrumentService instrumentService;

    @Test
    @DisplayName("Service token from auth-service is accepted")
    void acceptsServiceToken() throws Exception {
        mockMvc.perform(instruments(TestJwtIssuer.serviceToken())).andExpect(status().isOk());
    }

    @Test
    @DisplayName("User token from auth-service is accepted")
    void acceptsUserToken() throws Exception {
        mockMvc.perform(instruments(TestJwtIssuer.userToken())).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Missing token is rejected with 401")
    void rejectsMissingToken() throws Exception {
        mockMvc.perform(get(INSTRUMENTS)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Garbage token is rejected with 401")
    void rejectsGarbageToken() throws Exception {
        mockMvc.perform(instruments("not-a-jwt")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token signed with a key auth-service does not publish is rejected with 401")
    void rejectsUnknownKey() throws Exception {
        mockMvc.perform(instruments(TestJwtIssuer.token().roles("SERVICE").signedWithUnknownKey().build()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("HS256 token signed with the old shared secret is rejected with 401")
    void rejectsSharedSecretToken() throws Exception {
        mockMvc.perform(instruments(TestJwtIssuer.sharedSecretToken())).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Expired token is rejected with 401")
    void rejectsExpiredToken() throws Exception {
        String expired = TestJwtIssuer.token().roles("SERVICE")
                .expiresAt(Instant.now().minus(Duration.ofMinutes(5))).build();
        mockMvc.perform(instruments(expired)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token from a different issuer is rejected with 401")
    void rejectsWrongIssuer() throws Exception {
        mockMvc.perform(instruments(TestJwtIssuer.token().issuer("someone-else").build()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token meant for a different audience is rejected with 401")
    void rejectsWrongAudience() throws Exception {
        mockMvc.perform(instruments(TestJwtIssuer.token().audience("another-app").build()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("The roles claim becomes ROLE_ authorities")
    void rolesClaimBecomesRoleAuthorities() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256")
                .subject("trade-executor").claim("roles", List.of("SERVICE", "ADMIN")).build();

        assertThat(jwtAuthenticationConverter.convert(jwt).getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_SERVICE", "ROLE_ADMIN");
    }

    private static RequestBuilder instruments(String token) {
        return get(INSTRUMENTS).header(HttpHeaders.AUTHORIZATION, bearer(token));
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
