package com.example.seedbe.domain.project.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import static org.assertj.core.api.Assertions.assertThat;

class PdfServiceTest {

    private final PdfService pdfService = new PdfService();

    @Test
    @DisplayName("PDF 텍스트 정규화 시 줄바꿈은 유지하고 가로 공백만 줄인다.")
    void parsePreservesLineBreaksAndNormalizesHorizontalWhitespace() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "assignment.pdf",
                "application/pdf",
                createPdf("""
                        first    line
                        second    line
                        """)
        );

        PdfService.PdfParseResult result = pdfService.parse(List.of(file));

        assertThat(result.text()).contains("first line\nsecond line");
        assertThat(result.text()).doesNotContain("first line second line");
        assertThat(result.text()).doesNotContain("    ");
    }

    private byte[] createPdf(String text) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 12);
                contentStream.newLineAtOffset(50, 700);

                for (String line : text.split("\\R", -1)) {
                    contentStream.showText(line);
                    contentStream.newLineAtOffset(0, -16);
                }

                contentStream.endText();
            }

            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }
}
