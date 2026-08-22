package com.utilityhub.api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.utilityhub.api.db.entity.Author;
import com.utilityhub.api.db.entity.Series;
import com.utilityhub.api.dto.request.AuthorRequestDTO;
import com.utilityhub.api.dto.request.BookRequestDTO;
import com.utilityhub.api.dto.request.SeriesRequestDTO;
import com.utilityhub.api.dto.response.BookResponseDTO;
import com.utilityhub.api.service.LibraryService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/library")
@Tag(name = "Library Controller", description = "Endpoints for managing library resources")
public class LibraryController {

    private LibraryService libraryService;

    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @GetMapping("/books")
    public ResponseEntity<List<BookResponseDTO>> getAllBooks() {
        try {
            List<BookResponseDTO> books = libraryService.findAllBooks();
            return ResponseEntity.ok(books);
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve books: " + e.getMessage());
        }
    }

    @PostMapping("/books")
    public ResponseEntity<String> createBook(@Valid @RequestBody BookRequestDTO newBook) {
        try {
            libraryService.createBook(newBook);
            return ResponseEntity.ok("Book created successfully");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create book: " + e.getMessage());
        }
    }

    @GetMapping("/authors")
    public ResponseEntity<List<Author>> getAllAuthors() {
        try {
            List<Author> authors = libraryService.findAllAuthors();
            return ResponseEntity.ok(authors);
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve authors: " + e.getMessage());
        }
    }

    @PostMapping("/authors")
    public ResponseEntity<String> createAuthor(@Valid @RequestBody AuthorRequestDTO newAuthor) {
        try {
            libraryService.createAuthor(newAuthor);
            return ResponseEntity.ok("Author created successfully");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create author: " + e.getMessage());
        }
    }

    @GetMapping("/series")
    public ResponseEntity<List<Series>> getAllSeries() {
        try {
            List<Series> series = libraryService.findAllSeries();
            return ResponseEntity.ok(series);
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve series: " + e.getMessage());
        }
    }

    @PostMapping("/series")
    public ResponseEntity<String> createSeries(@Valid @RequestBody SeriesRequestDTO newSeries) {
        try {
            libraryService.createSeries(newSeries);
            return ResponseEntity.ok("Series created successfully");
        } catch (Exception e) {
            throw new RuntimeException("Failed to create series: " + e.getMessage());
        }
    }

}
