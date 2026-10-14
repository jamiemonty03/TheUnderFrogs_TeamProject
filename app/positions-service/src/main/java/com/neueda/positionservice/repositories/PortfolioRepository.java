package com.neueda.positionservice.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.neueda.positionservice.models.Portfolio;

public interface PortfolioRepository extends JpaRepository<Portfolio, String> {

    List<Portfolio> findByAccountIdOrderByDefaultPortfolioDescNameAsc(String accountId);

    Optional<Portfolio> findByPortfolioIdAndAccountId(String portfolioId, String accountId);

    Optional<Portfolio> findByAccountIdAndDefaultPortfolioTrue(String accountId);

    boolean existsByAccountIdAndNameIgnoreCase(String accountId, String name);

    @Query(value = "SELECT 'PF' || LPAD(CAST(nextval('portfolio_id_seq') AS TEXT), 4, '0')", nativeQuery = true)
    String nextPortfolioId();

    @Modifying
    @Query(value = """
            INSERT INTO portfolios (account_id, name, risk, is_default)
            VALUES (:accountId, 'Main', 'MEDIUM', TRUE)
            ON CONFLICT (account_id) WHERE is_default DO NOTHING
            """, nativeQuery = true)
    int insertDefaultIfAbsent(@Param("accountId") String accountId);
}
