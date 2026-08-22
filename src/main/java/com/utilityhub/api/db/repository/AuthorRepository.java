package com.utilityhub.api.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.utilityhub.api.db.entity.Author;

public interface AuthorRepository extends JpaRepository<Author, Integer> {
    
}
