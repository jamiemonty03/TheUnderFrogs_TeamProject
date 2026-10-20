package com.neueda.instrumentservice.services.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.neueda.instrumentservice.dtos.ai.AssistantSource;
import com.neueda.instrumentservice.dtos.ai.ToolResult;

class ReplyVerifierTest {

    private static final String NOW = "Monday 19 October 2026, 14:32 UTC";
    private static final ToolResult PRICE = new ToolResult("get_price_change", """
            {"symbol":"MSFT","price":412.37,"previousClose":421.05,"changePercent":-2.06,
            "priceSource":"DAILY_CLOSE","asOf":"2026-10-16T20:00:00Z"}""", List.of(), false);
    private static final ToolResult TREND = new ToolResult("get_trend", """
            {"symbol":"MSFT","movingAvg50":420.00,"latestVolume":"2.50 million","volumeVsAverage":2.50}""",
            List.of(), false);
    private static final ToolResult FUNDAMENTALS = new ToolResult("get_fundamentals", """
            {"symbol":"MSFT","marketCap":"3.07 trillion","trailingPe":34.5,"totalRevenue":"245.10 billion"}""",
            List.of(), false);
    private static final ToolResult NEWS = new ToolResult("get_news", """
            {"symbol":"MSFT","articles":[{"headline":"Azure growth slows to 28%","url":"https://news.example.com/azure"}]}""",
            List.of(new AssistantSource(AssistantSource.Type.NEWS, "MSFT", "Azure growth slows to 28%",
                    "https://news.example.com/azure", Instant.parse("2026-10-19T13:05:00Z"))), false);

    private final ReplyVerifier verifier = new ReplyVerifier();

    private ReplyVerifier.Verification verify(String reply, ToolResult... results) {
        return verifier.verify(reply, List.of(results), List.of("Why is Microsoft down today?", NOW));
    }

    @Test
    void passesAReplyThatOnlyUsesFiguresFromTheData() {
        String reply = """
                MSFT is down 2.06% at $412.37, against a previous close of $421.05 (daily close, 16 October 2026 \
                at 4pm New York time). Volume was 2.5 million, about 2.5 times its usual level, and the stock is \
                below its 50-day average of 420. Its P/E is 34.5 and its market cap is $3.07 trillion. One \
                relevant headline: "Azure growth slows to 28%". The S&P 500 was not checked.""";

        ReplyVerifier.Verification result = verify(reply, PRICE, TREND, FUNDAMENTALS, NEWS);

        assertThat(result.problems()).isEmpty();
        assertThat(result.passed()).isTrue();
    }

    @Test
    void acceptsFiguresRoundedFromTheData() {
        assertThat(verify("MSFT fell about 2.1% to around $412.", PRICE).passed()).isTrue();
        assertThat(verify("MSFT fell about 2% today.", PRICE).passed()).isTrue();
    }

    @Test
    void rejectsAFigureThatIsNotInTheData() {
        ReplyVerifier.Verification result = verify("MSFT fell 2.3% to $409.12.", PRICE);

        assertThat(result.passed()).isFalse();
        assertThat(result.problems()).containsExactly(
                "The figure 2.3% does not appear in the data", "The figure $409.12 does not appear in the data");
    }

    @Test
    void rejectsCompanyFiguresGivenWithoutAnyToolData() {
        ReplyVerifier.Verification result = verify("Microsoft trades at $415 with a P/E of 36.");

        assertThat(result.problems()).containsExactly(
                "The figure $415 does not appear in the data", "The figure 36 does not appear in the data");
    }

    @Test
    void ignoresSmallWholeNumbersUsedInOrdinaryWording() {
        assertThat(verify("Over the last 5 days and in Q3, the 20-day trend held; 3 symbols were compared.")
                .passed()).isTrue();
    }

    @Test
    void readsNumbersWrittenWithThousandsSeparators() {
        ToolResult volume = new ToolResult("get_trend", "{\"symbol\":\"MSFT\",\"latestVolume\":\"1250000\"}",
                List.of(), false);

        assertThat(verify("Volume was 1,250,000 shares.", volume).passed()).isTrue();
        assertThat(verify("Volume was 1,350,000 shares.", volume).passed()).isFalse();
    }

    @Test
    void acceptsLinksTheNewsReturnedAndRejectsOthers() {
        assertThat(verify("Source: https://news.example.com/azure.", NEWS).passed()).isTrue();

        ReplyVerifier.Verification result = verify("See https://made-up.example.com/story", NEWS);

        assertThat(result.problems()).containsExactly(
                "The link https://made-up.example.com/story was not returned by the news data");
    }

    @Test
    void failsAnEmptyReply() {
        assertThat(verify("  ", PRICE).passed()).isFalse();
    }

    @Test
    void matchesWithinHalfOfTheLastQuotedDecimalPlace() {
        java.util.Set<java.math.BigDecimal> allowed = java.util.Set.of(new java.math.BigDecimal("2.06"));

        assertThat(ReplyVerifier.matchesAny(new java.math.BigDecimal("2.1"), allowed)).isTrue();
        assertThat(ReplyVerifier.matchesAny(new java.math.BigDecimal("2"), allowed)).isTrue();
        assertThat(ReplyVerifier.matchesAny(new java.math.BigDecimal("2.0"), allowed)).isFalse();
        assertThat(ReplyVerifier.matchesAny(new java.math.BigDecimal("2.07"), allowed)).isFalse();
    }
}
