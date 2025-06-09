package pl.jit.robotsystem.service.pdf;

import lombok.extern.java.Log;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@Log
public class PDFService {

    public boolean isValidPdf(File pdfFile) {
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            return document.getNumberOfPages() > 0;
        } catch (Exception e) {
            log.warning("Invalid PDF file: " + pdfFile.getName() + " - " + e.getMessage());
            return false;
        }
    }

    public int pageCount(File pdfFile) {
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            return document.getNumberOfPages();
        } catch (IOException e) {
            log.severe("Error reading PDF page count: " + pdfFile.getName() + " - " + e.getMessage());
            throw new RuntimeException("Failed to read PDF page count", e);
        }
    }

    public String getTextFromPage(File pdfFile, int pageNo) {
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            if (pageNo < 1 || pageNo > document.getNumberOfPages()) {
                throw new IllegalArgumentException("Page number out of range: " + pageNo);
            }

            PDFTextStripper textStripper = new PDFTextStripper();
            textStripper.setStartPage(pageNo);
            textStripper.setEndPage(pageNo);

            String text = textStripper.getText(document).trim();

            // Save to file
            String baseName = pdfFile.getName().replaceAll("\\.pdf$", "");
            String fileName = String.format("%s-strona-%02d.txt", baseName, pageNo);
            Path targetDir = pdfFile.toPath().getParent();
            File outputFile = new File(targetDir.toFile(), fileName);

            Files.writeString(outputFile.toPath(), text, StandardCharsets.UTF_8);

            return text;
        } catch (IOException e) {
            log.severe("Error extracting text from page " + pageNo + ": " + pdfFile.getName() + " - " + e.getMessage());
            throw new RuntimeException("Failed to extract text from PDF", e);
        }
    }

    public File getImageFromPage(File pdfFile, int pageNo, String fileName) {
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            if (pageNo < 1 || pageNo > document.getNumberOfPages()) {
                throw new IllegalArgumentException("Page number out of range: " + pageNo);
            }

            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(pageNo - 1, 300);

            Path targetDir = pdfFile.toPath().getParent();
            File outputFile = new File(targetDir.toFile(), fileName);
            outputFile.getParentFile().mkdirs();

            ImageIO.write(image, "PNG", outputFile);
            return outputFile;
        } catch (IOException e) {
            log.severe("Error extracting image from page " + pageNo + ": " + pdfFile.getName() + " - " + e.getMessage());
            throw new RuntimeException("Failed to extract image from PDF", e);
        }
    }
}