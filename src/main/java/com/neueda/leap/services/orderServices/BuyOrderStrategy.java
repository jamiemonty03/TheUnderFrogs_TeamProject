package com.neueda.leap.services.orderServices;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.neueda.leap.enums.OrderStatus;
import com.neueda.leap.exceptions.*;
import com.neueda.leap.models.Account;
import com.neueda.leap.models.Instrument;
import com.neueda.leap.models.Order;
import com.neueda.leap.services.AccountService;
import com.neueda.leap.services.PositionManager;

/**
 * Strategy for executing BUY orders.
 * 
 * Logic:
 * 1. Order already validated by OrderService before reaching here
 * 2. Snapshot account state for rollback
 * 3. Debit cash from account
 * 4. Update position (add quantity, recalculate average cost)
 * 5. Mark order FILLED
 * 6. Return success
 * 
 * On failure: reverse debit, mark REJECTED, return failure
 */
public class BuyOrderStrategy implements OrderExecutionStrategy {
    
    private final AccountService accountService;
    private final PositionManager positionManager;

    public BuyOrderStrategy(
            AccountService accountService,
            PositionManager positionManager) {
        this.accountService = accountService;
        this.positionManager = positionManager;
    }

    @Override
    public OrderResult execute(Order order, Account account, Instrument instrument) {
        boolean cashDebited = false;
        try {
            BigDecimal accountBalanceSnapshot = account.getCashBalance();
            
            BigDecimal totalCost = order.getPrice().multiply(BigDecimal.valueOf(order.getQuantity()));
            accountService.debit(account, totalCost);
            cashDebited = true;

            positionManager.updatePositionAfterBuy(account.getAccountId(), order.getSymbol(), 
                order.getQuantity(), order.getPrice());
            

            order.setOrderStatus(OrderStatus.FILLED);
            order.setVersion(order.getVersion() + 1);
            order.setLastUpdated(LocalDateTime.now());

            String successMessage = String.format(
                "BUY order %s FILLED: %d shares of %s @ $%.2f = $%.2f. Account balance: $%.2f → $%.2f",
                order.getOrderId(),
                order.getQuantity(),
                order.getSymbol(),
                order.getPrice(),
                totalCost,
                accountBalanceSnapshot,
                account.getCashBalance()
            );
            return new OrderResult(true, successMessage, null);
            
        } catch (Exception e) {
            try {
                if (cashDebited) {
                    BigDecimal totalCost = order.getPrice().multiply(BigDecimal.valueOf(order.getQuantity()));
                    accountService.credit(account, totalCost);
                }
                order.setOrderStatus(OrderStatus.REJECTED);
                order.setVersion(order.getVersion() + 1);
                order.setLastUpdated(LocalDateTime.now());
                
                String failureMessage = "BUY order " + order.getOrderId() + " execution FAILED: " + e.getMessage();
                return new OrderResult(false, failureMessage, null);
                
            } catch (Exception rollbackError) {
                String failureMessage = String.format(
                    "BUY order %s execution FAILED and rollback FAILED: %s. Rollback error: %s",
                    order.getOrderId(),
                    e.getMessage(),
                    rollbackError.getMessage()
                );
                return new OrderResult(false, failureMessage, null);
            }
        }
    }
}
