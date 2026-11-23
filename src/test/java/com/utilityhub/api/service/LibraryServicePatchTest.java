package com.utilityhub.api.service;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.utilityhub.api.db.entity.library.Author;
import com.utilityhub.api.db.entity.library.Book;
import com.utilityhub.api.db.entity.library.Book.BookStatus;
import com.utilityhub.api.db.entity.library.Series;
import com.utilityhub.api.db.repository.library.AuthorRepository;
import com.utilityhub.api.db.repository.library.BookRepository;
import com.utilityhub.api.db.repository.library.ReadingGoalRepository;
import com.utilityhub.api.db.repository.library.SeriesRepository;
import com.utilityhub.api.dto.response.library.BookResponseDTO;
import com.utilityhub.api.exception.InvalidRequestException;
import com.utilityhub.api.exception.ResourceNotFoundException;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Plain unit test: no Spring context. Repositories are mocks; the JSON mapper
 * and validator are real, because the merge-and-validate behaviour is exactly
 * what we want to test.
 */
class LibraryServicePatchTest {

    private final JsonMapper json = JsonMapper.builder().build();

    private BookRepository bookRepository;
    private AuthorRepository authorRepository;
    private SeriesRepository seriesRepository;
    private LibraryService service;
    private Book book;

    @BeforeEach
    void setUp() {
        bookRepository = mock(BookRepository.class);
        authorRepository = mock(AuthorRepository.class);
        seriesRepository = mock(SeriesRepository.class);
        service = new LibraryService(
                bookRepository,
                authorRepository,
                seriesRepository,
                mock(ReadingGoalRepository.class),
                mock(S3Service.class),
                json,
                Validation.buildDefaultValidatorFactory().getValidator());

        Author herbert = new Author();
        herbert.setFullName("Frank Herbert");

        book = new Book();
        book.setTitle("Dune");
        book.setPages(412);
        book.setAuthor(herbert);
        book.setStatus(BookStatus.IN_PROGRESS);
        book.setStartDate(LocalDate.of(2026, 1, 1));
        book.setCurrentPage(100);

        given(bookRepository.findById(1)).willReturn(Optional.of(book));
        given(bookRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
    }

    private BookResponseDTO patch(String body) {
        return service.patchBook(1, json.readValue(body, new TypeReference<Map<String, Object>>() {}));
    }

    @Test
    void onlyProvidedFieldsChange() {
        BookResponseDTO result = patch("{\"currentPage\": 250}");

        assertThat(result.currentPage()).isEqualTo(250);
        assertThat(result.title()).isEqualTo("Dune");
        assertThat(result.status()).isEqualTo(BookStatus.IN_PROGRESS);
        assertThat(result.startDate()).isEqualTo(LocalDate.of(2026, 1, 1));
    }

    @Test
    void explicitNullClearsAField() {
        BookResponseDTO result = patch("{\"status\": \"UNREAD\", \"startDate\": null, \"currentPage\": 0}");

        assertThat(result.status()).isEqualTo(BookStatus.UNREAD);
        assertThat(result.startDate()).isNull();
    }

    @Test
    void resettingToUnreadClearsRating() {
        book.setStatus(BookStatus.READ);
        book.setRating(4);

        BookResponseDTO result = patch("{\"status\": \"UNREAD\", \"startDate\": null, \"currentPage\": 0}");

        assertThat(result.rating()).isNull();
    }

    @Test
    void ratingIsKeptForOtherStatusChanges() {
        book.setStatus(BookStatus.READ);
        book.setRating(4);

        assertThat(patch("{\"status\": \"IN_PROGRESS\"}").rating()).isEqualTo(4);
    }

    @Test
    void unchangedAuthorIsNotLookedUpAgain() {
        patch("{\"authorName\": \"Frank Herbert\", \"rating\": 4}");

        verify(authorRepository, never()).findFirstByFullNameIgnoreCaseOrderByIdAsc(any());
    }

    @Test
    void unknownAuthorIsNotFound() {
        given(authorRepository.findFirstByFullNameIgnoreCaseOrderByIdAsc("Nobody"))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> patch("{\"authorName\": \"Nobody\"}")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void unknownFieldIsRejected() {
        assertThatThrownBy(() -> patch("{\"rateing\": 4}"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("rateing");
    }

    @Test
    void ratingOutOfRangeIsRejected() {
        assertThatThrownBy(() -> patch("{\"rating\": 6}"))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("rating");
    }

    @Test
    void requiredFieldCannotBeCleared() {
        assertThatThrownBy(() -> patch("{\"title\": null}"))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("title");
    }

    @Test
    void currentPageBeyondStoredPageCountIsRejected() {
        // "pages" isn't in the patch; the rule still applies because the merged state is validated.
        assertThatThrownBy(() -> patch("{\"currentPage\": 500}"))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("currentPage cannot exceed pages");
    }

    @Test
    void endDateBeforeStoredStartDateIsRejected() {
        assertThatThrownBy(() -> patch("{\"endDate\": \"2025-12-31\"}"))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("endDate cannot be before startDate");
    }

    @Test
    void wrongTypeIsRejectedInsteadOfSilentlyBecomingZero() {
        assertThatThrownBy(() -> patch("{\"pages\": \"abc\"}")).isInstanceOf(MismatchedInputException.class);
    }

    @Test
    void invalidDateIsRejected() {
        assertThatThrownBy(() -> patch("{\"startDate\": \"2026-13-01\"}")).isInstanceOf(MismatchedInputException.class);
    }

    @Test
    void failedValidationDoesNotSave() {
        assertThatThrownBy(() -> patch("{\"rating\": 0}"));

        verify(bookRepository, never()).save(any());
    }

    @Test
    void authorIsFoundIgnoringCaseAndSpaces() {
        Author sanderson = new Author();
        sanderson.setFullName("Brandon Sanderson");
        given(authorRepository.findFirstByFullNameIgnoreCaseOrderByIdAsc("brandon sanderson"))
                .willReturn(Optional.of(sanderson));

        assertThat(patch("{\"authorName\": \" brandon sanderson \"}").authorName())
                .isEqualTo("Brandon Sanderson");
    }

    @Test
    void seriesPositionIsSavedWithASeries() {
        Series dune = new Series();
        dune.setName("Dune Chronicles");
        given(seriesRepository.findFirstByNameIgnoreCaseOrderByIdAsc("Dune Chronicles"))
                .willReturn(Optional.of(dune));

        BookResponseDTO result = patch("{\"seriesName\": \"Dune Chronicles\", \"seriesPosition\": 1}");

        assertThat(result.seriesName()).isEqualTo("Dune Chronicles");
        assertThat(result.seriesPosition()).isEqualTo(1);
    }

    @Test
    void leavingTheSeriesClearsItsPosition() {
        Series dune = new Series();
        dune.setName("Dune Chronicles");
        book.setSeries(dune);
        book.setSeriesPosition(1);

        assertThat(patch("{\"seriesName\": null}").seriesPosition()).isNull();
    }

    @Test
    void positionWithoutASeriesIsDropped() {
        assertThat(patch("{\"seriesPosition\": 3}").seriesPosition()).isNull();
    }

    @Test
    void positionBelowOneIsRejected() {
        assertThatThrownBy(() -> patch("{\"seriesPosition\": 0}")).isInstanceOf(ConstraintViolationException.class);
    }

    @Test
    void decimalPositionIsRejected() {
        assertThatThrownBy(() -> patch("{\"seriesPosition\": 2.5}")).isInstanceOf(MismatchedInputException.class);
    }
}
