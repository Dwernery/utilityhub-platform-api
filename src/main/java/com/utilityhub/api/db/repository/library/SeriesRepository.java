package com.utilityhub.api.db.repository.library;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.utilityhub.api.db.entity.library.Series;

public interface SeriesRepository extends JpaRepository<Series, Integer> {

    Optional<Series> findByName(String seriesName);

}
