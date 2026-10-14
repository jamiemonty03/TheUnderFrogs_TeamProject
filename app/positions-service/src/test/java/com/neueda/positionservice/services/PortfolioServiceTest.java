package com.neueda.positionservice.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.neueda.positionservice.dtos.requests.CreatePortfolioRequest;
import com.neueda.positionservice.dtos.requests.UpdatePortfolioRequest;
import com.neueda.positionservice.enums.PortfolioRisk;
import com.neueda.positionservice.exceptions.PortfolioConflictException;
import com.neueda.positionservice.exceptions.PortfolioNotFoundException;
import com.neueda.positionservice.models.Portfolio;
import com.neueda.positionservice.models.Position;
import com.neueda.positionservice.models.PositionId;
import com.neueda.positionservice.repositories.PortfolioRepository;
import com.neueda.positionservice.repositories.PositionRepository;

@ExtendWith(MockitoExtension.class)
class PortfolioServiceTest {

    private static final String ACCOUNT = "ACC0011";

    @Mock
    private PortfolioRepository portfolioRepository;

    @Mock
    private PositionRepository positionRepository;

    @InjectMocks
    private PortfolioService portfolioService;

    private static Portfolio main() {
        return new Portfolio("PF0011", ACCOUNT, "Main", PortfolioRisk.MEDIUM, null);
    }

    private static Portfolio growth() {
        return new Portfolio("PF0042", ACCOUNT, "Growth", PortfolioRisk.HIGH, "Nasdaq-100");
    }

    @Test
    @DisplayName("resolve: no portfolioId means the account's existing Main portfolio, without inserting")
    void resolveWithoutIdReturnsExistingMain() {
        Portfolio main = main();
        when(portfolioRepository.findByAccountIdAndDefaultPortfolioTrue(ACCOUNT)).thenReturn(Optional.of(main));

        assertSame(main, portfolioService.resolve(ACCOUNT, null));
        assertSame(main, portfolioService.resolve(ACCOUNT, " "));
        verify(portfolioRepository, never()).insertDefaultIfAbsent(anyString());
    }

    @Test
    @DisplayName("resolve: creates Main the first time an account needs it")
    void resolveCreatesMainWhenMissing() {
        Portfolio main = main();
        when(portfolioRepository.findByAccountIdAndDefaultPortfolioTrue(ACCOUNT))
            .thenReturn(Optional.empty(), Optional.of(main));

        assertSame(main, portfolioService.resolve(ACCOUNT, null));
        verify(portfolioRepository).insertDefaultIfAbsent(ACCOUNT);
    }

    @Test
    @DisplayName("resolve: a named portfolio must belong to the account")
    void resolveNamedPortfolio() {
        Portfolio growth = growth();
        when(portfolioRepository.findByPortfolioIdAndAccountId("PF0042", ACCOUNT)).thenReturn(Optional.of(growth));
        when(portfolioRepository.findByPortfolioIdAndAccountId("PF0042", "ACC0001")).thenReturn(Optional.empty());

        assertSame(growth, portfolioService.resolve(ACCOUNT, "PF0042"));
        assertThrows(PortfolioNotFoundException.class, () -> portfolioService.resolve("ACC0001", "PF0042"));
    }

    @Test
    @DisplayName("getPortfolios: makes sure Main exists, then lists Main first")
    void getPortfoliosEnsuresMain() {
        when(portfolioRepository.findByAccountIdAndDefaultPortfolioTrue(ACCOUNT)).thenReturn(Optional.of(main()));
        when(portfolioRepository.findByAccountIdOrderByDefaultPortfolioDescNameAsc(ACCOUNT))
            .thenReturn(List.of(main(), growth()));

        List<Portfolio> portfolios = portfolioService.getPortfolios(ACCOUNT);

        assertEquals(List.of("PF0011", "PF0042"), portfolios.stream().map(Portfolio::getPortfolioId).toList());
    }

    @Test
    @DisplayName("createPortfolio: trims the name, gives it the next PF id and blanks an empty benchmark")
    void createPortfolio() {
        when(portfolioRepository.findByAccountIdAndDefaultPortfolioTrue(ACCOUNT)).thenReturn(Optional.of(main()));
        when(portfolioRepository.existsByAccountIdAndNameIgnoreCase(ACCOUNT, "Income")).thenReturn(false);
        when(portfolioRepository.nextPortfolioId()).thenReturn("PF0043");
        when(portfolioRepository.save(any(Portfolio.class))).thenAnswer(inv -> inv.getArgument(0));

        Portfolio created = portfolioService.createPortfolio(ACCOUNT,
            new CreatePortfolioRequest("  Income  ", PortfolioRisk.LOW, "  "));

        assertEquals("PF0043", created.getPortfolioId());
        assertEquals(ACCOUNT, created.getAccountId());
        assertEquals("Income", created.getName());
        assertEquals(PortfolioRisk.LOW, created.getRisk());
        assertNull(created.getBenchmark());
    }

    @Test
    @DisplayName("createPortfolio: a name already used in the account is a conflict, whatever its case")
    void createPortfolioDuplicateName() {
        when(portfolioRepository.findByAccountIdAndDefaultPortfolioTrue(ACCOUNT)).thenReturn(Optional.of(main()));
        when(portfolioRepository.existsByAccountIdAndNameIgnoreCase(ACCOUNT, "main")).thenReturn(true);

        assertThrows(PortfolioConflictException.class, () -> portfolioService.createPortfolio(ACCOUNT,
            new CreatePortfolioRequest("main", PortfolioRisk.LOW, null)));
        verify(portfolioRepository, never()).save(any());
    }

    @Test
    @DisplayName("updatePortfolio: changes only the fields sent")
    void updatePortfolioChangesOnlySentFields() {
        Portfolio growth = growth();
        when(portfolioRepository.findByPortfolioIdAndAccountId("PF0042", ACCOUNT)).thenReturn(Optional.of(growth));
        when(portfolioRepository.save(any(Portfolio.class))).thenAnswer(inv -> inv.getArgument(0));

        Portfolio updated = portfolioService.updatePortfolio(ACCOUNT, "PF0042",
            new UpdatePortfolioRequest(null, PortfolioRisk.MEDIUM, null));

        assertEquals("Growth", updated.getName());
        assertEquals(PortfolioRisk.MEDIUM, updated.getRisk());
        assertEquals("Nasdaq-100", updated.getBenchmark());
    }

    @Test
    @DisplayName("updatePortfolio: renaming to its own name in another case is not a conflict")
    void updatePortfolioRenameCaseOnly() {
        when(portfolioRepository.findByPortfolioIdAndAccountId("PF0042", ACCOUNT)).thenReturn(Optional.of(growth()));
        when(portfolioRepository.save(any(Portfolio.class))).thenAnswer(inv -> inv.getArgument(0));

        Portfolio updated = portfolioService.updatePortfolio(ACCOUNT, "PF0042",
            new UpdatePortfolioRequest("GROWTH", null, null));

        assertEquals("GROWTH", updated.getName());
        verify(portfolioRepository, never()).existsByAccountIdAndNameIgnoreCase(anyString(), anyString());
    }

    @Test
    @DisplayName("updatePortfolio: renaming to another portfolio's name is a conflict")
    void updatePortfolioRenameClash() {
        when(portfolioRepository.findByPortfolioIdAndAccountId("PF0042", ACCOUNT)).thenReturn(Optional.of(growth()));
        when(portfolioRepository.existsByAccountIdAndNameIgnoreCase(ACCOUNT, "Main")).thenReturn(true);

        assertThrows(PortfolioConflictException.class, () -> portfolioService.updatePortfolio(ACCOUNT, "PF0042",
            new UpdatePortfolioRequest("Main", null, null)));
    }

    @Test
    @DisplayName("deletePortfolio: deletes an empty portfolio")
    void deleteEmptyPortfolio() {
        Portfolio growth = growth();
        when(portfolioRepository.findByPortfolioIdAndAccountId("PF0042", ACCOUNT)).thenReturn(Optional.of(growth));
        when(portfolioRepository.findByAccountIdAndDefaultPortfolioTrue(ACCOUNT)).thenReturn(Optional.of(main()));
        when(positionRepository.findByPortfolioIdOrderBySymbol("PF0042")).thenReturn(List.of());

        portfolioService.deletePortfolio(ACCOUNT, "PF0042");

        ArgumentCaptor<Portfolio> deleted = ArgumentCaptor.forClass(Portfolio.class);
        verify(portfolioRepository).delete(deleted.capture());
        assertEquals("PF0042", deleted.getValue().getPortfolioId());
        verify(positionRepository, never()).save(any());
    }

    @Test
    @DisplayName("deletePortfolio: a holding Main doesn't have moves into Main unchanged")
    void deletePortfolioMovesHoldingToMain() {
        Position inGrowth = new Position(ACCOUNT, "PF0042", "NVDA", new BigDecimal("4"), new BigDecimal("120.0000"));
        when(portfolioRepository.findByPortfolioIdAndAccountId("PF0042", ACCOUNT)).thenReturn(Optional.of(growth()));
        when(portfolioRepository.findByAccountIdAndDefaultPortfolioTrue(ACCOUNT)).thenReturn(Optional.of(main()));
        when(positionRepository.findByPortfolioIdOrderBySymbol("PF0042")).thenReturn(List.of(inGrowth));
        when(positionRepository.findById(new PositionId("PF0011", "NVDA"))).thenReturn(Optional.empty());

        portfolioService.deletePortfolio(ACCOUNT, "PF0042");

        ArgumentCaptor<Position> saved = ArgumentCaptor.forClass(Position.class);
        verify(positionRepository).save(saved.capture());
        assertEquals("PF0011", saved.getValue().getPortfolioId());
        assertEquals(ACCOUNT, saved.getValue().getAccountId());
        assertEquals(0, new BigDecimal("4").compareTo(saved.getValue().getQuantity()));
        assertEquals(0, new BigDecimal("120").compareTo(saved.getValue().getAverageCost()));
        verify(positionRepository).delete(inGrowth);
        verify(portfolioRepository).delete(any(Portfolio.class));
    }

    @Test
    @DisplayName("deletePortfolio: a holding Main already has is combined, with a weighted average cost")
    void deletePortfolioCombinesWithMain() {
        Position inGrowth = new Position(ACCOUNT, "PF0042", "AAPL", new BigDecimal("4"), new BigDecimal("150.0000"));
        Position inMain = new Position(ACCOUNT, "PF0011", "AAPL", new BigDecimal("10"), new BigDecimal("100.0000"));
        when(portfolioRepository.findByPortfolioIdAndAccountId("PF0042", ACCOUNT)).thenReturn(Optional.of(growth()));
        when(portfolioRepository.findByAccountIdAndDefaultPortfolioTrue(ACCOUNT)).thenReturn(Optional.of(main()));
        when(positionRepository.findByPortfolioIdOrderBySymbol("PF0042")).thenReturn(List.of(inGrowth));
        when(positionRepository.findById(new PositionId("PF0011", "AAPL"))).thenReturn(Optional.of(inMain));

        portfolioService.deletePortfolio(ACCOUNT, "PF0042");

        // (10 x 100 + 4 x 150) / 14 = 114.2857
        assertEquals(0, new BigDecimal("14").compareTo(inMain.getQuantity()));
        assertEquals(new BigDecimal("114.2857"), inMain.getAverageCost());
        verify(positionRepository).save(inMain);
        verify(positionRepository).delete(inGrowth);
    }

    @Test
    @DisplayName("deletePortfolio: unknown portfolio is not found")
    void deleteUnknownPortfolio() {
        when(portfolioRepository.findByPortfolioIdAndAccountId("PF9999", ACCOUNT)).thenReturn(Optional.empty());

        assertThrows(PortfolioNotFoundException.class, () -> portfolioService.deletePortfolio(ACCOUNT, "PF9999"));
    }
}
