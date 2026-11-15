package com.utilityhub.api.db.repository.mcu;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.utilityhub.api.db.entity.mcu.Movie;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {}
