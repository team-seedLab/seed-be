package com.example.seedbe.domain.project.service;

import com.example.seedbe.domain.project.component.ProjectValidator;
import com.example.seedbe.domain.project.dto.ProjectCreateRequest;
import com.example.seedbe.domain.project.dto.ProjectDetailResponse;
import com.example.seedbe.domain.project.entity.Project;
import com.example.seedbe.domain.project.enums.ProjectStatus;
import com.example.seedbe.domain.project.enums.RoadmapType;
import com.example.seedbe.domain.project.repository.ProjectRepository;
import com.example.seedbe.domain.project.repository.ProjectStepLogRepository;
import com.example.seedbe.domain.user.entity.User;
import com.example.seedbe.domain.user.enums.Role;
import com.example.seedbe.global.exception.BusinessException;
import com.example.seedbe.global.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectValidator projectValidator;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ProjectStepLogRepository stepLogRepository;

    @Mock
    private PdfService pdfService;

    @Mock
    private AIService aiService;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Mock
    private TransactionStatus transactionStatus;

    @Test
    @DisplayName("PDF와 사용자 입력이 모두 비어 있으면 Gemini 호출과 DB 저장을 수행하지 않는다.")
    void createProjectDoesNotCallAiOrSaveWhenThereIsNoContentToAnalyze() {
        ProjectService projectService = createProjectService();
        ProjectCreateRequest request = new ProjectCreateRequest("title", RoadmapType.REPORT, "", null);

        when(pdfService.parse(request.files())).thenReturn(PdfService.PdfParseResult.empty());

        assertThatThrownBy(() -> projectService.createProject(createUser(), request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.NO_CONTENT_TO_ANALYZE);

        verify(aiService, never()).analyzeToJSON(any(), any(), any());
        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    @DisplayName("Gemini 호출이 실패하면 DB 저장을 수행하지 않는다.")
    void createProjectDoesNotSaveWhenAiAnalysisFails() {
        ProjectService projectService = createProjectService();
        ProjectCreateRequest request = new ProjectCreateRequest("title", RoadmapType.REPORT, "요구사항", null);

        when(pdfService.parse(request.files())).thenReturn(new PdfService.PdfParseResult("PDF 텍스트"));
        when(aiService.analyzeToJSON("PDF 텍스트", "요구사항", RoadmapType.REPORT))
                .thenThrow(new BusinessException(ErrorType.AI_SERVER_ERROR));

        assertThatThrownBy(() -> projectService.createProject(createUser(), request))
                .isInstanceOf(BusinessException.class)
                .extracting("errorType")
                .isEqualTo(ErrorType.AI_SERVER_ERROR);

        verify(projectRepository, never()).save(any(Project.class));
    }

    @Test
    @DisplayName("PDF/Gemini 처리가 성공한 뒤 DB 저장 구간만 transaction으로 실행한다.")
    void createProjectSavesProjectInsideTransactionAfterIoWorkSucceeds() {
        ProjectService projectService = createProjectService();
        ProjectCreateRequest request = new ProjectCreateRequest("title", RoadmapType.REPORT, "요구사항", null);
        Map<String, Object> extractedVariables = Map.of("topic", "테스트 주제");

        when(pdfService.parse(request.files())).thenReturn(new PdfService.PdfParseResult("PDF 텍스트"));
        when(aiService.analyzeToJSON("PDF 텍스트", "요구사항", RoadmapType.REPORT)).thenReturn(extractedVariables);
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(transactionStatus);
        });
        when(projectRepository.save(any(Project.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectDetailResponse response = projectService.createProject(createUser(), request);

        ArgumentCaptor<Project> projectCaptor = ArgumentCaptor.forClass(Project.class);
        verify(transactionTemplate).execute(any());
        verify(projectRepository).save(projectCaptor.capture());

        Project savedProject = projectCaptor.getValue();
        assertThat(savedProject.getTitle()).isEqualTo("title");
        assertThat(savedProject.getRoadmapType()).isEqualTo(RoadmapType.REPORT);
        assertThat(savedProject.getStatus()).isEqualTo(ProjectStatus.IN_PROGRESS);
        assertThat(savedProject.getInitialContext()).isEqualTo(extractedVariables);
        assertThat(response.title()).isEqualTo("title");
    }

    private ProjectService createProjectService() {
        return new ProjectService(
                projectValidator,
                projectRepository,
                stepLogRepository,
                pdfService,
                aiService,
                transactionTemplate
        );
    }

    private User createUser() {
        return User.builder()
                .provider("google")
                .providerId("provider-id")
                .email("seed@example.com")
                .nickname("seed")
                .role(Role.ROLE_USER)
                .profileUrl("https://example.com/profile.png")
                .build();
    }
}
