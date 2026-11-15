package com.utilityhub.api.dto.response.mcu;

import java.time.LocalDate;

import com.utilityhub.api.db.entity.mcu.WatchStatus;

public record MovieResponseDTO(
        String globalId,
        String title,
        LocalDate premiereDate,
        Integer runtime,
        String synopsis,
        WatchStatus status,
        String s3Url,
        ContentType type) {}
