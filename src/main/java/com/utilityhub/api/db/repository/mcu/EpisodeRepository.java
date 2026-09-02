package com.utilityhub.api.db.repository.mcu;

import com.utilityhub.api.db.entity.mcu.Episode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EpisodeRepository extends JpaRepository<Episode, Long> {
    List<Episode> findByShowId(Long showId);
}
