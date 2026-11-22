package com.utilityhub.api.db.repository.mcu;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    List<Episode> findAllByShowIdOrderByEpisodeNumber(Long showId);

    /** Used to refuse two "episode 3"s in one season. */
    Optional<Episode> findFirstByShowIdAndEpisodeNumber(Long showId, Integer episodeNumber);

    /**
     * Removes a season's episodes before the season itself. The foreign key also
     * cascades, but doing it here keeps the behavior out of the schema.
     */
    @Modifying
    @Query("DELETE FROM Episode e WHERE e.show.id = :showId")
    int deleteByShowId(@Param("showId") Long showId);
}
