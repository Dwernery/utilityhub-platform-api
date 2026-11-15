package com.utilityhub.api.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utilityhub.api.db.entity.mcu.Episode;
import com.utilityhub.api.db.entity.mcu.Movie;
import com.utilityhub.api.db.entity.mcu.Show;
import com.utilityhub.api.db.entity.mcu.WatchStatus;
import com.utilityhub.api.db.repository.mcu.EpisodeRepository;
import com.utilityhub.api.db.repository.mcu.MovieRepository;
import com.utilityhub.api.db.repository.mcu.ShowRepository;
import com.utilityhub.api.dto.response.mcu.ContentType;
import com.utilityhub.api.dto.response.mcu.DomainResponseDTO;
import com.utilityhub.api.dto.response.mcu.EpisodeResponseDTO;
import com.utilityhub.api.dto.response.mcu.MCUTrackerResponseDTO;
import com.utilityhub.api.dto.response.mcu.MovieResponseDTO;
import com.utilityhub.api.dto.response.mcu.SeasonResponseDTO;
import com.utilityhub.api.dto.response.mcu.ShowResponseDTO;
import com.utilityhub.api.exception.InvalidRequestException;
import com.utilityhub.api.exception.ResourceNotFoundException;

@Service
@Transactional(readOnly = true)
public class MCUService {

    private static final Logger log = LoggerFactory.getLogger(MCUService.class);

    private final MovieRepository movieRepository;
    private final ShowRepository showRepository;
    private final EpisodeRepository episodeRepository;

    public MCUService(
            MovieRepository movieRepository, ShowRepository showRepository, EpisodeRepository episodeRepository) {
        this.movieRepository = movieRepository;
        this.showRepository = showRepository;
        this.episodeRepository = episodeRepository;
    }

    /** Oldest first; undated items last; id breaks ties so the order is deterministic. */
    private static final Sort BY_PREMIERE_DATE =
            Sort.by(Sort.Order.asc("premiereDate").nullsLast(), Sort.Order.asc("id"));

    /**
     * Runs exactly three queries (movies, shows, all episodes) no matter how
     * much content there is. Sorting happens in the database.
     */
    public MCUTrackerResponseDTO getAllContentGroupedByDomain() {
        // groupingBy into a LinkedHashMap keeps the database's order within each group.
        Map<String, List<Movie>> moviesByDomain = movieRepository.findAll(BY_PREMIERE_DATE).stream()
                .collect(Collectors.groupingBy(Movie::getDomain, LinkedHashMap::new, Collectors.toList()));
        Map<String, List<Show>> showsByDomain = showRepository.findAll(BY_PREMIERE_DATE).stream()
                .collect(Collectors.groupingBy(Show::getDomain, LinkedHashMap::new, Collectors.toList()));
        // getShow().getId() reads the id from the lazy proxy without loading the show.
        Map<Long, List<Episode>> episodesByShowId = episodeRepository.findAllOrderedByShowAndEpisodeNumber().stream()
                .collect(Collectors.groupingBy(episode -> episode.getShow().getId()));

        // TreeSet: every domain that has a movie or a show, alphabetically.
        Set<String> allDomains = new TreeSet<>(moviesByDomain.keySet());
        allDomains.addAll(showsByDomain.keySet());

        List<DomainResponseDTO> domains = allDomains.stream()
                .map(domain -> new DomainResponseDTO(
                        domain,
                        moviesByDomain.getOrDefault(domain, List.of()).stream()
                                .map(this::convertMovieToDTO)
                                .toList(),
                        showsByDomain.getOrDefault(domain, List.of()).stream()
                                .map(show ->
                                        convertShowToDTO(show, episodesByShowId.getOrDefault(show.getId(), List.of())))
                                .toList()))
                .toList();

        return new MCUTrackerResponseDTO(domains);
    }

    private MovieResponseDTO convertMovieToDTO(Movie movie) {
        return new MovieResponseDTO(
                "movie:" + movie.getId(),
                movie.getTitle(),
                movie.getPremiereDate(),
                movie.getRuntime(),
                movie.getSynopsis(),
                movie.getStatus(),
                movie.getS3Url(),
                Boolean.TRUE.equals(movie.getIsSpecialPres()) ? ContentType.SPECIAL : ContentType.MOVIE);
    }

    /** Each Show row is one season, so the DTO always has exactly one season. */
    private ShowResponseDTO convertShowToDTO(Show show, List<Episode> episodes) {
        List<EpisodeResponseDTO> episodeDTOs =
                episodes.stream().map(this::convertEpisodeToDTO).toList();
        return new ShowResponseDTO(
                "show:" + show.getId(),
                show.getTitle(),
                show.getPremiereDate(),
                show.getSynopsis(),
                show.getS3Url(),
                List.of(new SeasonResponseDTO(show.getSeason(), episodeDTOs)));
    }

    private EpisodeResponseDTO convertEpisodeToDTO(Episode episode) {
        return new EpisodeResponseDTO(
                episode.getId(),
                "episode:" + episode.getId(),
                episode.getEpisodeNumber(),
                episode.getTitle(),
                episode.getRuntime(),
                episode.getStatus());
    }

    /** @param globalId "movie:{id}" or "episode:{id}" (format already validated by the controller) */
    @Transactional
    public void updateContentStatus(String globalId, WatchStatus status) {
        String[] parts = globalId.split(":");
        String contentType = parts[0].toLowerCase();
        long contentId = Long.parseLong(parts[1]);

        switch (contentType) {
            case "movie" -> {
                Movie movie = movieRepository
                        .findById(contentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Movie", contentId));
                movie.setStatus(status);
                movieRepository.save(movie);
            }
            case "episode" -> {
                Episode episode = episodeRepository
                        .findById(contentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Episode", contentId));
                episode.setStatus(status);
                episodeRepository.save(episode);
            }
            default ->
                throw new InvalidRequestException(
                        "Invalid content type: " + contentType + ". Must be 'movie' or 'episode'");
        }
        log.info("Set status of {} to {}", globalId, status);
    }
}
