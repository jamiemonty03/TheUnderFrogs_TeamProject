package com.neueda.accountservice.integration;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Base64;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * characterisation tests for /auth/register and /auth/login
 */
@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "jwt.secret=accounts-test-secret-that-is-32-bytes-min",
        "jwt.expiration=3600000"
})
@DisplayName("Auth characterisation: current accounts-service /auth behaviour")
class AuthCharacterisationTest {

    private static final String PASSWORD = "SecurePassword123";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeAll
    static void createSchema() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Stream<Path> files = Files.list(Path.of("db/schema"))) {
            for (Path file : files.sorted().toList()) {
                ScriptUtils.executeSqlScript(connection, new FileSystemResource(file));
            }
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM cash_movements");
        jdbcTemplate.update("DELETE FROM accounts");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    @DisplayName("register success: 201 with { token, username, email, fullName } and a BCrypt hash stored")
    void registerSuccess() throws Exception {
        // Deliberate change: body becomes { accessToken, refreshToken, expiresIn, mfaRequired }
        String token = assertAuthResponse(register("alice", "alice@example.com", PASSWORD, "Alice Smith")
                .andExpect(status().isCreated()));

        assertCurrentTokenShape(token, "alice");

        String storedHash = jdbcTemplate.queryForObject(
                "SELECT password FROM users WHERE username = 'alice'", String.class);
        assertTrue(storedHash.startsWith("$2a$"), "expected a BCrypt hash but got " + storedHash);
        assertTrue(passwordEncoder.matches(PASSWORD, storedHash));
        assertEquals(Boolean.TRUE, jdbcTemplate.queryForObject(
                "SELECT is_active FROM users WHERE username = 'alice'", Boolean.class));
    }

    @Test
    @DisplayName("register duplicate username: 422 VALIDATION_ERROR 'Username already exists'")
    void registerDuplicateUsername() throws Exception {
        seedUser("alice", "alice@example.com", true);

        // Deliberate change: 409 AUTH-409
        assertServiceError(register("alice", "other@example.com", PASSWORD, "Alice Two"),
                "Username already exists");

        assertEquals(1, userCount());
    }

    @Test
    @DisplayName("register duplicate email: 422 VALIDATION_ERROR 'Email already exists'")
    void registerDuplicateEmail() throws Exception {
        seedUser("alice", "alice@example.com", true);

        // Deliberate change: 409 AUTH-409
        assertServiceError(register("bob", "alice@example.com", PASSWORD, "Bob Jones"),
                "Email already exists");

        assertEquals(1, userCount());
    }

    @Test
    @DisplayName("register duplicate username and email: username is checked first")
    void registerDuplicateUsernameAndEmail() throws Exception {
        seedUser("alice", "alice@example.com", true);

        register("alice", "alice@example.com", PASSWORD, "Alice Smith")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", equalTo("Username already exists")));
    }

    @Test
    @DisplayName("register missing username: 422 VAL-422 'Username is required'")
    void registerMissingUsername() throws Exception {
        assertRegisterValidationError(null, "alice@example.com", PASSWORD, "Alice Smith",
                "Username is required");
    }

    @Test
    @DisplayName("register missing email: 422 VAL-422 'Email is required'")
    void registerMissingEmail() throws Exception {
        assertRegisterValidationError("alice", null, PASSWORD, "Alice Smith", "Email is required");
    }

    @Test
    @DisplayName("register missing password: 422 VAL-422 'Password is required'")
    void registerMissingPassword() throws Exception {
        assertRegisterValidationError("alice", "alice@example.com", null, "Alice Smith",
                "Password is required");
    }

    @Test
    @DisplayName("register missing full name: 422 VAL-422 'Full name is required'")
    void registerMissingFullName() throws Exception {
        assertRegisterValidationError("alice", "alice@example.com", PASSWORD, null,
                "Full name is required");
    }

    @Test
    @DisplayName("register username shorter than 3: 422 VAL-422")
    void registerUsernameTooShort() throws Exception {
        assertRegisterValidationError("al", "alice@example.com", PASSWORD, "Alice Smith",
                "Username must be between 3 and 50 characters");
    }

    @Test
    @DisplayName("register password shorter than 8: 422 VAL-422")
    void registerPasswordTooShort() throws Exception {
        assertRegisterValidationError("alice", "alice@example.com", "short", "Alice Smith",
                "Password must be at least 8 characters");
    }

    @Test
    @DisplayName("register malformed email: 422 VAL-422 'Email should be valid'")
    void registerMalformedEmail() throws Exception {
        assertRegisterValidationError("alice", "not-an-email", PASSWORD, "Alice Smith",
                "Email should be valid");
    }

    @Test
    @DisplayName("register email without a TLD: passes @Email, then fails the User regex with the VALIDATION_ERROR shape")
    void registerEmailWithoutTld() throws Exception {
        // Deliberate change: VAL-422 like other validation errors
        assertServiceError(register("alice", "alice@example", PASSWORD, "Alice Smith"),
                "Email should be valid");

        assertEquals(0, userCount());
    }

    @Test
    @DisplayName("login success: 200 with { token, username, email, fullName }")
    void loginSuccess() throws Exception {
        seedUser("alice", "alice@example.com", true);

        // Deliberate change: body becomes { accessToken, refreshToken, expiresIn, mfaRequired }
        String token = assertAuthResponse(login("alice", PASSWORD).andExpect(status().isOk()));

        assertCurrentTokenShape(token, "alice");
    }

    @Test
    @DisplayName("login after register: the registered password works")
    void loginAfterRegister() throws Exception {
        register("alice", "alice@example.com", PASSWORD, "Alice Smith").andExpect(status().isCreated());

        login("alice", PASSWORD).andExpect(status().isOk());
    }

    @Test
    @DisplayName("login wrong password: 422 VALIDATION_ERROR 'Invalid username or password'")
    void loginWrongPassword() throws Exception {
        seedUser("alice", "alice@example.com", true);

        // Deliberate change: 401 AUTH-401
        assertServiceError(login("alice", "WrongPassword123"), "Invalid username or password");
    }

    @Test
    @DisplayName("login unknown user: identical response to wrong password")
    void loginUnknownUser() throws Exception {
        seedUser("alice", "alice@example.com", true);

        String unknownUser = login("nobody", PASSWORD)
                .andExpect(status().isUnprocessableEntity())
                .andReturn().getResponse().getContentAsString();
        String wrongPassword = login("alice", "WrongPassword123")
                .andReturn().getResponse().getContentAsString();

        // Keep in auth-service: these must stay identical
        assertEquals(wrongPassword, unknownUser);
    }

    @Test
    @DisplayName("login inactive user with correct password: 422 VALIDATION_ERROR 'User account is not active'")
    void loginInactiveUser() throws Exception {
        seedUser("alice", "alice@example.com", false);

        // Deliberate change: generic 401 AUTH-401
        assertServiceError(login("alice", PASSWORD), "User account is not active");
    }

    @Test
    @DisplayName("login inactive user with wrong password: still 'User account is not active' (active check runs before password check)")
    void loginInactiveUserWrongPassword() throws Exception {
        seedUser("alice", "alice@example.com", false);

        // Deliberate change: reveals the username exists (OWASP A07); check password first, generic 401 AUTH-401
        login("alice", "WrongPassword123")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", equalTo("User account is not active")));
    }

    @Test
    @DisplayName("login missing username: 422 VAL-422 'Username is required'")
    void loginMissingUsername() throws Exception {
        login(null, PASSWORD)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$", aMapWithSize(2)))
                .andExpect(jsonPath("$.errorCode", equalTo("VAL-422")))
                .andExpect(jsonPath("$.message", equalTo("Username is required")));
    }

    @Test
    @DisplayName("login missing password: 422 VAL-422 'Password is required'")
    void loginMissingPassword() throws Exception {
        login("alice", null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode", equalTo("VAL-422")))
                .andExpect(jsonPath("$.message", equalTo("Password is required")));
    }

    private String assertAuthResponse(ResultActions result) throws Exception {
        String body = result
                .andExpect(jsonPath("$", aMapWithSize(4)))
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andExpect(jsonPath("$.username", equalTo("alice")))
                .andExpect(jsonPath("$.email", equalTo("alice@example.com")))
                .andExpect(jsonPath("$.fullName", equalTo("Alice Smith")))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }

    // Deliberate change: RS256, sub = userId, plus username, roles, accountId and aud
    private void assertCurrentTokenShape(String token, String username) throws Exception {
        String header = new String(Base64.getUrlDecoder().decode(token.split("\\.")[0]));
        assertEquals("HS256", objectMapper.readTree(header).get("alg").asText());

        Jwt jwt = jwtDecoder.decode(token);
        assertEquals(username, jwt.getSubject());
        assertEquals("accounts-service", jwt.getClaimAsString("iss"));
        assertNull(jwt.getClaim("roles"));
        assertNull(jwt.getClaim("aud"));
    }

    // Deliberate change: { code, message, status } becomes { errorCode, message }
    private void assertServiceError(ResultActions result, String message) throws Exception {
        result.andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$", aMapWithSize(3)))
                .andExpect(jsonPath("$.code", equalTo("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.message", equalTo(message)))
                .andExpect(jsonPath("$.status", equalTo(422)));
    }

    private void assertRegisterValidationError(String username, String email, String password, String fullName,
                                               String message) throws Exception {
        register(username, email, password, fullName)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$", aMapWithSize(2)))
                .andExpect(jsonPath("$.errorCode", equalTo("VAL-422")))
                .andExpect(jsonPath("$.message", equalTo(message)));

        assertEquals(0, userCount());
    }

    private ResultActions register(String username, String email, String password, String fullName)
            throws Exception {
        return mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(username, email, password, fullName)));
    }

    private ResultActions login(String username, String password) throws Exception {
        return mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(username, null, password, null)));
    }

    private String json(String username, String email, String password, String fullName) throws Exception {
        var node = objectMapper.createObjectNode();
        if (username != null) node.put("username", username);
        if (email != null) node.put("email", email);
        if (password != null) node.put("password", password);
        if (fullName != null) node.put("fullName", fullName);
        return objectMapper.writeValueAsString(node);
    }

    private void seedUser(String username, String email, boolean active) {
        jdbcTemplate.update(
                "INSERT INTO users (username, email, password, full_name, is_active) VALUES (?, ?, ?, ?, ?)",
                username, email, passwordEncoder.encode(PASSWORD), "Alice Smith", active);
    }

    private int userCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
    }
}
