package com.utilityhub.api.db.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.utilityhub.api.db.entity.Author;

public interface AuthorRepository extends JpaRepository<Author, Integer> {

    Optional<Author> findByFullName(String authorName);
    
}
