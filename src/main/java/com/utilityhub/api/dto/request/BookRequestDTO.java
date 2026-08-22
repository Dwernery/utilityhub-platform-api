package com.utilityhub.api.dto.request;

public record BookRequestDTO(
        String title,
        Integer pages,
        Integer authorId,
        Integer seriesId,
        String isbn13
){}
