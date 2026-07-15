package com.example.seedbe.domain.project.service;

import com.example.seedbe.domain.project.component.pdf.PdfTextNormalizer;
import com.example.seedbe.domain.project.component.pdf.PdfDocumentTextExtractor;
import com.example.seedbe.domain.project.component.pdf.PdfImageDetector;
import com.example.seedbe.domain.project.component.pdf.PdfTextLayerExtractor;
import com.example.seedbe.domain.project.component.pdf.TesseractOcrClient;
import com.example.seedbe.global.exception.BusinessException;
import com.example.seedbe.global.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.ByteArrayOutputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfServiceTest {

    private final PdfTextNormalizer textNormalizer = new PdfTextNormalizer();

    @Test
    @DisplayName("PDF 텍스트 정규화 시 줄바꿈은 유지하고 가로 공백만 줄인다.")
    void normalizePreservesLineBreaksAndNormalizesHorizontalWhitespace() {
        String text = """
                first    line
                second\t\tline

                third      line
                """;

        String normalizedText = textNormalizer.normalize(text);

        assertThat(normalizedText).contains("first line\nsecond line");
        assertThat(normalizedText).contains("\nthird line");
        assertThat(normalizedText).doesNotContain("first line second line");
        assertThat(normalizedText).doesNotContain("    ");
    }

    @Test
    @DisplayName("PDF fixture에서 실제 텍스트를 추출한다.")
    void parseExtractsTextFromPdfFixture() throws Exception {
        try (InputStream input = Files.newInputStream(
                Path.of("src/test/resources/fixtures/text-assignment.pdf"))) {
            MockMultipartFile file = new MockMultipartFile(
                    "files", "assignment.pdf", "application/pdf", input);

            PdfService.PdfParseResult result = createPdfService().parse(List.of(file));

            assertThat(result.text()).contains("SEED assignment fixture text");
            assertThat(result.text()).contains("[문서 1 시작]");
        }
    }

    @Test
    @DisplayName("확장자만 PDF인 파일은 signature 검증에서 거부한다.")
    void parseRejectsInvalidPdfSignature() {
        MockMultipartFile file = new MockMultipartFile(
                "files", "fake.pdf", "application/pdf", "not a pdf".getBytes());

        assertThatThrownBy(() -> createPdfService().parse(List.of(file)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorType").isEqualTo(ErrorType.UNSUPPORTED_FILE_TYPE);
    }

    @Test
    @DisplayName("텍스트가 없는 PDF는 명확한 예외를 반환한다.")
    void parseRejectsPdfWithoutExtractableText() throws Exception {
        byte[] emptyPdf;
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.save(output);
            emptyPdf = output.toByteArray();
        }
        MockMultipartFile file = new MockMultipartFile(
                "files", "empty.pdf", "application/pdf", emptyPdf);

        assertThatThrownBy(() -> createPdfService().parse(List.of(file)))
                .isInstanceOf(BusinessException.class)
                .extracting("errorType").isEqualTo(ErrorType.EMPTY_PDF_TEXT);
    }

    @Test
    @DisplayName("정규화된 PDF 원문은 40페이지 문서를 수용할 수 있는 최대 길이를 넘지 않는다.")
    void normalizeAndLimitCapsContextLength() {
        String result = textNormalizer.normalizeAndLimit(
                "a".repeat(PdfService.MAX_EXTRACTED_TEXT_LENGTH + 100),
                PdfService.MAX_EXTRACTED_TEXT_LENGTH);

        assertThat(result).hasSize(PdfService.MAX_EXTRACTED_TEXT_LENGTH);
    }

    @Test
    @DisplayName("최대 길이 경계에서 Unicode surrogate pair를 분리하지 않는다.")
    void normalizeAndLimitDoesNotSplitSurrogatePair() {
        String result = textNormalizer.normalizeAndLimit("1234😀after", 5);

        assertThat(result).isEqualTo("1234");
        assertThat(result).doesNotContain("�");
    }

    private PdfService createPdfService() {
        PdfTextLayerExtractor textLayerExtractor = new PdfTextLayerExtractor(textNormalizer);
        TesseractOcrClient ocrClient = new TesseractOcrClient(textNormalizer);
        PdfDocumentTextExtractor documentExtractor = new PdfDocumentTextExtractor(
                new PdfImageDetector(), ocrClient, textLayerExtractor);
        return new PdfService(documentExtractor, textNormalizer);
    }
}
