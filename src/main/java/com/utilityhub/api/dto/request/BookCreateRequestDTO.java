package com.utilityhub.api.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import com.utilityhub.api.dto.json.WholeNumberDeserializer;

import tools.jackson.databind.annotation.JsonDeserialize;

public record BookCreateRequestDTO(
        @NotBlank @Size(max = 255) String title,
        @NotNull @Positive Integer pages,
        @NotBlank @Size(max = 255) String authorName,
        @Size(max = 255) String seriesName,
        /** The book's number in its series: a whole number from 1. Ignored without a series. */
        @Positive @Max(999) @JsonDeserialize(using = WholeNumberDeserializer.class)
        Integer seriesPosition) {}
