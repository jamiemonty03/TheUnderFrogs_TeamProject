package com.neueda.instrumentservice.services.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neueda.instrumentservice.clients.AlpacaNewsClient;
import com.neueda.instrumentservice.dtos.ai.AssistantSource;
import com.neueda.instrumentservice.dtos.ai.ToolCall;
import com.neueda.instrumentservice.dtos.ai.ToolDefinition;
import com.neueda.instrumentservice.dtos.ai.ToolResult;
import com.neueda.instrumentservice.dtos.responses.BondResponse;
import com.neueda.instrumentservice.dtos.responses.EtfResponse;
import com.neueda.instrumentservice.dtos.responses.InstrumentResponse;
import com.neueda.instrumentservice.dtos.responses.NewsResponse;
import com.neueda.instrumentservice.dtos.responses.PricePointResponse;
import com.neueda.instrumentservice.dtos.responses.PriceSnapshot;
import com.neueda.instrumentservice.dtos.responses.StockResponse;
import com.neueda.instrumentservice.exceptions.AlpacaNewsException;
import com.neueda.instrumentservice.exceptions.InstrumentNotFoundException;
import com.neueda.instrumentservice.mappers.TrackedTickerMapper;
import com.neueda.instrumentservice.services.BondService;
import com.neueda.instrumentservice.services.EtfService;
import com.neueda.instrumentservice.services.InstrumentService;
import com.neueda.instrumentservice.services.PriceHistoryService;
import com.neueda.instrumentservice.services.PriceSnapshotService;
import com.neueda.instrumentservice.services.StockService;

@ExtendWith(MockitoExtension.class)
class AssistantToolsTest {

    @Mock private InstrumentService instrumentService;
    @Mock private PriceSnapshotService priceSnapshotService;
    @Mock private PriceHistoryService priceHistoryService;
    @Mock private StockService stockService;
    @Mock private EtfService etfService;
    @Mock private BondService bondService;
    @Mock private AlpacaNewsClient newsClient;
    @Mock private TrackedTickerMapper trackedTickerMapper;

    private final ObjectMapper mapper = new ObjectMapper();
    private AssistantTools tools;

    @BeforeEach
    void setUp() {
        tools = new AssistantTools(instrumentService, priceSnapshotService, priceHistoryService, stockService,
                etfService, bondService, newsClient, trackedTickerMapper, mapper);
    }

    private static ToolCall call(String name, String arguments) {
        return new ToolCall("call_1", "function", new ToolCall.Function(name, arguments));
    }

    private JsonNode content(ToolResult result) throws Exception {
        return mapper.readTree(result.content());
    }

    private static InstrumentResponse instrument(String symbol, String name, String assetClass) {
        return new InstrumentResponse(symbol, name, assetClass, "USD", "NASDAQ", true);
    }

    @Test
    void offersTheFiveReadOnlyTools() {
        assertThat(tools.definitions()).extracting(d -> d.function().name()).containsExactly(
                "resolve_ticker", "get_price_change", "get_news", "get_trend", "get_fundamentals");
        assertThat(tools.definitions()).allSatisfy(d -> assertThat(d.type()).isEqualTo("function"));
    }

    @Test
    void resolvesATickerToOneMatch() throws Exception {
        when(instrumentService.resolveTicker("Microsoft"))
                .thenReturn(List.of(instrument("MSFT", "Microsoft Corporation", "Equity")));

        JsonNode out = content(tools.execute(call("resolve_ticker", "{\"query\":\"Microsoft\"}")));

        assertThat(out.get("status").asText()).isEqualTo("MATCH");
        assertThat(out.get("matches").get(0).get("symbol").asText()).isEqualTo("MSFT");
    }

    @Test
    void reportsAmbiguousAndUncoveredTickers() throws Exception {
        when(instrumentService.resolveTicker("Alphabet")).thenReturn(List.of(
                instrument("GOOG", "Alphabet Inc. Class C", "Equity"),
                instrument("GOOGL", "Alphabet Inc. Class A", "Equity")));
        when(instrumentService.resolveTicker("Nothing Corp")).thenReturn(List.of());

        assertThat(content(tools.execute(call("resolve_ticker", "{\"query\":\"Alphabet\"}")))
                .get("status").asText()).isEqualTo("AMBIGUOUS");
        assertThat(content(tools.execute(call("resolve_ticker", "{\"query\":\"Nothing Corp\"}")))
                .get("status").asText()).isEqualTo("NOT_COVERED");
    }

    @Test
    void returnsThePriceChangeForACleanedSymbolWithItsSource() throws Exception {
        Instant asOf = Instant.parse("2026-10-19T14:32:00Z");
        when(priceSnapshotService.getSnapshot("MSFT")).thenReturn(Optional.of(new PriceSnapshot("MSFT",
                new BigDecimal("412.37"), new BigDecimal("421.05"), new BigDecimal("-2.06"),
                PriceSnapshot.Source.LIVE, asOf)));

        ToolResult result = tools.execute(call("get_price_change", "{\"symbol\":\"MSF T\"}"));
        JsonNode out = content(result);

        assertThat(result.failed()).isFalse();
        assertThat(out.get("price").decimalValue()).isEqualByComparingTo("412.37");
        assertThat(out.get("changePercent").decimalValue()).isEqualByComparingTo("-2.06");
        assertThat(out.get("priceSource").asText()).isEqualTo("LIVE");
        assertThat(out.get("asOf").asText()).isEqualTo("2026-10-19T14:32:00Z");
        assertThat(result.sources()).containsExactly(new AssistantSource(AssistantSource.Type.PRICE, "MSFT",
                AssistantTools.ALPACA_LIVE, null, asOf));
    }

    @Test
    void saysWhenASymbolIsNotCoveredOrHasNoPrices() throws Exception {
        when(priceSnapshotService.getSnapshot("NOPE")).thenThrow(new InstrumentNotFoundException("NOPE"));
        when(priceSnapshotService.getSnapshot("NEW")).thenReturn(Optional.empty());

        ToolResult notCovered = tools.execute(call("get_price_change", "{\"symbol\":\"nope\"}"));
        ToolResult noPrices = tools.execute(call("get_price_change", "{\"symbol\":\"NEW\"}"));

        assertThat(notCovered.failed()).isTrue();
        assertThat(content(notCovered).get("error").asText()).isEqualTo("NOT_COVERED");
        assertThat(content(noPrices).get("error").asText()).isEqualTo("NO_PRICE_DATA");
    }

    @Test
    void returnsNewsWithSummariesAndLinksAsSources() throws Exception {
        Instant published = Instant.parse("2026-10-19T13:05:00Z");
        when(trackedTickerMapper.findActiveSymbols()).thenReturn(List.of("MSFT", "AAPL"));
        when(newsClient.getNews(5, List.of("MSFT"))).thenReturn(List.of(new NewsResponse(
                "Azure growth slows", "https://news.example.com/azure", published, null, List.of("MSFT"),
                "Cloud growth came in below estimates.", "benzinga")));

        ToolResult result = tools.execute(call("get_news", "{\"symbol\":\"MSFT\"}"));
        JsonNode article = content(result).get("articles").get(0);

        assertThat(article.get("headline").asText()).isEqualTo("Azure growth slows");
        assertThat(article.get("summary").asText()).isEqualTo("Cloud growth came in below estimates.");
        assertThat(article.get("publisher").asText()).isEqualTo("benzinga");
        assertThat(article.get("publishedAt").asText()).isEqualTo("2026-10-19T13:05:00Z");
        assertThat(result.sources()).extracting(AssistantSource::url).containsExactly("https://news.example.com/azure");
    }

    @Test
    void capsTheNumberOfNewsArticles() {
        when(trackedTickerMapper.findActiveSymbols()).thenReturn(List.of("MSFT"));
        when(newsClient.getNews(anyInt(), any())).thenReturn(List.of());

        tools.execute(call("get_news", "{\"symbol\":\"MSFT\",\"limit\":50}"));

        verify(newsClient).getNews(AssistantTools.MAX_NEWS_LIMIT, List.of("MSFT"));
    }

    @Test
    void doesNotFetchNewsForAnUntrackedSymbol() throws Exception {
        when(trackedTickerMapper.findActiveSymbols()).thenReturn(List.of("AAPL"));

        ToolResult result = tools.execute(call("get_news", "{\"symbol\":\"MSFT\"}"));

        assertThat(content(result).get("error").asText()).isEqualTo("NOT_TRACKED");
        verifyNoInteractions(newsClient);
    }

    @Test
    void reportsWhenTheNewsProviderIsDown() throws Exception {
        when(trackedTickerMapper.findActiveSymbols()).thenReturn(List.of("MSFT"));
        when(newsClient.getNews(5, List.of("MSFT"))).thenThrow(
                new AlpacaNewsException(HttpStatus.TOO_MANY_REQUESTS, "NEWS-429", "rate limited"));

        assertThat(content(tools.execute(call("get_news", "{\"symbol\":\"MSFT\"}")))
                .get("error").asText()).isEqualTo("NEWS_UNAVAILABLE");
    }

    @Test
    void calculatesTheTrendFromDailyCloses() throws Exception {
        List<PricePointResponse> days = new ArrayList<>();
        LocalDate start = LocalDate.parse("2026-09-21");
        for (int i = 0; i < 20; i++) {
            days.add(new PricePointResponse(start.plusDays(i), null, null, null,
                    new BigDecimal(400 + i), 1_000_000L, null, null));
        }
        days.add(new PricePointResponse(LocalDate.parse("2026-10-16"), null, null, null,
                new BigDecimal("412.37"), 2_500_000L, new BigDecimal("410.00"), new BigDecimal("420.00")));
        when(priceHistoryService.getHistory("MSFT", AssistantTools.TREND_DAYS)).thenReturn(days);

        ToolResult result = tools.execute(call("get_trend", "{\"symbol\":\"MSFT\"}"));
        JsonNode out = content(result);

        assertThat(out.get("asOfDate").asText()).isEqualTo("2026-10-16");
        assertThat(out.get("change5DayPercent").decimalValue()).isEqualByComparingTo("-0.63");
        assertThat(out.get("change20DayPercent").decimalValue()).isEqualByComparingTo("3.09");
        assertThat(out.get("closeVsMovingAvg20Percent").decimalValue()).isEqualByComparingTo("0.58");
        assertThat(out.get("closeVsMovingAvg50Percent").decimalValue()).isEqualByComparingTo("-1.82");
        assertThat(out.get("latestVolume").asText()).isEqualTo("2.50 million");
        assertThat(out.get("averageVolume20Day").asText()).isEqualTo("1.00 million");
        assertThat(out.get("volumeVsAverage").decimalValue()).isEqualByComparingTo("2.50");
        assertThat(result.sources().get(0).asOf()).isEqualTo(Instant.parse("2026-10-16T20:00:00Z"));
    }

    @Test
    void leavesTrendFiguresEmptyWhenThereIsTooLittleHistory() throws Exception {
        when(priceHistoryService.getHistory("NEW", AssistantTools.TREND_DAYS)).thenReturn(List.of(
                new PricePointResponse(LocalDate.parse("2026-10-16"), null, null, null,
                        new BigDecimal("10.00"), null, null, null)));

        JsonNode out = content(tools.execute(call("get_trend", "{\"symbol\":\"NEW\"}")));

        assertThat(out.get("change5DayPercent").isNull()).isTrue();
        assertThat(out.get("volumeVsAverage").isNull()).isTrue();
    }

    @Test
    void returnsStockFundamentalsWithReadableAmountsAndPercentages() throws Exception {
        when(instrumentService.getInstrumentBySymbol("MSFT"))
                .thenReturn(instrument("MSFT", "Microsoft Corporation", "Equity"));
        when(stockService.getStockBySymbol("MSFT")).thenReturn(new StockResponse("MSFT", "Microsoft Corporation",
                null, null, null, null, LocalDateTime.parse("2026-10-12T06:00:00"), "Technology", "Software",
                "United States", null, null, new BigDecimal("34.5"), null, new BigDecimal("11.95"), null,
                new BigDecimal("0.2456"), null, new BigDecimal("0.35"), new BigDecimal("3065000000000"), null,
                new BigDecimal("245100000000"), null));

        ToolResult result = tools.execute(call("get_fundamentals", "{\"symbol\":\"MSFT\"}"));
        JsonNode out = content(result);

        assertThat(out.get("type").asText()).isEqualTo("Stock");
        assertThat(out.get("trailingPe").decimalValue()).isEqualByComparingTo("34.5");
        assertThat(out.get("forwardPe").isNull()).isTrue();
        assertThat(out.get("marketCap").asText()).isEqualTo("3.07 trillion");
        assertThat(out.get("totalRevenue").asText()).isEqualTo("245.10 billion");
        assertThat(out.get("payoutRatioPercent").decimalValue()).isEqualByComparingTo("24.56");
        assertThat(out.get("returnOnEquityPercent").decimalValue()).isEqualByComparingTo("35.00");
        assertThat(out.get("dataLoadedOn").asText()).isEqualTo("2026-10-12");
        assertThat(result.sources().get(0).asOf()).isEqualTo(Instant.parse("2026-10-12T06:00:00Z"));
    }

    @Test
    void returnsEtfAndBondFundamentalsFromTheRightService() throws Exception {
        when(instrumentService.getInstrumentBySymbol("SPY")).thenReturn(instrument("SPY", "SPDR S&P 500", "ETF"));
        when(etfService.getEtfBySymbol("SPY")).thenReturn(new EtfResponse("SPY", "SPDR S&P 500", null, null, null,
                null, null, "Large Blend", "SPDR", null, new BigDecimal("0.09"), null,
                new BigDecimal("560000000000"), null, null, null, null, null, null));
        when(instrumentService.getInstrumentBySymbol("BND")).thenReturn(instrument("BND", "Vanguard Bond", "Bond"));
        when(bondService.getBondBySymbol("BND")).thenReturn(new BondResponse("BND", "Vanguard Bond", null, null,
                null, null, null, "Intermediate Core Bond", "Vanguard", null, new BigDecimal("0.03"), null, null,
                null, new BigDecimal("4.6"), null, new BigDecimal("5.9"), null, null, null, null, null));

        JsonNode etf = content(tools.execute(call("get_fundamentals", "{\"symbol\":\"SPY\"}")));
        JsonNode bond = content(tools.execute(call("get_fundamentals", "{\"symbol\":\"BND\"}")));

        assertThat(etf.get("type").asText()).isEqualTo("ETF");
        assertThat(etf.get("netExpenseRatio").decimalValue()).isEqualByComparingTo("0.09");
        assertThat(etf.get("totalAssets").asText()).isEqualTo("560.00 billion");
        assertThat(bond.get("type").asText()).isEqualTo("Bond fund");
        assertThat(bond.get("yieldToMaturity").decimalValue()).isEqualByComparingTo("4.6");
        assertThat(bond.get("duration").decimalValue()).isEqualByComparingTo("5.9");
        verifyNoInteractions(stockService);
    }

    @Test
    void neverThrowsForBadInput() throws Exception {
        when(priceSnapshotService.getSnapshot("MSFT")).thenThrow(new IllegalStateException("database down"));

        assertThat(content(tools.execute(call("get_price_change", "not json"))).get("error").asText())
                .isEqualTo("BAD_ARGUMENTS");
        assertThat(content(tools.execute(call("place_order", "{}"))).get("error").asText())
                .isEqualTo("UNKNOWN_TOOL");
        assertThat(content(tools.execute(call("get_price_change", "{\"symbol\":\"MSFT\"}"))).get("error").asText())
                .isEqualTo("TOOL_FAILED");
    }

    @Test
    void formatsLargeAmountsForReading() {
        assertThat(AssistantTools.readable(new BigDecimal("3065000000000"))).isEqualTo("3.07 trillion");
        assertThat(AssistantTools.readable(new BigDecimal("1500000"))).isEqualTo("1.50 million");
        assertThat(AssistantTools.readable(new BigDecimal("999.6"))).isEqualTo("1000");
        assertThat(AssistantTools.readable(null)).isNull();
    }
}
