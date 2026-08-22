package com.utilityhub.api.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.utilityhub.api.db.entity.Series;

public interface SeriesRepository extends JpaRepository<Series, Integer> {

}
