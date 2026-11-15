package com.utilityhub.api.db.entity.mcu;

import java.time.LocalDate;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "shows")
@Getter
@Setter
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, name = "premiere_date")
    private LocalDate premiereDate;

    @Column(columnDefinition = "TEXT")
    private String synopsis;

    @Column(name = "s3_url", length = 255)
    private String s3Url;

    @Column(length = 255)
    private String domain;

    @Column(nullable = false)
    private Integer season;
}
