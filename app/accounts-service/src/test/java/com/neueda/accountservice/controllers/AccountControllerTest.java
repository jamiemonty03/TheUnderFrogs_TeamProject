package com.neueda.accountservice.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neueda.accountservice.models.Account;
import com.neueda.accountservice.services.AccountService;
import com.neueda.accountservice.enums.AccountStatus;
import com.neueda.accountservice.exceptions.AccountNotFoundException;
import com.neueda.accountservice.exceptions.AccountNotActiveException;
import com.neueda.accountservice.exceptions.InsufficientFundsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@WebMvcTest(AccountController.class)
@DisplayName("AccountController Integration Tests")
public class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    @Autowired
    private ObjectMapper objectMapper;

    private Account testAccount;

    @BeforeEach
    public void setUp() {
        testAccount = new Account("ACC001", "John Doe", new BigDecimal("5000"), AccountStatus.ACTIVE);
        testAccount.setVersion(1);
        testAccount.setCreatedAt(LocalDateTime.now());
        testAccount.setLastUpdated(LocalDateTime.now());
    }

    // ==================== CREATE ACCOUNT TESTS ====================

    @Test
    @DisplayName("POST /accounts: Successfully creates account and returns 201 CREATED")
    public void testCreateAccountSuccess() throws Exception {
        Account newAccount = new Account("ACC002", "Jane Smith", new BigDecimal("1000"), AccountStatus.ACTIVE);
        when(accountService.createAccount(any(Account.class))).thenReturn(newAccount);

        mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newAccount)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountId", equalTo("ACC002")))
                .andExpect(jsonPath("$.holderName", equalTo("Jane Smith")))
                .andExpect(jsonPath("$.cashBalance", notNullValue()))
                .andExpect(jsonPath("$.status", equalTo("ACTIVE")));

        verify(accountService).createAccount(any(Account.class));
    }

    @Test
    @DisplayName("POST /accounts: Returns 422 when account body is invalid")
    public void testCreateAccountInvalidBody() throws Exception {
        mockMvc.perform(post("/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnprocessableEntity());

        verify(accountService, never()).createAccount(any());
    }

    // ==================== GET ACCOUNT TESTS ====================

    @Test
    @DisplayName("GET /accounts/{accountId}: Successfully retrieves account by ID")
    public void testGetAccountSuccess() throws Exception {
        when(accountService.getAccountById("ACC001")).thenReturn(testAccount);

        mockMvc.perform(get("/accounts/ACC001")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId", equalTo("ACC001")))
                .andExpect(jsonPath("$.holderName", equalTo("John Doe")))
                .andExpect(jsonPath("$.cashBalance", notNullValue()))
                .andExpect(jsonPath("$.status", equalTo("ACTIVE")));

        verify(accountService).getAccountById("ACC001");
    }

    @Test
    @DisplayName("GET /accounts/{accountId}: Returns 404 when account not found")
    public void testGetAccountNotFound() throws Exception {
        when(accountService.getAccountById("INVALID")).thenThrow(
                new AccountNotFoundException("Account not found: INVALID"));

        mockMvc.perform(get("/accounts/INVALID")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(accountService).getAccountById("INVALID");
    }

    // ==================== UPDATE ACCOUNT TESTS ====================

    @Test
    @DisplayName("PUT /accounts/{accountId}: Successfully updates account")
    public void testUpdateAccountSuccess() throws Exception {
        Account updateData = new Account("ACC001", "John Updated", new BigDecimal("0"), AccountStatus.ACTIVE);
        Account updatedAccount = new Account("ACC001", "John Updated", new BigDecimal("5000"), AccountStatus.ACTIVE);
        updatedAccount.setVersion(2);

        when(accountService.updateAccount(eq("ACC001"), any(Account.class))).thenReturn(updatedAccount);

        mockMvc.perform(put("/accounts/ACC001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId", equalTo("ACC001")))
                .andExpect(jsonPath("$.holderName", equalTo("John Updated")))
                .andExpect(jsonPath("$.version", equalTo(2)));

        verify(accountService).updateAccount(eq("ACC001"), any(Account.class));
    }

    @Test
    @DisplayName("PUT /accounts/{accountId}: Returns 404 when account not found")
    public void testUpdateAccountNotFound() throws Exception {
        Account updateData = new Account("INVALID", "Updated", new BigDecimal("0"), AccountStatus.ACTIVE);
        when(accountService.updateAccount(eq("INVALID"), any(Account.class))).thenThrow(
                new AccountNotFoundException("Account not found: INVALID"));

        mockMvc.perform(put("/accounts/INVALID")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isNotFound());
    }

    // ==================== DELETE ACCOUNT TESTS ====================

    @Test
    @DisplayName("DELETE /accounts/{accountId}: Successfully deletes account and returns 204 NO CONTENT")
    public void testDeleteAccountSuccess() throws Exception {
        doNothing().when(accountService).deleteAccount("ACC001");

        mockMvc.perform(delete("/accounts/ACC001")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(accountService).deleteAccount("ACC001");
    }

    @Test
    @DisplayName("DELETE /accounts/{accountId}: Returns 404 when account not found")
    public void testDeleteAccountNotFound() throws Exception {
        doThrow(new AccountNotFoundException("Account not found: INVALID"))
                .when(accountService).deleteAccount("INVALID");

        mockMvc.perform(delete("/accounts/INVALID")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(accountService).deleteAccount("INVALID");
    }

    // ==================== CREDIT ACCOUNT TESTS ====================

    @Test
    @DisplayName("POST /accounts/{accountId}/credit: Successfully credits amount to account")
    public void testCreditAccountSuccess() throws Exception {
        Account creditedAccount = new Account("ACC001", "John Doe", new BigDecimal("6000"), AccountStatus.ACTIVE);
        creditedAccount.setVersion(2);

        when(accountService.credit("ACC001", new BigDecimal("1000"))).thenReturn(creditedAccount);

        Map<String, BigDecimal> request = new HashMap<>();
        request.put("amount", new BigDecimal("1000"));

        mockMvc.perform(post("/accounts/ACC001/credit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId", equalTo("ACC001")))
                .andExpect(jsonPath("$.cashBalance", notNullValue()))
                .andExpect(jsonPath("$.version", equalTo(2)));

        verify(accountService).credit("ACC001", new BigDecimal("1000"));
    }

    @Test
    @DisplayName("POST /accounts/{accountId}/credit: Returns 404 when account not found")
    public void testCreditAccountNotFound() throws Exception {
        when(accountService.credit("INVALID", new BigDecimal("1000"))).thenThrow(
                new AccountNotFoundException("Account not found: INVALID"));

        Map<String, BigDecimal> request = new HashMap<>();
        request.put("amount", new BigDecimal("1000"));

        mockMvc.perform(post("/accounts/INVALID/credit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /accounts/{accountId}/credit: Returns 403 when account is inactive")
    public void testCreditAccountInactive() throws Exception {
        when(accountService.credit("ACC001", new BigDecimal("1000"))).thenThrow(
                new AccountNotActiveException("Cannot credit an inactive account"));

        Map<String, BigDecimal> request = new HashMap<>();
        request.put("amount", new BigDecimal("1000"));

        mockMvc.perform(post("/accounts/ACC001/credit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /accounts/{accountId}/credit: Returns 422 when amount is invalid")
    public void testCreditAccountInvalidAmount() throws Exception {
        when(accountService.credit("ACC001", new BigDecimal("-1000"))).thenThrow(
                new IllegalArgumentException("Credit amount must be positive"));

        Map<String, BigDecimal> request = new HashMap<>();
        request.put("amount", new BigDecimal("-1000"));

        mockMvc.perform(post("/accounts/ACC001/credit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    // ==================== DEBIT ACCOUNT TESTS ====================

    @Test
    @DisplayName("POST /accounts/{accountId}/debit: Successfully debits amount from account")
    public void testDebitAccountSuccess() throws Exception {
        Account debitedAccount = new Account("ACC001", "John Doe", new BigDecimal("4000"), AccountStatus.ACTIVE);
        debitedAccount.setVersion(2);

        when(accountService.debit("ACC001", new BigDecimal("1000"))).thenReturn(debitedAccount);

        Map<String, BigDecimal> request = new HashMap<>();
        request.put("amount", new BigDecimal("1000"));

        mockMvc.perform(post("/accounts/ACC001/debit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId", equalTo("ACC001")))
                .andExpect(jsonPath("$.cashBalance", notNullValue()))
                .andExpect(jsonPath("$.version", equalTo(2)));

        verify(accountService).debit("ACC001", new BigDecimal("1000"));
    }

    @Test
    @DisplayName("POST /accounts/{accountId}/debit: Returns 404 when account not found")
    public void testDebitAccountNotFound() throws Exception {
        when(accountService.debit("INVALID", new BigDecimal("1000"))).thenThrow(
                new AccountNotFoundException("Account not found: INVALID"));

        Map<String, BigDecimal> request = new HashMap<>();
        request.put("amount", new BigDecimal("1000"));

        mockMvc.perform(post("/accounts/INVALID/debit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /accounts/{accountId}/debit: Returns 403 when account is inactive")
    public void testDebitAccountInactive() throws Exception {
        when(accountService.debit("ACC001", new BigDecimal("1000"))).thenThrow(
                new AccountNotActiveException("Cannot debit an inactive account"));

        Map<String, BigDecimal> request = new HashMap<>();
        request.put("amount", new BigDecimal("1000"));

        mockMvc.perform(post("/accounts/ACC001/debit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /accounts/{accountId}/debit: Returns 400 when insufficient funds")
    public void testDebitAccountInsufficientFunds() throws Exception {
        when(accountService.debit("ACC001", new BigDecimal("6000"))).thenThrow(
                new InsufficientFundsException("Insufficient funds. Balance: 5000, Requested: 6000"));

        Map<String, BigDecimal> request = new HashMap<>();
        request.put("amount", new BigDecimal("6000"));

        mockMvc.perform(post("/accounts/ACC001/debit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /accounts/{accountId}/debit: Returns 422 when amount is invalid")
    public void testDebitAccountInvalidAmount() throws Exception {
        when(accountService.debit("ACC001", new BigDecimal("-1000"))).thenThrow(
                new IllegalArgumentException("Debit amount must be positive"));

        Map<String, BigDecimal> request = new HashMap<>();
        request.put("amount", new BigDecimal("-1000"));

        mockMvc.perform(post("/accounts/ACC001/debit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }
}
