package com.utilityhub.api.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.utilityhub.api.dto.request.ContentStatusUpdateRequestDTO;
import com.utilityhub.api.dto.response.mcu.MCUTrackerResponseDTO;
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
}
