package com.utilityhub.api.exception;

import java.time.Duration;

/** Too many wrong sign-ins from one place (or overall) in a short time: answered with 429. */
public class TooManyLoginAttemptsException extends RuntimeException {

    private final Duration retryAfter;

    public TooManyLoginAttemptsException(Duration retryAfter) {
        super("Too many sign-in attempts. Try again later.");
        this.retryAfter = retryAfter;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}
