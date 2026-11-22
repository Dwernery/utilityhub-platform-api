package com.utilityhub.api.dto.response.mcu;

import java.time.LocalDate;
import java.util.List;

/** One season of a show (each Show row is a season), with its episodes in order. */
public record ShowResponseDTO(
        Long id,
        String globalId,
        String title,
        Integer seasonNumber,
        LocalDate premiereDate,
        String synopsis,
        String s3Url,
        List<EpisodeResponseDTO> episodes) {}
