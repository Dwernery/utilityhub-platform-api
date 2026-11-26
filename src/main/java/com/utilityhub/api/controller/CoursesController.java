package com.utilityhub.api.controller;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.utilityhub.api.dto.request.CourseCoverAttachRequestDTO;
import com.utilityhub.api.dto.request.CourseCreatorCreateRequestDTO;
import com.utilityhub.api.dto.request.CourseGroupOrderRequestDTO;
import com.utilityhub.api.dto.request.CourseGroupRequestDTO;
import com.utilityhub.api.dto.request.CourseProgressRequestDTO;
import com.utilityhub.api.dto.request.CourseRequestDTO;
import com.utilityhub.api.dto.response.courses.CourseCoverUploadResponseDTO;
import com.utilityhub.api.dto.response.courses.CourseCreatorResponseDTO;
import com.utilityhub.api.dto.response.courses.CourseGroupResponseDTO;
import com.utilityhub.api.dto.response.courses.CourseResponseDTO;
import com.utilityhub.api.service.CoursesService;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/courses")
@Tag(name = "Courses", description = "Learning courses, their creators and groups")
public class CoursesController {

    private final CoursesService coursesService;

    public CoursesController(CoursesService coursesService) {
        this.coursesService = coursesService;
    }

    // --- Courses ---

    @GetMapping
    public List<CourseResponseDTO> getAllCourses() {
        return coursesService.findAllCourses();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseResponseDTO createCourse(@Valid @RequestBody CourseRequestDTO request) {
        return coursesService.createCourse(request);
    }

    @PutMapping("/{id}")
    public CourseResponseDTO updateCourse(@PathVariable Integer id, @Valid @RequestBody CourseRequestDTO request) {
        return coursesService.updateCourse(id, request);
    }

    /** Sets the number of lessons done (the apps send the new count for +1 / −1 too). */
    @PatchMapping("/{id}/progress")
    public CourseResponseDTO setProgress(
            @PathVariable Integer id, @Valid @RequestBody CourseProgressRequestDTO request) {
        return coursesService.setProgress(id, request.completedLessons());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCourse(@PathVariable Integer id) {
        coursesService.deleteCourse(id);
    }

    /** Step 1 of a cover upload: where to PUT the image. */
    @GetMapping("/{id}/cover-upload-url")
    public CourseCoverUploadResponseDTO coverUploadUrl(
            @PathVariable Integer id, @RequestParam @NotBlank String fileType) {
        return coursesService.coverUploadUrl(id, fileType);
    }

    /** Step 2, after the PUT succeeded: make it the course's cover. */
    @PostMapping("/{id}/attach-cover")
    public CourseResponseDTO attachCover(
            @PathVariable Integer id, @Valid @RequestBody CourseCoverAttachRequestDTO request) {
        return coursesService.attachCover(id, request.s3Key());
    }

    // --- Creators ---

    @GetMapping("/creators")
    public List<CourseCreatorResponseDTO> getAllCreators() {
        return coursesService.findAllCreators();
    }

    /** 201 with the new creator, or 200 with the existing one if the name (ignoring case) is taken. */
    @PostMapping("/creators")
    public ResponseEntity<CourseCreatorResponseDTO> createCreator(
            @Valid @RequestBody CourseCreatorCreateRequestDTO request) {
        return created(coursesService.createCreator(request));
    }

    /** Renames a creator; every course by them shows the new name. */
    @PutMapping("/creators/{id}")
    public CourseCreatorResponseDTO renameCreator(
            @PathVariable Integer id, @Valid @RequestBody CourseCreatorCreateRequestDTO request) {
        return coursesService.renameCreator(id, request);
    }

    /** Deletes a creator that no course uses (400 otherwise). */
    @DeleteMapping("/creators/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCreator(@PathVariable Integer id) {
        coursesService.deleteCreator(id);
    }

    // --- Groups ---

    @GetMapping("/groups")
    public List<CourseGroupResponseDTO> getAllGroups() {
        return coursesService.findAllGroups();
    }

    /** 201 with the new group (added last), or 200 with the existing one if the name is taken. */
    @PostMapping("/groups")
    public ResponseEntity<CourseGroupResponseDTO> createGroup(@Valid @RequestBody CourseGroupRequestDTO request) {
        return created(coursesService.createGroup(request));
    }

    @PutMapping("/groups/{id}")
    public CourseGroupResponseDTO renameGroup(
            @PathVariable Integer id, @Valid @RequestBody CourseGroupRequestDTO request) {
        return coursesService.renameGroup(id, request);
    }

    /** Sets the display order. Returns every group in the new order. */
    @PutMapping("/groups/order")
    public List<CourseGroupResponseDTO> reorderGroups(@Valid @RequestBody CourseGroupOrderRequestDTO request) {
        return coursesService.reorderGroups(request);
    }

    /** Deletes the group. Its courses stay, ungrouped. */
    @DeleteMapping("/groups/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGroup(@PathVariable Integer id) {
        coursesService.deleteGroup(id);
    }

    private static <T> ResponseEntity<T> created(CoursesService.Created<T> result) {
        return ResponseEntity.status(result.isNew() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(result.value());
    }
}
