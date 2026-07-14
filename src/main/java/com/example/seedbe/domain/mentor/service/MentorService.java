package com.example.seedbe.domain.mentor.service;

import com.example.seedbe.domain.mentor.dto.MentorDashboardSummary;
import com.example.seedbe.domain.mentor.dto.MentorProjectDetailResponse;
import com.example.seedbe.domain.mentor.dto.MentorProjectStepDetailResponse;
import com.example.seedbe.domain.mentor.dto.MentorProjectSummaryResponse;
import com.example.seedbe.domain.mentor.dto.MentorStudentListResponse;
import com.example.seedbe.domain.mentor.dto.MentorStudentProjectListResponse;
import com.example.seedbe.domain.mentor.dto.MentorStudentSummaryResponse;
import com.example.seedbe.domain.mentor.dto.ProjectReviewResponse;
import com.example.seedbe.domain.mentor.entity.MentorStudent;
import com.example.seedbe.domain.mentor.entity.ProjectReview;
import com.example.seedbe.domain.mentor.enums.ProjectReviewStatus;
import com.example.seedbe.domain.mentor.repository.MentorStudentRepository;
import com.example.seedbe.domain.mentor.repository.ProjectReviewRepository;
import com.example.seedbe.domain.project.entity.Project;
import com.example.seedbe.domain.project.entity.ProjectStep;
import com.example.seedbe.domain.project.enums.ProjectStatus;
import com.example.seedbe.domain.project.enums.ProjectStepStatus;
import com.example.seedbe.domain.project.enums.RoadmapStep;
import com.example.seedbe.domain.project.repository.ProjectRepository;
import com.example.seedbe.domain.project.repository.ProjectStepRepository;
import com.example.seedbe.domain.prompt.entity.ProjectStepPrompt;
import com.example.seedbe.domain.prompt.repository.ProjectStepPromptRepository;
import com.example.seedbe.domain.result.entity.ProjectStepResult;
import com.example.seedbe.domain.result.repository.ProjectStepResultRepository;
import com.example.seedbe.domain.selfcheck.entity.ProjectStepSelfCheck;
import com.example.seedbe.domain.selfcheck.repository.ProjectStepSelfCheckRepository;
import com.example.seedbe.domain.user.entity.User;
import com.example.seedbe.global.exception.BusinessException;
import com.example.seedbe.global.exception.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MentorService {
    private final MentorStudentRepository mentorStudentRepository;
    private final ProjectReviewRepository projectReviewRepository;
    private final ProjectRepository projectRepository;
    private final ProjectStepRepository stepRepository;
    private final ProjectStepPromptRepository promptRepository;
    private final ProjectStepResultRepository resultRepository;
    private final ProjectStepSelfCheckRepository selfCheckRepository;

    @Transactional(readOnly = true)
    public MentorStudentListResponse getStudents(UUID mentorId) {
        List<MentorStudent> assignments = mentorStudentRepository.findAllActiveByMentorId(mentorId);
        if (assignments.isEmpty()) {
            return new MentorStudentListResponse(new MentorDashboardSummary(0, 0, 0), List.of());
        }

        List<UUID> studentIds = assignments.stream()
                .map(assignment -> assignment.getStudent().getUserId())
                .toList();
        List<Project> projects = projectRepository.findAllByStudentIds(studentIds);
        Map<UUID, List<Project>> projectsByStudentId = projects.stream()
                .collect(Collectors.groupingBy(project -> project.getUser().getUserId()));
        Map<UUID, ProjectReviewStatus> reviewStatusByProjectId = getReviewStatusByProjectId(mentorId, projects);

        List<MentorStudentSummaryResponse> students = assignments.stream()
                .map(assignment -> toStudentSummary(
                        assignment.getStudent(),
                        projectsByStudentId.getOrDefault(assignment.getStudent().getUserId(), List.of()),
                        reviewStatusByProjectId
                ))
                .toList();
        long reviewingCount = students.stream()
                .filter(student -> student.reviewStatus() == ProjectReviewStatus.REVIEWING)
                .count();
        long reviewedCount = students.stream()
                .filter(student -> student.reviewStatus() == ProjectReviewStatus.REVIEWED)
                .count();

        return new MentorStudentListResponse(
                new MentorDashboardSummary(students.size(), reviewingCount, reviewedCount),
                students
        );
    }

    @Transactional(readOnly = true)
    public MentorStudentProjectListResponse getStudentProjects(UUID mentorId, UUID studentId) {
        MentorStudent assignment = getActiveAssignment(mentorId, studentId);
        User student = assignment.getStudent();
        List<Project> projects = projectRepository.findAllByStudentIds(List.of(studentId));
        if (projects.isEmpty()) {
            return new MentorStudentProjectListResponse(
                    student.getUserId(), student.getNickname(), student.getEmail(), student.getProfileUrl(), List.of());
        }

        List<ProjectStep> steps = stepRepository.findByProjectsOrderByStepOrderAsc(projects);
        Map<UUID, List<ProjectStep>> stepsByProjectId = steps.stream()
                .collect(Collectors.groupingBy(step -> step.getProject().getProjectId()));
        Map<UUID, ProjectReviewStatus> reviewStatusByProjectId = getReviewStatusByProjectId(mentorId, projects);
        List<MentorProjectSummaryResponse> projectResponses = projects.stream()
                .map(project -> toProjectSummary(
                        project,
                        stepsByProjectId.getOrDefault(project.getProjectId(), List.of()),
                        reviewStatusByProjectId.getOrDefault(
                                project.getProjectId(), ProjectReviewStatus.REVIEWING)
                ))
                .toList();

        return new MentorStudentProjectListResponse(
                student.getUserId(), student.getNickname(), student.getEmail(), student.getProfileUrl(), projectResponses);
    }

    @Transactional(readOnly = true)
    public MentorProjectDetailResponse getProjectDetail(UUID mentorId, UUID projectId) {
        MentorProjectContext context = getAssignedProject(mentorId, projectId);
        Project project = context.project();
        List<ProjectStep> steps = stepRepository.findByProjectOrderByStepOrderAsc(project);

        Map<UUID, ProjectStepPrompt> promptsByStepId = steps.isEmpty()
                ? Map.of()
                : promptRepository.findByStepIn(steps).stream()
                        .collect(Collectors.toMap(prompt -> prompt.getStep().getStepId(), Function.identity()));
        Map<UUID, ProjectStepResult> resultsByStepId = steps.isEmpty()
                ? Map.of()
                : resultRepository.findByStepIn(steps).stream()
                        .collect(Collectors.toMap(result -> result.getStep().getStepId(), Function.identity()));
        Map<UUID, ProjectStepSelfCheck> selfChecksByStepId = steps.isEmpty()
                ? Map.of()
                : selfCheckRepository.findByStepIn(steps).stream()
                        .collect(Collectors.toMap(selfCheck -> selfCheck.getStep().getStepId(), Function.identity()));
        ProjectReview review = projectReviewRepository.findByProjectAndMentor(project, context.assignment().getMentor())
                .orElse(null);

        List<MentorProjectStepDetailResponse> stepResponses = steps.stream()
                .map(step -> toStepDetail(
                        step,
                        promptsByStepId.get(step.getStepId()),
                        resultsByStepId.get(step.getStepId()),
                        selfChecksByStepId.get(step.getStepId())
                ))
                .toList();

        return new MentorProjectDetailResponse(
                project.getProjectId(),
                project.getUser().getUserId(),
                project.getUser().getNickname(),
                project.getTitle(),
                project.getRoadmapType(),
                project.getStatus(),
                project.getDesiredOutcome(),
                project.getKeyFocus(),
                project.getRequiredElements(),
                review == null ? ProjectReviewStatus.REVIEWING : review.getStatus(),
                review == null ? null : review.getReviewedAt(),
                project.getCreatedAt(),
                project.getUpdatedAt(),
                project.getCompletedAt(),
                stepResponses
        );
    }

    @Transactional
    public ProjectReviewResponse completeProjectReview(UUID mentorId, UUID projectId) {
        MentorProjectContext context = getAssignedProjectForUpdate(mentorId, projectId);
        ProjectReview review = projectReviewRepository
                .findByProjectAndMentor(context.project(), context.assignment().getMentor())
                .map(existing -> {
                    existing.markReviewed();
                    return existing;
                })
                .orElseGet(() -> ProjectReview.builder()
                        .project(context.project())
                        .mentor(context.assignment().getMentor())
                        .status(ProjectReviewStatus.REVIEWED)
                        .build());
        projectReviewRepository.saveAndFlush(review);
        return ProjectReviewResponse.from(review);
    }

    private MentorStudentSummaryResponse toStudentSummary(
            User student,
            List<Project> projects,
            Map<UUID, ProjectReviewStatus> reviewStatusByProjectId
    ) {
        int inProgressCount = (int) projects.stream()
                .filter(project -> project.getStatus() == ProjectStatus.IN_PROGRESS)
                .count();
        int completedCount = (int) projects.stream()
                .filter(project -> project.getStatus() == ProjectStatus.COMPLETED)
                .count();
        ProjectReviewStatus aggregateReviewStatus = aggregateReviewStatus(projects, reviewStatusByProjectId);
        LocalDateTime lastProjectUpdatedAt = projects.stream()
                .map(Project::getUpdatedAt)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
        return new MentorStudentSummaryResponse(
                student.getUserId(), student.getNickname(), student.getEmail(), student.getProfileUrl(),
                projects.size(), inProgressCount, completedCount, aggregateReviewStatus, lastProjectUpdatedAt);
    }

    private ProjectReviewStatus aggregateReviewStatus(
            List<Project> projects,
            Map<UUID, ProjectReviewStatus> reviewStatusByProjectId
    ) {
        if (projects.isEmpty()) {
            return null;
        }
        boolean allReviewed = projects.stream()
                .allMatch(project -> reviewStatusByProjectId.get(project.getProjectId()) == ProjectReviewStatus.REVIEWED);
        return allReviewed ? ProjectReviewStatus.REVIEWED : ProjectReviewStatus.REVIEWING;
    }

    private MentorProjectSummaryResponse toProjectSummary(
            Project project,
            List<ProjectStep> steps,
            ProjectReviewStatus reviewStatus
    ) {
        ProjectProgress progress = calculateProgress(steps);
        return new MentorProjectSummaryResponse(
                project.getProjectId(), project.getTitle(), project.getRoadmapType(), project.getStatus(),
                progress.currentRoadmapStep(), progress.currentStepOrder(), progress.totalStepCount(),
                progress.completedStepCount(), progress.progressPercent(), reviewStatus,
                project.getCreatedAt(), project.getUpdatedAt(), project.getCompletedAt());
    }

    private ProjectProgress calculateProgress(List<ProjectStep> steps) {
        List<ProjectStep> orderedSteps = steps.stream()
                .sorted(Comparator.comparing(ProjectStep::getStepOrder))
                .toList();
        int total = orderedSteps.size();
        int completed = (int) orderedSteps.stream()
                .filter(step -> step.getStatus() == ProjectStepStatus.COMPLETED)
                .count();
        ProjectStep current = orderedSteps.stream()
                .filter(step -> step.getStatus() == ProjectStepStatus.IN_PROGRESS)
                .findFirst()
                .or(() -> orderedSteps.stream()
                        .filter(step -> step.getStatus() == ProjectStepStatus.PENDING)
                        .findFirst())
                .orElseGet(() -> orderedSteps.isEmpty() ? null : orderedSteps.getLast());
        return new ProjectProgress(
                current == null ? null : current.getRoadmapStep(),
                current == null ? null : current.getStepOrder(),
                total,
                completed,
                total == 0 ? 0 : completed * 100 / total
        );
    }

    private MentorProjectStepDetailResponse toStepDetail(
            ProjectStep step,
            ProjectStepPrompt prompt,
            ProjectStepResult result,
            ProjectStepSelfCheck selfCheck
    ) {
        MentorProjectStepDetailResponse.PromptDetail promptDetail = prompt == null ? null
                : new MentorProjectStepDetailResponse.PromptDetail(
                        prompt.getProvidedPromptSnapshot(), prompt.getEditedPrompt(), prompt.getAddedCount(),
                        prompt.getRemovedCount(), prompt.getDiffJson());
        MentorProjectStepDetailResponse.ResultDetail resultDetail = result == null ? null
                : new MentorProjectStepDetailResponse.ResultDetail(result.getContentMarkdown());
        MentorProjectStepDetailResponse.SelfCheckDetail selfCheckDetail = selfCheck == null ? null
                : new MentorProjectStepDetailResponse.SelfCheckDetail(
                        selfCheck.getCheckItems(), selfCheck.getSubmittedAt());
        return new MentorProjectStepDetailResponse(
                step.getStepId(), step.getRoadmapStep().getStepCode(), step.getRoadmapStep().getDescription(),
                step.getStepOrder(), step.getStatus(), step.getCompletedAt(), promptDetail, resultDetail, selfCheckDetail);
    }

    private Map<UUID, ProjectReviewStatus> getReviewStatusByProjectId(UUID mentorId, List<Project> projects) {
        if (projects.isEmpty()) {
            return Collections.emptyMap();
        }
        return projectReviewRepository.findByMentorIdAndProjects(mentorId, projects).stream()
                .collect(Collectors.toMap(
                        review -> review.getProject().getProjectId(),
                        ProjectReview::getStatus
                ));
    }

    private MentorStudent getActiveAssignment(UUID mentorId, UUID studentId) {
        return mentorStudentRepository.findActiveAssignment(mentorId, studentId)
                .orElseThrow(() -> new BusinessException(ErrorType.MENTOR_STUDENT_NOT_ASSIGNED));
    }

    private MentorProjectContext getAssignedProject(UUID mentorId, UUID projectId) {
        Project project = projectRepository.findByIdWithUser(projectId)
                .orElseThrow(() -> new BusinessException(ErrorType.PROJECT_NOT_FOUND));
        MentorStudent assignment = getActiveAssignment(mentorId, project.getUser().getUserId());
        return new MentorProjectContext(project, assignment);
    }

    private MentorProjectContext getAssignedProjectForUpdate(UUID mentorId, UUID projectId) {
        Project project = projectRepository.findByIdWithUserForUpdate(projectId)
                .orElseThrow(() -> new BusinessException(ErrorType.PROJECT_NOT_FOUND));
        MentorStudent assignment = getActiveAssignment(mentorId, project.getUser().getUserId());
        return new MentorProjectContext(project, assignment);
    }

    private record ProjectProgress(RoadmapStep currentRoadmapStep, Integer currentStepOrder,
                                   int totalStepCount, int completedStepCount, int progressPercent) {
    }

    private record MentorProjectContext(Project project, MentorStudent assignment) {
    }
}
