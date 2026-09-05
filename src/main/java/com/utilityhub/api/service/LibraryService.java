package com.utilityhub.api.service;

import java.util.List;
import org.springframework.stereotype.Service;

import com.utilityhub.api.db.entity.library.Author;
import com.utilityhub.api.db.entity.library.Book;
import com.utilityhub.api.db.entity.library.Series;
import com.utilityhub.api.db.repository.library.AuthorRepository;
import com.utilityhub.api.db.repository.library.BookRepository;
import com.utilityhub.api.db.repository.library.SeriesRepository;
import com.utilityhub.api.dto.request.AuthorRequestDTO;
import com.utilityhub.api.dto.request.BookRequestDTO;
import com.utilityhub.api.dto.request.SeriesRequestDTO;
import com.utilityhub.api.dto.response.library.BookResponseDTO;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;

@Service
public class LibraryService {

    private BookRepository bookRepository;
    private AuthorRepository authorRepository;
    private SeriesRepository seriesRepository;

    public LibraryService(BookRepository bookRepository, AuthorRepository authorRepository,
            SeriesRepository seriesRepository) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
        this.seriesRepository = seriesRepository;
    }

    public List<BookResponseDTO> findAllBooks() {
        return bookRepository.findAllFlat();
    }

    public List<Author> findAllAuthors() {
        return authorRepository.findAll();
    }

    public List<Series> findAllSeries() {
        return seriesRepository.findAll();
    }

    public void createAuthor(AuthorRequestDTO newAuthor) {
        Author author = new Author();
        author.setFullName(newAuthor.fullName());
        authorRepository.save(author);
    }

    public void createSeries(SeriesRequestDTO newSeries) {
        Series series = new Series();
        series.setName(newSeries.name());
        seriesRepository.save(series);
    }

    public void createBook(BookRequestDTO newBook) {
        Author author = authorRepository.findByFullName(newBook.authorName())
                .orElseThrow(() -> new RuntimeException("Author not found: " + newBook.authorName()));

        Series series = null;
        if (newBook.seriesName() != null && !newBook.seriesName().isBlank()) {
            series = seriesRepository.findByName(newBook.seriesName())
                    .orElseThrow(() -> new RuntimeException("Series not found: " + newBook.seriesName()));
        }

        Book book = new Book();
        book.setTitle(newBook.title());
        book.setPages(newBook.pages());
        book.setIsbn13(newBook.isbn13());
        book.setAuthor(author);
        book.setSeries(series);
        book.setStatus(Book.BookStatus.UNREAD);

        bookRepository.save(book);
    }

    public void patchBook(String id, JsonNode payload) {
        Book book = bookRepository.findById(Integer.parseInt(id))
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));

        if (payload.has("title")) {
            JsonNode n = payload.get("title");
            book.setTitle(n.isNull() ? null : n.asText());
        }

        if (payload.has("pages")) {
            JsonNode n = payload.get("pages");
            book.setPages(n.isNull() ? null : n.asInt());
        }

        if (payload.has("isbn13")) {
            JsonNode n = payload.get("isbn13");
            book.setIsbn13(n.isNull() ? null : n.asText());
        }

        if (payload.has("status")) {
            JsonNode n = payload.get("status");
            if (n.isNull()) {
                book.setStatus(null);
            } else {
                try {
                    book.setStatus(Book.BookStatus.valueOf(n.asText().toUpperCase()));
                } catch (IllegalArgumentException ex) {
                    throw new RuntimeException("Invalid status value: " + n.asText());
                }
            }
        }

        if (payload.has("startDate")) {
            JsonNode n = payload.get("startDate");
            book.setStartDate(n.isNull() ? null : LocalDate.parse(n.asText()));
        }

        if (payload.has("endDate")) {
            JsonNode n = payload.get("endDate");
            book.setEndDate(n.isNull() ? null : LocalDate.parse(n.asText()));
        }

        if (payload.has("authorName")) {
            JsonNode n = payload.get("authorName");
            if (n.isNull()) {
                book.setAuthor(null);
            } else {
                String name = n.asText();
                Author author = authorRepository.findByFullName(name)
                        .orElseThrow(() -> new RuntimeException("Author not found: " + name));
                book.setAuthor(author);
            }
        }

        if (payload.has("seriesName")) {
            JsonNode n = payload.get("seriesName");
            if (n.isNull()) {
                book.setSeries(null);
            } else {
                String sname = n.asText();
                if (!sname.isBlank()) {
                    Series series = seriesRepository.findByName(sname)
                            .orElseThrow(() -> new RuntimeException("Series not found: " + sname));
                    book.setSeries(series);
                } else {
                    book.setSeries(null);
                }
            }
        }

        if (payload.has("currentPage")) {
            JsonNode n = payload.get("currentPage");
            book.setCurrentPage(n.isNull() ? null : n.asInt());
        }

        if (payload.has("rating")) {
            JsonNode n = payload.get("rating");
            book.setRating(n.isNull() ? null : n.asInt());
        }

        bookRepository.save(book);
    }

    public void editBookRating(String id, Integer newRating) {
        Book book = bookRepository.findById(Integer.parseInt(id))
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));
        book.setRating(newRating);
        bookRepository.save(book);
    }

    public void deleteBook(String id) {
        Book book = bookRepository.findById(Integer.parseInt(id))
                .orElseThrow(() -> new RuntimeException("Book not found with id: " + id));
        bookRepository.delete(book);
    }

}
