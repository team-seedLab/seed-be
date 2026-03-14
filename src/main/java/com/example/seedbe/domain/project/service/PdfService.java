package com.example.seedbe.domain.project.service;

import com.example.seedbe.global.exception.BusinessException;
import com.example.seedbe.global.exception.ErrorType;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Slf4j
@Service
public class PdfService {
    private static final int MAX_FILE_COUNT = 3;

    public String combineTexts(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return "";
        }

        if (files.size() > MAX_FILE_COUNT) {
            throw new BusinessException(ErrorType.MAX_FILE_COUNT_EXCEEDED);
        }

        StringBuilder combinedPdfText = new StringBuilder();
        int documentCount = 1;

        for (MultipartFile file : files) {
            String text = extractTextFromPDF(file);

            combinedPdfText.append("[문서 ").append(documentCount).append(" 시작]\n")
                    .append(text).append("\n")
                    .append("[문서 ").append(documentCount).append(" 끝]\n\n");

            documentCount++;
        }

        return combinedPdfText.toString();
    }

    public String extractTextFromPDF(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream();
             PDDocument document = PDDocument.load(inputStream)){

            PDFTextStripper pdfStripper = new PDFTextStripper();
            String text = pdfStripper.getText(document);

            if (text == null || text.trim().isEmpty()) {
                log.warn("이미지 PDF 의심: 텍스트 추출 불가");
                return ""; // 에러 던지는 대신 빈칸 리턴 (유저 텍스트로 커버 가능하게)
            }

            return text.trim();
        } catch (IOException e) {
            log.error("PDF 파싱 에러", e);
            throw new BusinessException(ErrorType.PDF_PARSING_FAILED);
        }
    }
}
