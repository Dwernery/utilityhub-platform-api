package com.utilityhub.api.db.repository.mcu;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.utilityhub.api.db.entity.mcu.Show;

@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {

    /** Used to refuse a second "Loki" season 2. */
    Optional<Show> findFirstByTitleIgnoreCaseAndSeason(String title, Integer season);
}
