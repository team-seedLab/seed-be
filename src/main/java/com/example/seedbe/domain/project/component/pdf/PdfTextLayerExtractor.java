package com.example.seedbe.domain.project.component.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PdfTextLayerExtractor {
    private final PdfTextNormalizer textNormalizer;

    public PdfTextLayerExtractor(PdfTextNormalizer textNormalizer) {
        this.textNormalizer = textNormalizer;
    }

    public String extractPage(PDDocument document, int pageNumber) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);
        stripper.setStartPage(pageNumber);
        stripper.setEndPage(pageNumber);
        return textNormalizer.normalize(stripper.getText(document));
    }
}
