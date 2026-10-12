package com.neueda.orderservice.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;
import com.neueda.orderservice.models.ClientTrade;

/**
 * Repository for ClientTrade persistence.
 *
 * Handles all database operations for client trade historical records.
 * Supports queries by account, account+symbol, individual trade lookup,
 * and filtered searches through JpaSpecificationExecutor.
 */
@Repository
public interface ClientTradeRepository extends JpaRepository<ClientTrade, Integer>, JpaSpecificationExecutor<ClientTrade> {
    List<ClientTrade> findByAccountIdOrderByTradeDateDescTradeIdDesc(String accountId);
    List<ClientTrade> findByAccountIdAndSymbolOrderByTradeDateDescTradeIdDesc(String accountId, String symbol);
}
