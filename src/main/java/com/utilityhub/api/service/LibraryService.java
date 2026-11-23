package com.utilityhub.api.service;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utilityhub.api.db.entity.library.Author;
import com.utilityhub.api.db.entity.library.Book;
import com.utilityhub.api.db.entity.library.ReadingGoal;
import com.utilityhub.api.db.entity.library.Series;
import com.utilityhub.api.db.repository.library.AuthorRepository;
import com.utilityhub.api.db.repository.library.BookRepository;
import com.utilityhub.api.db.repository.library.ReadingGoalRepository;
import com.utilityhub.api.db.repository.library.SeriesRepository;
import com.utilityhub.api.dto.request.AttachFileRequestDTO;
import com.utilityhub.api.dto.request.AuthorCreateRequestDTO;
import com.utilityhub.api.dto.request.BookCreateRequestDTO;
import com.utilityhub.api.dto.request.BookUpdateRequestDTO;
import com.utilityhub.api.dto.request.ReadingGoalRequestDTO;
import com.utilityhub.api.dto.request.SeriesCreateRequestDTO;
import com.utilityhub.api.dto.response.library.AuthorResponseDTO;
import com.utilityhub.api.dto.response.library.BookResponseDTO;
import com.utilityhub.api.dto.response.library.ReadingGoalResponseDTO;
import com.utilityhub.api.dto.response.library.SeriesResponseDTO;
import com.utilityhub.api.exception.InvalidRequestException;
import com.utilityhub.api.exception.ResourceNotFoundException;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@Service
@Transactional(readOnly = true)
public class LibraryService {

    private static final Logger log = LoggerFactory.getLogger(LibraryService.class);

    /** The JSON property names a PATCH may contain, derived from the DTO so they can't drift apart. */
    private static final Set<String> PATCHABLE_FIELDS = Arrays.stream(BookUpdateRequestDTO.class.getRecordComponents())
            .map(RecordComponent::getName)
            .collect(Collectors.toCollection(LinkedHashSet::new));

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final SeriesRepository seriesRepository;
    private final ReadingGoalRepository readingGoalRepository;
    private final S3Service s3Service;
    private final JsonMapper jsonMapper;
    private final Validator validator;

    public LibraryService(
            BookRepository bookRepository,
            AuthorRepository authorRepository,
            SeriesRepository seriesRepository,
            ReadingGoalRepository readingGoalRepository,
            S3Service s3Service,
            JsonMapper jsonMapper,
            Validator validator) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
        this.seriesRepository = seriesRepository;
        this.readingGoalRepository = readingGoalRepository;
        this.s3Service = s3Service;
        this.jsonMapper = jsonMapper;
        this.validator = validator;
    }

    public List<BookResponseDTO> findAllBooks() {
        return bookRepository.findAllFlat();
    }

    public List<AuthorResponseDTO> findAllAuthors() {
        return authorRepository.findAll().stream()
                .map(LibraryService::toResponse)
                .toList();
    }

    public List<SeriesResponseDTO> findAllSeries() {
        return seriesRepository.findAll().stream()
                .map(LibraryService::toResponse)
                .toList();
    }

    /** What a create returned, and whether it was new (201) or an existing match (200). */
    public record Created<T>(T value, boolean isNew) {}

    /**
     * Adds an author, unless one with the same name (ignoring case and surrounding spaces)
     * exists: then that one is returned, so "brandon sanderson" can't split an author in two.
     */
    @Transactional
    public Created<AuthorResponseDTO> createAuthor(AuthorCreateRequestDTO newAuthor) {
        String name = newAuthor.fullName().trim();
        var existing = authorRepository.findFirstByFullNameIgnoreCaseOrderByIdAsc(name);
        if (existing.isPresent()) {
            return new Created<>(toResponse(existing.get()), false);
        }
        Author author = new Author();
        author.setFullName(name);
        Author saved = authorRepository.save(author);
        log.info("Created author id={}", saved.getId());
        return new Created<>(toResponse(saved), true);
    }

    /** Adds a series, or returns the existing one with the same name (see {@link #createAuthor}). */
    @Transactional
    public Created<SeriesResponseDTO> createSeries(SeriesCreateRequestDTO newSeries) {
        String name = newSeries.name().trim();
        var existing = seriesRepository.findFirstByNameIgnoreCaseOrderByIdAsc(name);
        if (existing.isPresent()) {
            return new Created<>(toResponse(existing.get()), false);
        }
        Series series = new Series();
        series.setName(name);
        Series saved = seriesRepository.save(series);
        log.info("Created series id={}", saved.getId());
        return new Created<>(toResponse(saved), true);
    }

    public List<ReadingGoalResponseDTO> findAllGoals() {
        return readingGoalRepository.findAllByOrderByYearAsc().stream()
                .map(goal -> new ReadingGoalResponseDTO(goal.getYear(), goal.getTarget()))
                .toList();
    }

    /** Sets (or replaces) the number of books to finish in {@code year}. */
    @Transactional
    public ReadingGoalResponseDTO setGoal(int year, ReadingGoalRequestDTO request) {
        ReadingGoal goal = readingGoalRepository.findById(year).orElseGet(ReadingGoal::new);
        goal.setYear(year);
        goal.setTarget(request.target());
        ReadingGoal saved = readingGoalRepository.save(goal);
        log.info("Set reading goal year={} target={}", year, saved.getTarget());
        return new ReadingGoalResponseDTO(saved.getYear(), saved.getTarget());
    }

    /** Removes the goal for {@code year}; nothing happens if there is none. */
    @Transactional
    public void deleteGoal(int year) {
        readingGoalRepository.deleteById(year);
        log.info("Deleted reading goal year={}", year);
    }

    @Transactional
    public BookResponseDTO createBook(BookCreateRequestDTO newBook) {
        Book book = new Book();
        book.setTitle(newBook.title());
        book.setPages(newBook.pages());
        book.setAuthor(findAuthor(newBook.authorName()));
        book.setSeries(findSeriesOrNull(newBook.seriesName()));
        // A number only means something within a series.
        book.setSeriesPosition(book.getSeries() != null ? newBook.seriesPosition() : null);
        book.setStatus(Book.BookStatus.UNREAD);

        Book saved = bookRepository.save(book);
        log.info("Created book id={}", saved.getId());
        return toResponse(saved);
    }

    /**
     * Applies a JSON Merge Patch (RFC 7396): fields present in {@code patch}
     * replace the current value (an explicit {@code null} clears it); absent
     * fields are left unchanged.
     *
     * Steps: load the current state, merge the patch over it, validate the
     * merged result as a whole, then write it back to the entity.
     */
    @Transactional
    public BookResponseDTO patchBook(Integer id, Map<String, Object> patch) {
        Book book = bookRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Book", id));

        List<String> unknownFields = patch.keySet().stream()
                .filter(name -> !PATCHABLE_FIELDS.contains(name))
                .toList();
        if (!unknownFields.isEmpty()) {
            throw new InvalidRequestException("Unknown field(s): " + unknownFields + ". Allowed: " + PATCHABLE_FIELDS);
        }

        ObjectNode merged = jsonMapper.valueToTree(BookUpdateRequestDTO.from(book));
        merged.setAll((ObjectNode) jsonMapper.valueToTree(patch));
        // Throws MismatchedInputException (-> 400) for e.g. "pages": "abc" or an invalid date.
        BookUpdateRequestDTO update = jsonMapper.treeToValue(merged, BookUpdateRequestDTO.class);

        Set<ConstraintViolation<BookUpdateRequestDTO>> violations = validator.validate(update);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        book.setTitle(update.title());
        book.setPages(update.pages());
        book.setStatus(update.status());
        book.setStartDate(update.startDate());
        book.setEndDate(update.endDate());
        book.setCurrentPage(update.currentPage());
        // Business rule: a book that hasn't been read can't keep a rating, however it got back to UNREAD.
        book.setRating(update.status() == Book.BookStatus.UNREAD ? null : update.rating());
        // Only hit the database for lookups when the name actually changed.
        if (book.getAuthor() == null || !book.getAuthor().getFullName().equals(update.authorName())) {
            book.setAuthor(findAuthor(update.authorName()));
        }
        String seriesName = update.seriesName();
        String currentSeries = book.getSeries() != null ? book.getSeries().getName() : null;
        if (!Objects.equals(currentSeries, seriesName)) {
            book.setSeries(findSeriesOrNull(seriesName));
        }
        // A number only means something within a series: leaving the series clears it.
        book.setSeriesPosition(book.getSeries() != null ? update.seriesPosition() : null);

        Book saved = bookRepository.save(book);
        log.info("Updated book id={} fields={}", id, patch.keySet());
        return toResponse(saved);
    }

    @Transactional
    public void deleteBook(Integer id) {
        Book book = bookRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Book", id));
        bookRepository.delete(book);
        log.info("Deleted book id={}", id);
    }

    public S3Service.S3PresignedUrlResponse generatePresignedUploadUrl(String fileName, String fileType) {
        return s3Service.generatePresignedUploadUrl(fileName, fileType);
    }

    @Transactional
    public String attachFileToBook(Integer bookId, AttachFileRequestDTO request) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new ResourceNotFoundException("Book", bookId));

        String s3Url = s3Service.getPublicS3Url(request.s3Key());
        book.setS3Url(s3Url);
        bookRepository.save(book);
        log.info("Attached file to book id={}", bookId);

        return s3Url;
    }

    private Author findAuthor(String name) {
        return authorRepository
                .findFirstByFullNameIgnoreCaseOrderByIdAsc(name.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Author", name));
    }

    /** A blank series name means "no series". */
    private Series findSeriesOrNull(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return seriesRepository
                .findFirstByNameIgnoreCaseOrderByIdAsc(name.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Series", name));
    }

    private static AuthorResponseDTO toResponse(Author author) {
        return new AuthorResponseDTO(author.getId(), author.getFullName());
    }

    private static SeriesResponseDTO toResponse(Series series) {
        return new SeriesResponseDTO(series.getId(), series.getName());
    }

    private static BookResponseDTO toResponse(Book book) {
        return new BookResponseDTO(
                book.getId(),
                book.getTitle(),
                book.getPages(),
                book.getStatus(),
                book.getAuthor() != null ? book.getAuthor().getFullName() : null,
                book.getSeries() != null ? book.getSeries().getName() : null,
                book.getSeriesPosition(),
                book.getStartDate(),
                book.getEndDate(),
                book.getCurrentPage(),
                book.getRating(),
                book.getS3Url());
    }
}
