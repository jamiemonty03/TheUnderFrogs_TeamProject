package com.neueda.accountservice.services;

import com.neueda.accountservice.models.User;
import com.neueda.accountservice.repositories.UserRepository;
import com.neueda.accountservice.services.JwtService;
import com.neueda.accountservice.dtos.LoginRequest;
import com.neueda.accountservice.dtos.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    private UserService userService;

    @BeforeEach
    public void setUp() {
        userService = new UserService(userRepository, passwordEncoder, jwtService);
    }

    // ==================== REGISTER TESTS ====================

    @Test
    @DisplayName("register: Successfully registers a new user with valid request")
    public void testRegisterSuccess() {
        RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "SecurePassword123", "Alice Smith");

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("SecurePassword123")).thenReturn("encodedPassword123");
        when(jwtService.generateToken("alice")).thenReturn("jwt-token-123");

        User result = userService.register(request);

        assertNotNull(result);
        assertEquals("alice", result.getUsername());
        assertEquals("alice@example.com", result.getEmail());
        assertEquals("alice", result.getUsername());
        assertEquals("Alice Smith", result.getFullName());
        assertEquals("jwt-token-123", result.getToken());
        assertTrue(result.getIsActive());

        verify(userRepository).existsByUsername("alice");
        verify(userRepository).existsByEmail("alice@example.com");
        verify(passwordEncoder).encode("SecurePassword123");
        verify(userRepository).save(any(User.class));
        verify(jwtService).generateToken("alice");
    }

    @Test
    @DisplayName("register: Throws exception when username already exists")
    public void testRegisterUsernameExists() {
        RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "SecurePassword123", "Alice Smith");

        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.register(request),
                "Username already exists");

        verify(userRepository).existsByUsername("alice");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register: Throws exception when email already exists")
    public void testRegisterEmailExists() {
        RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "SecurePassword123", "Alice Smith");

        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.register(request),
                "Email already exists");

        verify(userRepository).existsByUsername("alice");
        verify(userRepository).existsByEmail("alice@example.com");
        verify(userRepository, never()).save(any());
    }

    // ==================== LOGIN TESTS ====================

    @Test
    @DisplayName("login: Successfully logs in a user with valid credentials")
    public void testLoginSuccess() {
        User existingUser = new User("alice", "alice@example.com", "encodedPassword123", "Alice Smith");
        existingUser.setId(1L);
        existingUser.setIsActive(true);

        LoginRequest request = new LoginRequest("alice", "SecurePassword123");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("SecurePassword123", "encodedPassword123")).thenReturn(true);
        when(jwtService.generateToken("alice")).thenReturn("jwt-token-123");

        User result = userService.login(request);

        assertNotNull(result);
        assertEquals("alice", result.getUsername());
        assertEquals("alice@example.com", result.getEmail());
        assertEquals("jwt-token-123", result.getToken());

        verify(userRepository).findByUsername("alice");
        verify(passwordEncoder).matches("SecurePassword123", "encodedPassword123");
        verify(jwtService).generateToken("alice");
    }

    @Test
    @DisplayName("login: Throws exception when username not found")
    public void testLoginUsernameNotFound() {
        LoginRequest request = new LoginRequest("nonexistent", "SecurePassword123");

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> userService.login(request),
                "Invalid username or password");

        verify(userRepository).findByUsername("nonexistent");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("login: Throws exception when account is inactive")
    public void testLoginInactiveAccount() {
        User inactiveUser = new User("alice", "alice@example.com", "encodedPassword123", "Alice Smith");
        inactiveUser.setIsActive(false);

        LoginRequest request = new LoginRequest("alice", "SecurePassword123");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(inactiveUser));

        assertThrows(IllegalArgumentException.class, () -> userService.login(request),
                "User account is not active");

        verify(userRepository).findByUsername("alice");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    @DisplayName("login: Throws exception when password is incorrect")
    public void testLoginIncorrectPassword() {
        User existingUser = new User("alice", "alice@example.com", "encodedPassword123", "Alice Smith");
        existingUser.setIsActive(true);

        LoginRequest request = new LoginRequest("alice", "WrongPassword123");

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("WrongPassword123", "encodedPassword123")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> userService.login(request),
                "Invalid username or password");

        verify(userRepository).findByUsername("alice");
        verify(passwordEncoder).matches("WrongPassword123", "encodedPassword123");
        verify(jwtService, never()).generateToken(anyString());
    }

    // ==================== GET USER TESTS ====================

    @Test
    @DisplayName("getUserByUsername: Successfully retrieves user by username")
    public void testGetUserByUsernameSuccess() {
        User user = new User("alice", "alice@example.com", "encodedPassword123", "Alice Smith");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        Optional<User> result = userService.getUserByUsername("alice");

        assertTrue(result.isPresent());
        assertEquals("alice", result.get().getUsername());
        verify(userRepository).findByUsername("alice");
    }

    @Test
    @DisplayName("getUserByUsername: Returns empty when user not found")
    public void testGetUserByUsernameNotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        Optional<User> result = userService.getUserByUsername("nonexistent");

        assertFalse(result.isPresent());
        verify(userRepository).findByUsername("nonexistent");
    }

    @Test
    @DisplayName("getUserByEmail: Successfully retrieves user by email")
    public void testGetUserByEmailSuccess() {
        User user = new User("alice", "alice@example.com", "encodedPassword123", "Alice Smith");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        Optional<User> result = userService.getUserByEmail("alice@example.com");

        assertTrue(result.isPresent());
        assertEquals("alice@example.com", result.get().getEmail());
        verify(userRepository).findByEmail("alice@example.com");
    }

    @Test
    @DisplayName("getUserById: Successfully retrieves user by ID")
    public void testGetUserByIdSuccess() {
        User user = new User("alice", "alice@example.com", "encodedPassword123", "Alice Smith");
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        Optional<User> result = userService.getUserById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        verify(userRepository).findById(1L);
    }

    // ==================== UPDATE USER TESTS ====================

    @Test
    @DisplayName("updateUser: Successfully updates user details")
    public void testUpdateUserSuccess() {
        User existingUser = new User("alice", "alice@example.com", "encodedPassword123", "Alice Smith");
        existingUser.setId(1L);
        existingUser.setVersion(0);

        User updatedData = new User("alice", "newemail@example.com", "encodedPassword123", "Alice Johnson");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmail("newemail@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = userService.updateUser(1L, updatedData);

        assertEquals("Alice Johnson", result.getFullName());
        assertEquals("newemail@example.com", result.getEmail());
        assertEquals(1, result.getVersion());

        verify(userRepository).findById(1L);
        verify(userRepository).existsByEmail("newemail@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("updateUser: Throws exception when user not found")
    public void testUpdateUserNotFound() {
        User updatedData = new User("alice", "alice@example.com", "encodedPassword123", "Alice Smith");

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> userService.updateUser(999L, updatedData),
                "User not found");

        verify(userRepository).findById(999L);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateUser: Throws exception when new email already exists")
    public void testUpdateUserEmailExists() {
        User existingUser = new User("alice", "alice@example.com", "encodedPassword123", "Alice Smith");
        existingUser.setId(1L);

        User updatedData = new User("alice", "taken@example.com", "encodedPassword123", "Alice Smith");

        when(userRepository.findById(1L)).thenReturn(Optional.of(existingUser));
        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.updateUser(1L, updatedData),
                "Email already exists");

        verify(userRepository).findById(1L);
        verify(userRepository).existsByEmail("taken@example.com");
        verify(userRepository, never()).save(any());
    }

    // ==================== DELETE USER TESTS ====================

    @Test
    @DisplayName("deleteUser: Successfully deletes a user")
    public void testDeleteUserSuccess() {
        when(userRepository.existsById(1L)).thenReturn(true);
        doNothing().when(userRepository).deleteById(1L);

        assertDoesNotThrow(() -> userService.deleteUser(1L));

        verify(userRepository).existsById(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteUser: Throws exception when user not found")
    public void testDeleteUserNotFound() {
        when(userRepository.existsById(999L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> userService.deleteUser(999L),
                "User not found");

        verify(userRepository).existsById(999L);
        verify(userRepository, never()).deleteById(any());
    }
}
