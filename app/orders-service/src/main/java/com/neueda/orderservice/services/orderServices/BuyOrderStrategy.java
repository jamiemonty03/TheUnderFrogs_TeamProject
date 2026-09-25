package com.neueda.orderservice.services.orderServices;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.neueda.orderservice.enums.OrderStatus;
import com.neueda.orderservice.exceptions.*;
import com.neueda.orderservice.models.Account;
import com.neueda.orderservice.models.Instrument;
import com.neueda.orderservice.models.Order;
import com.neueda.orderservice.clients.AccountsClient;

import org.springframework.stereotype.Component;

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
@Component
public class BuyOrderStrategy implements OrderExecutionStrategy {
    
    private final AccountsClient accountsClient;

    public BuyOrderStrategy(AccountsClient accountsClient) {
        this.accountsClient = accountsClient;
    }

    @Override
    public OrderResult execute(Order order, Account account, Instrument instrument) {
        boolean cashDebited = false;
        try {
            BigDecimal totalCost = order.getPrice().multiply(BigDecimal.valueOf(order.getQuantity()));
            accountsClient.debit(account, totalCost);
            cashDebited = true;

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
            return new OrderResult(true, successMessage, null, order);
            
        } catch (Exception e) {
            if (cashDebited) {
                try {
                    
                    BigDecimal totalCost = order.getPrice().multiply(BigDecimal.valueOf(order.getQuantity()));
                    accountsClient.credit(account, totalCost);
                } catch (Exception rollbackError) {
                    System.out.println("Rollback failed: " + rollbackError.getMessage());
                }
            }

            order.setOrderStatus(OrderStatus.REJECTED);
            order.setLastUpdated(LocalDateTime.now());

            String failureMessage = "BUY order " + order.getOrderId() + " execution FAILED: " + e.getMessage();
            System.out.println(failureMessage);
            return new OrderResult(false, failureMessage, null, order);
        }
    }
}
