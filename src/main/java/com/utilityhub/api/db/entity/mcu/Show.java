package com.utilityhub.api.db.entity.mcu;

import java.sql.Date;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "shows")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Show {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, name = "premiere_date")
    private Date premiereDate;

    @Column(columnDefinition = "TEXT")
    private String synopsis;

    @Column(name = "s3_url", length = 255)
    private String s3Url;

    @Column(length = 255)
    private String domain;

    @Column(nullable = false)
    private Integer season;
}
