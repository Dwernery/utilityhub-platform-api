package com.utilityhub.api.config;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.utilityhub.api.controller.FinanceController;
import com.utilityhub.api.dto.response.finance.AccountResponseDTO;
import com.utilityhub.api.service.FinanceService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Every API request must carry {@code Authorization: Bearer <api.token>}. Anything
 * else is rejected with a 401 before it reaches a controller.
 *
 * <p>Unlike the controller tests, this class does not add the token automatically.
 */
@WebMvcTest(FinanceController.class)
@Import(SecurityConfig.class)
class ApiTokenAuthenticationTest {

    /** Matches api.token in src/test/resources/config/application.properties. */
    private static final String TOKEN = "test-token-0123456789abcdef0123456789abcdef";

    private static final String ACCOUNTS = "/api/finance/accounts";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private FinanceService financeService;

    @Test
    void requestWithoutToken_isRejectedBeforeReachingTheController() {
        MvcTestResult result = mvc.get().uri(ACCOUNTS).exchange();

        assertUnauthorized(result);
        assertThat(result).headers().hasValue(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        verifyNoInteractions(financeService);
    }

    @Test
    void writeWithoutToken_isRejectedAndChangesNothing() {
        MvcTestResult result = mvc.post()
                .uri(ACCOUNTS)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"accountName":"X","accountType":"Bank","category":"ASSET"}
                        """)
                .exchange();

        assertUnauthorized(result);
        verifyNoInteractions(financeService);
    }

    @Test
    void wrongToken_isRejected_withoutEchoingTheToken() {
        MvcTestResult result = mvc.get()
                .uri(ACCOUNTS)
                .header(HttpHeaders.AUTHORIZATION, "Bearer not-the-token")
                .exchange();

        assertUnauthorized(result);
        assertThat(result).headers().hasValue(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\"");
        assertThat(result).bodyText().doesNotContain("not-the-token");
        verifyNoInteractions(financeService);
    }

    @Test
    void tokenThatIsOnlyAPrefixOfTheRealOne_isRejected() {
        assertUnauthorized(mvc.get()
                .uri(ACCOUNTS)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN.substring(0, 20))
                .exchange());
    }

    @Test
    void rightTokenWithAnotherScheme_isRejected() {
        assertUnauthorized(mvc.get()
                .uri(ACCOUNTS)
                .header(HttpHeaders.AUTHORIZATION, "Basic " + TOKEN)
                .exchange());
    }

    @Test
    void rightToken_reachesTheController() {
        given(financeService.getAllAccounts())
                .willReturn(List.of(new AccountResponseDTO(1, "Checking", "Bank", "ASSET")));

        assertThat(mvc.get().uri(ACCOUNTS).header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[0].accountName")
                .isEqualTo("Checking");
        verify(financeService).getAllAccounts();
    }

    @Test
    void schemeNameIsCaseInsensitive() {
        // RFC 6750 / RFC 9110: auth scheme names are case-insensitive.
        assertThat(mvc.get().uri(ACCOUNTS).header(HttpHeaders.AUTHORIZATION, "bearer " + TOKEN))
                .hasStatusOk();
    }

    @Test
    void corsPreflightFromAllowedOrigin_needsNoToken() {
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

    /** A 401 in the same RFC 9457 problem-detail format as every other API error. */
    private static void assertUnauthorized(MvcTestResult result) {
        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(result).hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo(401);
        assertThat(result).bodyJson().extractingPath("$.title").isEqualTo("Unauthorized");
    }
}
