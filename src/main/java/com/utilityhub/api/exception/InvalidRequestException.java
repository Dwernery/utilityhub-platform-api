package com.utilityhub.api.exception;

/**
 * Thrown when a request is well-formed JSON but violates a business rule
 * (e.g. an unknown enum value or a malformed id). Mapped to HTTP 400 by
 * {@link GlobalExceptionHandler}.
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
