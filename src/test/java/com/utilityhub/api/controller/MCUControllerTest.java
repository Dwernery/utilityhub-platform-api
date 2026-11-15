package com.utilityhub.api.controller;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.utilityhub.api.config.SecurityConfig;
import com.utilityhub.api.db.entity.mcu.WatchStatus;
import com.utilityhub.api.dto.response.mcu.DomainResponseDTO;
import com.utilityhub.api.dto.response.mcu.MCUTrackerResponseDTO;
import com.utilityhub.api.service.MCUService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@WebMvcTest(MCUController.class)
@Import(SecurityConfig.class)
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
}
