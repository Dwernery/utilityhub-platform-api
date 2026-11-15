package com.utilityhub.api.db.entity.mcu;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "episodes")
@Getter
@Setter
public class Episode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @Column(nullable = false)
    private Integer episodeNumber;

    @Column
    private String title;

    @Column
    private Integer runtime;

    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    private WatchStatus status;
}
