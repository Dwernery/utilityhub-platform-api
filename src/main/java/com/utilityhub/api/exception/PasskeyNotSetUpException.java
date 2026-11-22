package com.utilityhub.api.exception;

/** Touch ID sign-in isn't available: passkeys are off, or none is registered yet. Answered with 404. */
public class PasskeyNotSetUpException extends RuntimeException {

    public PasskeyNotSetUpException(String message) {
        super(message);
    }
}
