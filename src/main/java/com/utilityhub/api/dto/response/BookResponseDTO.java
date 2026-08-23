package com.utilityhub.api.dto.response;

import java.time.LocalDate;
import com.utilityhub.api.db.entity.Book.BookStatus;

public record BookResponseDTO(
        Integer id,
        String title,
        Integer pages,
        BookStatus status,
        String authorName,
        String seriesName,
        LocalDate startDate,
        LocalDate endDate,
        Integer currentPage,
        Integer rating,
        String isbn13) {
}
