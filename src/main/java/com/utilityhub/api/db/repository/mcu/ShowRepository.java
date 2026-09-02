package com.utilityhub.api.db.repository.mcu;

import com.utilityhub.api.db.entity.mcu.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {
    List<Show> findByDomain(String domain);
}
