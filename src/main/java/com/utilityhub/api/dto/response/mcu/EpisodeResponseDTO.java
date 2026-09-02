package com.utilityhub.api.dto.response.mcu;

import com.utilityhub.api.db.entity.mcu.WatchStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EpisodeResponseDTO {
    private Long id;
    private String globalId;
    private Integer episodeNumber;
    private String title;
    private Integer runtime;
    private WatchStatus status;
}
