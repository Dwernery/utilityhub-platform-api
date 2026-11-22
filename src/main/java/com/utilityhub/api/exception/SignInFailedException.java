package com.utilityhub.api.exception;

/** Wrong password or a passkey that didn't check out: answered with 401. The message is shown to the user. */
public class SignInFailedException extends RuntimeException {

    public SignInFailedException(String message) {
        super(message);
    }
}
