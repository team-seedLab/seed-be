package com.example.seedbe.domain.mentor.service;

import com.example.seedbe.domain.mentor.dto.MentorProjectDetailResponse;
import com.example.seedbe.domain.mentor.dto.MentorStudentListResponse;
import com.example.seedbe.domain.mentor.dto.MentorStudentProjectListResponse;
import com.example.seedbe.domain.mentor.dto.ProjectReviewResponse;
import com.example.seedbe.domain.mentor.entity.MentorStudent;
import com.example.seedbe.domain.mentor.entity.ProjectReview;
import com.example.seedbe.domain.mentor.enums.ProjectReviewStatus;
import com.example.seedbe.domain.mentor.repository.MentorStudentRepository;
import com.example.seedbe.domain.mentor.repository.ProjectReviewRepository;
import com.example.seedbe.domain.project.entity.Project;
import com.example.seedbe.domain.project.entity.ProjectStep;
import com.example.seedbe.domain.project.enums.ProjectStatus;
import com.example.seedbe.domain.project.enums.RoadmapStep;
import com.example.seedbe.domain.project.enums.RoadmapType;
import com.example.seedbe.domain.project.repository.ProjectRepository;
import com.example.seedbe.domain.project.repository.ProjectStepRepository;
import com.example.seedbe.domain.prompt.entity.ProjectStepPrompt;
import com.example.seedbe.domain.prompt.entity.PromptTemplate;
import com.example.seedbe.domain.prompt.repository.ProjectStepPromptRepository;
import com.example.seedbe.domain.result.entity.ProjectStepResult;
import com.example.seedbe.domain.result.repository.ProjectStepResultRepository;
import com.example.seedbe.domain.selfcheck.entity.ProjectStepSelfCheck;
import com.example.seedbe.domain.selfcheck.model.SelfCheckItem;
import com.example.seedbe.domain.selfcheck.repository.ProjectStepSelfCheckRepository;
import com.example.seedbe.domain.user.entity.User;
import com.example.seedbe.domain.user.enums.Role;
import com.example.seedbe.global.exception.BusinessException;
import com.example.seedbe.global.exception.ErrorType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MentorServiceTest {
    @Mock private MentorStudentRepository mentorStudentRepository;
    @Mock private ProjectReviewRepository projectReviewRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectStepRepository stepRepository;
    @Mock private ProjectStepPromptRepository promptRepository;
    @Mock private ProjectStepResultRepository resultRepository;
    @Mock private ProjectStepSelfCheckRepository selfCheckRepository;

    private final UUID mentorId = UUID.randomUUID();

    @Test
    void returnsOnlyAssignedStudentsWithDashboardSummary() {
        User mentor = user(mentorId, "mentor", Role.ROLE_MENTOR);
        User reviewedStudent = user(UUID.randomUUID(), "reviewed", Role.ROLE_USER);
        User reviewingStudent = user(UUID.randomUUID(), "reviewing", Role.ROLE_USER);
        List<MentorStudent> assignments = List.of(
                MentorStudent.builder().mentor(mentor).student(reviewedStudent).build(),
                MentorStudent.builder().mentor(mentor).student(reviewingStudent).build()
        );
        Project reviewedProject = project(reviewedStudent, ProjectStatus.COMPLETED, "검토 완료 프로젝트");
        Project reviewingProject = project(reviewingStudent, ProjectStatus.IN_PROGRESS, "검토중 프로젝트");
        ProjectReview review = ProjectReview.builder()
                .project(reviewedProject).mentor(mentor).status(ProjectReviewStatus.REVIEWED).build();
        when(mentorStudentRepository.findAllActiveByMentorId(mentorId)).thenReturn(assignments);
        when(projectRepository.findAllByStudentIds(List.of(
                reviewedStudent.getUserId(), reviewingStudent.getUserId())))
                .thenReturn(List.of(reviewedProject, reviewingProject));
        when(projectReviewRepository.findByMentorIdAndProjects(
                mentorId, List.of(reviewedProject, reviewingProject))).thenReturn(List.of(review));

        MentorStudentListResponse response = service().getStudents(mentorId);

        assertThat(response.summary().totalStudentCount()).isEqualTo(2);
        assertThat(response.summary().reviewingCount()).isEqualTo(1);
        assertThat(response.summary().reviewedCount()).isEqualTo(1);
        assertThat(response.students()).extracting("studentId")
                .containsExactly(reviewedStudent.getUserId(), reviewingStudent.getUserId());
        assertThat(response.students()).extracting("reviewStatus")
                .containsExactly(ProjectReviewStatus.REVIEWED, ProjectReviewStatus.REVIEWING);
    }

    @Test
    void countsAssignedStudentWithoutProjectsOnlyInTotal() {
        User mentor = user(mentorId, "mentor", Role.ROLE_MENTOR);
        User student = user(UUID.randomUUID(), "student", Role.ROLE_USER);
        when(mentorStudentRepository.findAllActiveByMentorId(mentorId)).thenReturn(List.of(
                MentorStudent.builder().mentor(mentor).student(student).build()));
        when(projectRepository.findAllByStudentIds(List.of(student.getUserId()))).thenReturn(List.of());

        MentorStudentListResponse response = service().getStudents(mentorId);

        assertThat(response.summary().totalStudentCount()).isEqualTo(1);
        assertThat(response.summary().reviewingCount()).isZero();
        assertThat(response.summary().reviewedCount()).isZero();
        assertThat(response.students()).singleElement().satisfies(item -> {
            assertThat(item.totalProjectCount()).isZero();
            assertThat(item.reviewStatus()).isNull();
        });
        verify(projectReviewRepository, never()).findByMentorIdAndProjects(any(), any());
    }

    @Test
    void returnsAssignedStudentsProjectsWithProgress() {
        User mentor = user(mentorId, "mentor", Role.ROLE_MENTOR);
        User student = user(UUID.randomUUID(), "student", Role.ROLE_USER);
        MentorStudent assignment = MentorStudent.builder().mentor(mentor).student(student).build();
        Project project = project(student, ProjectStatus.IN_PROGRESS, "진행 프로젝트");
        ProjectStep completed = step(project, RoadmapStep.CONSTRAINT_ANALYSIS, 1);
        completed.complete();
        ProjectStep current = step(project, RoadmapStep.ARGUMENT_STRUCTURING, 2);
        current.start();
        when(mentorStudentRepository.findActiveAssignment(mentorId, student.getUserId()))
                .thenReturn(Optional.of(assignment));
        when(projectRepository.findAllByStudentIds(List.of(student.getUserId()))).thenReturn(List.of(project));
        when(stepRepository.findByProjectsOrderByStepOrderAsc(List.of(project)))
                .thenReturn(List.of(completed, current));
        when(projectReviewRepository.findByMentorIdAndProjects(mentorId, List.of(project)))
                .thenReturn(List.of());

        MentorStudentProjectListResponse response = service().getStudentProjects(mentorId, student.getUserId());

        assertThat(response.projects()).singleElement().satisfies(item -> {
            assertThat(item.currentRoadmapStep()).isEqualTo(RoadmapStep.ARGUMENT_STRUCTURING);
            assertThat(item.completedStepCount()).isEqualTo(1);
            assertThat(item.progressPercent()).isEqualTo(50);
            assertThat(item.reviewStatus()).isEqualTo(ProjectReviewStatus.REVIEWING);
        });
    }

    @Test
    void rejectsUnassignedStudentAccess() {
        UUID studentId = UUID.randomUUID();
        when(mentorStudentRepository.findActiveAssignment(mentorId, studentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getStudentProjects(mentorId, studentId))
                .isInstanceOf(BusinessException.class)
                .extracting("errorType").isEqualTo(ErrorType.MENTOR_STUDENT_NOT_ASSIGNED);
        verify(projectRepository, never()).findAllByStudentIds(any());
    }

    @Test
    void returnsProjectDetailWithPromptResultAndSelfCheckButNoAiMessages() {
        User mentor = user(mentorId, "mentor", Role.ROLE_MENTOR);
        User student = user(UUID.randomUUID(), "student", Role.ROLE_USER);
        MentorStudent assignment = MentorStudent.builder().mentor(mentor).student(student).build();
        Project project = project(student, ProjectStatus.IN_PROGRESS, "상세 프로젝트");
        ProjectStep step = step(project, RoadmapStep.CONSTRAINT_ANALYSIS, 1);
        ProjectStepPrompt prompt = ProjectStepPrompt.builder()
                .step(step).providedPromptSnapshot("original").editedPrompt("edited")
                .addedCount(2).removedCount(1).diffJson(Map.of("version", "v1")).build();
        ProjectStepResult result = ProjectStepResult.builder().step(step).contentMarkdown("# 결과물").build();
        ProjectStepSelfCheck selfCheck = ProjectStepSelfCheck.builder()
                .step(step)
                .checkItems(List.of(new SelfCheckItem("core_understanding", "질문", "충분한 길이의 답변입니다.")))
                .build();
        when(projectRepository.findByIdWithUser(project.getProjectId())).thenReturn(Optional.of(project));
        when(mentorStudentRepository.findActiveAssignment(mentorId, student.getUserId()))
                .thenReturn(Optional.of(assignment));
        when(stepRepository.findByProjectOrderByStepOrderAsc(project)).thenReturn(List.of(step));
        when(promptRepository.findByStepIn(List.of(step))).thenReturn(List.of(prompt));
        when(resultRepository.findByStepIn(List.of(step))).thenReturn(List.of(result));
        when(selfCheckRepository.findByStepIn(List.of(step))).thenReturn(List.of(selfCheck));
        when(projectReviewRepository.findByProjectAndMentor(project, mentor)).thenReturn(Optional.empty());

        MentorProjectDetailResponse response = service().getProjectDetail(mentorId, project.getProjectId());

        assertThat(response.reviewStatus()).isEqualTo(ProjectReviewStatus.REVIEWING);
        assertThat(response.steps()).singleElement().satisfies(item -> {
            assertThat(item.prompt().providedPromptSnapshot()).isEqualTo("original");
            assertThat(item.prompt().editedPrompt()).isEqualTo("edited");
            assertThat(item.result().contentMarkdown()).isEqualTo("# 결과물");
            assertThat(item.selfCheck().checkItems()).hasSize(1);
        });
        assertThat(MentorProjectDetailResponse.class.getRecordComponents())
                .extracting(component -> component.getName())
                .doesNotContain("aiMessages");
    }

    @Test
    void rejectsUnassignedProjectAccess() {
        User student = user(UUID.randomUUID(), "student", Role.ROLE_USER);
        Project project = project(student, ProjectStatus.IN_PROGRESS, "타 학생 프로젝트");
        when(projectRepository.findByIdWithUser(project.getProjectId())).thenReturn(Optional.of(project));
        when(mentorStudentRepository.findActiveAssignment(mentorId, student.getUserId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getProjectDetail(mentorId, project.getProjectId()))
                .isInstanceOf(BusinessException.class)
                .extracting("errorType").isEqualTo(ErrorType.MENTOR_STUDENT_NOT_ASSIGNED);
        verify(stepRepository, never()).findByProjectOrderByStepOrderAsc(any());
    }

    @Test
    void createsReviewedProjectReview() {
        User mentor = user(mentorId, "mentor", Role.ROLE_MENTOR);
        User student = user(UUID.randomUUID(), "student", Role.ROLE_USER);
        MentorStudent assignment = MentorStudent.builder().mentor(mentor).student(student).build();
        Project project = project(student, ProjectStatus.COMPLETED, "검토 프로젝트");
        when(projectRepository.findByIdWithUserForUpdate(project.getProjectId())).thenReturn(Optional.of(project));
        when(mentorStudentRepository.findActiveAssignment(mentorId, student.getUserId()))
                .thenReturn(Optional.of(assignment));
        when(projectReviewRepository.findByProjectAndMentor(project, mentor)).thenReturn(Optional.empty());
        when(projectReviewRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectReviewResponse response = service().completeProjectReview(
                mentorId, project.getProjectId());

        assertThat(response.status()).isEqualTo(ProjectReviewStatus.REVIEWED);
        assertThat(response.reviewedAt()).isNotNull();
        verify(projectReviewRepository).saveAndFlush(any(ProjectReview.class));
    }

    @Test
    void marksExistingReviewAsReviewed() {
        User mentor = user(mentorId, "mentor", Role.ROLE_MENTOR);
        User student = user(UUID.randomUUID(), "student", Role.ROLE_USER);
        MentorStudent assignment = MentorStudent.builder().mentor(mentor).student(student).build();
        Project project = project(student, ProjectStatus.COMPLETED, "검토 프로젝트");
        ProjectReview review = ProjectReview.builder()
                .project(project).mentor(mentor).build();
        when(projectRepository.findByIdWithUserForUpdate(project.getProjectId())).thenReturn(Optional.of(project));
        when(mentorStudentRepository.findActiveAssignment(mentorId, student.getUserId()))
                .thenReturn(Optional.of(assignment));
        when(projectReviewRepository.findByProjectAndMentor(project, mentor)).thenReturn(Optional.of(review));
        when(projectReviewRepository.saveAndFlush(review)).thenReturn(review);

        ProjectReviewResponse response = service().completeProjectReview(
                mentorId, project.getProjectId());

        assertThat(response.status()).isEqualTo(ProjectReviewStatus.REVIEWED);
        assertThat(response.reviewedAt()).isNotNull();
    }

    private MentorService service() {
        return new MentorService(mentorStudentRepository, projectReviewRepository, projectRepository,
                stepRepository, promptRepository, resultRepository, selfCheckRepository);
    }

    private User user(UUID userId, String nickname, Role role) {
        User user = User.builder().provider("local").email(nickname + "@seed.test")
                .nickname(nickname).role(role).build();
        ReflectionTestUtils.setField(user, "userId", userId);
        return user;
    }

    private Project project(User student, ProjectStatus status, String title) {
        Project project = Project.builder().user(student).title(title).roadmapType(RoadmapType.REPORT)
                .status(status).initialContext(Map.of()).build();
        ReflectionTestUtils.setField(project, "projectId", UUID.randomUUID());
        return project;
    }

    private ProjectStep step(Project project, RoadmapStep roadmapStep, int order) {
        ProjectStep step = ProjectStep.builder().project(project)
                .promptTemplate(org.mockito.Mockito.mock(PromptTemplate.class))
                .roadmapStep(roadmapStep).stepOrder(order).build();
        ReflectionTestUtils.setField(step, "stepId", UUID.randomUUID());
        return step;
    }
}
