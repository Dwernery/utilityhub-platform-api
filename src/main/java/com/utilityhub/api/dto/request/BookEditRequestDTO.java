package com.utilityhub.api.dto.request;

import java.time.LocalDate;

import com.utilityhub.api.db.entity.library.Book.BookStatus;

public record BookEditRequestDTO(
        String title,
        Integer pages,
        String authorName,
        String seriesName,
        String isbn13,
        BookStatus status,
        LocalDate startDate,
        LocalDate endDate) {

}
