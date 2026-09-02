package com.utilityhub.api.db.repository.library;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.utilityhub.api.db.entity.library.Author;

public interface AuthorRepository extends JpaRepository<Author, Integer> {

    Optional<Author> findByFullName(String authorName);
    
}
