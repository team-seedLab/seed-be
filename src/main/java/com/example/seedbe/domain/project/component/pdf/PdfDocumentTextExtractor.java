package com.example.seedbe.domain.project.component.pdf;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class PdfDocumentTextExtractor {
    private static final int MAX_OCR_PAGE_COUNT_PER_FILE = 5;

    private final PdfImageDetector pdfImageDetector;
    private final TesseractOcrClient tesseractOcrClient;
    private final PdfTextLayerExtractor textLayerExtractor;

    public String extract(PDDocument document) throws IOException {
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        StringBuilder documentText = new StringBuilder();

        int ocrPageCount = 0;

        for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
            int pageNumber = pageIndex + 1;
            String pageText = textLayerExtractor.extractPage(document, pageNumber);
            PDPage page = document.getPage(pageIndex);

            if (pdfImageDetector.hasImage(page) && ocrPageCount < MAX_OCR_PAGE_COUNT_PER_FILE) {
                String ocrText = tesseractOcrClient.extractText(pdfRenderer, pageIndex);
                pageText = mergePageText(pageText, ocrText);
                ocrPageCount++;
            }

            appendPageText(documentText, pageNumber, pageText);
        }

        return documentText.toString().trim();
    }

    private String mergePageText(String pageText, String ocrText) {
        if (ocrText.isBlank() || ocrText.equals(pageText)) {
            return pageText;
        }
        if (pageText.isBlank()) {
            return ocrText;
        }
        return pageText + "\n[이미지 OCR 텍스트]\n" + ocrText;
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
