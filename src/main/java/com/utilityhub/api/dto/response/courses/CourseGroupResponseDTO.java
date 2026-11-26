package com.utilityhub.api.dto.response.courses;

/** A group's stats aren't here: the apps sum its courses, so they never go stale. */
public record CourseGroupResponseDTO(Integer id, String name, Integer position) {}
