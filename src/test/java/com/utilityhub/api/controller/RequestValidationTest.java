package com.utilityhub.api.controller;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.utilityhub.api.service.FinanceService;
import com.utilityhub.api.service.LibraryService;
import com.utilityhub.api.service.MCUService;
import com.utilityhub.api.support.WithSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Invalid input must be rejected at the boundary with a 400 and a per-field
 * message, and must never reach the service layer.
 */
@WebMvcTest({FinanceController.class, LibraryController.class, MCUController.class})
@Import(WithSession.class)
class RequestValidationTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private FinanceService financeService;

    @MockitoBean
    private LibraryService libraryService;

    @MockitoBean
    private MCUService mcuService;

    @AfterEach
    void serviceIsNeverCalled() {
        verifyNoInteractions(financeService, libraryService, mcuService);
    }

    private MvcTestResult send(String method, String uri, String json) {
        return switch (method) {
            case "POST" ->
                mvc.post()
                        .uri(uri)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .exchange();
            case "PUT" ->
                mvc.put()
                        .uri(uri)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .exchange();
            case "DELETE" ->
                mvc.delete()
                        .uri(uri)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .exchange();
            default -> throw new IllegalArgumentException(method);
        };
    }

    private void assertFieldError(MvcTestResult result, String field, String expectedMessage) {
        assertThat(result)
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.errors." + field)
                .isEqualTo(expectedMessage);
    }

    // --- Finance ---

    @Test
    void editTransaction_missingPaid_isRejectedInsteadOfDefaultingToFalse() {
        assertFieldError(send("PUT", "/api/finance/transactions/5", """
                {"name":"Rent","amount":1500}
                """), "paid", "must not be null");
    }

    @Test
    void editTransaction_tooManyDecimals_isRejected() {
        assertFieldError(
                send("PUT", "/api/finance/transactions/5", """
                {"name":"Rent","amount":10.999,"paid":true}
                """),
                "amount",
                "numeric value out of bounds (<10 digits>.<2 digits> expected)");
    }

    @Test
    void createTransaction_missingType_isRejected() {
        assertFieldError(send("POST", "/api/finance/transactions", """
                {"name":"Gym","amount":45}
                """), "transactionType", "must not be null");
    }

    @Test
    void createTransaction_blankName_isRejected() {
        assertFieldError(send("POST", "/api/finance/transactions", """
                {"name":" ","transactionType":"INCOME","amount":45}
                """), "name", "must not be blank");
    }

    @Test
    void createAccount_unknownCategory_listsAllowedValues() {
        assertFieldError(send("POST", "/api/finance/accounts", """
                {"accountName":"Checking","accountType":"Bank","category":"SAVINGS"}
                """), "category", "must be one of [ASSET, LIABILITY]");
    }

    @Test
    void createAccount_balanceWithoutDate_failsCrossFieldRule() {
        assertFieldError(
                send("POST", "/api/finance/accounts", """
                {"accountName":"Checking","accountType":"Bank","category":"ASSET","balance":100}
                """),
                "initialBalanceComplete",
                "balance and balanceDate must be provided together");
    }

    @Test
    void createIncomeSource_blankName_isRejected() {
        assertFieldError(send("POST", "/api/finance/income-sources", """
                {"year":2026,"name":"   ","amount":100}
                """), "name", "must not be blank");
    }

    @Test
    void accountBalance_invalidDateInPath_isRejected() {
        assertThat(send("PUT", "/api/finance/accounts/1/balances/2026-13-01", "{\"balance\":1}"))
                .hasStatus(400);
    }

    @Test
    void accountBalance_missingBalance_isRejected() {
        assertFieldError(
                send("PUT", "/api/finance/accounts/1/balances/2026-11-01", "{}"), "balance", "must not be null");
    }

    // --- Library ---

    @Test
    void createBook_zeroPages_isRejected() {
        assertFieldError(send("POST", "/api/library/books", """
                {"title":"Dune","pages":0,"authorName":"Frank Herbert"}
                """), "pages", "must be greater than 0");
    }

    @Test
    void uploadUrl_blankFileName_isRejected() {
        assertThat(mvc.get().uri("/api/library/books/upload-url?fileName=&fileType=image/png"))
                .hasStatus(400);
    }

    // --- MCU ---

    @Test
    void updateStatus_unsupportedGlobalIdType_isRejected() {
        assertFieldError(
                send("PUT", "/api/mcu/tracker/show:1/status", """
                {"status":"WATCHED"}
                """),
                "globalId",
                "must look like 'movie:12' or 'episode:34'");
    }

    @Test
    void updateStatus_idTooLargeForLong_isRejectedNotA500() {
        assertFieldError(
                send("PUT", "/api/mcu/tracker/movie:99999999999999999999/status", """
                {"status":"WATCHED"}
                """),
                "globalId",
                "must look like 'movie:12' or 'episode:34'");
    }

    @Test
    void updateStatus_unknownStatus_listsAllowedValues() {
        assertFieldError(
                send("PUT", "/api/mcu/tracker/movie:1/status", """
                {"status":"DONE"}
                """),
                "status",
                "must be one of [UNWATCHED, IN_PROGRESS, WATCHED]");
    }

    @Test
    void createMovie_missingRuntimeAndSynopsis_areRejected() {
        assertThat(send("POST", "/api/mcu/movies", """
                {"title":"Thunderbolts*","premiereDate":"2025-05-02","domain":"Phase 5","special":false}
                """))
                .hasStatus(400)
                .bodyJson()
                .satisfies(body -> {
                    assertThat(body).extractingPath("$.errors.runtime").isEqualTo("must not be null");
                    assertThat(body).extractingPath("$.errors.synopsis").isEqualTo("must not be blank");
                });
    }

    @Test
    void createMovie_blankDomain_isRejected() {
        assertFieldError(send("POST", "/api/mcu/movies", """
                {"title":"Thunderbolts*","premiereDate":"2025-05-02","runtime":127,
                 "synopsis":"A team of antiheroes.","domain":" ","special":false}
                """), "domain", "must not be blank");
    }

    @Test
    void createShowSeason_zeroSeason_isRejected() {
        assertFieldError(send("POST", "/api/mcu/shows", """
                {"title":"Loki","season":0,"premiereDate":"2021-06-09","synopsis":"Variant.","domain":"Phase 4"}
                """), "season", "must be greater than 0");
    }

    @Test
    void createEpisode_missingTitle_isRejected() {
        assertFieldError(send("POST", "/api/mcu/shows/3/episodes", """
                {"episodeNumber":1,"runtime":50}
                """), "title", "must not be blank");
    }

    @Test
    void attachPoster_keyOutsidePosterFolder_isRejected() {
        assertFieldError(
                send("POST", "/api/mcu/movies/1/poster", """
                {"s3Key":"books/secret.pdf"}
                """),
                "s3Key",
                "must be a key from /api/mcu/poster-upload-url");
    }

    @Test
    void posterUploadUrl_nonImageType_isRejected() {
        assertThat(mvc.get()
                        .uri("/api/mcu/poster-upload-url?fileName=a.pdf&fileType=application/pdf")
                        .exchange())
                .hasStatus(400);
    }
}
