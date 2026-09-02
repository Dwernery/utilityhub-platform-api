package com.utilityhub.api.db.repository.mcu;

import com.utilityhub.api.db.entity.mcu.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    List<Movie> findByDomain(String domain);
}
