package com.neueda.accountservice.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import com.neueda.accountservice.controllers.AccountController;
import com.neueda.accountservice.services.AccountService;

@WebMvcTest(AccountController.class)
@Import(SecurityConfig.class)
class ServiceTokenSecurityTest {

    private static final String DEBIT = "/accounts/ACC0001/debit";
    private static final String NEW_ACCOUNT =
            "{\"userId\":12,\"holderName\":\"Zed Smith\"}";

    @DynamicPropertySource
    static void auth(DynamicPropertyRegistry registry) {
        TestJwtIssuer.registerProperties(registry);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @MockBean
    private AccountService accountService;

    @Test
    @DisplayName("Service token from auth-service is accepted")
    void acceptsServiceToken() throws Exception {
        mockMvc.perform(get("/accounts").header(HttpHeaders.AUTHORIZATION, bearer(TestJwtIssuer.serviceToken())))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Service token can debit an account")
    void serviceTokenCanDebit() throws Exception {
        mockMvc.perform(debit(TestJwtIssuer.serviceToken())).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Authenticated user can still debit an account")
    void userTokenCanDebit() throws Exception {
        mockMvc.perform(debit(TestJwtIssuer.userToken())).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Service token can create an account")
    void serviceTokenCanCreateAccount() throws Exception {
        mockMvc.perform(createAccount(TestJwtIssuer.serviceToken())).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("User token cannot create an account and gets 403")
    void userTokenCannotCreateAccount() throws Exception {
        mockMvc.perform(createAccount(TestJwtIssuer.userToken())).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Creating an account without a token is rejected with 401")
    void createAccountWithoutTokenIsRejected() throws Exception {
        mockMvc.perform(post("/accounts").contentType(MediaType.APPLICATION_JSON).content(NEW_ACCOUNT))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Missing token is rejected with 401")
    void rejectsMissingToken() throws Exception {
        mockMvc.perform(get("/accounts")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Garbage token is rejected with 401")
    void rejectsGarbageToken() throws Exception {
        mockMvc.perform(get("/accounts").header(HttpHeaders.AUTHORIZATION, bearer("not-a-jwt")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token signed with a key auth-service does not publish is rejected with 401")
    void rejectsUnknownKey() throws Exception {
        mockMvc.perform(debit(TestJwtIssuer.token().roles("SERVICE").signedWithUnknownKey().build()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("HS256 token signed with the old shared secret is rejected with 401")
    void rejectsSharedSecretToken() throws Exception {
        mockMvc.perform(debit(TestJwtIssuer.sharedSecretToken())).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Expired token is rejected with 401")
    void rejectsExpiredToken() throws Exception {
        String expired = TestJwtIssuer.token().roles("SERVICE")
                .expiresAt(Instant.now().minus(Duration.ofMinutes(5))).build();
        mockMvc.perform(debit(expired)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token from a different issuer is rejected with 401")
    void rejectsWrongIssuer() throws Exception {
        mockMvc.perform(debit(TestJwtIssuer.token().issuer("someone-else").build()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token meant for a different audience is rejected with 401")
    void rejectsWrongAudience() throws Exception {
        mockMvc.perform(debit(TestJwtIssuer.token().audience("another-app").build()))
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

    private static RequestBuilder createAccount(String token) {
        return post("/accounts")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(NEW_ACCOUNT);
    }

    private static RequestBuilder debit(String token) {
        return post(DEBIT)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": 100}");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
