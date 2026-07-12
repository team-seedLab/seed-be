package com.example.seedbe.domain.project.component.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PdfDocumentTextExtractorTest {
    @Test
    void usesOcrTextWhenTextLayerIsEmptyAndPageHasImage() throws Exception {
        PdfImageDetector imageDetector = mock(PdfImageDetector.class);
        TesseractOcrClient ocrClient = mock(TesseractOcrClient.class);
        PdfTextLayerExtractor textLayerExtractor = mock(PdfTextLayerExtractor.class);
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            when(textLayerExtractor.extractPage(document, 1)).thenReturn("");
            when(imageDetector.hasImage(page)).thenReturn(true);
            when(ocrClient.extractText(any(), org.mockito.ArgumentMatchers.eq(0))).thenReturn("OCR text");

            String result = new PdfDocumentTextExtractor(
                    imageDetector, ocrClient, textLayerExtractor).extract(document);

            assertThat(result).contains("OCR text");
        }
    }

    @Test
    void mergesOcrTextWhenTextLayerAndImageBothExist() throws Exception {
        PdfImageDetector imageDetector = mock(PdfImageDetector.class);
        TesseractOcrClient ocrClient = mock(TesseractOcrClient.class);
        PdfTextLayerExtractor textLayerExtractor = mock(PdfTextLayerExtractor.class);
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            when(textLayerExtractor.extractPage(document, 1)).thenReturn("text layer");
            when(imageDetector.hasImage(page)).thenReturn(true);
            when(ocrClient.extractText(any(), org.mockito.ArgumentMatchers.eq(0))).thenReturn("chart labels");

            String result = new PdfDocumentTextExtractor(
                    imageDetector, ocrClient, textLayerExtractor).extract(document);

            assertThat(result).contains("text layer");
            assertThat(result).contains("[이미지 OCR 텍스트]\nchart labels");
            verify(ocrClient).extractText(any(), anyInt());
        }
    }

    @Test
    void doesNotAppendOcrTextWhenItMatchesTextLayer() throws Exception {
        PdfImageDetector imageDetector = mock(PdfImageDetector.class);
        TesseractOcrClient ocrClient = mock(TesseractOcrClient.class);
        PdfTextLayerExtractor textLayerExtractor = mock(PdfTextLayerExtractor.class);
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            when(textLayerExtractor.extractPage(document, 1)).thenReturn("same text");
            when(imageDetector.hasImage(page)).thenReturn(true);
            when(ocrClient.extractText(any(), anyInt())).thenReturn("same text");

            String result = new PdfDocumentTextExtractor(
                    imageDetector, ocrClient, textLayerExtractor).extract(document);

            assertThat(result).isEqualTo("[페이지 1]\nsame text");
        }
    }
}
