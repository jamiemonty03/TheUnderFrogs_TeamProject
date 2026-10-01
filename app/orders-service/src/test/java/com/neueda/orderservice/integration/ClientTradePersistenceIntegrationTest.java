package com.neueda.orderservice.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import com.neueda.orderservice.enums.OrderSide;
import com.neueda.orderservice.models.ClientTrade;
import com.neueda.orderservice.models.PositionStep;
import com.neueda.orderservice.repositories.ClientTradeRepository;
import com.neueda.orderservice.services.ClientTradeService;

// Runs against the real orders-db schema and client_trades seed, so a mapping that drifts from the table fails here.
@Testcontainers
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(ClientTradeService.class)
@Sql("file:db/seed/client-trades.sql")
class ClientTradePersistenceIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("orders_client_trades")
            .withUsername("test")
            .withPassword("test")
            .withCopyFileToContainer(MountableFile.forHostPath("db/schema"), "/docker-entrypoint-initdb.d/");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
    }

    @Autowired
    private ClientTradeRepository repository;

    @Autowired
    private ClientTradeService service;

    @Test
    void seededHistoryExplainsAliceHolding10Aapl() {
        List<PositionStep> steps = service.getPositionHistory("ACC0001", "AAPL");

        assertThat(steps).extracting(step -> step.trade().getTradeType())
                .containsExactly(OrderSide.BUY, OrderSide.SELL);
        assertThat(steps).extracting(PositionStep::quantityAfter)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("15"), new BigDecimal("10"));
        assertThat(service.reconcileWithPosition("ACC0001", "AAPL", new BigDecimal("10"))).isTrue();
        assertThat(service.getWeightedAverageCost("ACC0001", "AAPL")).isEqualByComparingTo("188.00");
    }

    @Test
    void savedTradeGetsGeneratedIdAndRoundTrips() {
        ClientTrade saved = repository.saveAndFlush(new ClientTrade("ACC0099", "AAPL", OrderSide.BUY,
                new BigDecimal("2.5000"), new BigDecimal("190.10"), LocalDate.of(2026, 9, 30)));

        assertThat(saved.getTradeId()).isNotNull();
        ClientTrade loaded = repository.findById(saved.getTradeId()).orElseThrow();
        assertThat(loaded.getTradeType()).isEqualTo(OrderSide.BUY);
        assertThat(loaded.getQuantity()).isEqualByComparingTo("2.5");
        assertThat(loaded.getTradeDate()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(loaded.getUpdatedBy()).isEqualTo("SYSTEM");
    }

    @Test
    void historyIsNewestFirstWithSameDayTradesLatestSavedFirst() {
        LocalDate earlier = LocalDate.of(2026, 9, 1);
        LocalDate later = LocalDate.of(2026, 9, 30);
        ClientTrade first = repository.saveAndFlush(new ClientTrade("ACC0099", "NVDA", OrderSide.BUY, new BigDecimal("10"), new BigDecimal("900"), earlier));
        ClientTrade secondSameDay = repository.saveAndFlush(new ClientTrade("ACC0099", "NVDA", OrderSide.SELL, new BigDecimal("2"), new BigDecimal("910"), later));
        ClientTrade thirdSameDay = repository.saveAndFlush(new ClientTrade("ACC0099", "NVDA", OrderSide.SELL, new BigDecimal("3"), new BigDecimal("920"), later));

        assertThat(service.getSymbolHistory("ACC0099", "NVDA")).extracting(ClientTrade::getTradeId)
                .containsExactly(thirdSameDay.getTradeId(), secondSameDay.getTradeId(), first.getTradeId());
    }

    @Test
    void sameDayTradesReplayInInsertOrder() {
        LocalDate day = LocalDate.of(2026, 9, 30);
        repository.saveAndFlush(new ClientTrade("ACC0099", "MSFT", OrderSide.BUY, new BigDecimal("15"), new BigDecimal("400"), day));
        repository.saveAndFlush(new ClientTrade("ACC0099", "MSFT", OrderSide.SELL, new BigDecimal("5"), new BigDecimal("401"), day));
        repository.saveAndFlush(new ClientTrade("ACC0099", "MSFT", OrderSide.SELL, new BigDecimal("5"), new BigDecimal("402"), day));

        assertThat(service.getPositionHistory("ACC0099", "MSFT")).extracting(PositionStep::quantityAfter)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("15"), new BigDecimal("10"), new BigDecimal("5"));
    }
}
