package com.utilityhub.api;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.utilityhub.api.db.entity.Author;
import com.utilityhub.api.db.entity.Book;
import com.utilityhub.api.db.repository.AuthorRepository;
import com.utilityhub.api.db.repository.BookRepository;
import com.utilityhub.api.db.repository.SeriesRepository;
import com.utilityhub.api.dto.request.BookRequestDTO;
import com.utilityhub.api.service.LibraryService;

class PlatformApiApplicationTests {

	@Test
	void createBookWithoutSeriesAllowsNullSeriesName() {
		BookRepository bookRepository = mock(BookRepository.class);
		AuthorRepository authorRepository = mock(AuthorRepository.class);
		SeriesRepository seriesRepository = mock(SeriesRepository.class);

		Author author = new Author();
		author.setFullName("Jane Doe");
		when(authorRepository.findByFullName("Jane Doe")).thenReturn(Optional.of(author));

		LibraryService libraryService = new LibraryService(bookRepository, authorRepository, seriesRepository);
		BookRequestDTO request = new BookRequestDTO("Clean Title", 320, "Jane Doe", null, "9780000000001");

		assertDoesNotThrow(() -> libraryService.createBook(request));

		ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
		verify(bookRepository).save(captor.capture());
		assertNull(captor.getValue().getSeries());
	}

}
