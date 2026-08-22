package com.utilityhub.api.dto.request;

public record BookRequestDTO(
        String title,
        Integer pages,
        String authorName,
        String seriesName,
        String isbn13
){}
