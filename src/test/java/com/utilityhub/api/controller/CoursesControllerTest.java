package com.utilityhub.api.controller;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import com.utilityhub.api.dto.response.courses.CourseCreatorResponseDTO;
import com.utilityhub.api.dto.response.courses.CourseGroupResponseDTO;
import com.utilityhub.api.dto.response.courses.CourseResponseDTO;
import com.utilityhub.api.service.CoursesService;
import com.utilityhub.api.support.WithSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** Web-layer tests: routing, status codes and request validation. The service is a mock. */
@WebMvcTest(CoursesController.class)
@Import(WithSession.class)
class CoursesControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CoursesService coursesService;

    private static CourseResponseDTO spring() {
        return new CourseResponseDTO(
                1, "Spring Boot", "Dan Vega", 3, 40, 12, "https://example.com", null, Instant.EPOCH);
    }

    @Test
    void getAllCourses() {
        given(coursesService.findAllCourses()).willReturn(List.of(spring()));

        assertThat(mvc.get().uri("/api/courses"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[0].creatorName")
                .isEqualTo("Dan Vega");
    }

    @Test
    void createCourse_returns201() {
        given(coursesService.createCourse(any())).willReturn(spring());

        assertThat(mvc.post()
                        .uri("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"title":"Spring Boot","creatorName":"Dan Vega","totalLessons":40,"groupId":3}
                        """))
                .hasStatus(201)
                .bodyJson()
                .extractingPath("$.id")
                .isEqualTo(1);
    }

    @Test
    void createCourse_rejectsMissingCreatorAndZeroLessons() {
        assertThat(mvc.post()
                        .uri("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"title":"Spring Boot","totalLessons":0}
                        """))
                .hasStatus(400)
                .bodyJson()
                .hasPathSatisfying("$.errors.creatorName", v -> assertThat(v).isNotNull())
                .hasPathSatisfying("$.errors.totalLessons", v -> assertThat(v).isNotNull());
        verify(coursesService, never()).createCourse(any());
    }

    @Test
    void createCourse_rejectsProgressPastTheTotal() {
        assertThat(mvc.post()
                        .uri("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"title":"x","creatorName":"y","totalLessons":5,"completedLessons":6}
                        """))
                .hasStatus(400);
        verify(coursesService, never()).createCourse(any());
    }

    @Test
    void createCourse_rejectsANonWebUrl() {
        assertThat(mvc.post()
                        .uri("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"title":"x","creatorName":"y","totalLessons":5,"url":"javascript:alert(1)"}
                        """))
                .hasStatus(400);
    }

    @Test
    void createCourse_acceptsAnEmptyUrlAsNoLink() {
        given(coursesService.createCourse(any())).willReturn(spring());

        assertThat(mvc.post()
                        .uri("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"title":"x","creatorName":"y","totalLessons":5,"url":""}
                        """))
                .hasStatus(201);
    }

    @Test
    void createCourse_rejectsFractionalLessons() {
        assertThat(mvc.post()
                        .uri("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"title":"x","creatorName":"y","totalLessons":4.5}
                        """))
                .hasStatus(400);
    }

    @Test
    void updateCourse() {
        given(coursesService.updateCourse(eq(1), any())).willReturn(spring());

        assertThat(mvc.put()
                        .uri("/api/courses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"title":"Spring Boot","creatorName":"Dan Vega","totalLessons":40,"completedLessons":12}
                        """))
                .hasStatusOk();
    }

    @Test
    void setProgress() {
        given(coursesService.setProgress(1, 13)).willReturn(spring());

        assertThat(mvc.patch()
                        .uri("/api/courses/1/progress")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"completedLessons":13}
                        """))
                .hasStatusOk();
        verify(coursesService).setProgress(1, 13);
    }

    @Test
    void setProgress_rejectsNegative() {
        assertThat(mvc.patch()
                        .uri("/api/courses/1/progress")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {"completedLessons":-1}
                        """))
                .hasStatus(400);
        verify(coursesService, never()).setProgress(anyInt(), anyInt());
    }

    @Test
    void deleteCourse() {
        assertThat(mvc.delete().uri("/api/courses/1")).hasStatus(204);
        verify(coursesService).deleteCourse(1);
    }

    @Test
    void createCreator_201WhenNew_200WhenExisting() {
        given(coursesService.createCreator(any()))
                .willReturn(new CoursesService.Created<>(new CourseCreatorResponseDTO(7, "Dan Vega"), true))
                .willReturn(new CoursesService.Created<>(new CourseCreatorResponseDTO(7, "Dan Vega"), false));
        var request = mvc.post()
                .uri("/api/courses/creators")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Dan Vega\"}");

        assertThat(request).hasStatus(201);
        assertThat(request).hasStatusOk().bodyJson().extractingPath("$.name").isEqualTo("Dan Vega");
    }

    @Test
    void creators_renameAndDelete() {
        given(coursesService.renameCreator(eq(7), any())).willReturn(new CourseCreatorResponseDTO(7, "Dan V."));

        assertThat(mvc.put()
                        .uri("/api/courses/creators/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Dan V.\"}"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.name")
                .isEqualTo("Dan V.");
        assertThat(mvc.put()
                        .uri("/api/courses/creators/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .hasStatus(400);
        assertThat(mvc.delete().uri("/api/courses/creators/7")).hasStatus(204);
        verify(coursesService).deleteCreator(7);
    }

    @Test
    void groups_listCreateRenameReorderDelete() {
        var group = new CourseGroupResponseDTO(3, "Java", 0);
        given(coursesService.findAllGroups()).willReturn(List.of(group));
        given(coursesService.createGroup(any())).willReturn(new CoursesService.Created<>(group, true));
        given(coursesService.renameGroup(eq(3), any())).willReturn(group);
        given(coursesService.reorderGroups(any())).willReturn(List.of(group));

        assertThat(mvc.get().uri("/api/courses/groups"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[0].name")
                .isEqualTo("Java");
        assertThat(mvc.post()
                        .uri("/api/courses/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Java\"}"))
                .hasStatus(201);
        assertThat(mvc.put()
                        .uri("/api/courses/groups/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Java\"}"))
                .hasStatusOk();
        // "order" is its own route, not a group id.
        assertThat(mvc.put()
                        .uri("/api/courses/groups/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"groupIds\":[3]}"))
                .hasStatusOk();
        assertThat(mvc.delete().uri("/api/courses/groups/3")).hasStatus(204);
        verify(coursesService).deleteGroup(3);
    }

    @Test
    void createGroup_rejectsBlankName() {
        assertThat(mvc.post()
                        .uri("/api/courses/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  \"}"))
                .hasStatus(400);
    }

    @Test
    void cover_uploadUrlThenAttach() {
        given(coursesService.attachCover(1, "course-covers/1-abc.jpg")).willReturn(spring());

        assertThat(mvc.get().uri("/api/courses/1/cover-upload-url").param("fileType", "image/jpeg"))
                .hasStatusOk();
        verify(coursesService).coverUploadUrl(1, "image/jpeg");
        assertThat(mvc.post()
                        .uri("/api/courses/1/attach-cover")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"s3Key\":\"course-covers/1-abc.jpg\"}"))
                .hasStatusOk();
    }
}
