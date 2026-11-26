package com.utilityhub.api.db.repository.courses;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.utilityhub.api.db.entity.courses.Course;
import com.utilityhub.api.dto.response.courses.CourseResponseDTO;

public interface CourseRepository extends JpaRepository<Course, Integer> {

    @Query("""
                SELECT new com.utilityhub.api.dto.response.courses.CourseResponseDTO(
                    c.id,
                    c.title,
                    cr.name,
                    g.id,
                    c.totalLessons,
                    c.completedLessons,
                    c.url,
                    c.s3Url,
                    c.createdAt
                )
                FROM Course c
                JOIN c.creator cr
                LEFT JOIN c.group g
                ORDER BY c.title ASC, c.id ASC
            """)
    List<CourseResponseDTO> findAllFlat();

    /** How many courses a creator has (one with courses can't be deleted). */
    long countByCreator_Id(Integer creatorId);

    /** Takes every course out of a group (before the group is deleted). */
    @Modifying
    @Query("UPDATE Course c SET c.group = null WHERE c.group.id = :groupId")
    int ungroupAll(@Param("groupId") Integer groupId);
}
