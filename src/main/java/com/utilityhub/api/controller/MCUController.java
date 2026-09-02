package com.utilityhub.api.controller;

import com.utilityhub.api.dto.request.UpdateContentStatusRequestDTO;
import com.utilityhub.api.dto.response.mcu.MCUContentGroupedDTO;
import com.utilityhub.api.service.MCUService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mcu")
public class MCUController {

    private final MCUService mcuService;

    public MCUController(MCUService mcuService) {
        this.mcuService = mcuService;
    }

    @GetMapping("/tracker")
    public ResponseEntity<MCUContentGroupedDTO> getAllContentGroupedByDomain() {
        MCUContentGroupedDTO content = mcuService.getAllContentGroupedByDomain();
        return ResponseEntity.ok(content);
    }

    @PutMapping("/tracker/status")
    public ResponseEntity<Void> updateContentStatus(@RequestBody UpdateContentStatusRequestDTO request) {
        mcuService.updateContentStatus(request);
        return ResponseEntity.noContent().build();
    }
}
