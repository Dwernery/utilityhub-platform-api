package com.utilityhub.api.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.utilityhub.api.auth.SessionTokenService.IssuedSession;
import com.utilityhub.api.config.ApiSecurityProperties;
import com.utilityhub.api.exception.SignInFailedException;

/** Password sign-in for the app's single owner. There are no user accounts: one password, from config. */
@Service
public class AuthService {

    static final int MAX_DEVICE_NAME_LENGTH = 100;

    private final String passwordHash;
    private final SessionTokenService tokens;
    private final LoginRateLimiter rateLimiter;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(ApiSecurityProperties properties, SessionTokenService tokens, LoginRateLimiter rateLimiter) {
        this.passwordHash = properties.passwordHash();
        this.tokens = tokens;
        this.rateLimiter = rateLimiter;
    }

    /**
     * @param client the caller's IP address, for rate limiting
     * @throws SignInFailedException wrong password
     * @throws com.utilityhub.api.exception.TooManyLoginAttemptsException too many wrong ones lately
     */
    public IssuedSession signInWithPassword(String password, String deviceName, String client) {
        rateLimiter.checkAllowed(client);
        if (!passwordMatches(password)) {
            rateLimiter.recordFailure(client);
            throw new SignInFailedException("Wrong password.");
        }
        rateLimiter.recordSuccess(client);
        return tokens.issue(cleanDeviceName(deviceName));
    }

    private boolean passwordMatches(String password) {
        try {
            return passwordEncoder.matches(password, passwordHash);
        } catch (IllegalArgumentException tooLong) {
            // bcrypt only looks at the first 72 bytes, and Spring refuses longer input outright.
            return false;
        }
    }

    /** What the session list and "signed in as" show; never trusted for anything. */
    static String cleanDeviceName(String deviceName) {
        String name = deviceName == null ? "" : deviceName.strip().replaceAll("\\p{Cntrl}", "");
        if (name.isEmpty()) return "Unknown device";
        return name.length() > MAX_DEVICE_NAME_LENGTH ? name.substring(0, MAX_DEVICE_NAME_LENGTH) : name;
    }
}
