package com.utilityhub.api.db.repository.library;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.utilityhub.api.db.entity.library.Book;
import com.utilityhub.api.dto.response.library.BookResponseDTO;

@Repository
public interface BookRepository extends JpaRepository<Book, Integer> {
    @Query("""
                SELECT new com.utilityhub.api.dto.response.library.BookResponseDTO(
                    b.id,
                    b.title,
                    b.pages,
                    b.status,
                    b.author.fullName,
                    b.series.name,
                    b.startDate,
                    b.endDate,
                    b.currentPage,
                    b.rating,
                    b.isbn13
                )
                FROM Book b
                LEFT JOIN b.author
                LEFT JOIN b.series
                ORDER BY b.author.fullName ASC, b.title ASC
            """)
    List<BookResponseDTO> findAllFlat();
}
