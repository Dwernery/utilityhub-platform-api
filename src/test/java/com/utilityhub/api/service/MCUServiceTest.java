package com.utilityhub.api.service;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import com.utilityhub.api.db.entity.mcu.Episode;
import com.utilityhub.api.db.entity.mcu.Movie;
import com.utilityhub.api.db.entity.mcu.Show;
import com.utilityhub.api.db.entity.mcu.WatchStatus;
import com.utilityhub.api.db.repository.mcu.EpisodeRepository;
import com.utilityhub.api.db.repository.mcu.MovieRepository;
import com.utilityhub.api.db.repository.mcu.ShowRepository;
import com.utilityhub.api.dto.request.EpisodeRequestDTO;
import com.utilityhub.api.dto.request.MovieRequestDTO;
import com.utilityhub.api.dto.request.ShowSeasonRequestDTO;
import com.utilityhub.api.exception.InvalidRequestException;
import com.utilityhub.api.exception.ResourceNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** Plain unit test with mocked repositories: the create/edit/delete rules. */
class MCUServiceTest {

    private MovieRepository movieRepository;
    private ShowRepository showRepository;
    private EpisodeRepository episodeRepository;
    private S3Service s3Service;
    private MCUService service;

    @BeforeEach
    void setUp() {
        movieRepository = mock(MovieRepository.class);
        showRepository = mock(ShowRepository.class);
        episodeRepository = mock(EpisodeRepository.class);
        s3Service = mock(S3Service.class);
        service = new MCUService(movieRepository, showRepository, episodeRepository, s3Service);
        given(movieRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(showRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(episodeRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
    }

    private static Show show(long id, String title, int season) {
        Show show = new Show();
        show.setId(id);
        show.setTitle(title);
        show.setSeason(season);
        show.setPremiereDate(LocalDate.of(2021, 6, 9));
        show.setDomain("Phase 4");
        return show;
    }

    private static ShowSeasonRequestDTO seasonRequest(String title, int season) {
        return new ShowSeasonRequestDTO(title, season, LocalDate.of(2023, 10, 5), "Synopsis.", "Phase 5");
    }

    @Test
    void createMovie_startsUnwatchedAndTrimsFields() {
        service.createMovie(new MovieRequestDTO(
                "  Thunderbolts*  ", LocalDate.of(2025, 5, 2), 127, " Antiheroes. ", " Phase 5 ", false));

        verify(movieRepository).save(argThat(m -> m.getStatus() == WatchStatus.UNWATCHED
                && m.getTitle().equals("Thunderbolts*")
                && m.getDomain().equals("Phase 5")
                && !m.getSpecial()));
    }

    @Test
    void updateMovie_keepsStatusAndPoster() {
        Movie movie = new Movie();
        movie.setId(7L);
        movie.setStatus(WatchStatus.WATCHED);
        movie.setS3Url("https://bucket/poster.jpg");
        given(movieRepository.findById(7L)).willReturn(Optional.of(movie));

        var dto = service.updateMovie(
                7L, new MovieRequestDTO("Werewolf by Night", LocalDate.of(2022, 10, 7), 53, "Monsters.", "Phase 4", true));

        assertThat(dto.status()).isEqualTo(WatchStatus.WATCHED);
        assertThat(dto.s3Url()).isEqualTo("https://bucket/poster.jpg");
        assertThat(dto.type().name()).isEqualTo("SPECIAL");
    }

    @Test
    void createShowSeason_duplicateTitleAndSeason_isRejected() {
        given(showRepository.findFirstByTitleIgnoreCaseAndSeason("Loki", 2)).willReturn(Optional.of(show(9, "Loki", 2)));

        assertThatThrownBy(() -> service.createShowSeason(seasonRequest("Loki", 2)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("'Loki' season 2 already exists");
        verify(showRepository, never()).save(any());
    }

    @Test
    void updateShowSeason_keepingItsOwnTitleAndSeason_isAllowed() {
        Show loki = show(9, "Loki", 2);
        given(showRepository.findById(9L)).willReturn(Optional.of(loki));
        given(showRepository.findFirstByTitleIgnoreCaseAndSeason("Loki", 2)).willReturn(Optional.of(loki));

        var dto = service.updateShowSeason(9L, seasonRequest("Loki", 2));

        assertThat(dto.synopsis()).isEqualTo("Synopsis.");
    }

    @Test
    void deleteShowSeason_deletesEpisodesFirst() {
        Show loki = show(9, "Loki", 2);
        given(showRepository.findById(9L)).willReturn(Optional.of(loki));

        service.deleteShowSeason(9L);

        InOrder order = inOrder(episodeRepository, showRepository);
        order.verify(episodeRepository).deleteByShowId(9L);
        order.verify(showRepository).delete(loki);
    }

    @Test
    void createEpisode_unknownShow_is404() {
        given(showRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.createEpisode(99L, new EpisodeRequestDTO(1, "Pilot", 50)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createEpisode_duplicateNumber_isRejected() {
        Show loki = show(9, "Loki", 2);
        Episode existing = new Episode();
        existing.setId(100L);
        given(showRepository.findById(9L)).willReturn(Optional.of(loki));
        given(episodeRepository.findFirstByShowIdAndEpisodeNumber(9L, 3)).willReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.createEpisode(9L, new EpisodeRequestDTO(3, "1893", 50)))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessage("Episode 3 already exists in this season");
    }

    @Test
    void createEpisode_startsUnwatched() {
        Show loki = show(9, "Loki", 2);
        given(showRepository.findById(9L)).willReturn(Optional.of(loki));
        given(episodeRepository.findFirstByShowIdAndEpisodeNumber(9L, 1)).willReturn(Optional.empty());

        var dto = service.createEpisode(9L, new EpisodeRequestDTO(1, " Ouroboros ", 50));

        assertThat(dto.status()).isEqualTo(WatchStatus.UNWATCHED);
        assertThat(dto.title()).isEqualTo("Ouroboros");
    }

    @Test
    void posterUploadUrl_usesUniqueKeyUnderPosterFolder() {
        service.generatePosterUploadUrl("Loki S2 (final).jpg", "image/jpeg");

        verify(s3Service).generateMcuPosterUploadUrl(
                argThat(key -> key.matches("mcu-posters/[0-9a-f-]{36}-Loki_S2__final_\\.jpg")), any());
    }
}
