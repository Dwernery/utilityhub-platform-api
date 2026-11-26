package com.utilityhub.api.db.repository.courses;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.utilityhub.api.db.entity.courses.CourseCreator;

public interface CourseCreatorRepository extends JpaRepository<CourseCreator, Integer> {

    /** Case-insensitive; the oldest wins, as with authors. */
    Optional<CourseCreator> findFirstByNameIgnoreCaseOrderByIdAsc(String name);

    List<CourseCreator> findAllByOrderByNameAsc();
}
