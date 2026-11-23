package com.utilityhub.api.service;

import java.util.List;
import java.util.Optional;
import jakarta.validation.Validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.utilityhub.api.db.entity.library.Author;
import com.utilityhub.api.db.entity.library.ReadingGoal;
import com.utilityhub.api.db.entity.library.Series;
import com.utilityhub.api.db.repository.library.AuthorRepository;
import com.utilityhub.api.db.repository.library.BookRepository;
import com.utilityhub.api.db.repository.library.ReadingGoalRepository;
import com.utilityhub.api.db.repository.library.SeriesRepository;
import com.utilityhub.api.dto.request.AuthorCreateRequestDTO;
import com.utilityhub.api.dto.request.ReadingGoalRequestDTO;
import com.utilityhub.api.dto.request.SeriesCreateRequestDTO;
import com.utilityhub.api.dto.response.library.ReadingGoalResponseDTO;

import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** Author/series creation without duplicates, and reading goals. Repositories are mocks. */
class LibraryServiceNamesAndGoalsTest {

    private AuthorRepository authorRepository;
    private SeriesRepository seriesRepository;
    private ReadingGoalRepository goalRepository;
    private LibraryService service;

    @BeforeEach
    void setUp() {
        authorRepository = mock(AuthorRepository.class);
        seriesRepository = mock(SeriesRepository.class);
        goalRepository = mock(ReadingGoalRepository.class);
        service = new LibraryService(
                mock(BookRepository.class),
                authorRepository,
                seriesRepository,
                goalRepository,
                mock(S3Service.class),
                JsonMapper.builder().build(),
                Validation.buildDefaultValidatorFactory().getValidator());
        given(authorRepository.save(any())).willAnswer(invocation -> {
            Author author = invocation.getArgument(0);
            author.setId(7);
            return author;
        });
        given(seriesRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(goalRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void newAuthorIsTrimmedAndCreated() {
        given(authorRepository.findFirstByFullNameIgnoreCaseOrderByIdAsc("Brandon Sanderson"))
                .willReturn(Optional.empty());

        var result = service.createAuthor(new AuthorCreateRequestDTO("  Brandon Sanderson "));

        assertThat(result.isNew()).isTrue();
        assertThat(result.value().fullName()).isEqualTo("Brandon Sanderson");
        assertThat(result.value().id()).isEqualTo(7);
    }

    @Test
    void existingAuthorIsReturnedInsteadOfADuplicate() {
        Author existing = new Author();
        existing.setId(3);
        existing.setFullName("Brandon Sanderson");
        given(authorRepository.findFirstByFullNameIgnoreCaseOrderByIdAsc("brandon sanderson"))
                .willReturn(Optional.of(existing));

        var result = service.createAuthor(new AuthorCreateRequestDTO("brandon sanderson"));

        assertThat(result.isNew()).isFalse();
        assertThat(result.value().id()).isEqualTo(3);
        assertThat(result.value().fullName()).isEqualTo("Brandon Sanderson");
        verify(authorRepository, never()).save(any());
    }

    @Test
    void existingSeriesIsReturnedInsteadOfADuplicate() {
        Series existing = new Series();
        existing.setId(4);
        existing.setName("Mistborn");
        given(seriesRepository.findFirstByNameIgnoreCaseOrderByIdAsc("MISTBORN")).willReturn(Optional.of(existing));

        var result = service.createSeries(new SeriesCreateRequestDTO("MISTBORN"));

        assertThat(result.isNew()).isFalse();
        assertThat(result.value().name()).isEqualTo("Mistborn");
        verify(seriesRepository, never()).save(any());
    }

    @Test
    void setGoalCreatesOrReplacesTheYearsGoal() {
        given(goalRepository.findById(2026)).willReturn(Optional.empty());
        assertThat(service.setGoal(2026, new ReadingGoalRequestDTO(25)))
                .isEqualTo(new ReadingGoalResponseDTO(2026, 25));

        ReadingGoal existing = new ReadingGoal();
        existing.setYear(2026);
        existing.setTarget(25);
        given(goalRepository.findById(2026)).willReturn(Optional.of(existing));
        assertThat(service.setGoal(2026, new ReadingGoalRequestDTO(30)).target()).isEqualTo(30);
        assertThat(existing.getTarget()).isEqualTo(30);
    }

    @Test
    void goalsAreListedByYear() {
        ReadingGoal goal = new ReadingGoal();
        goal.setYear(2025);
        goal.setTarget(20);
        given(goalRepository.findAllByOrderByYearAsc()).willReturn(List.of(goal));

        assertThat(service.findAllGoals()).containsExactly(new ReadingGoalResponseDTO(2025, 20));
    }
}
