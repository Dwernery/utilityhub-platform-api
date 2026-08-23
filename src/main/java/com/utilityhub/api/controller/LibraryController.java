package com.utilityhub.api.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.utilityhub.api.db.entity.Author;
import com.utilityhub.api.db.entity.Series;
import com.utilityhub.api.dto.request.AuthorRequestDTO;
// BookEditRequestDTO removed; PATCH endpoint uses JsonNode for partial updates
import com.utilityhub.api.dto.request.BookRequestDTO;
import com.utilityhub.api.dto.request.SeriesRequestDTO;
import com.utilityhub.api.dto.response.BookResponseDTO;
import com.utilityhub.api.service.LibraryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
// Removed PutMapping: rating updates are handled via PATCH now
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api/library")
@Tag(name = "Library Controller", description = "Endpoints for managing library resources")
public class LibraryController {

    private LibraryService libraryService;
    private ObjectMapper objectMapper;

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

    @PatchMapping("/books/{id}")
    public ResponseEntity<String> editBook(@PathVariable String id, @RequestBody(required = false) String rawBody) {
        try {
            if (rawBody == null || rawBody.isBlank()) {
                throw new RuntimeException("Empty request body");
            }

            if (this.objectMapper == null) {
                this.objectMapper = new ObjectMapper();
            }

            JsonNode payload;
            try {
                payload = objectMapper.readTree(rawBody);
            } catch (IOException ex) {
                throw new RuntimeException("Invalid JSON body: " + ex.getMessage());
            }

            // If client sent a bare number (e.g. 3), normalize to { "rating": 3 }
            if (payload.isNumber()) {
                ObjectNode node = objectMapper.createObjectNode();
                node.set("rating", payload);
                payload = node;
            }

            if (!payload.isObject()) {
                throw new RuntimeException("Expected a JSON object payload");
            }

            libraryService.patchBook(id, payload);
            return ResponseEntity.ok("Book updated successfully");
        } catch (Exception e) {
            throw new RuntimeException("Failed to update book: " + e.getMessage());
        }
    }

    @DeleteMapping("/books/{id}")
    public ResponseEntity<String> deleteBook(@PathVariable String id) {
        try {
            libraryService.deleteBook(id);
            return ResponseEntity.ok("Book deleted successfully");
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete book: " + e.getMessage());
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
