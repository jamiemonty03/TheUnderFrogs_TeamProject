package com.neueda.leap.services;

import com.neueda.leap.models.Account;
import com.neueda.leap.enums.AccountStatus;
import com.neueda.leap.exceptions.AccountNotActiveException;
import com.neueda.leap.exceptions.InsufficientFundsException;
import org.junit.jupiter.api.BeforeEach;
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
    public void testCreditValidAmount() throws AccountNotActiveException {
        accountService.credit(account, new BigDecimal("1000"));
        
        assertEquals(new BigDecimal("6000"), account.getCashBalance());
        assertEquals(1, account.getVersion());
    }
    
    @Test
    public void testCreditMultipleTimes() throws AccountNotActiveException {
        accountService.credit(account, new BigDecimal("500"));
        accountService.credit(account, new BigDecimal("300"));
        
        assertEquals(new BigDecimal("5800"), account.getCashBalance());
        assertEquals(2, account.getVersion());
    }
    
    @Test
    public void testCreditWithNullAccount() {
        assertThrows(IllegalArgumentException.class, () -> {
            accountService.credit(null, new BigDecimal("1000"));
        });
    }
    
    @Test
    public void testCreditWithNullAmount() {
        assertThrows(IllegalArgumentException.class, () -> {
            accountService.credit(account, null);
        });
    }
    
    @Test
    public void testCreditWithNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> {
            accountService.credit(account, new BigDecimal("-1000"));
        });
    }
    
    @Test
    public void testCreditWithZeroAmount() throws AccountNotActiveException {
        assertThrows(IllegalArgumentException.class, () -> {
            accountService.credit(account, BigDecimal.ZERO);
        });
    }
    
    @Test
    public void testCreditWithInactiveAccount() {
        account.setStatus(AccountStatus.SUSPENDED);
        
        assertThrows(AccountNotActiveException.class, () -> {
            accountService.credit(account, new BigDecimal("1000"));
        });
    }
    
    @Test
    public void testCreditWithClosedAccount() {
        account.setStatus(AccountStatus.INACTIVE);
        
        assertThrows(AccountNotActiveException.class, () -> {
            accountService.credit(account, new BigDecimal("1000"));
        });
    }
    
    @Test
    public void testDebitValidAmount() throws AccountNotActiveException, InsufficientFundsException {
        accountService.debit(account, new BigDecimal("1000"));
        
        assertEquals(new BigDecimal("4000"), account.getCashBalance());
        assertEquals(1, account.getVersion());
    }
    
    @Test
    public void testDebitMultipleTimes() throws AccountNotActiveException, InsufficientFundsException {
        accountService.debit(account, new BigDecimal("500"));
        accountService.debit(account, new BigDecimal("300"));
        
        assertEquals(new BigDecimal("4200"), account.getCashBalance());
        assertEquals(2, account.getVersion());
    }
    
    @Test
    public void testDebitExactBalance() throws AccountNotActiveException, InsufficientFundsException {
        accountService.debit(account, new BigDecimal("5000"));
        
        assertEquals(BigDecimal.ZERO, account.getCashBalance());
    }
    
    @Test
    public void testDebitMoreThanBalance() {
        assertThrows(InsufficientFundsException.class, () -> {
            accountService.debit(account, new BigDecimal("6000"));
        });
    }
    
    @Test
    public void testDebitWithNullAccount() {
        assertThrows(IllegalArgumentException.class, () -> {
            accountService.debit(null, new BigDecimal("1000"));
        });
    }
    
    @Test
    public void testDebitWithNullAmount() {
        assertThrows(IllegalArgumentException.class, () -> {
            accountService.debit(account, null);
        });
    }
    
    @Test
    public void testDebitWithNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> {
            accountService.debit(account, new BigDecimal("-1000"));
        });
    }
    
    @Test
    public void testDebitWithZeroAmount() throws AccountNotActiveException, InsufficientFundsException {
        assertThrows(IllegalArgumentException.class, () -> {
            accountService.debit(account, BigDecimal.ZERO);
        });
    }
    
    @Test
    public void testDebitWithInactiveAccount() {
        account.setStatus(AccountStatus.SUSPENDED);
        
        assertThrows(AccountNotActiveException.class, () -> {
            accountService.debit(account, new BigDecimal("1000"));
        });
    }
    
    @Test
    public void testDebitWithClosedAccount() {
        account.setStatus(AccountStatus.INACTIVE);
        
        assertThrows(AccountNotActiveException.class, () -> {
            accountService.debit(account, new BigDecimal("1000"));
        });
    }
    
    @Test
    public void testCreditAndDebitSequence() throws AccountNotActiveException, InsufficientFundsException {
        accountService.credit(account, new BigDecimal("2000"));
        accountService.debit(account, new BigDecimal("3000"));
        accountService.credit(account, new BigDecimal("1500"));
        
        assertEquals(new BigDecimal("5500"), account.getCashBalance());
        assertEquals(3, account.getVersion());
    }
}