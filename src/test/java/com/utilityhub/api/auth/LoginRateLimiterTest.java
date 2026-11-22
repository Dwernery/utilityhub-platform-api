package com.utilityhub.api.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.utilityhub.api.exception.TooManyLoginAttemptsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginRateLimiterTest {

    /** A clock the test moves forward by hand. */
    private static final class TestClock extends Clock {
        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }
    }

    private final TestClock clock = new TestClock();
    private final LoginRateLimiter limiter = new LoginRateLimiter(clock);

    @Test
    void fewFailures_areAllowed() {
        fail("a", LoginRateLimiter.MAX_FAILURES_PER_CLIENT - 1);
        assertThatCode(() -> limiter.checkAllowed("a")).doesNotThrowAnyException();
    }

    @Test
    void tooManyFailures_blockThatClient_untilTheOldestLeavesTheWindow() {
        fail("a", LoginRateLimiter.MAX_FAILURES_PER_CLIENT);

        assertThatThrownBy(() -> limiter.checkAllowed("a"))
                .isInstanceOfSatisfying(
                        TooManyLoginAttemptsException.class,
                        e -> assertThat(e.getRetryAfter()).isEqualTo(LoginRateLimiter.WINDOW));
        assertThatCode(() -> limiter.checkAllowed("b")).doesNotThrowAnyException();

        clock.advance(LoginRateLimiter.WINDOW.minusSeconds(1));
        assertThatThrownBy(() -> limiter.checkAllowed("a")).isInstanceOf(TooManyLoginAttemptsException.class);
        clock.advance(Duration.ofSeconds(1));
        assertThatCode(() -> limiter.checkAllowed("a")).doesNotThrowAnyException();
    }

    @Test
    void success_clearsThatClientsRecord() {
        fail("a", LoginRateLimiter.MAX_FAILURES_PER_CLIENT - 1);
        limiter.recordSuccess("a");
        fail("a", 1);
        assertThatCode(() -> limiter.checkAllowed("a")).doesNotThrowAnyException();
    }

    @Test
    void manyFailuresFromManyClients_blockEveryone() {
        for (int i = 0; i < LoginRateLimiter.MAX_FAILURES_TOTAL; i++) {
            limiter.recordFailure("client-" + i);
        }
        assertThatThrownBy(() -> limiter.checkAllowed("someone-new")).isInstanceOf(TooManyLoginAttemptsException.class);
    }

    /** {@code times} wrong sign-ins at the current moment. */
    private void fail(String client, int times) {
        for (int i = 0; i < times; i++) {
            limiter.checkAllowed(client);
            limiter.recordFailure(client);
        }
    }
}
