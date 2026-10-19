package com.neueda.instrumentservice.services.assistant;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
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

@Component
public class AssistantTools {

    public static final String RESOLVE_TICKER = "resolve_ticker";
    public static final String GET_PRICE_CHANGE = "get_price_change";
    public static final String GET_NEWS = "get_news";
    public static final String GET_TREND = "get_trend";
    public static final String GET_FUNDAMENTALS = "get_fundamentals";

    static final int DEFAULT_NEWS_LIMIT = 5;
    static final int MAX_NEWS_LIMIT = 10;
    static final int TREND_DAYS = 21;
    static final String YAHOO = "Yahoo Finance";
    static final String ALPACA_LIVE = "Alpaca live quote";

    private static final Logger log = LoggerFactory.getLogger(AssistantTools.class);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final Map<String, Object> SYMBOL_ONLY = schema(
            Map.of("symbol", Map.of("type", "string", "description", "Ticker symbol, e.g. MSFT")), "symbol");

    private final InstrumentService instrumentService;
    private final PriceSnapshotService priceSnapshotService;
    private final PriceHistoryService priceHistoryService;
    private final StockService stockService;
    private final EtfService etfService;
    private final BondService bondService;
    private final AlpacaNewsClient newsClient;
    private final TrackedTickerMapper trackedTickerMapper;
    private final ObjectMapper json;

    public AssistantTools(InstrumentService instrumentService, PriceSnapshotService priceSnapshotService,
            PriceHistoryService priceHistoryService, StockService stockService, EtfService etfService,
            BondService bondService, AlpacaNewsClient newsClient, TrackedTickerMapper trackedTickerMapper,
            ObjectMapper mapper) {
        this.instrumentService = instrumentService;
        this.priceSnapshotService = priceSnapshotService;
        this.priceHistoryService = priceHistoryService;
        this.stockService = stockService;
        this.etfService = etfService;
        this.bondService = bondService;
        this.newsClient = newsClient;
        this.trackedTickerMapper = trackedTickerMapper;
        this.json = mapper.copy().enable(JsonGenerator.Feature.WRITE_BIGDECIMAL_AS_PLAIN);
    }

    public List<ToolDefinition> definitions() {
        return List.of(
                ToolDefinition.function(RESOLVE_TICKER,
                        "Find which instruments we cover for a company name or ticker the user mentioned. "
                                + "Call this before any other tool when you are not certain of the exact symbol.",
                        schema(Map.of("query", Map.of("type", "string",
                                "description", "Company name or ticker, e.g. 'Microsoft' or 'MSFT'")), "query")),
                ToolDefinition.function(GET_PRICE_CHANGE,
                        "Latest price, previous close and percentage change for one symbol, "
                                + "with whether the price is a live quote or a daily close and when it is from.",
                        SYMBOL_ONLY),
                ToolDefinition.function(GET_NEWS,
                        "Most recent news articles for one symbol: headline, summary, publisher, link and "
                                + "publish time. Only use what is returned; never add news from memory.",
                        schema(Map.of(
                                "symbol", Map.of("type", "string", "description", "Ticker symbol, e.g. MSFT"),
                                "limit", Map.of("type", "integer", "minimum", 1, "maximum", MAX_NEWS_LIMIT,
                                        "description", "How many articles, default " + DEFAULT_NEWS_LIMIT)),
                                "symbol")),
                ToolDefinition.function(GET_TREND,
                        "Recent trend for one symbol from daily closes: 5 and 20 day change, distance from the "
                                + "20 and 50 day moving averages, and the latest volume compared with its 20 day average.",
                        SYMBOL_ONLY),
                ToolDefinition.function(GET_FUNDAMENTALS,
                        "Company or fund fundamentals for one symbol, such as P/E, EPS, market cap, sector, expense "
                                + "ratio or yield, with the date the data was loaded. Missing values are null.",
                        SYMBOL_ONLY));
    }

    public ToolResult execute(ToolCall call) {
        String name = call.function() == null ? null : call.function().name();
        JsonNode args;
        try {
            args = json.readTree(call.function() == null || call.function().arguments() == null
                    ? "{}" : call.function().arguments());
        } catch (JsonProcessingException ex) {
            return failure(name, "BAD_ARGUMENTS", "The tool arguments were not valid JSON");
        }
        try {
            return switch (String.valueOf(name)) {
                case RESOLVE_TICKER -> resolveTicker(args.path("query").asText(""));
                case GET_PRICE_CHANGE -> priceChange(symbolArg(args));
                case GET_NEWS -> news(symbolArg(args), args.path("limit").asInt(DEFAULT_NEWS_LIMIT));
                case GET_TREND -> trend(symbolArg(args));
                case GET_FUNDAMENTALS -> fundamentals(symbolArg(args));
                default -> failure(name, "UNKNOWN_TOOL", "There is no tool called " + name);
            };
        } catch (InstrumentNotFoundException ex) {
            return failure(name, "NOT_COVERED", "We do not cover the symbol " + symbolArg(args));
        } catch (RuntimeException ex) {
            log.warn("Assistant tool {} failed: {}", name, ex.getClass().getSimpleName());
            return failure(name, "TOOL_FAILED", "The data could not be loaded right now");
        }
    }

    private ToolResult resolveTicker(String query) {
        List<InstrumentResponse> matches = instrumentService.resolveTicker(query);
        String status = matches.isEmpty() ? "NOT_COVERED" : matches.size() == 1 ? "MATCH" : "AMBIGUOUS";
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("query", query);
        out.put("status", status);
        out.put("matches", matches.stream()
                .map(m -> Map.of("symbol", m.symbol(), "name", m.name(), "assetClass", m.assetClass()))
                .toList());
        return success(RESOLVE_TICKER, out, List.of());
    }

    private ToolResult priceChange(String symbol) throws InstrumentNotFoundException {
        PriceSnapshot snapshot = priceSnapshotService.getSnapshot(symbol).orElse(null);
        if (snapshot == null) {
            return failure(GET_PRICE_CHANGE, "NO_PRICE_DATA", "No prices are loaded for " + symbol);
        }
        boolean live = snapshot.priceSource() == PriceSnapshot.Source.LIVE;
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("symbol", symbol);
        out.put("price", snapshot.price());
        out.put("previousClose", snapshot.previousClose());
        out.put("changePercent", snapshot.changePercent());
        out.put("priceSource", live ? "LIVE" : "DAILY_CLOSE");
        out.put("asOf", snapshot.asOf().toString());
        return success(GET_PRICE_CHANGE, out, List.of(new AssistantSource(AssistantSource.Type.PRICE, symbol,
                live ? ALPACA_LIVE : YAHOO + " daily close", null, snapshot.asOf())));
    }

    private ToolResult news(String symbol, int requestedLimit) {
        if (!trackedTickerMapper.findActiveSymbols().contains(symbol)) {
            return failure(GET_NEWS, "NOT_TRACKED", "We do not collect news for " + symbol);
        }
        List<NewsResponse> articles;
        try {
            articles = newsClient.getNews(Math.max(1, Math.min(requestedLimit, MAX_NEWS_LIMIT)), List.of(symbol));
        } catch (AlpacaNewsException ex) {
            return failure(GET_NEWS, "NEWS_UNAVAILABLE", "News is unavailable right now");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("symbol", symbol);
        out.put("articles", articles.stream().map(article -> {
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("headline", article.headline());
            a.put("summary", article.summary());
            a.put("publisher", article.source());
            a.put("url", article.articleUrl());
            a.put("publishedAt", Objects.toString(article.publishedAt(), null));
            return a;
        }).toList());
        List<AssistantSource> sources = articles.stream()
                .map(article -> new AssistantSource(AssistantSource.Type.NEWS, symbol, article.headline(),
                        article.articleUrl(), article.publishedAt()))
                .toList();
        return success(GET_NEWS, out, sources);
    }

    private ToolResult trend(String symbol) throws InstrumentNotFoundException {
        List<PricePointResponse> days = priceHistoryService.getHistory(symbol, TREND_DAYS).stream()
                .filter(day -> day.close() != null)
                .toList();
        if (days.isEmpty()) {
            return failure(GET_TREND, "NO_PRICE_DATA", "No price history is loaded for " + symbol);
        }
        PricePointResponse latest = days.get(days.size() - 1);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("symbol", symbol);
        out.put("asOfDate", latest.date().toString());
        out.put("latestClose", latest.close());
        out.put("change5DayPercent", percentChange(latest.close(), closeDaysBefore(days, 5)));
        out.put("change20DayPercent", percentChange(latest.close(), closeDaysBefore(days, 20)));
        out.put("movingAvg20", latest.movingAvg20());
        out.put("movingAvg50", latest.movingAvg50());
        out.put("closeVsMovingAvg20Percent", percentChange(latest.close(), latest.movingAvg20()));
        out.put("closeVsMovingAvg50Percent", percentChange(latest.close(), latest.movingAvg50()));
        BigDecimal averageVolume = averageVolumeBefore(days);
        out.put("latestVolume", latest.volume());
        out.put("averageVolume20Day", averageVolume);
        out.put("volumeVsAverage", latest.volume() == null || averageVolume == null || averageVolume.signum() == 0
                ? null
                : BigDecimal.valueOf(latest.volume()).divide(averageVolume, 2, RoundingMode.HALF_UP));
        return success(GET_TREND, out, List.of(new AssistantSource(AssistantSource.Type.TREND, symbol,
                YAHOO + " daily prices", null, PriceSnapshotService.closeOf(latest.date()))));
    }

    private ToolResult fundamentals(String symbol) throws InstrumentNotFoundException {
        String assetClass = instrumentService.getInstrumentBySymbol(symbol).assetClass();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("symbol", symbol);
        LocalDateTime loaded;
        if ("ETF".equalsIgnoreCase(assetClass)) {
            EtfResponse etf = etfService.getEtfBySymbol(symbol);
            putFund(out, etf.name(), "ETF", etf.category(), etf.fundFamily(), etf.netExpenseRatio(),
                    etf.totalAssets(), etf.ytdReturn(), etf.threeYearAvgReturn(), etf.fiveYearAvgReturn(),
                    etf.distributionYield(), etf.beta3Year());
            loaded = etf.lastUpdated();
        } else if ("Bond".equalsIgnoreCase(assetClass)) {
            BondResponse bond = bondService.getBondBySymbol(symbol);
            putFund(out, bond.name(), "Bond fund", bond.category(), bond.fundFamily(), bond.netExpenseRatio(),
                    bond.totalAssets(), bond.ytdReturn(), bond.threeYearAvgReturn(), bond.fiveYearAvgReturn(),
                    bond.distributionYield(), bond.beta());
            out.put("yieldToMaturity", bond.yieldToMaturity());
            out.put("duration", bond.duration());
            loaded = bond.lastUpdated();
        } else {
            StockResponse stock = stockService.getStockBySymbol(symbol);
            out.put("name", stock.name());
            out.put("type", "Stock");
            out.put("sector", stock.sector());
            out.put("industry", stock.industry());
            out.put("marketCap", readable(stock.marketCap()));
            out.put("trailingPe", stock.trailingPe());
            out.put("forwardPe", stock.forwardPe());
            out.put("trailingEps", stock.trailingEps());
            out.put("dividendRate", stock.dividendRate());
            out.put("payoutRatioPercent", asPercent(stock.payoutRatio()));
            out.put("priceToBook", stock.priceToBook());
            out.put("returnOnEquityPercent", asPercent(stock.returnOnEquity()));
            out.put("totalRevenue", readable(stock.totalRevenue()));
            loaded = stock.lastUpdated();
        }
        out.put("dataLoadedOn", loaded == null ? null : loaded.toLocalDate().toString());
        return success(GET_FUNDAMENTALS, out, List.of(new AssistantSource(AssistantSource.Type.FUNDAMENTALS,
                symbol, YAHOO + " fundamentals", null, loaded == null ? null : loaded.toInstant(ZoneOffset.UTC))));
    }

    private static void putFund(Map<String, Object> out, String name, String type, String category,
            String fundFamily, BigDecimal expenseRatio, BigDecimal totalAssets, BigDecimal ytdReturn,
            BigDecimal threeYearReturn, BigDecimal fiveYearReturn, BigDecimal distributionYield, BigDecimal beta) {
        out.put("name", name);
        out.put("type", type);
        out.put("category", category);
        out.put("fundFamily", fundFamily);
        out.put("netExpenseRatio", expenseRatio);
        out.put("totalAssets", readable(totalAssets));
        out.put("ytdReturn", ytdReturn);
        out.put("threeYearAvgReturn", threeYearReturn);
        out.put("fiveYearAvgReturn", fiveYearReturn);
        out.put("distributionYield", distributionYield);
        out.put("beta3Year", beta);
    }

    private static String symbolArg(JsonNode args) {
        return InstrumentService.cleanSymbol(args.path("symbol").asText(""));
    }

    private static BigDecimal closeDaysBefore(List<PricePointResponse> days, int tradingDays) {
        int index = days.size() - 1 - tradingDays;
        return index >= 0 ? days.get(index).close() : null;
    }

    private static BigDecimal averageVolumeBefore(List<PricePointResponse> days) {
        List<Long> volumes = days.subList(0, days.size() - 1).stream()
                .map(PricePointResponse::volume)
                .filter(Objects::nonNull)
                .toList();
        if (volumes.isEmpty()) {
            return null;
        }
        long total = volumes.stream().mapToLong(Long::longValue).sum();
        return BigDecimal.valueOf(total).divide(BigDecimal.valueOf(volumes.size()), 0, RoundingMode.HALF_UP);
    }

    static BigDecimal percentChange(BigDecimal value, BigDecimal base) {
        if (value == null || base == null || base.signum() == 0) {
            return null;
        }
        return value.subtract(base).multiply(HUNDRED).divide(base, 2, RoundingMode.HALF_UP);
    }

    static BigDecimal asPercent(BigDecimal fraction) {
        return fraction == null ? null : fraction.multiply(HUNDRED).setScale(2, RoundingMode.HALF_UP);
    }

    static String readable(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        BigDecimal abs = amount.abs();
        if (abs.compareTo(new BigDecimal("1e12")) >= 0) {
            return scaled(amount, "1e12", "trillion");
        }
        if (abs.compareTo(new BigDecimal("1e9")) >= 0) {
            return scaled(amount, "1e9", "billion");
        }
        if (abs.compareTo(new BigDecimal("1e6")) >= 0) {
            return scaled(amount, "1e6", "million");
        }
        return amount.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private static String scaled(BigDecimal amount, String unit, String word) {
        return amount.divide(new BigDecimal(unit), 2, RoundingMode.HALF_UP).toPlainString() + " " + word;
    }

    private static Map<String, Object> schema(Map<String, Object> properties, String... required) {
        return Map.of("type", "object", "properties", properties, "required", List.of(required));
    }

    private ToolResult success(String name, Map<String, Object> out, List<AssistantSource> sources) {
        return new ToolResult(name, write(out), sources, false);
    }

    private ToolResult failure(String name, String code, String message) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("error", code);
        out.put("message", message);
        return new ToolResult(name, write(out), List.of(), true);
    }

    private String write(Map<String, Object> out) {
        try {
            return json.writeValueAsString(out);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Could not write tool result", ex);
        }
    }
}
