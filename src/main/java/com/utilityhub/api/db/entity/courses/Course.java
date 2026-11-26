package com.utilityhub.api.db.entity.courses;

import java.time.Instant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/** A learning course. Progress is a lesson count: done when completedLessons equals totalLessons. */
@Entity
@Table(name = "courses")
@Getter
@Setter
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "title")
    private String title;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id")
    private CourseCreator creator;

    /** Null when the course isn't in a group. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    private CourseGroup group;

    @Column(name = "total_lessons")
    private Integer totalLessons;

    @Column(name = "completed_lessons")
    private Integer completedLessons;

    /** Where to take the course, if it's online. */
    @Column(name = "url")
    private String url;

    /** Cover image. */
    @Column(name = "s3_url")
    private String s3Url;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
