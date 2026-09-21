package com.neueda.leap.services.orderServices;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.neueda.leap.enums.OrderStatus;
import com.neueda.leap.exceptions.*;
import com.neueda.leap.models.Account;
import com.neueda.leap.models.Instrument;
import com.neueda.leap.models.Order;
import com.neueda.leap.services.AccountService;
import com.neueda.leap.services.PositionService;

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
    private final PositionService positionService;

    public BuyOrderStrategy(
            AccountService accountService,
            PositionService positionService) {
        this.accountService = accountService;
        this.positionService = positionService;
    }

    @Override
    public OrderResult execute(Order order, Account account, Instrument instrument) {
        boolean cashDebited = false;
        try {
            BigDecimal totalCost = order.getPrice().multiply(BigDecimal.valueOf(order.getQuantity()));
            accountService.debit(account, totalCost);
            cashDebited = true;

            positionService.updatePositionAfterBuy(account.getAccountId(), order.getSymbol(), 
                order.getQuantity(), order.getPrice());
            

            order.setOrderStatus(OrderStatus.FILLED);
            order.setLastUpdated(LocalDateTime.now());

            String successMessage = String.format(
                "BUY order %s FILLED: %d shares of %s @ $%.2f = $%.2f",
                order.getOrderId(),
                order.getQuantity(),
                order.getSymbol(),
                order.getPrice(),
                totalCost
            );
            return new OrderResult(true, successMessage, null);
            
        } catch (Exception e) {
            if (cashDebited) {
                try {
                    
                    BigDecimal totalCost = order.getPrice().multiply(BigDecimal.valueOf(order.getQuantity()));
                    accountService.credit(account, totalCost);
                } catch (Exception rollbackError) {
                    System.out.println("Rollback failed: " + rollbackError.getMessage());
                }
            }

            order.setOrderStatus(OrderStatus.REJECTED);
            order.setLastUpdated(LocalDateTime.now());

            String failureMessage = "BUY order " + order.getOrderId() + " execution FAILED: " + e.getMessage();
            System.out.println(failureMessage);
            return new OrderResult(false, failureMessage, null);
        }
    }
}
