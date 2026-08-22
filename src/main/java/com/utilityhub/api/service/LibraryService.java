package com.utilityhub.api.service;

import java.util.List;
import org.springframework.stereotype.Service;
import com.utilityhub.api.db.entity.Author;
import com.utilityhub.api.db.entity.Book;
import com.utilityhub.api.db.entity.Series;
import com.utilityhub.api.db.repository.AuthorRepository;
import com.utilityhub.api.db.repository.BookRepository;
import com.utilityhub.api.db.repository.SeriesRepository;
import com.utilityhub.api.dto.request.AuthorRequestDTO;
import com.utilityhub.api.dto.request.BookRequestDTO;
import com.utilityhub.api.dto.request.SeriesRequestDTO;
import com.utilityhub.api.dto.response.BookResponseDTO;

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

}
