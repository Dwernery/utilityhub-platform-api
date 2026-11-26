package com.utilityhub.api.db.entity.courses;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/** A custom group of courses. A course is in at most one group. */
@Entity
@Table(name = "course_groups")
@Getter
@Setter
public class CourseGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name")
    private String name;

    /** Display order chosen by the user, 0 first. */
    @Column(name = "position")
    private Integer position;
}
