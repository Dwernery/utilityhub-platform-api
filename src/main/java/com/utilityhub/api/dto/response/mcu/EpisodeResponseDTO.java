package com.utilityhub.api.dto.response.mcu;

import com.utilityhub.api.db.entity.mcu.WatchStatus;

public record EpisodeResponseDTO(
        Long id, String globalId, Integer episodeNumber, String title, Integer runtime, WatchStatus status) {}
