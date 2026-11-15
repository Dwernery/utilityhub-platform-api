package com.utilityhub.api.dto.request;

import jakarta.validation.constraints.NotNull;

import com.utilityhub.api.db.entity.mcu.WatchStatus;

/** Body of PUT /tracker/{globalId}/status; the URL identifies which item. */
public record ContentStatusUpdateRequestDTO(@NotNull WatchStatus status) {}
