package com.neueda.accountservice.services;

import com.neueda.accountservice.models.Account;
import com.neueda.accountservice.repositories.AccountRepository;
import com.neueda.accountservice.enums.AccountStatus;
import com.neueda.accountservice.exceptions.AccountNotActiveException;
import com.neueda.accountservice.exceptions.InsufficientFundsException;
import com.neueda.accountservice.exceptions.AccountNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService Unit Tests")
public class AccountServiceTest {
    
    @Mock
    private AccountRepository accountRepository;
    
    private AccountService accountService;
    private Account account;
    
    @BeforeEach
    public void setUp() {
        accountService = new AccountService(accountRepository);
        account = new Account("ACC001", 1L, "John Doe", new BigDecimal("5000"), AccountStatus.ACTIVE);
        account.setVersion(1);
        account.setCreatedAt(LocalDateTime.now());
        account.setLastUpdated(LocalDateTime.now());
    }
    
    // ==================== CREATE ACCOUNT TESTS ====================
    
    @Test
    @DisplayName("createAccount: Successfully creates a new account with default timestamps")
    public void testCreateAccountSuccess() {
        Account newAccount = new Account("ACC002", 1L, "Jane Smith", new BigDecimal("1000"), AccountStatus.ACTIVE);
        doAnswer(invocation -> {
            Account acc = invocation.getArgument(0);
            assertNotNull(acc.getCreatedAt());
            assertNotNull(acc.getLastUpdated());
            assertEquals(1, acc.getVersion());
            return null;
        }).when(accountRepository).save(any(Account.class));
        
        Account result = accountService.createAccount(newAccount);
        
        assertNotNull(result.getCreatedAt());
        assertNotNull(result.getLastUpdated());
        assertEquals(1, result.getVersion());
        verify(accountRepository).save(any(Account.class));
    }
    
    @Test
    @DisplayName("createAccount: Throws exception when account is null")
    public void testCreateAccountWithNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            accountService.createAccount(null);
        });
        
        verify(accountRepository, never()).save(any());
    }
    
    // ==================== GET ACCOUNT TESTS ====================
    
    @Test
    @DisplayName("getAccountById: Successfully retrieves account by ID")
    public void testGetAccountByIdSuccess() throws AccountNotFoundException {
        when(accountRepository.findById("ACC001")).thenReturn(Optional.of(account));
        
        Account result = accountService.getAccountById("ACC001");
        
        assertEquals("ACC001", result.getAccountId());
        assertEquals("John Doe", result.getHolderName());
        verify(accountRepository).findById("ACC001");
    }
    
    @Test
    @DisplayName("getAccountById: Throws exception when account not found")
    public void testGetAccountByIdNotFound() {
        when(accountRepository.findById("INVALID")).thenReturn(Optional.empty());
        
        assertThrows(AccountNotFoundException.class, () -> {
            accountService.getAccountById("INVALID");
        });
        
        verify(accountRepository).findById("INVALID");
    }
    
    @Test
    @DisplayName("getAccountById: Throws exception when account ID is null")
    public void testGetAccountByIdWithNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            accountService.getAccountById(null);
        });
        
        verify(accountRepository, never()).findById(any());
    }
    
    @Test
    @DisplayName("getAccountById: Throws exception when account ID is empty")
    public void testGetAccountByIdWithEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            accountService.getAccountById("   ");
        });
        
        verify(accountRepository, never()).findById(any());
    }
    
    // ==================== UPDATE ACCOUNT TESTS ====================
    
    @Test
    @DisplayName("updateAccount: Successfully updates account details")
    public void testUpdateAccountSuccess() throws AccountNotFoundException {
        Account updateData = new Account("ACC001", 1L, "Jane Updated", new BigDecimal("0"), AccountStatus.ACTIVE);
        updateData.setHolderName("Jane Updated");
        
        when(accountRepository.findById("ACC001")).thenReturn(Optional.of(account));
        doAnswer(invocation -> {
            Account acc = invocation.getArgument(0);
            assertEquals(2, acc.getVersion());
            return null;
        }).when(accountRepository).save(any(Account.class));
        
        Account result = accountService.updateAccount("ACC001", updateData);
        
        assertEquals("Jane Updated", result.getHolderName());
        assertEquals(2, result.getVersion());
        verify(accountRepository).findById("ACC001");
        verify(accountRepository).save(any(Account.class));
    }
    
    @Test
    @DisplayName("updateAccount: Throws exception when account not found")
    public void testUpdateAccountNotFound() {
        when(accountRepository.findById("INVALID")).thenReturn(Optional.empty());
        
        assertThrows(AccountNotFoundException.class, () -> {
            accountService.updateAccount("INVALID", account);
        });
    }
    
    // ==================== DELETE ACCOUNT TESTS ====================
    
    @Test
    @DisplayName("deleteAccount: Successfully deletes an account")
    public void testDeleteAccountSuccess() throws AccountNotFoundException {
        when(accountRepository.existsById("ACC001")).thenReturn(true);
        
        accountService.deleteAccount("ACC001");
        
        verify(accountRepository).existsById("ACC001");
        verify(accountRepository).deleteById("ACC001");
    }
    
    @Test
    @DisplayName("deleteAccount: Throws exception when account not found")
    public void testDeleteAccountNotFound() {
        when(accountRepository.existsById("INVALID")).thenReturn(false);
        
        assertThrows(AccountNotFoundException.class, () -> {
            accountService.deleteAccount("INVALID");
        });
        
        verify(accountRepository, never()).deleteById(any());
    }
    
    // ==================== CREDIT TESTS ====================
    
    @Test
    @DisplayName("credit: Successfully credits valid amount to active account")
    public void testCreditValidAmount() throws AccountNotFoundException, AccountNotActiveException {
        when(accountRepository.findById("ACC001")).thenReturn(Optional.of(account));
        doAnswer(invocation -> {
            Account acc = invocation.getArgument(0);
            assertEquals(new BigDecimal("6000"), acc.getCashBalance());
            assertEquals(2, acc.getVersion());
            return null;
        }).when(accountRepository).save(any(Account.class));
        
        Account result = accountService.credit("ACC001", new BigDecimal("1000"));
        
        assertEquals(new BigDecimal("6000"), result.getCashBalance());
        assertEquals(2, result.getVersion());
        verify(accountRepository).findById("ACC001");
        verify(accountRepository).save(any(Account.class));
    }
    
    @Test
    @DisplayName("credit: Throws exception when amount is null")
    public void testCreditWithNullAmount() {
        assertThrows(IllegalArgumentException.class, () -> {
            accountService.credit("ACC001", null);
        });
    }
    
    @Test
    @DisplayName("credit: Throws exception when amount is negative")
    public void testCreditWithNegativeAmount() {
        assertThrows(IllegalArgumentException.class, () -> {
            accountService.credit("ACC001", new BigDecimal("-1000"));
        });
    }
    
    @Test
    @DisplayName("credit: Throws exception when amount is zero")
    public void testCreditWithZeroAmount() {
        assertThrows(IllegalArgumentException.class, () -> {
            accountService.credit("ACC001", BigDecimal.ZERO);
        });
    }
    
    @Test
    @DisplayName("credit: Throws exception when account is not active")
    public void testCreditWithInactiveAccount() {
        account.setStatus(AccountStatus.SUSPENDED);
        when(accountRepository.findById("ACC001")).thenReturn(Optional.of(account));
        
        assertThrows(AccountNotActiveException.class, () -> {
            accountService.credit("ACC001", new BigDecimal("1000"));
        });
        
        verify(accountRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("credit: Throws exception when account not found")
    public void testCreditAccountNotFound() {
        when(accountRepository.findById("INVALID")).thenReturn(Optional.empty());
        
        assertThrows(AccountNotFoundException.class, () -> {
            accountService.credit("INVALID", new BigDecimal("1000"));
        });
    }
    
    // ==================== DEBIT TESTS ====================
    
    @Test
    @DisplayName("debit: Successfully debits valid amount from active account")
    public void testDebitValidAmount() throws AccountNotFoundException, AccountNotActiveException, InsufficientFundsException {
        when(accountRepository.findById("ACC001")).thenReturn(Optional.of(account));
        doAnswer(invocation -> {
            Account acc = invocation.getArgument(0);
            assertEquals(new BigDecimal("4000"), acc.getCashBalance());
            assertEquals(2, acc.getVersion());
            return null;
        }).when(accountRepository).save(any(Account.class));
        
        Account result = accountService.debit("ACC001", new BigDecimal("1000"));
        
        assertEquals(new BigDecimal("4000"), result.getCashBalance());
        assertEquals(2, result.getVersion());
        verify(accountRepository).findById("ACC001");
        verify(accountRepository).save(any(Account.class));
    }
    
    @Test
    @DisplayName("debit: Can debit exact balance amount")
    public void testDebitExactBalance() throws AccountNotFoundException, AccountNotActiveException, InsufficientFundsException {
        when(accountRepository.findById("ACC001")).thenReturn(Optional.of(account));
        doAnswer(invocation -> {
            Account acc = invocation.getArgument(0);
            assertEquals(BigDecimal.ZERO, acc.getCashBalance());
            return null;
        }).when(accountRepository).save(any(Account.class));
        
        Account result = accountService.debit("ACC001", new BigDecimal("5000"));
        
        assertEquals(BigDecimal.ZERO, result.getCashBalance());
    }
    
    @Test
    @DisplayName("debit: Throws exception when amount exceeds balance")
    public void testDebitMoreThanBalance() throws AccountNotFoundException, AccountNotActiveException, InsufficientFundsException {
        when(accountRepository.findById("ACC001")).thenReturn(Optional.of(account));
        
        assertThrows(InsufficientFundsException.class, () -> {
            accountService.debit("ACC001", new BigDecimal("6000"));
        });
    }
    
    @Test
    @DisplayName("debit: Throws exception when amount is null")
    public void testDebitWithNullAmount() throws AccountNotFoundException, AccountNotActiveException, InsufficientFundsException {
        assertThrows(IllegalArgumentException.class, () -> {
            accountService.debit("ACC001", null);
        });
    }
    
    @Test
    @DisplayName("debit: Throws exception when amount is negative")
    public void testDebitWithNegativeAmount() throws AccountNotFoundException, AccountNotActiveException, InsufficientFundsException {
        assertThrows(IllegalArgumentException.class, () -> {
            accountService.debit("ACC001", new BigDecimal("-1000"));
        });
    }
    
    @Test
    @DisplayName("debit: Throws exception when amount is zero")
    public void testDebitWithZeroAmount() throws AccountNotFoundException, AccountNotActiveException, InsufficientFundsException {
        assertThrows(IllegalArgumentException.class, () -> {
            accountService.debit("ACC001", BigDecimal.ZERO);
        });
    }
    
    @Test
    @DisplayName("debit: Throws exception when account is not active")
    public void testDebitWithInactiveAccount() throws AccountNotFoundException, AccountNotActiveException, InsufficientFundsException {
        account.setStatus(AccountStatus.INACTIVE);
        when(accountRepository.findById("ACC001")).thenReturn(Optional.of(account));
        
        assertThrows(AccountNotActiveException.class, () -> {
            accountService.debit("ACC001", new BigDecimal("1000"));
        });
        
        verify(accountRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("debit: Throws exception when account not found")
    public void testDebitAccountNotFound() throws AccountNotFoundException, AccountNotActiveException, InsufficientFundsException {
        when(accountRepository.findById("INVALID")).thenReturn(Optional.empty());
        
        assertThrows(AccountNotFoundException.class, () -> {
            accountService.debit("INVALID", new BigDecimal("1000"));
        });
    }
}