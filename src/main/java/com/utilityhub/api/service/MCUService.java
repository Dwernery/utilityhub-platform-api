package com.utilityhub.api.service;

import com.utilityhub.api.db.entity.mcu.Episode;
import com.utilityhub.api.db.entity.mcu.Movie;
import com.utilityhub.api.db.entity.mcu.Show;
import com.utilityhub.api.db.repository.mcu.EpisodeRepository;
import com.utilityhub.api.db.repository.mcu.MovieRepository;
import com.utilityhub.api.db.repository.mcu.ShowRepository;
import com.utilityhub.api.dto.request.UpdateContentStatusRequestDTO;
import com.utilityhub.api.dto.response.mcu.ContentType;
import com.utilityhub.api.dto.response.mcu.DomainDTO;
import com.utilityhub.api.dto.response.mcu.EpisodeResponseDTO;
import com.utilityhub.api.dto.response.mcu.MCUContentGroupedDTO;
import com.utilityhub.api.dto.response.mcu.MovieResponseDTO;
import com.utilityhub.api.dto.response.mcu.SeasonResponseDTO;
import com.utilityhub.api.dto.response.mcu.ShowResponseDTO;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MCUService {

    private final MovieRepository movieRepository;
    private final ShowRepository showRepository;
    private final EpisodeRepository episodeRepository;

    public MCUService(MovieRepository movieRepository, ShowRepository showRepository,
            EpisodeRepository episodeRepository) {
        this.movieRepository = movieRepository;
        this.showRepository = showRepository;
        this.episodeRepository = episodeRepository;
    }

    public MCUContentGroupedDTO getAllContentGroupedByDomain() {
        List<Movie> movies = movieRepository.findAll().stream()
                .sorted((m1, m2) -> {
                    if (m1.getPremiereDate() == null && m2.getPremiereDate() == null)
                        return 0;
                    if (m1.getPremiereDate() == null)
                        return 1;
                    if (m2.getPremiereDate() == null)
                        return -1;
                    return m1.getPremiereDate().compareTo(m2.getPremiereDate());
                })
                .collect(Collectors.toList());
        List<Show> shows = showRepository.findAll().stream()
                .sorted((s1, s2) -> {
                    if (s1.getPremiereDate() == null && s2.getPremiereDate() == null)
                        return 0;
                    if (s1.getPremiereDate() == null)
                        return 1;
                    if (s2.getPremiereDate() == null)
                        return -1;
                    return s1.getPremiereDate().compareTo(s2.getPremiereDate());
                })
                .collect(Collectors.toList());

        // Group by domain preserving insertion order
        Map<String, List<Movie>> moviesByDomain = movies.stream()
                .collect(Collectors.groupingBy(Movie::getDomain, java.util.LinkedHashMap::new, Collectors.toList()));

        // Group shows by domain first (preserving insertion order)
        Map<String, List<Show>> showsByDomain = shows.stream()
                .collect(Collectors.groupingBy(Show::getDomain, java.util.LinkedHashMap::new, Collectors.toList()));

        // Get all unique domains
        List<String> allDomains = new ArrayList<>(moviesByDomain.keySet());
        allDomains.addAll(showsByDomain.keySet());
        allDomains = allDomains.stream().distinct().sorted().collect(Collectors.toList());

        // Build domain DTOs
        List<DomainDTO> domains = allDomains.stream()
                .map(domain -> {
                    List<MovieResponseDTO> movieDTOs = moviesByDomain.getOrDefault(domain, List.of())
                            .stream()
                            .map(this::convertMovieToDTO)
                            .collect(Collectors.toList());

                    // Build show DTOs - each Show record is one season per show
                    List<ShowResponseDTO> showDTOs = showsByDomain.getOrDefault(domain, List.of())
                            .stream()
                            .map(this::convertShowToDTO)
                            .collect(Collectors.toList());

                    return new DomainDTO(domain, movieDTOs, showDTOs);
                })
                .collect(Collectors.toList());

        MCUContentGroupedDTO result = new MCUContentGroupedDTO();
        result.setDomains(domains);
        return result;
    }

    private MovieResponseDTO convertMovieToDTO(Movie movie) {
        MovieResponseDTO dto = new MovieResponseDTO();
        dto.setGlobalId("movie:" + movie.getId());
        dto.setTitle(movie.getTitle());
        dto.setPremiereDate(movie.getPremiereDate());
        dto.setRuntime(movie.getRuntime());
        dto.setSynopsis(movie.getSynopsis());
        dto.setStatus(movie.getStatus());
        dto.setS3Url(movie.getS3Url());
        dto.setType(Boolean.TRUE.equals(movie.getIsSpecialPres()) ? ContentType.SPECIAL : ContentType.MOVIE);
        return dto;
    }

    private ShowResponseDTO convertShowToDTO(Show show) {
        // Each Show record represents one season
        ShowResponseDTO dto = new ShowResponseDTO();

        dto.setGlobalId("show:" + show.getId());
        dto.setTitle(show.getTitle());
        dto.setPremiereDate(show.getPremiereDate());
        dto.setSynopsis(show.getSynopsis());
        dto.setS3Url(show.getS3Url());

        // Fetch episodes for this specific season
        List<EpisodeResponseDTO> episodes = episodeRepository.findByShowId(show.getId())
                .stream()
                .map(this::convertEpisodeToDTO)
                .sorted(Comparator.comparingInt(EpisodeResponseDTO::getEpisodeNumber))
                .collect(Collectors.toList());

        // Return only one season per show
        SeasonResponseDTO season = new SeasonResponseDTO(show.getSeason(), episodes);
        dto.setSeasons(List.of(season));
        return dto;
    }

    private EpisodeResponseDTO convertEpisodeToDTO(Episode episode) {
        EpisodeResponseDTO dto = new EpisodeResponseDTO();
        dto.setId(episode.getId());
        dto.setGlobalId("episode:" + episode.getId());
        dto.setEpisodeNumber(episode.getEpisodeNumber());
        dto.setTitle(episode.getTitle());
        dto.setRuntime(episode.getRuntime());
        dto.setStatus(episode.getStatus());
        return dto;
    }

    public void updateContentStatus(UpdateContentStatusRequestDTO request) {
        String[] parts = request.globalId().split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException(
                    "Invalid globalId format. Expected 'type:id' but got: " + request.globalId());
        }

        String contentType = parts[0];
        Long contentId;
        try {
            contentId = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid id in globalId: " + parts[1], e);
        }

        if ("movie".equalsIgnoreCase(contentType)) {
            Movie movie = movieRepository.findById(contentId)
                    .orElseThrow(() -> new IllegalArgumentException("Movie not found with id: " + contentId));
            movie.setStatus(request.status());
            movieRepository.save(movie);
        } else if ("episode".equalsIgnoreCase(contentType)) {
            Episode episode = episodeRepository.findById(contentId)
                    .orElseThrow(
                            () -> new IllegalArgumentException("Episode not found with id: " + contentId));
            episode.setStatus(request.status());
            episodeRepository.save(episode);
        } else {
            throw new IllegalArgumentException(
                    "Invalid content type: " + contentType + ". Must be 'movie' or 'episode'");
        }
    }
}
