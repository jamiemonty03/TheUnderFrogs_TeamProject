package com.neueda.orderservice.services.orderServices;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.neueda.orderservice.enums.OrderStatus;
import com.neueda.orderservice.exceptions.*;
import com.neueda.orderservice.models.Account;
import com.neueda.orderservice.models.Instrument;
import com.neueda.orderservice.models.Order;
import com.neueda.orderservice.services.AccountService;

import org.springframework.stereotype.Component;

@Component
public class SellOrderStrategy implements OrderExecutionStrategy {
    
    private final AccountService accountService;

    public SellOrderStrategy(AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    public OrderResult execute(Order order, Account account, Instrument instrument) {
        boolean cashCredited = false;
        try {
            
            BigDecimal totalProceeds = order.getPrice().multiply(BigDecimal.valueOf(order.getQuantity()));
            accountService.credit(account, totalProceeds);
            cashCredited = true;

            order.setOrderStatus(OrderStatus.FILLED);
            order.setLastUpdated(LocalDateTime.now());

            String successMessage = String.format(
                "SELL order %s FILLED: %d shares of %s @ $%.2f = $%.2f",
                order.getOrderId(),
                order.getQuantity(),
                order.getSymbol(),
                order.getPrice(),
                totalProceeds
            );
            return new OrderResult(true, successMessage, null, order);
            
        } catch (Exception e) {
            if (cashCredited) {
                try {
                    BigDecimal totalProceeds = order.getPrice().multiply(BigDecimal.valueOf(order.getQuantity()));
                    accountService.debit(account, totalProceeds);
                } catch (Exception rollbackError) {
                    System.out.println("Rollback failed: " + rollbackError.getMessage());
                }
            }

            order.setOrderStatus(OrderStatus.REJECTED);
            order.setLastUpdated(LocalDateTime.now());

            String failureMessage = "SELL order " + order.getOrderId() + " execution FAILED: " + e.getMessage();
            System.out.println(failureMessage);
            return new OrderResult(false, failureMessage, null, order);
        }
    }
}
