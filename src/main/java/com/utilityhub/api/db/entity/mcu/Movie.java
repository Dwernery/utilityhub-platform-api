package com.utilityhub.api.db.entity.mcu;

import java.sql.Date;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "movies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, name="premiere_date")
    private Date premiereDate;

    @Column
    private Integer runtime;

    @Column(columnDefinition = "TEXT")
    private String synopsis;

    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    private WatchStatus status;

    @Column(name = "s3_url", length = 255)
    private String s3Url;

    @Column(length = 255)
    private String domain;

    @Column(name = "is_special_pres")
    private Boolean isSpecialPres;
}
