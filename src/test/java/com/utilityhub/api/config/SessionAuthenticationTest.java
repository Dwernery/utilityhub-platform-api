package com.utilityhub.api.config;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.utilityhub.api.auth.SessionTokenService;
import com.utilityhub.api.controller.FinanceController;
import com.utilityhub.api.dto.response.finance.AccountResponseDTO;
import com.utilityhub.api.service.FinanceService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Every API request needs a valid session token, as {@code Authorization: Bearer} or the
 * {@code uh_session} cookie. Anything else is rejected with a 401 before it reaches a controller.
 *
 * <p>Unlike the controller tests, this class does not add a session automatically.
 */
@WebMvcTest(FinanceController.class)
@Import(SecurityConfig.class)
class SessionAuthenticationTest {

    /** Matches api.session-secret in src/test/resources/config/application.properties. */
    private static final String SECRET = "test-session-secret-0123456789abcdef0123456789";

    private static final String ACCOUNTS = "/api/finance/accounts";
    private static final String NEW_ACCOUNT = """
            {"accountName":"X","accountType":"Bank","category":"ASSET"}
            """;

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private SessionTokenService tokens;

    @MockitoBean
    private FinanceService financeService;

    @Test
    void requestWithoutSession_isRejectedBeforeReachingTheController() {
        MvcTestResult result = mvc.get().uri(ACCOUNTS).exchange();

        assertUnauthorized(result);
        assertThat(result).headers().hasValue(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        assertThat(result).bodyJson().extractingPath("$.detail").isEqualTo("Please sign in.");
        verifyNoInteractions(financeService);
    }

    @Test
    void writeWithoutSession_isRejectedAndChangesNothing() {
        assertUnauthorized(mvc.post()
                .uri(ACCOUNTS)
                .contentType(MediaType.APPLICATION_JSON)
                .content(NEW_ACCOUNT)
                .exchange());
        verifyNoInteractions(financeService);
    }

    @Test
    void bearerToken_reachesTheController() {
        given(financeService.getAllAccounts())
                .willReturn(List.of(new AccountResponseDTO(1, "Checking", "Bank", "ASSET", false)));

        assertThat(mvc.get().uri(ACCOUNTS).header(HttpHeaders.AUTHORIZATION, bearer(validToken())))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[0].accountName")
                .isEqualTo("Checking");
        verify(financeService).getAllAccounts();
    }

    @Test
    void schemeNameIsCaseInsensitive() {
        // RFC 6750 / RFC 9110: auth scheme names are case-insensitive.
        assertThat(mvc.get().uri(ACCOUNTS).header(HttpHeaders.AUTHORIZATION, "bearer " + validToken()))
                .hasStatusOk();
    }

    @Test
    void sessionCookie_reachesTheController() {
        assertThat(mvc.get().uri(ACCOUNTS).cookie(sessionCookie(validToken()))).hasStatusOk();
    }

    @Test
    void tamperedToken_isRejected_withoutEchoingIt() {
        String token = validToken();
        String tampered = token.substring(0, token.length() - 4) + "AAAA";

        MvcTestResult result = mvc.get()
                .uri(ACCOUNTS)
                .header(HttpHeaders.AUTHORIZATION, bearer(tampered))
                .exchange();

        assertUnauthorized(result);
        assertThat(result).headers().hasValue(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\"");
        assertThat(result).bodyText().doesNotContain(tampered);
        verifyNoInteractions(financeService);
    }

    @Test
    void tokenSignedWithAnotherSecret_isRejected() {
        String foreign = new SessionTokenService("another-secret-".repeat(4), Duration.ofDays(30), Clock.systemUTC())
                .issue("Phone")
                .token();

        assertUnauthorized(mvc.get()
                .uri(ACCOUNTS)
                .header(HttpHeaders.AUTHORIZATION, bearer(foreign))
                .exchange());
    }

    @Test
    void expiredToken_isRejected_andSaysSo() {
        String expired = tokenIssuedAgo(Duration.ofDays(31));

        MvcTestResult result =
                mvc.get().uri(ACCOUNTS).cookie(sessionCookie(expired)).exchange();

        assertUnauthorized(result);
        assertThat(result)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("Your session has expired. Please sign in again.");
    }

    @Test
    void validlySignedTokenThatIsNotASession_isRejected() {
        // E.g. a passkey challenge token: same key, other audience.
        Instant now = Instant.now();
        String challenge = NimbusJwtEncoder.withSecretKey(
                        new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .build()
                .encode(JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(),
                        JwtClaimsSet.builder()
                                .issuer("utility-hub")
                                .audience(List.of("passkey-sign-in"))
                                .issuedAt(now)
                                .expiresAt(now.plusSeconds(120))
                                .build()))
                .getTokenValue();

        assertUnauthorized(mvc.get()
                .uri(ACCOUNTS)
                .header(HttpHeaders.AUTHORIZATION, bearer(challenge))
                .exchange());
    }

    @Test
    void rightTokenWithAnotherScheme_isRejected() {
        assertUnauthorized(mvc.get()
                .uri(ACCOUNTS)
                .header(HttpHeaders.AUTHORIZATION, "Basic " + validToken())
                .exchange());
    }

    @Test
    void freshToken_isNotReplaced() {
        MvcTestResult result = mvc.get()
                .uri(ACCOUNTS)
                .header(HttpHeaders.AUTHORIZATION, bearer(validToken()))
                .exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).headers().doesNotContainHeader("X-Session-Token");
        assertThat(result).headers().doesNotContainHeader(HttpHeaders.SET_COOKIE);
    }

    @Test
    void tokenPastHalfItsLife_getsAFreshOne_inAHeader_forTheMobileApp() {
        MvcTestResult result = mvc.get()
                .uri(ACCOUNTS)
                .header(HttpHeaders.AUTHORIZATION, bearer(tokenIssuedAgo(Duration.ofDays(20))))
                .exchange();

        assertThat(result).hasStatusOk();
        String fresh = result.getResponse().getHeader("X-Session-Token");
        assertThat(fresh).isNotBlank();
        assertThat(tokens.sessionDecoder().decode(fresh).getClaimAsString("dev"))
                .isEqualTo("Phone");
        assertThat(mvc.get().uri(ACCOUNTS).header(HttpHeaders.AUTHORIZATION, bearer(fresh)))
                .hasStatusOk();
    }

    @Test
    void tokenPastHalfItsLife_getsAFreshOne_asACookie_forTheWebApp() {
        MvcTestResult result = mvc.get()
                .uri(ACCOUNTS)
                .cookie(sessionCookie(tokenIssuedAgo(Duration.ofDays(20))))
                .exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).headers().doesNotContainHeader("X-Session-Token");
        assertThat(result.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .startsWith("uh_session=")
                .contains("HttpOnly", "SameSite=Strict", "Path=/api");
    }

    @Test
    void cookieWriteFromThisSite_isAllowed() {
        given(financeService.createAccount(any())).willReturn(new AccountResponseDTO(7, "X", "Bank", "ASSET", false));

        assertThat(mvc.post()
                        .uri(ACCOUNTS)
                        .header(HttpHeaders.ORIGIN, "http://localhost")
                        .cookie(sessionCookie(validToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(NEW_ACCOUNT))
                .hasStatus2xxSuccessful();
    }

    @Test
    void cookieWriteFromAnotherSite_isRefused() {
        MvcTestResult result = mvc.post()
                .uri(ACCOUNTS)
                .header(HttpHeaders.ORIGIN, "https://evil.example")
                .cookie(sessionCookie(validToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(NEW_ACCOUNT)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(financeService);
    }

    @Test
    void cookieWriteThatTheBrowserCallsCrossSite_isRefused_evenWithoutOrigin() {
        assertThat(mvc.delete()
                        .uri(ACCOUNTS + "/1")
                        .header("Sec-Fetch-Site", "cross-site")
                        .cookie(sessionCookie(validToken())))
                .hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(financeService);
    }

    @Test
    void corsPreflightFromAllowedOrigin_needsNoSession() {
        // Browsers send preflights without credentials, so they must not be rejected.
        MvcTestResult result = mvc.options()
                .uri(ACCOUNTS)
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization")
                .exchange();

        assertThat(result).hasStatusOk();
        assertThat(result).headers().hasValue(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173");
    }

    @Test
    void corsPreflightFromAnyOtherOrigin_isRefused() {
        assertThat(mvc.options()
                        .uri(ACCOUNTS)
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .hasStatus(HttpStatus.FORBIDDEN);
    }

    private String validToken() {
        return tokens.issue("Phone").token();
    }

    private static String tokenIssuedAgo(Duration age) {
        Clock past = Clock.fixed(Instant.now().minus(age), ZoneOffset.UTC);
        return new SessionTokenService(SECRET, Duration.ofDays(30), past)
                .issue("Phone")
                .token();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static Cookie sessionCookie(String token) {
        return new Cookie(SessionCookies.NAME, token);
    }

    /** A 401 in the same RFC 9457 problem-detail format as every other API error. */
    private static void assertUnauthorized(MvcTestResult result) {
        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(result).hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo(401);
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Unauthorized");
    }
}
