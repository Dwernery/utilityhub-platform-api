package com.utilityhub.api.controller;

import java.util.concurrent.atomic.AtomicInteger;
import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.utilityhub.api.auth.AuthService;
import com.utilityhub.api.auth.PasskeyService;
import com.utilityhub.api.auth.SessionTokenService;
import com.utilityhub.api.config.SecurityConfig;
import com.utilityhub.api.config.SessionCookies;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/** Signing in and out over HTTP. The passkey checks themselves are covered by PasskeyServiceTest. */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, AuthService.class, PasskeyService.class})
class AuthControllerTest {

    /** The password whose hash is in src/test/resources/config/application.properties. */
    private static final String PASSWORD = "test-password";

    /** Each test signs in from its own address, so the rate limiter doesn't carry over. */
    private static final AtomicInteger NEXT_ADDRESS = new AtomicInteger(1);

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private SessionTokenService tokens;

    @Autowired
    private JsonMapper json;

    private final String address = "10.1.0." + NEXT_ADDRESS.getAndIncrement();

    @Test
    void rightPassword_fromTheWebApp_setsAnHttpOnlyCookie_andNeverShowsTheToken() {
        MvcTestResult result = login(PASSWORD, "Safari on macOS").exchange();

        assertThat(result).hasStatusOk();
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .startsWith(SessionCookies.NAME + "=")
                .contains("HttpOnly", "SameSite=Strict", "Path=/api", "Max-Age=");
        assertThat(result).bodyJson().extractingPath("$.deviceName").isEqualTo("Safari on macOS");
        assertThat(result)
                .bodyJson()
                .hasPathSatisfying("$.expiresAt", value -> value.assertThat().isNotNull());
        assertThat(result).bodyText().doesNotContain("\"token\"");
    }

    @Test
    void rightPassword_fromTheMobileApp_returnsTheToken_whichThenWorks() throws Exception {
        MvcTestResult result = login(PASSWORD, "iPhone")
                .header(AuthController.CLIENT_HEADER, "mobile")
                .exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).headers().doesNotContainHeader(HttpHeaders.SET_COOKIE);
        String token = json.readTree(result.getResponse().getContentAsString())
                .get("token")
                .asString();

        assertThat(mvc.get().uri("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.deviceName")
                .isEqualTo("iPhone");
    }

    @Test
    void wrongPassword_isRefused() {
        MvcTestResult result = login("not-it", "Mac").exchange();

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("Wrong password.");
        assertThat(result).headers().doesNotContainHeader(HttpHeaders.SET_COOKIE);
    }

    @Test
    void emptySignIn_isABadRequest() {
        // deploy.sh relies on this to check the sign-in endpoint is up.
        assertThat(mvc.post()
                        .uri("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void tooManyWrongPasswords_blockEvenTheRightOne_forAWhile() {
        for (int i = 0; i < 5; i++) {
            assertThat(login("guess-" + i, "Mac")).hasStatus(HttpStatus.UNAUTHORIZED);
        }

        MvcTestResult result = login(PASSWORD, "Mac").exchange();

        assertThat(result).hasStatus(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(Long.parseLong(result.getResponse().getHeader(HttpHeaders.RETRY_AFTER)))
                .isBetween(1L, 15 * 60L);
    }

    @Test
    void signingIn_worksEvenWithAnExpiredCookieStillInTheBrowser() {
        assertThat(login(PASSWORD, "Mac").cookie(new Cookie(SessionCookies.NAME, "expired.or.garbage")))
                .hasStatusOk();
    }

    @Test
    void me_withoutASession_is401() {
        assertThat(mvc.get().uri("/api/auth/me")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void me_withTheCookie_describesTheSession() {
        String token = tokens.issue("Chrome on macOS").token();

        assertThat(mvc.get().uri("/api/auth/me").cookie(new Cookie(SessionCookies.NAME, token)))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.deviceName")
                .isEqualTo("Chrome on macOS");
    }

    @Test
    void logout_clearsTheCookie_evenWithoutAValidSession() {
        MvcTestResult result = mvc.post()
                .uri("/api/auth/logout")
                .header(HttpHeaders.ORIGIN, "http://localhost")
                .cookie(new Cookie(SessionCookies.NAME, "expired.or.garbage"))
                .exchange();

        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .startsWith(SessionCookies.NAME + "=;")
                .contains("Max-Age=0");
    }

    @Test
    void settingUpTouchId_needsASession() {
        assertThat(mvc.post().uri("/api/auth/passkey/register/options")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void settingUpTouchId_whenSignedIn_returnsOptionsForThisSite() throws Exception {
        MvcTestResult result = mvc.post()
                .uri("/api/auth/passkey/register/options")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + tokens.issue("Mac").token())
                .exchange();

        assertThat(result).hasStatusOk();
        JsonNode body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("challengeToken").asString()).isNotBlank();
        assertThat(body.at("/publicKey/rp/id").asString()).isEqualTo("localhost");
        assertThat(body.at("/publicKey/authenticatorSelection/userVerification").asString())
                .isEqualTo("required");
    }

    @Test
    void touchIdSignIn_beforeAnyPasskeyIsRegistered_explainsWhatToDo() {
        assertThat(mvc.post().uri("/api/auth/passkey/options"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.detail")
                .asString()
                .contains("Set up Touch ID");
    }

    @Test
    void touchIdSignIn_withAForgedAnswer_isRefused() {
        assertThat(mvc.post()
                        .uri("/api/auth/passkey/login")
                        .with(request -> {
                            request.setRemoteAddr(address);
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"challengeToken":"forged","credential":{"id":"x"},"deviceName":"Mac"}
                                """))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    private MockMvcRequestBuilder login(String password, String deviceName) {
        return mvc.post()
                .uri("/api/auth/login")
                .with(request -> {
                    request.setRemoteAddr(address);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"%s\",\"deviceName\":\"%s\"}".formatted(password, deviceName));
    }
}
