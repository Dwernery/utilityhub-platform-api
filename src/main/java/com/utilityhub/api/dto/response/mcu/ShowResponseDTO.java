package com.utilityhub.api.dto.response.mcu;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.sql.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShowResponseDTO {
    private Long id;
    private String globalId;
    private String title;
    private Date premiereDate;
    private String synopsis;
    private String s3Url;
    private List<SeasonResponseDTO> seasons;
}
