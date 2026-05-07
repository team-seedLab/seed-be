package com.example.seedbe.domain.project.component.pdf;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class PdfDocumentTextExtractor {
    private static final int MAX_OCR_PAGE_COUNT_PER_FILE = 10;

    private final PdfImageDetector pdfImageDetector;
    private final TesseractOcrClient tesseractOcrClient;
    private final PdfTextNormalizer textNormalizer;

    public String extract(PDDocument document) throws IOException {
        PDFTextStripper pdfStripper = new PDFTextStripper();
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        StringBuilder documentText = new StringBuilder();
        int ocrPageCount = 0;

        for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
            int pageNumber = pageIndex + 1;
            String pageText = extractPageText(pdfStripper, document, pageNumber);
            PDPage page = document.getPage(pageIndex);

            // 이미지가 포함된 페이지는 캡처/스캔 텍스트 누락 가능성이 있어 OCR을 병행한다.
            if (pdfImageDetector.hasImage(page) && ocrPageCount < MAX_OCR_PAGE_COUNT_PER_FILE) {
                String ocrText = tesseractOcrClient.extractText(pdfRenderer, pageIndex);
                pageText = mergePageText(pageText, ocrText);
                ocrPageCount++;
            }

            appendPageText(documentText, pageNumber, pageText);
        }

        return documentText.toString().trim();
    }

    private String extractPageText(PDFTextStripper pdfStripper, PDDocument document, int pageNumber) throws IOException {
        pdfStripper.setStartPage(pageNumber);
        pdfStripper.setEndPage(pageNumber);
        return textNormalizer.normalize(pdfStripper.getText(document));
    }

    private String mergePageText(String pageText, String ocrText) {
        if (ocrText.isBlank()) {
            return pageText;
        }

        if (pageText.isBlank()) {
            return ocrText;
        }

        return pageText + "\n[OCR 텍스트]\n" + ocrText;
    }

    private void appendPageText(StringBuilder documentText, int pageNumber, String pageText) {
        if (pageText.isBlank()) {
            return;
        }

        documentText.append("[페이지 ").append(pageNumber).append("]\n")
                .append(pageText)
                .append("\n\n");
    }
}
