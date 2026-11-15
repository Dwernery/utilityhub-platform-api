package com.utilityhub.api.dto.response.mcu;

import java.util.List;

public record DomainResponseDTO(String domainName, List<MovieResponseDTO> movies, List<ShowResponseDTO> shows) {}
