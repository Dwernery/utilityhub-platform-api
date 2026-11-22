package com.utilityhub.api.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
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
import com.utilityhub.api.dto.request.EpisodeRequestDTO;
import com.utilityhub.api.dto.request.MovieRequestDTO;
import com.utilityhub.api.dto.request.ShowSeasonRequestDTO;
import com.utilityhub.api.dto.response.mcu.ContentType;
import com.utilityhub.api.dto.response.mcu.DomainResponseDTO;
import com.utilityhub.api.dto.response.mcu.EpisodeResponseDTO;
import com.utilityhub.api.dto.response.mcu.MCUTrackerResponseDTO;
import com.utilityhub.api.dto.response.mcu.MovieResponseDTO;
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
    private final S3Service s3Service;

    public MCUService(
            MovieRepository movieRepository,
            ShowRepository showRepository,
            EpisodeRepository episodeRepository,
            S3Service s3Service) {
        this.movieRepository = movieRepository;
        this.showRepository = showRepository;
        this.episodeRepository = episodeRepository;
        this.s3Service = s3Service;
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
                movie.getId(),
                "movie:" + movie.getId(),
                movie.getTitle(),
                movie.getPremiereDate(),
                movie.getRuntime(),
                movie.getSynopsis(),
                movie.getStatus(),
                movie.getS3Url(),
                Boolean.TRUE.equals(movie.getSpecial()) ? ContentType.SPECIAL : ContentType.MOVIE);
    }

    /** Each Show row is one season. */
    private ShowResponseDTO convertShowToDTO(Show show, List<Episode> episodes) {
        return new ShowResponseDTO(
                show.getId(),
                "show:" + show.getId(),
                show.getTitle(),
                show.getSeason(),
                show.getPremiereDate(),
                show.getSynopsis(),
                show.getS3Url(),
                episodes.stream().map(this::convertEpisodeToDTO).toList());
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

    // --- Movies and specials ---

    /** New movies start UNWATCHED. */
    @Transactional
    public MovieResponseDTO createMovie(MovieRequestDTO request) {
        Movie movie = new Movie();
        movie.setStatus(WatchStatus.UNWATCHED);
        applyMovie(movie, request);
        Movie saved = movieRepository.save(movie);
        log.info("Created movie id={} ('{}')", saved.getId(), saved.getTitle());
        return convertMovieToDTO(saved);
    }

    /** Replaces the details; the watch status and poster are kept. */
    @Transactional
    public MovieResponseDTO updateMovie(Long id, MovieRequestDTO request) {
        Movie movie = findMovie(id);
        applyMovie(movie, request);
        Movie saved = movieRepository.save(movie);
        log.info("Updated movie id={}", id);
        return convertMovieToDTO(saved);
    }

    @Transactional
    public void deleteMovie(Long id) {
        movieRepository.delete(findMovie(id));
        log.info("Deleted movie id={}", id);
    }

    private static void applyMovie(Movie movie, MovieRequestDTO request) {
        movie.setTitle(request.title().trim());
        movie.setPremiereDate(request.premiereDate());
        movie.setRuntime(request.runtime());
        movie.setSynopsis(request.synopsis().trim());
        movie.setDomain(request.domain().trim());
        movie.setSpecial(request.special());
    }

    private Movie findMovie(Long id) {
        return movieRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Movie", id));
    }

    // --- Show seasons ---

    @Transactional
    public ShowResponseDTO createShowSeason(ShowSeasonRequestDTO request) {
        requireUniqueSeason(request, null);
        Show show = new Show();
        applyShow(show, request);
        Show saved = showRepository.save(show);
        log.info("Created show id={} ('{}' season {})", saved.getId(), saved.getTitle(), saved.getSeason());
        return convertShowToDTO(saved, List.of());
    }

    /** Replaces the season's details; its episodes and poster are kept. */
    @Transactional
    public ShowResponseDTO updateShowSeason(Long id, ShowSeasonRequestDTO request) {
        Show show = findShow(id);
        requireUniqueSeason(request, id);
        applyShow(show, request);
        Show saved = showRepository.save(show);
        log.info("Updated show id={}", id);
        return convertShowToDTO(saved, episodeRepository.findAllByShowIdOrderByEpisodeNumber(id));
    }

    /** Deletes the season and all of its episodes. */
    @Transactional
    public void deleteShowSeason(Long id) {
        Show show = findShow(id);
        int episodes = episodeRepository.deleteByShowId(id);
        showRepository.delete(show);
        log.info("Deleted show id={} and its {} episodes", id, episodes);
    }

    private void requireUniqueSeason(ShowSeasonRequestDTO request, Long exceptId) {
        showRepository
                .findFirstByTitleIgnoreCaseAndSeason(request.title().trim(), request.season())
                .filter(existing -> !existing.getId().equals(exceptId))
                .ifPresent(existing -> {
                    throw new InvalidRequestException(
                            "'" + existing.getTitle() + "' season " + existing.getSeason() + " already exists");
                });
    }

    private static void applyShow(Show show, ShowSeasonRequestDTO request) {
        show.setTitle(request.title().trim());
        show.setSeason(request.season());
        show.setPremiereDate(request.premiereDate());
        show.setSynopsis(request.synopsis().trim());
        show.setDomain(request.domain().trim());
    }

    private Show findShow(Long id) {
        return showRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Show", id));
    }

    // --- Episodes ---

    /** New episodes start UNWATCHED. */
    @Transactional
    public EpisodeResponseDTO createEpisode(Long showId, EpisodeRequestDTO request) {
        Show show = findShow(showId);
        requireUniqueEpisodeNumber(showId, request.episodeNumber(), null);
        Episode episode = new Episode();
        episode.setShow(show);
        episode.setStatus(WatchStatus.UNWATCHED);
        applyEpisode(episode, request);
        Episode saved = episodeRepository.save(episode);
        log.info("Created episode id={} (show id={}, episode {})", saved.getId(), showId, saved.getEpisodeNumber());
        return convertEpisodeToDTO(saved);
    }

    /** Replaces the episode's details; its watch status is kept. */
    @Transactional
    public EpisodeResponseDTO updateEpisode(Long id, EpisodeRequestDTO request) {
        Episode episode = findEpisode(id);
        requireUniqueEpisodeNumber(episode.getShow().getId(), request.episodeNumber(), id);
        applyEpisode(episode, request);
        Episode saved = episodeRepository.save(episode);
        log.info("Updated episode id={}", id);
        return convertEpisodeToDTO(saved);
    }

    @Transactional
    public void deleteEpisode(Long id) {
        episodeRepository.delete(findEpisode(id));
        log.info("Deleted episode id={}", id);
    }

    private void requireUniqueEpisodeNumber(Long showId, Integer episodeNumber, Long exceptId) {
        episodeRepository
                .findFirstByShowIdAndEpisodeNumber(showId, episodeNumber)
                .filter(existing -> !existing.getId().equals(exceptId))
                .ifPresent(existing -> {
                    throw new InvalidRequestException("Episode " + episodeNumber + " already exists in this season");
                });
    }

    private static void applyEpisode(Episode episode, EpisodeRequestDTO request) {
        episode.setEpisodeNumber(request.episodeNumber());
        episode.setTitle(request.title().trim());
        episode.setRuntime(request.runtime());
    }

    private Episode findEpisode(Long id) {
        return episodeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Episode", id));
    }

    // --- Posters (same presigned-upload flow as Library covers, but in the MCU bucket) ---

    /**
     * A presigned PUT URL for a poster image. The key is generated here, under
     * {@code mcu-posters/}, so two files with the same name can't overwrite each other.
     */
    public S3Service.S3PresignedUrlResponse generatePosterUploadUrl(String fileName, String fileType) {
        String safeName = fileName.replaceAll("[^A-Za-z0-9._-]", "_");
        String key = "mcu-posters/" + UUID.randomUUID() + "-" + safeName;
        return s3Service.generateMcuPosterUploadUrl(key, fileType);
    }

    /** Points the movie's poster at an uploaded object and returns its public URL. */
    @Transactional
    public String attachMoviePoster(Long id, String s3Key) {
        Movie movie = findMovie(id);
        String url = s3Service.getMcuPosterUrl(s3Key);
        movie.setS3Url(url);
        movieRepository.save(movie);
        log.info("Attached poster to movie id={}", id);
        return url;
    }

    /** Points the season's poster at an uploaded object and returns its public URL. */
    @Transactional
    public String attachShowPoster(Long id, String s3Key) {
        Show show = findShow(id);
        String url = s3Service.getMcuPosterUrl(s3Key);
        show.setS3Url(url);
        showRepository.save(show);
        log.info("Attached poster to show id={}", id);
        return url;
    }
}
