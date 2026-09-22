package com.neueda.orderservice.repositories;

import java.util.Optional;
import java.util.List;
import com.neueda.orderservice.models.ClientTrade;

/**
 * Repository interface for ClientTrade persistence.
 * 
 * Handles all database operations for client trade historical records.
 * Supports queries by account, account+symbol, and individual trade lookup.
 */
public interface ClientTradeRepository {
    ClientTrade save(ClientTrade trade);
    Optional<ClientTrade> findById(String tradeId);
    List<ClientTrade> findByAccountId(String accountId);
    List<ClientTrade> findByAccountAndSymbol(String accountId, String symbol);
    boolean delete(String tradeId);
    boolean exists(String tradeId);
}
