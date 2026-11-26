package com.utilityhub.api.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
import com.utilityhub.api.exception.InvalidRequestException;
import com.utilityhub.api.exception.ResourceNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** Courses, creators and groups. Repositories and S3 are mocks. */
class CoursesServiceTest {

    private CourseRepository courseRepository;
    private CourseCreatorRepository creatorRepository;
    private CourseGroupRepository groupRepository;
    private S3Service s3Service;
    private CoursesService service;

    private final CourseCreator danVega = creator(7, "Dan Vega");
    private final CourseGroup java = group(3, "Java", 0);

    private static CourseCreator creator(int id, String name) {
        var c = new CourseCreator();
        c.setId(id);
        c.setName(name);
        return c;
    }

    private static CourseGroup group(int id, String name, int position) {
        var g = new CourseGroup();
        g.setId(id);
        g.setName(name);
        g.setPosition(position);
        return g;
    }

    private Course course(int total, int completed) {
        var c = new Course();
        c.setId(1);
        c.setTitle("Spring Boot");
        c.setCreator(danVega);
        c.setTotalLessons(total);
        c.setCompletedLessons(completed);
        return c;
    }

    private static CourseRequestDTO request(String creator, Integer groupId, Integer completed, String url) {
        return new CourseRequestDTO(" Spring Boot ", creator, groupId, 40, completed, url);
    }

    @BeforeEach
    void setUp() {
        courseRepository = mock(CourseRepository.class);
        creatorRepository = mock(CourseCreatorRepository.class);
        groupRepository = mock(CourseGroupRepository.class);
        s3Service = mock(S3Service.class);
        service = new CoursesService(courseRepository, creatorRepository, groupRepository, s3Service);

        given(courseRepository.save(any())).willAnswer(invocation -> {
            Course c = invocation.getArgument(0);
            if (c.getId() == null) c.setId(1);
            return c;
        });
        given(creatorRepository.save(any())).willAnswer(invocation -> {
            CourseCreator c = invocation.getArgument(0);
            c.setId(9);
            return c;
        });
        given(groupRepository.save(any())).willAnswer(invocation -> {
            CourseGroup g = invocation.getArgument(0);
            if (g.getId() == null) g.setId(4);
            return g;
        });
        given(creatorRepository.findFirstByNameIgnoreCaseOrderByIdAsc("Dan Vega"))
                .willReturn(Optional.of(danVega));
        given(groupRepository.findById(3)).willReturn(Optional.of(java));
    }

    // --- Courses ---

    @Test
    void createCourse_setsFieldsWithZeroProgressByDefault() {
        var result = service.createCourse(request("Dan Vega", 3, null, " https://example.com "));

        assertThat(result.title()).isEqualTo("Spring Boot");
        assertThat(result.creatorName()).isEqualTo("Dan Vega");
        assertThat(result.groupId()).isEqualTo(3);
        assertThat(result.completedLessons()).isZero();
        assertThat(result.url()).isEqualTo("https://example.com");
        assertThat(result.createdAt()).isNotNull();
    }

    @Test
    void createCourse_withoutGroupOrUrl() {
        var result = service.createCourse(request("Dan Vega", null, 5, ""));

        assertThat(result.groupId()).isNull();
        assertThat(result.url()).isNull();
        assertThat(result.completedLessons()).isEqualTo(5);
    }

    @Test
    void createCourse_unknownCreatorIs404() {
        given(creatorRepository.findFirstByNameIgnoreCaseOrderByIdAsc("Nobody")).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.createCourse(request("Nobody", null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(courseRepository, never()).save(any());
    }

    @Test
    void createCourse_unknownGroupIs404() {
        given(groupRepository.findById(99)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.createCourse(request("Dan Vega", 99, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateCourse_canMoveOutOfItsGroup() {
        Course existing = course(40, 12);
        existing.setGroup(java);
        given(courseRepository.findById(1)).willReturn(Optional.of(existing));

        var result = service.updateCourse(1, request("Dan Vega", null, 12, null));

        assertThat(result.groupId()).isNull();
    }

    @Test
    void setProgress_withinTotal() {
        given(courseRepository.findById(1)).willReturn(Optional.of(course(40, 12)));

        assertThat(service.setProgress(1, 40).completedLessons()).isEqualTo(40);
    }

    @Test
    void setProgress_pastTotalIsRejected() {
        given(courseRepository.findById(1)).willReturn(Optional.of(course(40, 12)));

        assertThatThrownBy(() -> service.setProgress(1, 41)).isInstanceOf(InvalidRequestException.class);
        verify(courseRepository, never()).save(any());
    }

    @Test
    void setProgress_unknownCourseIs404() {
        given(courseRepository.findById(5)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.setProgress(5, 1)).isInstanceOf(ResourceNotFoundException.class);
    }

    // --- Cover ---

    @Test
    void coverUploadUrl_makesAUniqueKeyForTheCourse() {
        given(courseRepository.findById(1)).willReturn(Optional.of(course(40, 0)));
        given(s3Service.generatePresignedUploadUrl(startsWith("course-covers/1-"), any()))
                .willAnswer(
                        invocation -> new S3Service.S3PresignedUrlResponse("https://put", invocation.getArgument(0)));

        var result = service.coverUploadUrl(1, "image/png");

        assertThat(result.s3Key()).startsWith("course-covers/1-").endsWith(".png");
    }

    @Test
    void coverUploadUrl_rejectsNonImages() {
        given(courseRepository.findById(1)).willReturn(Optional.of(course(40, 0)));

        assertThatThrownBy(() -> service.coverUploadUrl(1, "application/pdf"))
                .isInstanceOf(InvalidRequestException.class);
        verify(s3Service, never()).generatePresignedUploadUrl(anyString(), anyString());
    }

    @Test
    void attachCover_onlyAcceptsThisCoursesKeys() {
        given(courseRepository.findById(1)).willReturn(Optional.of(course(40, 0)));
        given(s3Service.getPublicS3Url("course-covers/1-abc.jpg")).willReturn("https://bucket/course-covers/1-abc.jpg");

        assertThat(service.attachCover(1, "course-covers/1-abc.jpg").s3Url())
                .isEqualTo("https://bucket/course-covers/1-abc.jpg");
        assertThatThrownBy(() -> service.attachCover(1, "course-covers/12-abc.jpg"))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.attachCover(1, "book-cover.jpg")).isInstanceOf(InvalidRequestException.class);
    }

    // --- Creators ---

    @Test
    void createCreator_newIsTrimmed_existingIsReturned() {
        given(creatorRepository.findFirstByNameIgnoreCaseOrderByIdAsc("Fireship"))
                .willReturn(Optional.empty());

        var created = service.createCreator(new CourseCreatorCreateRequestDTO("  Fireship "));
        var existing = service.createCreator(new CourseCreatorCreateRequestDTO("Dan Vega"));

        assertThat(created.isNew()).isTrue();
        assertThat(created.value().name()).isEqualTo("Fireship");
        assertThat(existing.isNew()).isFalse();
        assertThat(existing.value().id()).isEqualTo(7);
    }

    @Test
    void renameCreator_changesTheName() {
        given(creatorRepository.findById(7)).willReturn(Optional.of(danVega));
        given(creatorRepository.findFirstByNameIgnoreCaseOrderByIdAsc("Dan Vega Jr"))
                .willReturn(Optional.empty());

        assertThat(service.renameCreator(7, new CourseCreatorCreateRequestDTO(" Dan Vega Jr "))
                        .name())
                .isEqualTo("Dan Vega Jr");
    }

    @Test
    void renameCreator_toAnotherCreatorsNameIsRejected() {
        given(creatorRepository.findById(7)).willReturn(Optional.of(danVega));
        given(creatorRepository.findFirstByNameIgnoreCaseOrderByIdAsc("fireship"))
                .willReturn(Optional.of(creator(8, "Fireship")));

        assertThatThrownBy(() -> service.renameCreator(7, new CourseCreatorCreateRequestDTO("fireship")))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void renameCreator_changingOnlyCaseIsAllowed() {
        given(creatorRepository.findById(7)).willReturn(Optional.of(danVega));
        given(creatorRepository.findFirstByNameIgnoreCaseOrderByIdAsc("DAN VEGA"))
                .willReturn(Optional.of(danVega));

        assertThat(service.renameCreator(7, new CourseCreatorCreateRequestDTO("DAN VEGA"))
                        .name())
                .isEqualTo("DAN VEGA");
    }

    @Test
    void deleteCreator_withoutCourses() {
        given(creatorRepository.findById(7)).willReturn(Optional.of(danVega));
        given(courseRepository.countByCreator_Id(7)).willReturn(0L);

        service.deleteCreator(7);

        verify(creatorRepository).delete(danVega);
    }

    @Test
    void deleteCreator_withCoursesIsRefused() {
        given(creatorRepository.findById(7)).willReturn(Optional.of(danVega));
        given(courseRepository.countByCreator_Id(7)).willReturn(3L);

        assertThatThrownBy(() -> service.deleteCreator(7))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("3 courses");
        verify(creatorRepository, never()).delete(any());
    }

    @Test
    void deleteCreator_unknownIs404() {
        given(creatorRepository.findById(99)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteCreator(99)).isInstanceOf(ResourceNotFoundException.class);
    }

    // --- Groups ---

    @Test
    void createGroup_goesLast() {
        given(groupRepository.findFirstByNameIgnoreCaseOrderByIdAsc("Design")).willReturn(Optional.empty());
        given(groupRepository.findTopByOrderByPositionDesc()).willReturn(Optional.of(group(5, "Web", 2)));

        var result = service.createGroup(new CourseGroupRequestDTO("Design"));

        assertThat(result.isNew()).isTrue();
        assertThat(result.value().position()).isEqualTo(3);
    }

    @Test
    void createGroup_firstGroupIsPositionZero() {
        given(groupRepository.findFirstByNameIgnoreCaseOrderByIdAsc("Design")).willReturn(Optional.empty());
        given(groupRepository.findTopByOrderByPositionDesc()).willReturn(Optional.empty());

        assertThat(service.createGroup(new CourseGroupRequestDTO("Design"))
                        .value()
                        .position())
                .isZero();
    }

    @Test
    void renameGroup_toAnotherGroupsNameIsRejected() {
        given(groupRepository.findFirstByNameIgnoreCaseOrderByIdAsc("web")).willReturn(Optional.of(group(5, "Web", 1)));

        assertThatThrownBy(() -> service.renameGroup(3, new CourseGroupRequestDTO("web")))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void renameGroup_changingOnlyCaseIsAllowed() {
        given(groupRepository.findFirstByNameIgnoreCaseOrderByIdAsc("JAVA")).willReturn(Optional.of(java));

        assertThat(service.renameGroup(3, new CourseGroupRequestDTO("JAVA")).name())
                .isEqualTo("JAVA");
    }

    @Test
    void reorderGroups_setsPositions() {
        var web = group(5, "Web", 1);
        given(groupRepository.findAll()).willReturn(List.of(java, web));
        given(groupRepository.findAllByOrderByPositionAscIdAsc()).willReturn(List.of(web, java));

        service.reorderGroups(new CourseGroupOrderRequestDTO(List.of(5, 3)));

        assertThat(web.getPosition()).isZero();
        assertThat(java.getPosition()).isEqualTo(1);
    }

    @Test
    void reorderGroups_mustListEveryGroupOnce() {
        given(groupRepository.findAll()).willReturn(List.of(java, group(5, "Web", 1)));

        assertThatThrownBy(() -> service.reorderGroups(new CourseGroupOrderRequestDTO(List.of(3))))
                .isInstanceOf(InvalidRequestException.class);
        assertThatThrownBy(() -> service.reorderGroups(new CourseGroupOrderRequestDTO(List.of(3, 3))))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void deleteGroup_ungroupsItsCoursesFirst() {
        service.deleteGroup(3);

        verify(courseRepository).ungroupAll(3);
        verify(groupRepository).delete(java);
    }
}
