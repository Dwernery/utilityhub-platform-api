package com.utilityhub.api.dto.response.library;

import java.time.LocalDate;

import com.utilityhub.api.db.entity.library.Book.BookStatus;

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
        String isbn13,
        String s3Url) {
}
