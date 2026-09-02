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
public class MCUContentGroupedDTO {
    private List<DomainDTO> domains;
}
