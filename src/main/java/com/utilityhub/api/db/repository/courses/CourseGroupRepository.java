package com.utilityhub.api.db.repository.courses;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.utilityhub.api.db.entity.courses.CourseGroup;

public interface CourseGroupRepository extends JpaRepository<CourseGroup, Integer> {

    Optional<CourseGroup> findFirstByNameIgnoreCaseOrderByIdAsc(String name);

    List<CourseGroup> findAllByOrderByPositionAscIdAsc();

    /** The last group, so a new one can go after it. */
    Optional<CourseGroup> findTopByOrderByPositionDesc();
}
