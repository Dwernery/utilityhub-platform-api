package com.utilityhub.api.dto.response.mcu;

import java.util.List;

public record SeasonResponseDTO(Integer seasonNumber, List<EpisodeResponseDTO> episodes) {}
