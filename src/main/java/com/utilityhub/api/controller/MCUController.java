package com.utilityhub.api.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.utilityhub.api.dto.request.ContentStatusUpdateRequestDTO;
import com.utilityhub.api.dto.request.EpisodeRequestDTO;
import com.utilityhub.api.dto.request.MovieRequestDTO;
import com.utilityhub.api.dto.request.PosterAttachRequestDTO;
import com.utilityhub.api.dto.request.ShowSeasonRequestDTO;
import com.utilityhub.api.dto.response.library.AttachFileResponseDTO;
import com.utilityhub.api.dto.response.library.PresignedUrlResponseDTO;
import com.utilityhub.api.dto.response.mcu.EpisodeResponseDTO;
import com.utilityhub.api.dto.response.mcu.MCUTrackerResponseDTO;
import com.utilityhub.api.dto.response.mcu.MovieResponseDTO;
import com.utilityhub.api.dto.response.mcu.ShowResponseDTO;
import com.utilityhub.api.service.MCUService;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/mcu")
@Tag(name = "MCU", description = "Marvel watch-tracker")
public class MCUController {

    private final MCUService mcuService;

    public MCUController(MCUService mcuService) {
        this.mcuService = mcuService;
    }

    @GetMapping("/tracker")
    public MCUTrackerResponseDTO getAllContentGroupedByDomain() {
        return mcuService.getAllContentGroupedByDomain();
    }

    /** {@code globalId} is "movie:{id}" or "episode:{id}", as returned by GET /tracker. */
    @PutMapping("/tracker/{globalId}/status")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateContentStatus(
            @PathVariable
                    @Pattern(
                            regexp = "(?i)(movie|episode):\\d{1,18}",
                            message = "must look like 'movie:12' or 'episode:34'")
                    String globalId,
            @Valid @RequestBody ContentStatusUpdateRequestDTO request) {
        mcuService.updateContentStatus(globalId, request.status());
    }

    // --- Movies and specials ---

    @PostMapping("/movies")
    @ResponseStatus(HttpStatus.CREATED)
    public MovieResponseDTO createMovie(@Valid @RequestBody MovieRequestDTO request) {
        return mcuService.createMovie(request);
    }

    @PutMapping("/movies/{id}")
    public MovieResponseDTO updateMovie(@PathVariable Long id, @Valid @RequestBody MovieRequestDTO request) {
        return mcuService.updateMovie(id, request);
    }

    @DeleteMapping("/movies/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMovie(@PathVariable Long id) {
        mcuService.deleteMovie(id);
    }

    @PostMapping("/movies/{id}/poster")
    public AttachFileResponseDTO attachMoviePoster(
            @PathVariable Long id, @Valid @RequestBody PosterAttachRequestDTO request) {
        return new AttachFileResponseDTO(true, mcuService.attachMoviePoster(id, request.s3Key()), "Poster attached");
    }

    // --- Show seasons (one row per season) ---

    @PostMapping("/shows")
    @ResponseStatus(HttpStatus.CREATED)
    public ShowResponseDTO createShowSeason(@Valid @RequestBody ShowSeasonRequestDTO request) {
        return mcuService.createShowSeason(request);
    }

    @PutMapping("/shows/{id}")
    public ShowResponseDTO updateShowSeason(@PathVariable Long id, @Valid @RequestBody ShowSeasonRequestDTO request) {
        return mcuService.updateShowSeason(id, request);
    }

    /** Also deletes the season's episodes. */
    @DeleteMapping("/shows/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteShowSeason(@PathVariable Long id) {
        mcuService.deleteShowSeason(id);
    }

    @PostMapping("/shows/{id}/poster")
    public AttachFileResponseDTO attachShowPoster(
            @PathVariable Long id, @Valid @RequestBody PosterAttachRequestDTO request) {
        return new AttachFileResponseDTO(true, mcuService.attachShowPoster(id, request.s3Key()), "Poster attached");
    }

    // --- Episodes ---

    @PostMapping("/shows/{showId}/episodes")
    @ResponseStatus(HttpStatus.CREATED)
    public EpisodeResponseDTO createEpisode(@PathVariable Long showId, @Valid @RequestBody EpisodeRequestDTO request) {
        return mcuService.createEpisode(showId, request);
    }

    @PutMapping("/episodes/{id}")
    public EpisodeResponseDTO updateEpisode(@PathVariable Long id, @Valid @RequestBody EpisodeRequestDTO request) {
        return mcuService.updateEpisode(id, request);
    }

    @DeleteMapping("/episodes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEpisode(@PathVariable Long id) {
        mcuService.deleteEpisode(id);
    }

    // --- Poster upload (same flow as Library covers) ---

    /** Step 1 of a poster upload: PUT the image to the returned URL, then POST its key to .../poster. */
    @GetMapping("/poster-upload-url")
    public PresignedUrlResponseDTO generatePosterUploadUrl(
            @RequestParam @NotBlank String fileName,
            @RequestParam
                    @Pattern(regexp = "image/(jpeg|png|webp|gif)", message = "must be a JPEG, PNG, WebP or GIF image")
                    String fileType) {
        var response = mcuService.generatePosterUploadUrl(fileName, fileType);
        return new PresignedUrlResponseDTO(response.presignedUrl(), response.s3Key());
    }
}
