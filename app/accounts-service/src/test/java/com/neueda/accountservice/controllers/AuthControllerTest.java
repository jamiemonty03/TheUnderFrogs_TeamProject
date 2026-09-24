package com.neueda.accountservice.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neueda.accountservice.models.User;
import com.neueda.accountservice.services.UserService;
import com.neueda.accountservice.dtos.LoginRequest;
import com.neueda.accountservice.dtos.RegisterRequest;
import com.neueda.accountservice.config.TestSecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(TestSecurityConfig.class)
@DisplayName("AuthController Integration Tests")
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private User testUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    public void setUp() {
        testUser = new User("alice", "alice@example.com", "encodedPassword123", "Alice Smith");
        testUser.setId(1L);
        testUser.setToken("jwt-token-123");

        registerRequest = new RegisterRequest("alice", "alice@example.com", "SecurePassword123", "Alice Smith");
        loginRequest = new LoginRequest("alice", "SecurePassword123");
    }

    // ==================== REGISTER ENDPOINT TESTS ====================

    @Test
    @DisplayName("POST /auth/register: Successfully registers a new user and returns 201 CREATED")
    public void testRegisterSuccess() throws Exception {
        when(userService.register(any(RegisterRequest.class))).thenReturn(testUser);

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", equalTo("jwt-token-123")))
                .andExpect(jsonPath("$.username", equalTo("alice")))
                .andExpect(jsonPath("$.email", equalTo("alice@example.com")))
                .andExpect(jsonPath("$.fullName", equalTo("Alice Smith")));

        verify(userService).register(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /auth/register: Returns 422 when username is missing")
    public void testRegisterMissingUsername() throws Exception {
        String invalidRequest = "{\"email\":\"alice@example.com\",\"password\":\"SecurePassword123\",\"fullName\":\"Alice Smith\"}";

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /auth/register: Returns 422 when email is missing")
    public void testRegisterMissingEmail() throws Exception {
        String invalidRequest = "{\"username\":\"alice\",\"password\":\"SecurePassword123\",\"fullName\":\"Alice Smith\"}";

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /auth/register: Returns 422 when password is missing")
    public void testRegisterMissingPassword() throws Exception {
        String invalidRequest = "{\"username\":\"alice\",\"email\":\"alice@example.com\",\"fullName\":\"Alice Smith\"}";

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /auth/register: Returns 422 when fullName is missing")
    public void testRegisterMissingFullName() throws Exception {
        String invalidRequest = "{\"username\":\"alice\",\"email\":\"alice@example.com\",\"password\":\"SecurePassword123\"}";

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /auth/register: Returns 422 when username is too short")
    public void testRegisterUsernameTooshort() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest("ab", "alice@example.com", "SecurePassword123", "Alice Smith");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /auth/register: Returns 422 when password is too short")
    public void testRegisterPasswordTooshort() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest("alice", "alice@example.com", "Short1", "Alice Smith");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /auth/register: Returns 422 when email format is invalid")
    public void testRegisterInvalidEmail() throws Exception {
        RegisterRequest invalidRequest = new RegisterRequest("alice", "notanemail", "SecurePassword123", "Alice Smith");

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /auth/register: Returns 422 when username already exists")
    public void testRegisterUsernameExists() throws Exception {
        doThrow(new IllegalArgumentException("Username already exists"))
                .when(userService).register(any(RegisterRequest.class));

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", equalTo("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.message", equalTo("Username already exists")));
    }

    @Test
    @DisplayName("POST /auth/register: Returns 422 when email already exists")
    public void testRegisterEmailExists() throws Exception {
        doThrow(new IllegalArgumentException("Email already exists"))
                .when(userService).register(any(RegisterRequest.class));

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", equalTo("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.message", equalTo("Email already exists")));
    }

    // ==================== LOGIN ENDPOINT TESTS ====================

    @Test
    @DisplayName("POST /auth/login: Successfully logs in a user and returns 200 OK with token")
    public void testLoginSuccess() throws Exception {
        when(userService.login(any(LoginRequest.class))).thenReturn(testUser);

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", equalTo("jwt-token-123")))
                .andExpect(jsonPath("$.username", equalTo("alice")))
                .andExpect(jsonPath("$.email", equalTo("alice@example.com")))
                .andExpect(jsonPath("$.fullName", equalTo("Alice Smith")));

        verify(userService).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /auth/login: Returns 422 when username is missing")
    public void testLoginMissingUsername() throws Exception {
        String invalidRequest = "{\"password\":\"SecurePassword123\"}";

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /auth/login: Returns 422 when password is missing")
    public void testLoginMissingPassword() throws Exception {
        String invalidRequest = "{\"username\":\"alice\"}";

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("POST /auth/login: Returns 422 when username not found")
    public void testLoginUsernameNotFound() throws Exception {
        doThrow(new IllegalArgumentException("Invalid username or password"))
                .when(userService).login(any(LoginRequest.class));

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", equalTo("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.message", equalTo("Invalid username or password")));
    }

    @Test
    @DisplayName("POST /auth/login: Returns 422 when password is incorrect")
    public void testLoginIncorrectPassword() throws Exception {
        doThrow(new IllegalArgumentException("Invalid username or password"))
                .when(userService).login(any(LoginRequest.class));

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", equalTo("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.message", equalTo("Invalid username or password")));
    }

    @Test
    @DisplayName("POST /auth/login: Returns 422 when user account is inactive")
    public void testLoginInactiveAccount() throws Exception {
        doThrow(new IllegalArgumentException("User account is not active"))
                .when(userService).login(any(LoginRequest.class));

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code", equalTo("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.message", equalTo("User account is not active")));
    }

    @Test
    @DisplayName("POST /auth/login: Request body cannot be empty")
    public void testLoginEmptyBody() throws Exception {
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnprocessableEntity());
    }
}
