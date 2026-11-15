package com.utilityhub.api.db.repository.mcu;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.utilityhub.api.db.entity.mcu.Episode;

@Repository
public interface EpisodeRepository extends JpaRepository<Episode, Long> {

    /**
     * Every episode in one query, already in display order. The service groups
     * them by show in memory instead of running one query per show.
     * ({@code e.show.id} reads the foreign-key column; no join is needed.)
     */
    @Query("SELECT e FROM Episode e ORDER BY e.show.id, e.episodeNumber")
    List<Episode> findAllOrderedByShowAndEpisodeNumber();
}
