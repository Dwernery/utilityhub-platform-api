package com.utilityhub.api.db.repository.library;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.utilityhub.api.db.entity.library.Series;

public interface SeriesRepository extends JpaRepository<Series, Integer> {

    /**
     * Case-insensitive, and the oldest one wins, so a name keeps working even if duplicates
     * were created before names were unique.
     */
    Optional<Series> findFirstByNameIgnoreCaseOrderByIdAsc(String name);
}
