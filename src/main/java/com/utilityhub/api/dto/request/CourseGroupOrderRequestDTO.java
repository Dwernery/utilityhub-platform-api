package com.utilityhub.api.dto.request;

import java.util.List;
import jakarta.validation.constraints.NotNull;

/** Every group's id, in the new display order. */
public record CourseGroupOrderRequestDTO(@NotNull List<@NotNull Integer> groupIds) {}
