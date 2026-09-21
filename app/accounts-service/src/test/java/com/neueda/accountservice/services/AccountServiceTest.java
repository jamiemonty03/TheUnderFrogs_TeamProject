package com.neueda.accountservice.services;

import com.neueda.accountservice.models.Account;
import com.neueda.accountservice.enums.AccountStatus;
import com.neueda.accountservice.exceptions.AccountNotActiveException;
import com.neueda.accountservice.exceptions.InsufficientFundsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

public class AccountServiceTest {
    
    private AccountService accountService;
    private Account account;
    
    @BeforeEach
    public void setUp() {
        accountService = new AccountService();
        account = new Account("ACC001", "John Doe", new BigDecimal("5000"), AccountStatus.ACTIVE);
    }
    
    @Test
    @DisplayName("credit: Valid amount increases cash balance and increments version")
    public void testCreditValidAmount() throws AccountNotActiveException {
        accountService.credit(account, new BigDecimal("1000"));
        
        assertEquals(new BigDecimal("6000"), account.getCashBalance());
        assertEquals(1, account.getVersion());
    }
    
    @Test
    @DisplayName("credit: Multiple credit operations accumulate balance correctly")
    public void testCreditMultipleTimes() throws AccountNotActiveException {
        accountService.credit(account, new BigDecimal("500"));
        accountService.credit(account, new BigDecimal("300"));
        
        assertEquals(new BigDecimal("5800"), account.getCashBalance());
        assertEquals(2, account.getVersion());
    }
    
    @Test
    @DisplayName("credit: Throws exception when account is null")
    public void testCreditWithNullAccount() {
        assertThrows(IllegalArgumentException.class, () -> {
            accountService.credit(null, new BigDecimal("1000"));
        });
    }
    
    @Test
    @DisplayName("credit: Throws exception when amount is null")
    public void testCreditWithNullAmount() {
        assertThrows(IllegalArgumentException.class, () -> {
            accountService.credit(account, null);
        });
    }
    
    @Test
    @DisplayName("credit: Throws exception when amount is negative")
    public void testCreditWithNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> {
            accountService.credit(account, new BigDecimal("-1000"));
        });
    }
    
    @Test
    @DisplayName("credit: Throws exception when amount is zero")
    public void testCreditWithZeroAmount() throws AccountNotActiveException {
        assertThrows(IllegalArgumentException.class, () -> {
            accountService.credit(account, BigDecimal.ZERO);
        });
    }
    
    @Test
    @DisplayName("credit: Throws exception when account is SUSPENDED")
    public void testCreditWithInactiveAccount() {
        account.setStatus(AccountStatus.SUSPENDED);
        
        assertThrows(AccountNotActiveException.class, () -> {
            accountService.credit(account, new BigDecimal("1000"));
        });
    }
    
    @Test
    @DisplayName("credit: Throws exception when account is INACTIVE")
    public void testCreditWithClosedAccount() {
        account.setStatus(AccountStatus.INACTIVE);
        
        assertThrows(AccountNotActiveException.class, () -> {
            accountService.credit(account, new BigDecimal("1000"));
        });
    }
    
    @Test
    @DisplayName("debit: Valid amount decreases cash balance and increments version")
    public void testDebitValidAmount() throws AccountNotActiveException, InsufficientFundsException {
        accountService.debit(account, new BigDecimal("1000"));
        
        assertEquals(new BigDecimal("4000"), account.getCashBalance());
        assertEquals(1, account.getVersion());
    }
    
    @Test
    @DisplayName("debit: Multiple debit operations decrease balance correctly")
    public void testDebitMultipleTimes() throws AccountNotActiveException, InsufficientFundsException {
        accountService.debit(account, new BigDecimal("500"));
        accountService.debit(account, new BigDecimal("300"));
        
        assertEquals(new BigDecimal("4200"), account.getCashBalance());
        assertEquals(2, account.getVersion());
    }
    
    @Test
    @DisplayName("debit: Can debit exact balance amount")
    public void testDebitExactBalance() throws AccountNotActiveException, InsufficientFundsException {
        accountService.debit(account, new BigDecimal("5000"));
        
        assertEquals(BigDecimal.ZERO, account.getCashBalance());
    }
    
    @Test
    @DisplayName("debit: Throws exception when amount exceeds balance")
    public void testDebitMoreThanBalance() {
        assertThrows(InsufficientFundsException.class, () -> {
            accountService.debit(account, new BigDecimal("6000"));
        });
    }
    
    @Test
    @DisplayName("debit: Throws exception when account is null")
    public void testDebitWithNullAccount() {
        assertThrows(IllegalArgumentException.class, () -> {
            accountService.debit(null, new BigDecimal("1000"));
        });
    }
    
    @Test
    @DisplayName("debit: Throws exception when amount is null")
    public void testDebitWithNullAmount() {
        assertThrows(IllegalArgumentException.class, () -> {
            accountService.debit(account, null);
        });
    }
    
    @Test
    @DisplayName("debit: Throws exception when amount is negative")
    public void testDebitWithNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> {
            accountService.debit(account, new BigDecimal("-1000"));
        });
    }
    
    @Test
    @DisplayName("debit: Throws exception when amount is zero")
    public void testDebitWithZeroAmount() throws AccountNotActiveException, InsufficientFundsException {
        assertThrows(IllegalArgumentException.class, () -> {
            accountService.debit(account, BigDecimal.ZERO);
        });
    }
    
    @Test
    @DisplayName("debit: Throws exception when account is SUSPENDED")
    public void testDebitWithInactiveAccount() {
        account.setStatus(AccountStatus.SUSPENDED);
        
        assertThrows(AccountNotActiveException.class, () -> {
            accountService.debit(account, new BigDecimal("1000"));
        });
    }
    
    @Test
    @DisplayName("debit: Throws exception when account is INACTIVE")
    public void testDebitWithClosedAccount() {
        account.setStatus(AccountStatus.INACTIVE);
        
        assertThrows(AccountNotActiveException.class, () -> {
            accountService.debit(account, new BigDecimal("1000"));
        });
    }
    
    @Test
    @DisplayName("credit/debit sequence: Multiple operations maintain correct balance and version")
    public void testCreditAndDebitSequence() throws AccountNotActiveException, InsufficientFundsException {
        accountService.credit(account, new BigDecimal("2000"));
        accountService.debit(account, new BigDecimal("3000"));
        accountService.credit(account, new BigDecimal("1500"));
        
        assertEquals(new BigDecimal("5500"), account.getCashBalance());
        assertEquals(3, account.getVersion());
    }
}