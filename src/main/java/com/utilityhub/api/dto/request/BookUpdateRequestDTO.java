package com.utilityhub.api.dto.request;

import java.time.LocalDate;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.utilityhub.api.db.entity.library.Book;
import com.utilityhub.api.db.entity.library.Book.BookStatus;

/**
 * The editable state of a book. A PATCH request is merged into the book's
 * current state (see {@code LibraryService.patchBook}) and the <em>result</em>
 * is validated against these constraints. That way rules that involve fields
 * the client didn't send, such as {@code currentPage <= pages}, are still
 * enforced.
 */
public record BookUpdateRequestDTO(
        @NotBlank @Size(max = 255) String title,
        @NotNull @Positive Integer pages,
        @NotBlank @Size(max = 255) String authorName,
        @Size(max = 255) String seriesName,
        @NotNull BookStatus status,
        LocalDate startDate,
        LocalDate endDate,
        @PositiveOrZero Integer currentPage,
        @Min(1) @Max(5) Integer rating) {

    public static BookUpdateRequestDTO from(Book book) {
        return new BookUpdateRequestDTO(
                book.getTitle(),
                book.getPages(),
                book.getAuthor() != null ? book.getAuthor().getFullName() : null,
                book.getSeries() != null ? book.getSeries().getName() : null,
                book.getStatus(),
                book.getStartDate(),
                book.getEndDate(),
                book.getCurrentPage(),
                book.getRating());
    }

    // @JsonIgnore keeps these validation-only "properties" out of JSON conversion.

    @JsonIgnore
    @AssertTrue(message = "currentPage cannot exceed pages")
    public boolean isCurrentPageWithinBook() {
        return currentPage == null || pages == null || currentPage <= pages;
    }

    @JsonIgnore
    @AssertTrue(message = "endDate cannot be before startDate")
    public boolean isDateRangeValid() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }
}
