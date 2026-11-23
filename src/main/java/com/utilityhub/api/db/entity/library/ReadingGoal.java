package com.utilityhub.api.db.entity.library;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/** How many books to finish in a year. One row per year that has a goal. */
@Entity
@Table(name = "reading_goals")
@Getter
@Setter
public class ReadingGoal {

    @Id
    @Column(name = "year")
    private Integer year;

    @Column(name = "target")
    private Integer target;
}
