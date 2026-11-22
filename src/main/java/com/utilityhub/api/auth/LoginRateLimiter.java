package com.utilityhub.api.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import com.utilityhub.api.exception.TooManyLoginAttemptsException;

/**
 * Slows down password guessing. After {@value #MAX_FAILURES_PER_CLIENT} wrong sign-ins
 * from one IP address within {@link #WINDOW}, that address has to wait; after
 * {@value #MAX_FAILURES_TOTAL} from anywhere, everyone does (guessing from many addresses).
 * A successful sign-in clears its address's record.
 *
 * <p>Kept in memory: a restart forgets it, which is fine for a single server.
 */
public class LoginRateLimiter {

    static final int MAX_FAILURES_PER_CLIENT = 5;
    static final int MAX_FAILURES_TOTAL = 30;
    static final Duration WINDOW = Duration.ofMinutes(15);

    private static final String EVERYONE = "*";

    private final Map<String, Deque<Instant>> failures = new HashMap<>();
    private final Clock clock;

    public LoginRateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Throws {@link TooManyLoginAttemptsException} if {@code client} must wait before trying again. */
    public synchronized void checkAllowed(String client) {
        Instant now = clock.instant();
        forgetOldFailures(now);
        Instant waitUntil =
                latestOf(blockedUntil(client, MAX_FAILURES_PER_CLIENT), blockedUntil(EVERYONE, MAX_FAILURES_TOTAL));
        if (waitUntil != null && waitUntil.isAfter(now)) {
            throw new TooManyLoginAttemptsException(Duration.between(now, waitUntil));
        }
    }

    public synchronized void recordFailure(String client) {
        Instant now = clock.instant();
        failures.computeIfAbsent(client, key -> new ArrayDeque<>()).addLast(now);
        failures.computeIfAbsent(EVERYONE, key -> new ArrayDeque<>()).addLast(now);
    }

    public synchronized void recordSuccess(String client) {
        failures.remove(client);
    }

    /** When the oldest failure that still counts drops out of the window, if the limit is reached. */
    private Instant blockedUntil(String key, int limit) {
        Deque<Instant> times = failures.get(key);
        if (times == null || times.size() < limit) return null;
        // With more failures than the limit, wait until only limit - 1 remain.
        return times.stream()
                .skip(times.size() - limit)
                .findFirst()
                .orElseThrow()
                .plus(WINDOW);
    }

    private void forgetOldFailures(Instant now) {
        Instant cutoff = now.minus(WINDOW);
        failures.values().forEach(times -> {
            while (!times.isEmpty() && !times.peekFirst().isAfter(cutoff)) times.removeFirst();
        });
        failures.values().removeIf(Deque::isEmpty);
    }

    private static Instant latestOf(Instant a, Instant b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }
}
