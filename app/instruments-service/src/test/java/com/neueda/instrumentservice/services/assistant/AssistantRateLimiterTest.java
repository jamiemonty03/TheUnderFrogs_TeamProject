package com.neueda.instrumentservice.services.assistant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import com.neueda.instrumentservice.exceptions.AssistantRateLimitException;

class AssistantRateLimiterTest {

    private static final Instant START = Instant.parse("2026-10-19T14:00:00Z");

    private final Clock clock = mock(Clock.class);
    private final AssistantRateLimiter limiter = new AssistantRateLimiter(2, clock);

    @Test
    void allowsQuestionsUpToTheLimitThenRefuses() {
        when(clock.instant()).thenReturn(START, START.plusSeconds(10), START.plusSeconds(20));

        limiter.check("alice");
        limiter.check("alice");

        assertThatThrownBy(() -> limiter.check("alice"))
                .isInstanceOf(AssistantRateLimitException.class)
                .hasMessageContaining("2 questions a minute");
    }

    @Test
    void allowsQuestionsAgainOnceAMinuteHasPassed() {
        when(clock.instant()).thenReturn(START, START.plusSeconds(10), START.plusSeconds(61));

        limiter.check("alice");
        limiter.check("alice");

        assertThatCode(() -> limiter.check("alice")).doesNotThrowAnyException();
    }

    @Test
    void countsEachUserSeparately() {
        when(clock.instant()).thenReturn(START);

        limiter.check("alice");
        limiter.check("alice");

        assertThatCode(() -> limiter.check("bob")).doesNotThrowAnyException();
    }

    @Test
    void doesNotCountARefusedQuestion() {
        when(clock.instant()).thenReturn(START, START, START.plusSeconds(30), START.plusSeconds(61));

        limiter.check("alice");
        limiter.check("alice");
        assertThatThrownBy(() -> limiter.check("alice")).isInstanceOf(AssistantRateLimitException.class);

        assertThatCode(() -> limiter.check("alice")).doesNotThrowAnyException();
    }
}
