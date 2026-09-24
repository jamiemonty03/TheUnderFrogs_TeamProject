package com.neueda.accountservice.models;

import com.neueda.accountservice.enums.AccountStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class AccountTest {

    @Test
    @DisplayName("constructor: Valid arguments create an account with zero version and ACTIVE-checkable status")
    public void testConstructorValidArguments() {
        Account account = new Account("ACC001", 1L, "John Doe", new BigDecimal("5000"), AccountStatus.ACTIVE);

        assertEquals("ACC001", account.getAccountId());
        assertEquals(1L, account.getUserId());
        assertEquals("John Doe", account.getHolderName());
        assertEquals(new BigDecimal("5000"), account.getCashBalance());
        assertEquals(AccountStatus.ACTIVE, account.getStatus());
        assertEquals(0, account.getVersion());
        assertNotNull(account.getCreatedAt());
        assertNotNull(account.getLastUpdated());
    }

    @Test
    @DisplayName("constructor: Throws exception when accountId is null")
    public void testConstructorNullAccountId() {
        assertThrows(IllegalArgumentException.class, () ->
                new Account(null, 1L, "John Doe", new BigDecimal("5000"), AccountStatus.ACTIVE));
    }

    @Test
    @DisplayName("constructor: Throws exception when accountId is blank")
    public void testConstructorBlankAccountId() {
        assertThrows(IllegalArgumentException.class, () ->
                new Account("   ", 1L, "John Doe", new BigDecimal("5000"), AccountStatus.ACTIVE));
    }

    @Test
    @DisplayName("constructor: Throws exception when holderName is null")
    public void testConstructorNullHolderName() {
        assertThrows(IllegalArgumentException.class, () ->
                new Account("ACC001", 1L, null, new BigDecimal("5000"), AccountStatus.ACTIVE));
    }

    @Test
    @DisplayName("constructor: Throws exception when holderName is blank")
    public void testConstructorBlankHolderName() {
        assertThrows(IllegalArgumentException.class, () ->
                new Account("ACC001", 1L, "   ", new BigDecimal("5000"), AccountStatus.ACTIVE));
    }

    @Test
    @DisplayName("constructor: Throws exception when cashBalance is null")
    public void testConstructorNullCashBalance() {
        assertThrows(IllegalArgumentException.class, () ->
                new Account("ACC001", 1L, "John Doe", null, AccountStatus.ACTIVE));
    }

    @Test
    @DisplayName("constructor: Throws exception when status is null")
    public void testConstructorNullStatus() {
        assertThrows(IllegalArgumentException.class, () ->
                new Account("ACC001", 1L, "John Doe", new BigDecimal("5000"), null));
    }

    @Test
    @DisplayName("constructor: Accepts a negative cash balance without validation (no lower bound enforced)")
    public void testConstructorAllowsNegativeCashBalance() {
        Account account = new Account("ACC001", 1L, "John Doe", new BigDecimal("-100"), AccountStatus.ACTIVE);

        assertEquals(new BigDecimal("-100"), account.getCashBalance());
    }

    @Test
    @DisplayName("isActive: Returns true only when status is ACTIVE")
    public void testIsActiveWhenActive() {
        Account account = new Account("ACC001", 1L, "John Doe", new BigDecimal("5000"), AccountStatus.ACTIVE);

        assertTrue(account.isActive());
    }

    @Test
    @DisplayName("isActive: Returns false when status is SUSPENDED")
    public void testIsActiveWhenSuspended() {
        Account account = new Account("ACC001", 1L, "John Doe", new BigDecimal("5000"), AccountStatus.SUSPENDED);

        assertFalse(account.isActive());
    }

    @Test
    @DisplayName("isActive: Returns false when status is CLOSED")
    public void testIsActiveWhenClosed() {
        Account account = new Account("ACC001", "John Doe", new BigDecimal("5000"), AccountStatus.CLOSED);

        assertFalse(account.isActive());
    }

    @Test
    @DisplayName("isActive: Reflects status changes made after construction via setStatus")
    public void testIsActiveTracksStatusChanges() {
        Account account = new Account("ACC001", 1L, "John Doe", new BigDecimal("5000"), AccountStatus.ACTIVE);
        assertTrue(account.isActive());

        account.setStatus(AccountStatus.SUSPENDED);

        assertFalse(account.isActive());
    }

    @Test
    @DisplayName("no-arg constructor: Leaves fields unset, unlike the validating constructor")
    public void testNoArgConstructorLeavesFieldsNull() {
        Account account = new Account();

        assertNull(account.getAccountId());
        assertNull(account.getUserId());
        assertNull(account.getHolderName());
        assertNull(account.getCashBalance());
        assertNull(account.getStatus());
        assertEquals(0, account.getVersion());
    }

    @Test
    @DisplayName("constructor: Throws exception when userId is null")
    public void testConstructorNullUserId() {
        assertThrows(IllegalArgumentException.class, () ->
                new Account("ACC001", null, "John Doe", new BigDecimal("5000"), AccountStatus.ACTIVE));
    }

    @Test
    @DisplayName("setters: Mutating fields after construction updates getters accordingly")
    public void testSettersUpdateState() {
        Account account = new Account("ACC001", 1L, "John Doe", new BigDecimal("5000"), AccountStatus.ACTIVE);

        account.setCashBalance(new BigDecimal("7500"));
        account.setVersion(3);
        account.setUpdatedBy("system");

        assertEquals(new BigDecimal("7500"), account.getCashBalance());
        assertEquals(3, account.getVersion());
        assertEquals("system", account.getUpdatedBy());
    }
}
