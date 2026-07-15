package com.example.seedbe.domain.project.component.pdf;

import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PdfDocumentTextExtractor {
    private static final int MAX_OCR_PAGE_COUNT_PER_FILE = 8;

    private final PdfImageDetector pdfImageDetector;
    private final TesseractOcrClient tesseractOcrClient;
    private final PdfTextLayerExtractor textLayerExtractor;

    public String extract(PDDocument document) throws IOException {
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        PDFTextStripper textStripper = new PDFTextStripper();
        textStripper.setSortByPosition(true);
        List<PageExtraction> pages = new ArrayList<>();

        for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
            int pageNumber = pageIndex + 1;
            String pageText = textLayerExtractor.extractPage(textStripper, document, pageNumber);
            PDPage page = document.getPage(pageIndex);
            pages.add(new PageExtraction(pageIndex, pageNumber, pageText, pdfImageDetector.hasImage(page)));
        }

        Set<Integer> ocrPageIndexes = selectOcrPageIndexes(pages);
        StringBuilder documentText = new StringBuilder();
        for (PageExtraction page : pages) {
            String pageText = page.textLayer();
            if (ocrPageIndexes.contains(page.pageIndex())) {
                String ocrText = tesseractOcrClient.extractText(pdfRenderer, page.pageIndex());
                pageText = mergePageText(pageText, ocrText);
            }
            appendPageText(documentText, page.pageNumber(), pageText);
        }

        return documentText.toString().trim();
    }

    private Set<Integer> selectOcrPageIndexes(List<PageExtraction> pages) {
        Set<Integer> selected = new LinkedHashSet<>();
        addOcrCandidates(pages, selected, true);
        addOcrCandidates(pages, selected, false);
        return selected;
    }

    private void addOcrCandidates(List<PageExtraction> pages, Set<Integer> selected,
                                  boolean requireBlankTextLayer) {
        for (PageExtraction page : pages) {
            if (selected.size() >= MAX_OCR_PAGE_COUNT_PER_FILE) {
                return;
            }
            if (page.hasImage() && page.textLayer().isBlank() == requireBlankTextLayer) {
                selected.add(page.pageIndex());
            }
        }
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

    private record PageExtraction(int pageIndex, int pageNumber, String textLayer, boolean hasImage) {
    }
}
