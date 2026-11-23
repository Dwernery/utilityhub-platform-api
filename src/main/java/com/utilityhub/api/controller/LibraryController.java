package com.utilityhub.api.controller;

import java.util.List;
import java.util.Map;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.utilityhub.api.dto.request.AttachFileRequestDTO;
import com.utilityhub.api.dto.request.AuthorCreateRequestDTO;
import com.utilityhub.api.dto.request.BookCreateRequestDTO;
import com.utilityhub.api.dto.request.BookUpdateRequestDTO;
import com.utilityhub.api.dto.request.ReadingGoalRequestDTO;
import com.utilityhub.api.dto.request.SeriesCreateRequestDTO;
import com.utilityhub.api.dto.response.library.AttachFileResponseDTO;
import com.utilityhub.api.dto.response.library.AuthorResponseDTO;
import com.utilityhub.api.dto.response.library.BookResponseDTO;
import com.utilityhub.api.dto.response.library.PresignedUrlResponseDTO;
import com.utilityhub.api.dto.response.library.ReadingGoalResponseDTO;
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

    /** 201 with the new author, or 200 with the existing one if the name (ignoring case) is taken. */
    @PostMapping("/authors")
    public ResponseEntity<AuthorResponseDTO> createAuthor(@Valid @RequestBody AuthorCreateRequestDTO newAuthor) {
        return created(libraryService.createAuthor(newAuthor));
    }

    @GetMapping("/series")
    public List<SeriesResponseDTO> getAllSeries() {
        return libraryService.findAllSeries();
    }

    /** 201 with the new series, or 200 with the existing one if the name (ignoring case) is taken. */
    @PostMapping("/series")
    public ResponseEntity<SeriesResponseDTO> createSeries(@Valid @RequestBody SeriesCreateRequestDTO newSeries) {
        return created(libraryService.createSeries(newSeries));
    }

    @GetMapping("/goals")
    public List<ReadingGoalResponseDTO> getGoals() {
        return libraryService.findAllGoals();
    }

    /** Sets the number of books to finish in {@code year}, replacing any earlier goal. */
    @PutMapping("/goals/{year}")
    public ReadingGoalResponseDTO setGoal(
            @PathVariable @Min(2000) @Max(2200) Integer year, @Valid @RequestBody ReadingGoalRequestDTO request) {
        return libraryService.setGoal(year, request);
    }

    @DeleteMapping("/goals/{year}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGoal(@PathVariable @Min(2000) @Max(2200) Integer year) {
        libraryService.deleteGoal(year);
    }

    private static <T> ResponseEntity<T> created(LibraryService.Created<T> result) {
        return ResponseEntity.status(result.isNew() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(result.value());
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
