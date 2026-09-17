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
 * Strategy for executing SELL orders.
 * 
 * Logic:
 * 1. Order already validated by OrderService before reaching here
 * 2. Snapshot account state for rollback
 * 3. Credit cash to account
 * 4. Update position (decrement quantity)
 * 5. Mark order FILLED
 * 6. Return success
 * 
 * On failure: reverse credit, mark REJECTED, return failure
 */
public class SellOrderStrategy implements OrderExecutionStrategy {
    
    private final AccountService accountService;
    private final PositionManager positionManager;

    public SellOrderStrategy(
            AccountService accountService,
            PositionManager positionManager) {
        this.accountService = accountService;
        this.positionManager = positionManager;
    }

    @Override
    public OrderResult execute(Order order, Account account, Instrument instrument) {
        boolean cashCredited = false;
        try {
            BigDecimal accountBalanceSnapshot = account.getCashBalance();
            
            BigDecimal totalProceeds = order.getPrice().multiply(BigDecimal.valueOf(order.getQuantity()));
            accountService.credit(account, totalProceeds);
            cashCredited = true;

            positionManager.updatePositionAfterSell(account.getAccountId(), order.getSymbol(), order.getQuantity());
            
            order.setOrderStatus(OrderStatus.FILLED);
            order.setVersion(order.getVersion() + 1);
            order.setLastUpdated(LocalDateTime.now());

            String successMessage = String.format(
                "SELL order %s FILLED: %d shares of %s @ $%.2f = $%.2f. Account balance: $%.2f → $%.2f",
                order.getOrderId(),
                order.getQuantity(),
                order.getSymbol(),
                order.getPrice(),
                totalProceeds,
                accountBalanceSnapshot,
                account.getCashBalance()
            );
            return new OrderResult(true, successMessage, null);
            
        } catch (Exception e) {
            try {
                if (cashCredited) {
                    BigDecimal totalProceeds = order.getPrice().multiply(BigDecimal.valueOf(order.getQuantity()));
                    accountService.debit(account, totalProceeds);
                }
                order.setOrderStatus(OrderStatus.REJECTED);
                order.setVersion(order.getVersion() + 1);
                order.setLastUpdated(LocalDateTime.now());
                
                String failureMessage = "SELL order " + order.getOrderId() + " execution FAILED: " + e.getMessage();
                return new OrderResult(false, failureMessage, null);
                
            } catch (Exception rollbackError) {
                String failureMessage = String.format(
                    "SELL order %s execution FAILED and rollback FAILED: %s. Rollback error: %s",
                    order.getOrderId(),
                    e.getMessage(),
                    rollbackError.getMessage()
                );
                return new OrderResult(false, failureMessage, null);
            }
        }
    }
}
