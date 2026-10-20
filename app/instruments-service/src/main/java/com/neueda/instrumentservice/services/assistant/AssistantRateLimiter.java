package com.neueda.instrumentservice.services.assistant;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.neueda.instrumentservice.exceptions.AssistantRateLimitException;

@Component
public class AssistantRateLimiter {

    static final Duration WINDOW = Duration.ofMinutes(1);

    private final int perMinute;
    private final Clock clock;
    private final Map<String, Deque<Instant>> recent = new ConcurrentHashMap<>();

    @Autowired
    public AssistantRateLimiter(@Value("${ai.questions-per-minute:5}") int perMinute) {
        this(perMinute, Clock.systemUTC());
    }

    AssistantRateLimiter(int perMinute, Clock clock) {
        this.perMinute = perMinute;
        this.clock = clock;
    }

    public void check(String user) {
        Instant now = clock.instant();
        boolean[] allowed = {false};
        recent.compute(user, (key, times) -> {
            Deque<Instant> window = times == null ? new ArrayDeque<>() : times;
            while (!window.isEmpty() && !window.peekFirst().isAfter(now.minus(WINDOW))) {
                window.pollFirst();
            }
            if (window.size() < perMinute) {
                window.addLast(now);
                allowed[0] = true;
            }
            return window;
        });
        if (!allowed[0]) {
            throw new AssistantRateLimitException(
                    "You can ask up to " + perMinute + " questions a minute. Please wait a moment.");
        }
    }
}
