package com.utilityhub.api.controller;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.utilityhub.api.db.entity.library.Book.BookStatus;
import com.utilityhub.api.dto.response.library.BookResponseDTO;
import com.utilityhub.api.service.LibraryService;
import com.utilityhub.api.support.WithApiToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Web-layer ("slice") tests: only the controller, Jackson and the MVC
 * infrastructure are started. The service is replaced by a Mockito mock, so no
 * database or AWS connection is needed and the tests run in milliseconds.
 */
@WebMvcTest(LibraryController.class)
@Import(WithApiToken.class)
class LibraryControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private LibraryService libraryService;

    private static BookResponseDTO dune() {
        return new BookResponseDTO(
                1,
                "Dune",
                412,
                BookStatus.UNREAD,
                "Frank Herbert",
                null,
                null,
                null,
                null,
                null,
                null);
    }

    @Test
    void getAllBooks_returnsBooksFromService() {
        given(libraryService.findAllBooks()).willReturn(List.of(dune()));

        assertThat(mvc.get().uri("/api/library/books"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[0].title")
                .isEqualTo("Dune");
    }

    @Test
    void createBook_returnsCreatedBook() {
        given(libraryService.createBook(any())).willReturn(dune());

        assertThat(mvc.post()
                        .uri("/api/library/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"title":"Dune","pages":412,"authorName":"Frank Herbert"}
                        """))
                .hasStatus(201)
                .bodyJson()
                .extractingPath("$.id")
                .isEqualTo(1);
    }

    @Test
    void patchBook_passesPatchToServiceAndReturnsUpdatedBook() {
        given(libraryService.patchBook(eq(1), any())).willReturn(dune());

        assertThat(mvc.patch()
                        .uri("/api/library/books/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":4}"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.title")
                .isEqualTo("Dune");

        verify(libraryService).patchBook(eq(1), eq(Map.of("rating", 4)));
    }

    @Test
    void patchBook_acceptsMergePatchContentType() {
        assertThat(mvc.patch()
                        .uri("/api/library/books/1")
                        .contentType("application/merge-patch+json")
                        .content("{\"rating\":4}"))
                .hasStatusOk();
    }

    @Test
    void patchBook_nonObjectBody_isRejected() {
        assertThat(mvc.patch()
                        .uri("/api/library/books/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("3"))
                .hasStatus(400);
    }

    @Test
    void deleteBook_delegatesToService() {
        assertThat(mvc.delete().uri("/api/library/books/1")).hasStatus(204);

        verify(libraryService).deleteBook(1);
    }
}
