package com.utilityhub.api.controller;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.utilityhub.api.db.entity.mcu.WatchStatus;
import com.utilityhub.api.dto.response.mcu.ContentType;
import com.utilityhub.api.dto.response.mcu.DomainResponseDTO;
import com.utilityhub.api.dto.response.mcu.EpisodeResponseDTO;
import com.utilityhub.api.dto.response.mcu.MCUTrackerResponseDTO;
import com.utilityhub.api.dto.response.mcu.MovieResponseDTO;
import com.utilityhub.api.dto.response.mcu.SeasonResponseDTO;
import com.utilityhub.api.dto.response.mcu.ShowResponseDTO;
import com.utilityhub.api.service.S3Service;
import com.utilityhub.api.service.MCUService;
import com.utilityhub.api.support.WithApiToken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@WebMvcTest(MCUController.class)
@Import(WithApiToken.class)
class MCUControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private MCUService mcuService;

    @Test
    void getTracker_returnsGroupedContent() {
        given(mcuService.getAllContentGroupedByDomain())
                .willReturn(new MCUTrackerResponseDTO(
                        List.of(new DomainResponseDTO("Infinity Saga", List.of(), List.of()))));

        assertThat(mvc.get().uri("/api/mcu/tracker"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.domains[0].domainName")
                .isEqualTo("Infinity Saga");
    }

    @Test
    void updateStatus_returnsNoContent() {
        assertThat(mvc.put()
                        .uri("/api/mcu/tracker/movie:1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"status":"WATCHED"}
                        """))
                .hasStatus(204);

        verify(mcuService).updateContentStatus("movie:1", WatchStatus.WATCHED);
    }

    @Test
    void createMovie_returnsCreatedMovie() {
        given(mcuService.createMovie(any()))
                .willReturn(new MovieResponseDTO(
                        "movie:40", "Thunderbolts*", LocalDate.of(2025, 5, 2), 127, "Antiheroes.",
                        WatchStatus.UNWATCHED, null, ContentType.MOVIE));

        assertThat(mvc.post()
                        .uri("/api/mcu/movies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"title":"Thunderbolts*","premiereDate":"2025-05-02","runtime":127,
                         "synopsis":"Antiheroes.","domain":"Phase 5","special":false}
                        """))
                .hasStatus(201)
                .bodyJson()
                .extractingPath("$.globalId")
                .isEqualTo("movie:40");

        verify(mcuService).createMovie(argThat(r -> r.domain().equals("Phase 5") && !r.special()));
    }

    @Test
    void updateMovie_takesIdFromPath() {
        given(mcuService.updateMovie(eq(7L), any()))
                .willReturn(new MovieResponseDTO(
                        "movie:7", "Werewolf by Night", LocalDate.of(2022, 10, 7), 53, "Monsters.",
                        WatchStatus.WATCHED, null, ContentType.SPECIAL));

        assertThat(mvc.put()
                        .uri("/api/mcu/movies/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"title":"Werewolf by Night","premiereDate":"2022-10-07","runtime":53,
                         "synopsis":"Monsters.","domain":"Phase 4","special":true}
                        """))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.type")
                .isEqualTo("SPECIAL");
    }

    @Test
    void deleteMovie_returnsNoContent() {
        assertThat(mvc.delete().uri("/api/mcu/movies/7")).hasStatus(204);
        verify(mcuService).deleteMovie(7L);
    }

    @Test
    void createShowSeason_returnsSeasonWithNoEpisodes() {
        given(mcuService.createShowSeason(any()))
                .willReturn(new ShowResponseDTO(
                        "show:15", "Daredevil: Born Again", LocalDate.of(2025, 3, 4), "Matt Murdock returns.", null,
                        List.of(new SeasonResponseDTO(1, List.of()))));

        assertThat(mvc.post()
                        .uri("/api/mcu/shows")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"title":"Daredevil: Born Again","season":1,"premiereDate":"2025-03-04",
                         "synopsis":"Matt Murdock returns.","domain":"Phase 5"}
                        """))
                .hasStatus(201)
                .bodyJson()
                .extractingPath("$.seasons[0].seasonNumber")
                .isEqualTo(1);
    }

    @Test
    void deleteShowSeason_returnsNoContent() {
        assertThat(mvc.delete().uri("/api/mcu/shows/15")).hasStatus(204);
        verify(mcuService).deleteShowSeason(15L);
    }

    @Test
    void createEpisode_takesShowIdFromPath() {
        given(mcuService.createEpisode(eq(15L), any()))
                .willReturn(new EpisodeResponseDTO(200L, "episode:200", 1, "Heaven's Half Hour", 50, WatchStatus.UNWATCHED));

        assertThat(mvc.post()
                        .uri("/api/mcu/shows/15/episodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"episodeNumber":1,"title":"Heaven's Half Hour","runtime":50}
                        """))
                .hasStatus(201)
                .bodyJson()
                .extractingPath("$.globalId")
                .isEqualTo("episode:200");
    }

    @Test
    void updateAndDeleteEpisode_takeIdFromPath() {
        given(mcuService.updateEpisode(eq(200L), any()))
                .willReturn(new EpisodeResponseDTO(200L, "episode:200", 2, "Optics", 45, WatchStatus.WATCHED));

        assertThat(mvc.put()
                        .uri("/api/mcu/episodes/200")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"episodeNumber":2,"title":"Optics","runtime":45}
                        """))
                .hasStatusOk();
        assertThat(mvc.delete().uri("/api/mcu/episodes/200")).hasStatus(204);

        verify(mcuService).deleteEpisode(200L);
    }

    @Test
    void posterUploadUrl_returnsPresignedUrlAndKey() {
        given(mcuService.generatePosterUploadUrl("loki.jpg", "image/jpeg"))
                .willReturn(new S3Service.S3PresignedUrlResponse("https://s3.example/put", "mcu-posters/x-loki.jpg"));

        assertThat(mvc.get().uri("/api/mcu/poster-upload-url?fileName=loki.jpg&fileType=image/jpeg"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.s3Key")
                .isEqualTo("mcu-posters/x-loki.jpg");
    }

    @Test
    void attachShowPoster_returnsPublicUrl() {
        given(mcuService.attachShowPoster(15L, "mcu-posters/x-loki.jpg")).willReturn("https://bucket/mcu-posters/x-loki.jpg");

        assertThat(mvc.post()
                        .uri("/api/mcu/shows/15/poster")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"s3Key":"mcu-posters/x-loki.jpg"}
                        """))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.fileUrl")
                .isEqualTo("https://bucket/mcu-posters/x-loki.jpg");
    }
}
