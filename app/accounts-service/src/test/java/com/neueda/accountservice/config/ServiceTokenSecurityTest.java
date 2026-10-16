package com.neueda.accountservice.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyString;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
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
import com.neueda.accountservice.controllers.TopUpController;
import com.neueda.accountservice.services.AccountService;
import com.neueda.accountservice.services.TopUpService;
import com.neueda.accountservice.utils.AuthorizationUtils;

@WebMvcTest({AccountController.class, TopUpController.class})
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

    @MockBean
    private TopUpService topUpService;
    
    private static org.mockito.MockedStatic<AuthorizationUtils> mockedAuthUtils;

    @BeforeEach
    void setUp() {
        mockedAuthUtils = mockStatic(AuthorizationUtils.class);
        mockedAuthUtils.when(() -> AuthorizationUtils.verifyAccountAccess(anyString())).thenAnswer(invocation -> null);
    }
    
    @AfterEach
    void tearDown() {
        if (mockedAuthUtils != null) {
            mockedAuthUtils.close();
        }
    }

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
    @DisplayName("User token cannot debit an account and gets 403")
    void userTokenCannotDebit() throws Exception {
        mockMvc.perform(debit(TestJwtIssuer.userToken())).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Service token can credit an account")
    void serviceTokenCanCredit() throws Exception {
        mockMvc.perform(credit(TestJwtIssuer.serviceToken())).andExpect(status().isOk());
    }

    @Test
    @DisplayName("User token cannot credit an account and gets 403, so cash only arrives through a verified payment")
    void userTokenCannotCredit() throws Exception {
        mockMvc.perform(credit(TestJwtIssuer.userToken())).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Service token can reverse a cash movement")
    void serviceTokenCanReverse() throws Exception {
        mockMvc.perform(reversal(TestJwtIssuer.serviceToken())).andExpect(status().isOk());
    }

    @Test
    @DisplayName("User token cannot reverse a cash movement and gets 403")
    void userTokenCannotReverse() throws Exception {
        mockMvc.perform(reversal(TestJwtIssuer.userToken())).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Service token can rename an account's holder")
    void serviceTokenCanUpdateHolderName() throws Exception {
        mockMvc.perform(holderName(TestJwtIssuer.serviceToken())).andExpect(status().isOk());
    }

    @Test
    @DisplayName("User token cannot rename an account's holder and gets 403")
    void userTokenCannotUpdateHolderName() throws Exception {
        mockMvc.perform(holderName(TestJwtIssuer.userToken())).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Crediting without a token is rejected with 401")
    void creditWithoutToken() throws Exception {
        mockMvc.perform(post("/accounts/ACC0001/credit")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": 100}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("User token can start a top-up and confirm it, the only way for a user to add cash")
    void userTokenCanTopUp() throws Exception {
        mockMvc.perform(post("/accounts/ACC0001/top-ups")
                .header(HttpHeaders.AUTHORIZATION, bearer(TestJwtIssuer.userToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": 50}"))
            .andExpect(status().isOk());
        mockMvc.perform(post("/accounts/ACC0001/top-ups/cs_test_a1B2c3/confirm")
                .header(HttpHeaders.AUTHORIZATION, bearer(TestJwtIssuer.userToken())))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Starting a top-up without a token is rejected with 401")
    void topUpWithoutToken() throws Exception {
        mockMvc.perform(post("/accounts/ACC0001/top-ups")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": 50}"))
            .andExpect(status().isUnauthorized());
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

    private static RequestBuilder credit(String token) {
        return post("/accounts/ACC0001/credit")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": 100}");
    }

    private static RequestBuilder reversal(String token) {
        return post("/accounts/ACC0001/reversal")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orderId\": \"ORD-1\"}");
    }

    private static RequestBuilder holderName(String token) {
        return patch("/accounts/ACC0001/holder-name")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"holderName\": \"Kermit\"}");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
