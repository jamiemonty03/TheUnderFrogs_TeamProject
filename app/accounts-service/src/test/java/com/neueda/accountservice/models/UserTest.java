package com.neueda.accountservice.models;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserTest {

    @Test
    @DisplayName("constructor: Valid arguments create a user with proper initialization")
    public void testConstructorValidArguments() {
        User user = new User("alice", "alice@example.com", "SecurePassword123", "Alice Smith");

        assertEquals("alice", user.getUsername());
        assertEquals("alice@example.com", user.getEmail());
        assertEquals("SecurePassword123", user.getPassword());
        assertEquals("Alice Smith", user.getFullName());
        assertTrue(user.getIsActive());
        assertEquals(0, user.getVersion());
        assertNotNull(user.getCreatedAt());
        assertNotNull(user.getLastUpdated());
    }

    @Test
    @DisplayName("constructor: Throws exception when username is null")
    public void testConstructorNullUsername() {
        assertThrows(IllegalArgumentException.class, () ->
                new User(null, "alice@example.com", "SecurePassword123", "Alice Smith"));
    }

    @Test
    @DisplayName("constructor: Throws exception when username is blank")
    public void testConstructorBlankUsername() {
        assertThrows(IllegalArgumentException.class, () ->
                new User("   ", "alice@example.com", "SecurePassword123", "Alice Smith"));
    }

    @Test
    @DisplayName("constructor: Throws exception when username is less than 3 characters")
    public void testConstructorUsernameTooshort() {
        assertThrows(IllegalArgumentException.class, () ->
                new User("ab", "alice@example.com", "SecurePassword123", "Alice Smith"));
    }

    @Test
    @DisplayName("constructor: Throws exception when username is more than 50 characters")
    public void testConstructorUsernameToolong() {
        String longUsername = "a".repeat(51);
        assertThrows(IllegalArgumentException.class, () ->
                new User(longUsername, "alice@example.com", "SecurePassword123", "Alice Smith"));
    }

    @Test
    @DisplayName("constructor: Accepts username with exactly 3 characters")
    public void testConstructorUsernameExactly3Chars() {
        User user = new User("abc", "alice@example.com", "SecurePassword123", "Alice Smith");
        assertEquals("abc", user.getUsername());
    }

    @Test
    @DisplayName("constructor: Accepts username with exactly 50 characters")
    public void testConstructorUsernameExactly50Chars() {
        String username = "a".repeat(50);
        User user = new User(username, "alice@example.com", "SecurePassword123", "Alice Smith");
        assertEquals(username, user.getUsername());
    }

    @Test
    @DisplayName("constructor: Throws exception when email is null")
    public void testConstructorNullEmail() {
        assertThrows(IllegalArgumentException.class, () ->
                new User("alice", null, "SecurePassword123", "Alice Smith"));
    }

    @Test
    @DisplayName("constructor: Throws exception when email is blank")
    public void testConstructorBlankEmail() {
        assertThrows(IllegalArgumentException.class, () ->
                new User("alice", "   ", "SecurePassword123", "Alice Smith"));
    }

    @Test
    @DisplayName("constructor: Throws exception when email is invalid format")
    public void testConstructorInvalidEmailFormat() {
        assertThrows(IllegalArgumentException.class, () ->
                new User("alice", "notanemail", "SecurePassword123", "Alice Smith"));
    }

    @Test
    @DisplayName("constructor: Accepts valid email addresses")
    public void testConstructorValidEmail() {
        User user = new User("alice", "alice.smith+tag@example.co.uk", "SecurePassword123", "Alice Smith");
        assertEquals("alice.smith+tag@example.co.uk", user.getEmail());
    }

    @Test
    @DisplayName("constructor: Throws exception when password is null")
    public void testConstructorNullPassword() {
        assertThrows(IllegalArgumentException.class, () ->
                new User("alice", "alice@example.com", null, "Alice Smith"));
    }

    @Test
    @DisplayName("constructor: Throws exception when password is blank")
    public void testConstructorBlankPassword() {
        assertThrows(IllegalArgumentException.class, () ->
                new User("alice", "alice@example.com", "   ", "Alice Smith"));
    }

    @Test
    @DisplayName("constructor: Throws exception when password is less than 8 characters")
    public void testConstructorPasswordTooshort() {
        assertThrows(IllegalArgumentException.class, () ->
                new User("alice", "alice@example.com", "Short1", "Alice Smith"));
    }

    @Test
    @DisplayName("constructor: Accepts password with exactly 8 characters")
    public void testConstructorPasswordExactly8Chars() {
        User user = new User("alice", "alice@example.com", "Pass1234", "Alice Smith");
        assertEquals("Pass1234", user.getPassword());
    }

    @Test
    @DisplayName("constructor: Throws exception when fullName is null")
    public void testConstructorNullFullName() {
        assertThrows(IllegalArgumentException.class, () ->
                new User("alice", "alice@example.com", "SecurePassword123", null));
    }

    @Test
    @DisplayName("constructor: Throws exception when fullName is blank")
    public void testConstructorBlankFullName() {
        assertThrows(IllegalArgumentException.class, () ->
                new User("alice", "alice@example.com", "SecurePassword123", "   "));
    }

    @Test
    @DisplayName("setters: Can update email and full name after construction")
    public void testSettersUpdateState() {
        User user = new User("alice", "alice@example.com", "SecurePassword123", "Alice Smith");

        user.setEmail("alice.newemail@example.com");
        user.setFullName("Alice Johnson");
        user.setVersion(2);
        user.setIsActive(false);

        assertEquals("alice.newemail@example.com", user.getEmail());
        assertEquals("Alice Johnson", user.getFullName());
        assertEquals(2, user.getVersion());
        assertFalse(user.getIsActive());
    }

    @Test
    @DisplayName("no-arg constructor: Leaves fields unset")
    public void testNoArgConstructorLeavesFieldsNull() {
        User user = new User();

        assertNull(user.getId());
        assertNull(user.getUsername());
        assertNull(user.getEmail());
        assertNull(user.getPassword());
        assertNull(user.getFullName());
    }

    @Test
    @DisplayName("getToken/setToken: Token is not persisted (@Transient)")
    public void testTokenIsTransient() {
        User user = new User("alice", "alice@example.com", "SecurePassword123", "Alice Smith");
        assertNull(user.getToken());

        user.setToken("eyJhbGc...");
        assertEquals("eyJhbGc...", user.getToken());
    }

    @Test
    @DisplayName("isActive: Returns true by default after construction")
    public void testIsActiveByDefault() {
        User user = new User("alice", "alice@example.com", "SecurePassword123", "Alice Smith");
        assertTrue(user.getIsActive());
    }

    @Test
    @DisplayName("constructor: Validates multiple invalid parameters independently")
    public void testConstructorValidatesAllParameters() {
        // Test each parameter is validated
        assertThrows(IllegalArgumentException.class, () ->
                new User("a", "alice@example.com", "SecurePassword123", "Alice Smith")); // username too short

        assertThrows(IllegalArgumentException.class, () ->
                new User("alice", "invalidemail", "SecurePassword123", "Alice Smith")); // invalid email

        assertThrows(IllegalArgumentException.class, () ->
                new User("alice", "alice@example.com", "short", "Alice Smith")); // password too short

        assertThrows(IllegalArgumentException.class, () ->
                new User("alice", "alice@example.com", "SecurePassword123", "")); // blank fullName
    }

    @Test
    @DisplayName("email validation: Accepts various valid email formats")
    public void testEmailValidationAcceptsValidFormats() {
        assertDoesNotThrow(() -> new User("user1", "simple@example.com", "Password123", "User One"));
        assertDoesNotThrow(() -> new User("user2", "user+tag@example.co.uk", "Password123", "User Two"));
        assertDoesNotThrow(() -> new User("user3", "user123@sub.example.com", "Password123", "User Three"));
        assertDoesNotThrow(() -> new User("user4", "first.last@example.org", "Password123", "User Four"));
    }

    @Test
    @DisplayName("email validation: Rejects invalid email formats")
    public void testEmailValidationRejectsInvalidFormats() {
        assertThrows(IllegalArgumentException.class, () ->
                new User("alice", "plainaddress", "Password123", "Alice"));

        assertThrows(IllegalArgumentException.class, () ->
                new User("alice", "@example.com", "Password123", "Alice"));

        assertThrows(IllegalArgumentException.class, () ->
                new User("alice", "alice@", "Password123", "Alice"));

        assertThrows(IllegalArgumentException.class, () ->
                new User("alice", "alice @example.com", "Password123", "Alice"));
    }
}
