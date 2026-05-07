package com.example.seedbe.domain.project.service;

import com.example.seedbe.global.exception.BusinessException;
import com.example.seedbe.global.exception.ErrorType;
import com.example.seedbe.domain.project.component.pdf.PdfDocumentTextExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfService {
    private static final int MAX_FILE_COUNT = 3;

    private final PdfDocumentTextExtractor pdfDocumentTextExtractor;

    public PdfParseResult parse(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return PdfParseResult.empty();
        }

        if (files.size() > MAX_FILE_COUNT) {
            throw new BusinessException(ErrorType.MAX_FILE_COUNT_EXCEEDED);
        }

        StringBuilder combinedPdfText = new StringBuilder();
        int documentCount = 1;

        for (MultipartFile file : files) {
            String text = extractTextFromPDF(file);

            // 이미지 기반 PDF처럼 텍스트 레이어가 없으면 문서 마커를 붙이지 않는다.
            if (text.isBlank()) {
                documentCount++;
                continue;
            }

            combinedPdfText.append("[문서 ").append(documentCount).append(" 시작]\n")
                    .append(text).append("\n")
                    .append("[문서 ").append(documentCount).append(" 끝]\n\n");

            documentCount++;
        }

        return new PdfParseResult(combinedPdfText.toString().trim());
    }

    public String extractTextFromPDF(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream();
             PDDocument document = PDDocument.load(inputStream)){

            String text = pdfDocumentTextExtractor.extract(document);
            if (text.isBlank()) {
                log.warn("PDF에서 분석 가능한 텍스트를 추출하지 못했습니다.");
                return "";
            }

            return text;
        } catch (IOException e) {
            log.error("PDF 파싱 에러", e);
            throw new BusinessException(ErrorType.PDF_PARSING_FAILED);
        }
    }

    public record PdfParseResult(String text) {
        public static PdfParseResult empty() {
            return new PdfParseResult("");
        }

        public boolean hasExtractedText() {
            return !text.isBlank();
        }
    }
}
