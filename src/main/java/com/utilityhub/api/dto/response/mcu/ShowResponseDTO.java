package com.utilityhub.api.dto.response.mcu;

import java.time.LocalDate;
import java.util.List;

public record ShowResponseDTO(
        String globalId,
        String title,
        LocalDate premiereDate,
        String synopsis,
        String s3Url,
        List<SeasonResponseDTO> seasons) {}
