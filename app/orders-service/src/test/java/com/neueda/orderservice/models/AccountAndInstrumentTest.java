package com.neueda.orderservice.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.neueda.orderservice.enums.AccountStatus;

class AccountAndInstrumentTest {

    @Test
    @DisplayName("Account setters populate an account created with the no-arg constructor")
    void accountSetters() {
        Account account = new Account();
        assertNull(account.getAccountId());

        account.setAccountId("ACC0001");
        account.setHolderName("Alice Johnson");
        account.setCashBalance(new BigDecimal("10500.75"));
        account.setStatus(AccountStatus.ACTIVE);

        assertEquals("ACC0001", account.getAccountId());
        assertEquals("Alice Johnson", account.getHolderName());
        assertEquals(new BigDecimal("10500.75"), account.getCashBalance());
        assertEquals(AccountStatus.ACTIVE, account.getStatus());
    }

    @Test
    @DisplayName("Only ACTIVE accounts are active")
    void accountIsActiveOnlyWhenActive() {
        Account account = new Account("ACC0001", "Alice", BigDecimal.TEN, AccountStatus.ACTIVE);
        assertTrue(account.isActive());

        account.setStatus(AccountStatus.SUSPENDED);
        assertFalse(account.isActive());

        account.setStatus(AccountStatus.CLOSED);
        assertFalse(account.isActive());
    }

    @Test
    @DisplayName("Instrument constructor and setters")
    void instrumentGettersAndSetters() {
        Instrument instrument = new Instrument("AAPL", "Apple", new BigDecimal("150.00"), true);
        assertEquals("AAPL", instrument.getSymbol());
        assertEquals("Apple", instrument.getName());
        assertEquals(new BigDecimal("150.00"), instrument.getPrice());
        assertTrue(instrument.isTradable());

        Instrument empty = new Instrument();
        empty.setSymbol("MSFT");
        empty.setName("Microsoft");
        empty.setPrice(new BigDecimal("300.00"));
        empty.setTradable(false);

        assertEquals("MSFT", empty.getSymbol());
        assertEquals("Microsoft", empty.getName());
        assertEquals(new BigDecimal("300.00"), empty.getPrice());
        assertFalse(empty.isTradable());
    }
}
