package com.utilityhub.api.service;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.utilityhub.api.db.entity.courses.Course;
import com.utilityhub.api.db.entity.courses.CourseCreator;
import com.utilityhub.api.db.entity.courses.CourseGroup;
import com.utilityhub.api.db.repository.courses.CourseCreatorRepository;
import com.utilityhub.api.db.repository.courses.CourseGroupRepository;
import com.utilityhub.api.db.repository.courses.CourseRepository;
import com.utilityhub.api.dto.request.CourseCreatorCreateRequestDTO;
import com.utilityhub.api.dto.request.CourseGroupOrderRequestDTO;
import com.utilityhub.api.dto.request.CourseGroupRequestDTO;
import com.utilityhub.api.dto.request.CourseRequestDTO;
import com.utilityhub.api.dto.response.courses.CourseCoverUploadResponseDTO;
import com.utilityhub.api.dto.response.courses.CourseCreatorResponseDTO;
import com.utilityhub.api.dto.response.courses.CourseGroupResponseDTO;
import com.utilityhub.api.dto.response.courses.CourseResponseDTO;
import com.utilityhub.api.exception.InvalidRequestException;
import com.utilityhub.api.exception.ResourceNotFoundException;

/**
 * Courses, their creators and groups. Stats (lessons done per course and per group) are
 * worked out by the apps from these lists, so they're never stored and never go stale.
 */
@Service
@Transactional(readOnly = true)
public class CoursesService {

    private static final Logger log = LoggerFactory.getLogger(CoursesService.class);

    /** Cover image types and the file extension each is stored under. */
    private static final Map<String, String> COVER_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/heic", "heic",
            "image/gif", "gif");

    private static final String COVER_PREFIX = "course-covers/";

    private final CourseRepository courseRepository;
    private final CourseCreatorRepository creatorRepository;
    private final CourseGroupRepository groupRepository;
    private final S3Service s3Service;

    public CoursesService(
            CourseRepository courseRepository,
            CourseCreatorRepository creatorRepository,
            CourseGroupRepository groupRepository,
            S3Service s3Service) {
        this.courseRepository = courseRepository;
        this.creatorRepository = creatorRepository;
        this.groupRepository = groupRepository;
        this.s3Service = s3Service;
    }

    /** What a create returned, and whether it was new (201) or an existing match (200). */
    public record Created<T>(T value, boolean isNew) {}

    // --- Courses ---

    public List<CourseResponseDTO> findAllCourses() {
        return courseRepository.findAllFlat();
    }

    @Transactional
    public CourseResponseDTO createCourse(CourseRequestDTO request) {
        Course course = new Course();
        course.setCreatedAt(Instant.now());
        apply(course, request);
        Course saved = courseRepository.save(course);
        log.info("Created course id={}", saved.getId());
        return toResponse(saved);
    }

    /** Replaces every editable field. The cover stays (it has its own endpoint). */
    @Transactional
    public CourseResponseDTO updateCourse(Integer id, CourseRequestDTO request) {
        Course course = findCourse(id);
        apply(course, request);
        Course saved = courseRepository.save(course);
        log.info("Updated course id={}", id);
        return toResponse(saved);
    }

    /** Sets how many lessons are done: from 0 to the course's total. */
    @Transactional
    public CourseResponseDTO setProgress(Integer id, int completedLessons) {
        Course course = findCourse(id);
        if (completedLessons > course.getTotalLessons()) {
            throw new InvalidRequestException(
                    "completedLessons cannot exceed totalLessons (" + course.getTotalLessons() + ")");
        }
        course.setCompletedLessons(completedLessons);
        Course saved = courseRepository.save(course);
        log.info("Set progress course id={} completed={}", id, completedLessons);
        return toResponse(saved);
    }

    @Transactional
    public void deleteCourse(Integer id) {
        courseRepository.delete(findCourse(id));
        log.info("Deleted course id={}", id);
    }

    // --- Cover image ---

    /**
     * A presigned URL to PUT a cover to. The key is made here, unique per upload, so two
     * covers called "cover.jpg" can't overwrite each other.
     */
    public CourseCoverUploadResponseDTO coverUploadUrl(Integer id, String fileType) {
        findCourse(id);
        String extension = COVER_TYPES.get(fileType);
        if (extension == null) {
            throw new InvalidRequestException("fileType must be one of " + COVER_TYPES.keySet());
        }
        String key = coverKeyPrefix(id) + UUID.randomUUID() + "." + extension;
        var presigned = s3Service.generatePresignedUploadUrl(key, fileType);
        return new CourseCoverUploadResponseDTO(presigned.presignedUrl(), presigned.s3Key());
    }

    /** Sets the cover to an uploaded image. Only keys handed out for this course are accepted. */
    @Transactional
    public CourseResponseDTO attachCover(Integer id, String s3Key) {
        Course course = findCourse(id);
        if (!s3Key.startsWith(coverKeyPrefix(id)) || s3Key.contains("..")) {
            throw new InvalidRequestException("s3Key wasn't issued for this course");
        }
        course.setS3Url(s3Service.getPublicS3Url(s3Key));
        Course saved = courseRepository.save(course);
        log.info("Attached cover to course id={}", id);
        return toResponse(saved);
    }

    private static String coverKeyPrefix(Integer courseId) {
        return COVER_PREFIX + courseId + "-";
    }

    // --- Creators ---

    public List<CourseCreatorResponseDTO> findAllCreators() {
        return creatorRepository.findAllByOrderByNameAsc().stream()
                .map(CoursesService::toResponse)
                .toList();
    }

    /** Adds a creator, or returns the existing one with the same name (ignoring case and spaces). */
    @Transactional
    public Created<CourseCreatorResponseDTO> createCreator(CourseCreatorCreateRequestDTO request) {
        String name = request.name().trim();
        var existing = creatorRepository.findFirstByNameIgnoreCaseOrderByIdAsc(name);
        if (existing.isPresent()) {
            return new Created<>(toResponse(existing.get()), false);
        }
        CourseCreator creator = new CourseCreator();
        creator.setName(name);
        CourseCreator saved = creatorRepository.save(creator);
        log.info("Created course creator id={}", saved.getId());
        return new Created<>(toResponse(saved), true);
    }

    /** Renames a creator everywhere it's used. Another creator's name (ignoring case) is refused. */
    @Transactional
    public CourseCreatorResponseDTO renameCreator(Integer id, CourseCreatorCreateRequestDTO request) {
        CourseCreator creator = findCreator(id);
        String name = request.name().trim();
        creatorRepository
                .findFirstByNameIgnoreCaseOrderByIdAsc(name)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new InvalidRequestException("A creator named \"" + other.getName() + "\" already exists");
                });
        creator.setName(name);
        CourseCreator saved = creatorRepository.save(creator);
        log.info("Renamed course creator id={}", id);
        return toResponse(saved);
    }

    /** Deletes a creator no course uses. One with courses is refused: move them to another creator first. */
    @Transactional
    public void deleteCreator(Integer id) {
        CourseCreator creator = findCreator(id);
        long courses = courseRepository.countByCreator_Id(id);
        if (courses > 0) {
            throw new InvalidRequestException("\"" + creator.getName() + "\" still has " + courses
                    + (courses == 1 ? " course" : " courses") + ". Give them another creator first.");
        }
        creatorRepository.delete(creator);
        log.info("Deleted course creator id={}", id);
    }

    // --- Groups ---

    public List<CourseGroupResponseDTO> findAllGroups() {
        return groupRepository.findAllByOrderByPositionAscIdAsc().stream()
                .map(CoursesService::toResponse)
                .toList();
    }

    /** Adds a group at the end, or returns the existing one with the same name. */
    @Transactional
    public Created<CourseGroupResponseDTO> createGroup(CourseGroupRequestDTO request) {
        String name = request.name().trim();
        var existing = groupRepository.findFirstByNameIgnoreCaseOrderByIdAsc(name);
        if (existing.isPresent()) {
            return new Created<>(toResponse(existing.get()), false);
        }
        CourseGroup group = new CourseGroup();
        group.setName(name);
        group.setPosition(groupRepository
                .findTopByOrderByPositionDesc()
                .map(last -> last.getPosition() + 1)
                .orElse(0));
        CourseGroup saved = groupRepository.save(group);
        log.info("Created course group id={}", saved.getId());
        return new Created<>(toResponse(saved), true);
    }

    @Transactional
    public CourseGroupResponseDTO renameGroup(Integer id, CourseGroupRequestDTO request) {
        CourseGroup group = findGroup(id);
        String name = request.name().trim();
        groupRepository
                .findFirstByNameIgnoreCaseOrderByIdAsc(name)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> {
                    throw new InvalidRequestException("A group named \"" + other.getName() + "\" already exists");
                });
        group.setName(name);
        CourseGroup saved = groupRepository.save(group);
        log.info("Renamed course group id={}", id);
        return toResponse(saved);
    }

    /** Puts the groups in the given order. The list must name every group exactly once. */
    @Transactional
    public List<CourseGroupResponseDTO> reorderGroups(CourseGroupOrderRequestDTO request) {
        List<Integer> ids = request.groupIds();
        List<CourseGroup> groups = groupRepository.findAll();
        var known = new HashSet<Integer>();
        groups.forEach(g -> known.add(g.getId()));
        if (ids.size() != groups.size() || !known.equals(new HashSet<>(ids))) {
            throw new InvalidRequestException("groupIds must list every group exactly once");
        }
        for (CourseGroup group : groups) {
            group.setPosition(ids.indexOf(group.getId()));
        }
        groupRepository.saveAll(groups);
        log.info("Reordered course groups {}", ids);
        return findAllGroups();
    }

    /** Deletes the group; its courses stay, ungrouped. */
    @Transactional
    public void deleteGroup(Integer id) {
        CourseGroup group = findGroup(id);
        int moved = courseRepository.ungroupAll(id);
        groupRepository.delete(group);
        log.info("Deleted course group id={} (ungrouped {} courses)", id, moved);
    }

    // --- Helpers ---

    private void apply(Course course, CourseRequestDTO request) {
        course.setTitle(request.title().trim());
        // Only look the creator up when it changed.
        if (course.getCreator() == null || !course.getCreator().getName().equals(request.creatorName())) {
            course.setCreator(findCreator(request.creatorName()));
        }
        course.setGroup(request.groupId() == null ? null : findGroup(request.groupId()));
        course.setTotalLessons(request.totalLessons());
        course.setCompletedLessons(request.completedLessons() == null ? 0 : request.completedLessons());
        course.setUrl(
                request.url() == null || request.url().isBlank()
                        ? null
                        : request.url().trim());
    }

    private Course findCourse(Integer id) {
        return courseRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Course", id));
    }

    private CourseGroup findGroup(Integer id) {
        return groupRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Course group", id));
    }

    private CourseCreator findCreator(Integer id) {
        return creatorRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Course creator", id));
    }

    private CourseCreator findCreator(String name) {
        return creatorRepository
                .findFirstByNameIgnoreCaseOrderByIdAsc(name.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Course creator", name));
    }

    private static CourseCreatorResponseDTO toResponse(CourseCreator creator) {
        return new CourseCreatorResponseDTO(creator.getId(), creator.getName());
    }

    private static CourseGroupResponseDTO toResponse(CourseGroup group) {
        return new CourseGroupResponseDTO(group.getId(), group.getName(), group.getPosition());
    }

    private static CourseResponseDTO toResponse(Course course) {
        return new CourseResponseDTO(
                course.getId(),
                course.getTitle(),
                course.getCreator().getName(),
                course.getGroup() != null ? course.getGroup().getId() : null,
                course.getTotalLessons(),
                course.getCompletedLessons(),
                course.getUrl(),
                course.getS3Url(),
                course.getCreatedAt());
    }
}
