package com.utilityhub.api.exception;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.utilityhub.api.controller.FinanceController;
import com.utilityhub.api.controller.LibraryController;
import com.utilityhub.api.service.FinanceService;
import com.utilityhub.api.service.LibraryService;
import com.utilityhub.api.support.WithApiToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

/**
 * Verifies the HTTP contract for errors. The controllers here are just a
 * convenient way to trigger each kind of exception.
 */
@WebMvcTest({LibraryController.class, FinanceController.class})
@Import(WithApiToken.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private LibraryService libraryService;

    @MockitoBean
    private FinanceService financeService;

    @Test
    void resourceNotFound_returns404ProblemDetail() {
        willThrow(new ResourceNotFoundException("Book", 42))
                .given(libraryService)
                .deleteBook(42);

        assertThat(mvc.delete().uri("/api/library/books/42"))
                .hasStatus(404)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("Book not found: 42");
    }

    @Test
    void invalidRequest_returns400WithMessage() {
        willThrow(new InvalidRequestException("Invalid status value: DONE"))
                .given(libraryService)
                .patchBook(anyInt(), any());

        assertThat(mvc.patch()
                        .uri("/api/library/books/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.detail")
                .isEqualTo("Invalid status value: DONE");
    }

    @Test
    void beanValidationFailure_returns400WithFieldErrors() {
        assertThat(mvc.post()
                        .uri("/api/library/books/1/attach-file")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .hasStatus(400)
                .bodyJson()
                .extractingPath("$.errors.s3Key")
                .isEqualTo("s3Key is required");
    }

    @Test
    void malformedJson_returns400() {
        assertThat(mvc.put()
                        .uri("/api/finance/income-sources/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad-json"))
                .hasStatus(400);
    }

    @Test
    void nonNumericPathVariable_returns400() {
        assertThat(mvc.delete().uri("/api/library/books/abc")).hasStatus(400);
    }

    @Test
    void unsupportedHttpMethod_returns405() {
        assertThat(mvc.delete().uri("/api/library/books")).hasStatus(405);
    }

    @Test
    void unknownUrl_returns404() {
        assertThat(mvc.get().uri("/api/does-not-exist")).hasStatus(404);
    }

    @Test
    void dataIntegrityViolation_returns409WithoutLeakingSql() {
        willThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint \"uq_x\""))
                .given(libraryService)
                .deleteBook(anyInt());

        assertThat(mvc.delete().uri("/api/library/books/1"))
                .hasStatus(409)
                .bodyText()
                .doesNotContain("uq_x");
    }

    @Test
    void unexpectedException_returns500WithoutLeakingMessage() {
        given(libraryService.findAllBooks()).willThrow(new IllegalStateException("db password is hunter2"));

        assertThat(mvc.get().uri("/api/library/books"))
                .hasStatus(500)
                .bodyText()
                .doesNotContain("hunter2");
    }
}
