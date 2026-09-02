package com.utilityhub.api.dto.response.mcu;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SeasonResponseDTO {
    private Integer seasonNumber;
    private List<EpisodeResponseDTO> episodes;
}
