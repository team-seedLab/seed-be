package com.example.seedbe.domain.project.component.pdf;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class TesseractOcrClient {
    private static final int OCR_DPI = 200;
    private static final Duration OCR_TIMEOUT = Duration.ofSeconds(30);
    private static final String TESSERACT_COMMAND = "tesseract";
    private static final String TESSERACT_LANGUAGE = "kor+eng";

    private final PdfTextNormalizer textNormalizer;

    public String extractText(PDFRenderer pdfRenderer, int pageIndex) {
        Path tempDirectory = null;

        try {
            tempDirectory = Files.createTempDirectory("seed-pdf-ocr-");
            Path imagePath = tempDirectory.resolve("page.png");
            Path outputPath = tempDirectory.resolve("ocr-output");
            Path logPath = tempDirectory.resolve("tesseract.log");

            renderPageImage(pdfRenderer, pageIndex, imagePath);
            Process process = startTesseract(imagePath, outputPath, logPath);

            if (!process.waitFor(OCR_TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
                process.destroyForcibly();
                log.warn("PDF OCR timeout. pageIndex: {}", pageIndex);
                return "";
            }

            if (process.exitValue() != 0) {
                String tesseractLog = Files.exists(logPath) ? Files.readString(logPath) : "";
                log.warn("PDF OCR failed. pageIndex: {}, output: {}", pageIndex, tesseractLog);
                return "";
            }

            return readOcrText(outputPath);
        } catch (IOException e) {
            log.warn("PDF OCR 실행 실패. tesseract 설치 또는 이미지 렌더링 상태를 확인해야 합니다.", e);
            return "";
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("PDF OCR interrupted. pageIndex: {}", pageIndex, e);
            return "";
        } finally {
            deleteTempDirectory(tempDirectory);
        }
    }

    private void renderPageImage(PDFRenderer pdfRenderer, int pageIndex, Path imagePath) throws IOException {
        BufferedImage pageImage = pdfRenderer.renderImageWithDPI(pageIndex, OCR_DPI, ImageType.RGB);
        ImageIO.write(pageImage, "png", imagePath.toFile());
    }

    private Process startTesseract(Path imagePath, Path outputPath, Path logPath) throws IOException {
        return new ProcessBuilder(
                TESSERACT_COMMAND,
                imagePath.toString(),
                outputPath.toString(),
                "-l",
                TESSERACT_LANGUAGE,
                "--psm",
                "3"
        ).redirectErrorStream(true)
                .redirectOutput(logPath.toFile())
                .start();
    }

    private String readOcrText(Path outputPath) throws IOException {
        Path ocrTextPath = Path.of(outputPath + ".txt");
        String ocrText = Files.exists(ocrTextPath) ? Files.readString(ocrTextPath) : "";
        return textNormalizer.normalize(ocrText);
    }

    private void deleteTempDirectory(Path tempDirectory) {
        if (tempDirectory == null) {
            return;
        }

        try (var paths = Files.walk(tempDirectory)) {
            for (Path path : paths.sorted((left, right) -> right.compareTo(left)).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            log.warn("임시 OCR 파일 삭제 실패. path: {}", tempDirectory, e);
        }
    }
}
