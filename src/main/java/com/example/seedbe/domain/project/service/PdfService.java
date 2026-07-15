package com.example.seedbe.domain.project.service;

import com.example.seedbe.global.exception.BusinessException;
import com.example.seedbe.global.exception.ErrorType;
import com.example.seedbe.domain.project.component.pdf.PdfDocumentTextExtractor;
import com.example.seedbe.domain.project.component.pdf.PdfTextNormalizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfService {
    private static final int MAX_FILE_COUNT = 2;
    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    static final int MAX_EXTRACTED_TEXT_LENGTH = 300_000;
    private static final byte[] PDF_SIGNATURE = {'%', 'P', 'D', 'F', '-'};

    private final PdfDocumentTextExtractor pdfDocumentTextExtractor;
    private final PdfTextNormalizer textNormalizer;

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
            validatePdf(file);
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

        String normalizedText = textNormalizer.normalizeAndLimit(
                combinedPdfText.toString(), MAX_EXTRACTED_TEXT_LENGTH);
        if (normalizedText.isBlank()) {
            throw new BusinessException(ErrorType.EMPTY_PDF_TEXT);
        }
        return new PdfParseResult(normalizedText);
    }

    private void validatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorType.EMPTY_PDF_TEXT);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorType.FILE_SIZE_EXCEEDED);
        }

        String filename = file.getOriginalFilename();
        String contentType = file.getContentType();
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".pdf")
                || !"application/pdf".equalsIgnoreCase(contentType)
                || !hasPdfSignature(file)) {
            throw new BusinessException(ErrorType.UNSUPPORTED_FILE_TYPE);
        }
    }

    private boolean hasPdfSignature(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            return Arrays.equals(PDF_SIGNATURE, inputStream.readNBytes(PDF_SIGNATURE.length));
        } catch (IOException e) {
            throw new BusinessException(ErrorType.PDF_PARSING_FAILED);
        }
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
