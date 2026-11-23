package com.utilityhub.api.db.repository.library;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.utilityhub.api.db.entity.library.Author;

public interface AuthorRepository extends JpaRepository<Author, Integer> {

    /**
     * Case-insensitive, and the oldest one wins, so a name keeps working even if duplicates
     * were created before names were unique.
     */
    Optional<Author> findFirstByFullNameIgnoreCaseOrderByIdAsc(String fullName);
}
