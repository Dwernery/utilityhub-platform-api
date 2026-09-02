package com.utilityhub.api.dto.request;

import com.utilityhub.api.db.entity.mcu.WatchStatus;

public record UpdateContentStatusRequestDTO(
        String globalId,
        WatchStatus status) {
}
