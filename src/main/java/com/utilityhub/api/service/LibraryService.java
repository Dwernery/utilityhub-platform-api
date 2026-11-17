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
import com.utilityhub.api.db.entity.library.Series;
import com.utilityhub.api.db.repository.library.AuthorRepository;
import com.utilityhub.api.db.repository.library.BookRepository;
import com.utilityhub.api.db.repository.library.SeriesRepository;
import com.utilityhub.api.dto.request.AttachFileRequestDTO;
import com.utilityhub.api.dto.request.AuthorCreateRequestDTO;
import com.utilityhub.api.dto.request.BookCreateRequestDTO;
import com.utilityhub.api.dto.request.BookUpdateRequestDTO;
import com.utilityhub.api.dto.request.SeriesCreateRequestDTO;
import com.utilityhub.api.dto.response.library.AuthorResponseDTO;
import com.utilityhub.api.dto.response.library.BookResponseDTO;
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
    private final S3Service s3Service;
    private final JsonMapper jsonMapper;
    private final Validator validator;

    public LibraryService(
            BookRepository bookRepository,
            AuthorRepository authorRepository,
            SeriesRepository seriesRepository,
            S3Service s3Service,
            JsonMapper jsonMapper,
            Validator validator) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
        this.seriesRepository = seriesRepository;
        this.s3Service = s3Service;
        this.jsonMapper = jsonMapper;
        this.validator = validator;
    }

    public List<BookResponseDTO> findAllBooks() {
        return bookRepository.findAllFlat();
    }

    public List<AuthorResponseDTO> findAllAuthors() {
        return authorRepository.findAll().stream()
                .map(author -> new AuthorResponseDTO(author.getId(), author.getFullName()))
                .toList();
    }

    public List<SeriesResponseDTO> findAllSeries() {
        return seriesRepository.findAll().stream()
                .map(series -> new SeriesResponseDTO(series.getId(), series.getName()))
                .toList();
    }

    @Transactional
    public AuthorResponseDTO createAuthor(AuthorCreateRequestDTO newAuthor) {
        Author author = new Author();
        author.setFullName(newAuthor.fullName().trim());
        Author saved = authorRepository.save(author);
        log.info("Created author id={}", saved.getId());
        return new AuthorResponseDTO(saved.getId(), saved.getFullName());
    }

    @Transactional
    public SeriesResponseDTO createSeries(SeriesCreateRequestDTO newSeries) {
        Series series = new Series();
        series.setName(newSeries.name().trim());
        Series saved = seriesRepository.save(series);
        log.info("Created series id={}", saved.getId());
        return new SeriesResponseDTO(saved.getId(), saved.getName());
    }

    @Transactional
    public BookResponseDTO createBook(BookCreateRequestDTO newBook) {
        Book book = new Book();
        book.setTitle(newBook.title());
        book.setPages(newBook.pages());
        book.setAuthor(findAuthor(newBook.authorName()));
        book.setSeries(findSeriesOrNull(newBook.seriesName()));
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
        return authorRepository.findByFullName(name).orElseThrow(() -> new ResourceNotFoundException("Author", name));
    }

    /** A blank series name means "no series". */
    private Series findSeriesOrNull(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return seriesRepository.findByName(name).orElseThrow(() -> new ResourceNotFoundException("Series", name));
    }

    private static BookResponseDTO toResponse(Book book) {
        return new BookResponseDTO(
                book.getId(),
                book.getTitle(),
                book.getPages(),
                book.getStatus(),
                book.getAuthor() != null ? book.getAuthor().getFullName() : null,
                book.getSeries() != null ? book.getSeries().getName() : null,
                book.getStartDate(),
                book.getEndDate(),
                book.getCurrentPage(),
                book.getRating(),
                book.getS3Url());
    }
}
