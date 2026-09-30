package com.neueda.accountservice.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import com.neueda.accountservice.models.Account;
import com.neueda.accountservice.repositories.AccountRepository;
import com.neueda.accountservice.enums.AccountStatus;
import com.neueda.accountservice.exceptions.AccountNotActiveException;
import com.neueda.accountservice.exceptions.InsufficientFundsException;
import com.neueda.accountservice.exceptions.AccountNotFoundException;
import org.springframework.transaction.annotation.Transactional;
import com.neueda.accountservice.enums.MovementType;
import com.neueda.accountservice.models.CashMovement;
import com.neueda.accountservice.repositories.CashMovementRepository;


@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final CashMovementRepository cashMovementRepository;

    public AccountService(AccountRepository accountRepository, CashMovementRepository cashMovementRepository) {
        this.accountRepository = accountRepository;
        this.cashMovementRepository = cashMovementRepository;
    }

    public Account createAccount(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null");
        }
        
        if (account.getCreatedAt() == null) {
            account.setCreatedAt(LocalDateTime.now());
        }
        if (account.getLastUpdated() == null) {
            account.setLastUpdated(LocalDateTime.now());
        }
        if (account.getVersion() == 0) {
            account.setVersion(1);
        }
        
        accountRepository.save(account);
        return account;
    }

    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    public Account getAccountById(String accountId) throws AccountNotFoundException {
        if (accountId == null || accountId.trim().isEmpty()) {
            throw new IllegalArgumentException("Account ID cannot be null or empty");
        }
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));
    }

    public Account updateAccount(String accountId, Account updatedAccount) throws AccountNotFoundException {
        Account existing = getAccountById(accountId);
        
        if (updatedAccount.getHolderName() != null && !updatedAccount.getHolderName().trim().isEmpty()) {
            existing.setHolderName(updatedAccount.getHolderName());
        }
        if (updatedAccount.getStatus() != null) {
            existing.setStatus(updatedAccount.getStatus());
        }
        
        existing.setLastUpdated(LocalDateTime.now());
        existing.setVersion(existing.getVersion() + 1);
        
        accountRepository.save(existing);
        return existing;
    }

    public void deleteAccount(String accountId) throws AccountNotFoundException {
        if (!accountRepository.existsById(accountId)) {
            throw new AccountNotFoundException("Account not found: " + accountId);
        }
        accountRepository.deleteById(accountId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Account credit(String accountId, BigDecimal amount) throws AccountNotActiveException, AccountNotFoundException {
        return credit(accountId, amount, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public Account credit(String accountId, BigDecimal amount, String orderId)
            throws AccountNotActiveException, AccountNotFoundException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }

        Account account = getAccountById(accountId);

        if (isRepeat(orderId, MovementType.CREDIT, accountId, amount)) {
            return account;
        }

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException("Cannot credit an inactive account");
        }

        return applyChange(account, amount);
    }

    @Transactional(rollbackFor = Exception.class)
    public Account debit(String accountId, BigDecimal amount)
            throws AccountNotActiveException, InsufficientFundsException, AccountNotFoundException {
        return debit(accountId, amount, null);
    }

    @Transactional(rollbackFor = Exception.class)
    public Account debit(String accountId, BigDecimal amount, String orderId)
            throws AccountNotActiveException, InsufficientFundsException, AccountNotFoundException {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Debit amount must be positive");
        }

        Account account = getAccountById(accountId);

        if (isRepeat(orderId, MovementType.DEBIT, accountId, amount)) {
            return account;
        }

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AccountNotActiveException("Cannot debit an inactive account");
        }

        if (account.getCashBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                "Insufficient funds. Balance: " + account.getCashBalance() +
                ", Requested: " + amount
            );
        }

        return applyChange(account, amount.negate());
    }

    @Transactional(rollbackFor = Exception.class)
    public Account reverse(String accountId, String orderId)
            throws AccountNotFoundException, InsufficientFundsException {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("Order ID is required for a reversal");
        }

        Account account = getAccountById(accountId);

        List<CashMovement> originals = cashMovementRepository.findByOrderId(orderId).stream()
            .filter(movement -> movement.getMovementType() != MovementType.REVERSAL)
            .toList();

        if (originals.isEmpty()) {
            return account;
        }
        if (originals.size() > 1) {
            throw new IllegalArgumentException("Order " + orderId + " has more than one cash movement to reverse");
        }

        CashMovement original = originals.get(0);
        if (!original.getAccountId().equals(accountId)) {
            throw new IllegalArgumentException("Order " + orderId + " belongs to a different account");
        }

        if (isRepeat(orderId, MovementType.REVERSAL, accountId, original.getAmount())) {
            return account;
        }

        BigDecimal change = original.getMovementType() == MovementType.DEBIT
            ? original.getAmount()
            : original.getAmount().negate();

        if (account.getCashBalance().add(change).compareTo(BigDecimal.ZERO) < 0) {
            throw new InsufficientFundsException(
                "Insufficient funds to reverse order " + orderId + ". Balance: " + account.getCashBalance()
            );
        }

        return applyChange(account, change);
    }

    private boolean isRepeat(String orderId, MovementType type, String accountId, BigDecimal amount) {
        return orderId != null
            && cashMovementRepository.insertIfAbsent(orderId, type.name(), accountId, amount) == 0;
    }

    private Account applyChange(Account account, BigDecimal change) {
        account.setCashBalance(account.getCashBalance().add(change));
        account.setLastUpdated(LocalDateTime.now());
        account.setVersion(account.getVersion() + 1);

        accountRepository.save(account);
        return account;
    }

}
