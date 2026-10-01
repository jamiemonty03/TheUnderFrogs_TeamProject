package com.neueda.orderservice.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.neueda.orderservice.models.ClientTrade;

/**
 * Repository for ClientTrade persistence.
 *
 * Handles all database operations for client trade historical records.
 * Supports queries by account, account+symbol, and individual trade lookup.
 */
@Repository
public interface ClientTradeRepository extends JpaRepository<ClientTrade, Integer> {
    List<ClientTrade> findByAccountIdOrderByTradeDateDescTradeIdDesc(String accountId);
    List<ClientTrade> findByAccountIdAndSymbolOrderByTradeDateDescTradeIdDesc(String accountId, String symbol);
}
