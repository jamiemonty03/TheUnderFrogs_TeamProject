package com.neueda.leap.services.orderServices;

import com.neueda.leap.models.Account;
import com.neueda.leap.models.Instrument;
import com.neueda.leap.models.Order;

/**
 * Strategy interface for order execution (BUY vs SELL).
 * 
 * Implementations handle the specific logic for different order types.
 * OrderProcessor uses this to cleanly separate BUY and SELL logic without
 * scattered if-else dispatch code.
 
 * - Cleanly separates BUY and SELL logic into separate files
 * - Easy to test each strategy independently
 * - Extensible for new order types (LIMIT, STOP orders in future)
 * - OrderProcessor focuses only on orchestration, not business logic
 */
public interface OrderExecutionStrategy {

    OrderResult execute(Order order, Account account, Instrument instrument);
}
