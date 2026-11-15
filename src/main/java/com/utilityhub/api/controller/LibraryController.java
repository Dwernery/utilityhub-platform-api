package com.utilityhub.api.controller;

import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.utilityhub.api.dto.request.AttachFileRequestDTO;
import com.utilityhub.api.dto.request.AuthorCreateRequestDTO;
import com.utilityhub.api.dto.request.BookCreateRequestDTO;
import com.utilityhub.api.dto.request.BookUpdateRequestDTO;
import com.utilityhub.api.dto.request.SeriesCreateRequestDTO;
import com.utilityhub.api.dto.response.library.AttachFileResponseDTO;
import com.utilityhub.api.dto.response.library.AuthorResponseDTO;
import com.utilityhub.api.dto.response.library.BookResponseDTO;
import com.utilityhub.api.dto.response.library.PresignedUrlResponseDTO;
import com.utilityhub.api.dto.response.library.SeriesResponseDTO;
import com.utilityhub.api.service.LibraryService;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/library")
@Tag(name = "Library", description = "Books, authors and series")
public class LibraryController {

    private final LibraryService libraryService;

    public LibraryController(LibraryService libraryService) {
        this.libraryService = libraryService;
    }

    @GetMapping("/books")
    public List<BookResponseDTO> getAllBooks() {
        return libraryService.findAllBooks();
    }

    @PostMapping("/books")
    @ResponseStatus(HttpStatus.CREATED)
    public BookResponseDTO createBook(@Valid @RequestBody BookCreateRequestDTO newBook) {
        return libraryService.createBook(newBook);
    }

    /**
     * Partial update using JSON Merge Patch semantics: send only the fields to
     * change; an explicit {@code null} clears a field. Returns the updated book.
     */
    @PatchMapping(
            value = "/books/{id}",
            consumes = {MediaType.APPLICATION_JSON_VALUE, "application/merge-patch+json"})
    public BookResponseDTO editBook(
            @PathVariable Integer id,
            // A Map (not a DTO) so "field absent" and "field: null" stay distinguishable.
            // The annotation tells Swagger what shape the map is expected to have.
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                            description = "Any subset of these fields. An explicit null clears a field.",
                            content = @Content(schema = @Schema(implementation = BookUpdateRequestDTO.class)))
                    @RequestBody
                    Map<String, Object> patch) {
        return libraryService.patchBook(id, patch);
    }

    @DeleteMapping("/books/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBook(@PathVariable Integer id) {
        libraryService.deleteBook(id);
    }

    @GetMapping("/authors")
    public List<AuthorResponseDTO> getAllAuthors() {
        return libraryService.findAllAuthors();
    }

    @PostMapping("/authors")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthorResponseDTO createAuthor(@Valid @RequestBody AuthorCreateRequestDTO newAuthor) {
        return libraryService.createAuthor(newAuthor);
    }

    @GetMapping("/series")
    public List<SeriesResponseDTO> getAllSeries() {
        return libraryService.findAllSeries();
    }

    @PostMapping("/series")
    @ResponseStatus(HttpStatus.CREATED)
    public SeriesResponseDTO createSeries(@Valid @RequestBody SeriesCreateRequestDTO newSeries) {
        return libraryService.createSeries(newSeries);
    }

    @GetMapping("/books/upload-url")
    public ResponseEntity<PresignedUrlResponseDTO> generateUploadUrl(
            @RequestParam @NotBlank String fileName, @RequestParam @NotBlank String fileType) {
        var response = libraryService.generatePresignedUploadUrl(fileName, fileType);
        return ResponseEntity.ok(new PresignedUrlResponseDTO(response.presignedUrl(), response.s3Key()));
    }

    @PostMapping("/books/{bookId}/attach-file")
    public ResponseEntity<AttachFileResponseDTO> attachFileToBook(
            @PathVariable Integer bookId, @Valid @RequestBody AttachFileRequestDTO request) {
        String fileUrl = libraryService.attachFileToBook(bookId, request);
        return ResponseEntity.ok(new AttachFileResponseDTO(true, fileUrl, "File attached to book"));
    }
}
