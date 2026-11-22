package com.utilityhub.api.dto.request.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of POST /api/auth/login.
 *
 * @param deviceName shown in the app so you can tell sessions apart, e.g. "Safari on macOS"
 */
public record PasswordLoginRequestDTO(
        @NotBlank @Size(max = 200) String password,
        @Size(max = 200) String deviceName) {

    /** Keep the password out of logs and error messages. */
    @Override
    public String toString() {
        return "PasswordLoginRequestDTO[password=<hidden>, deviceName=" + deviceName + "]";
    }
}
