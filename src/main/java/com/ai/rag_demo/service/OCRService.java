package com.ai.rag_demo.service;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
@Slf4j
public class OCRService {

    private final Tesseract tesseract;
    private static final int MIN_TEXT_LENGTH_FOR_SEARCHABLE_PDF = 50; // Lowered threshold
    private static final int DPI = 300; // High DPI for better OCR accuracy
    private static final boolean ALWAYS_RUN_OCR = true; // Always run OCR to catch images

    public OCRService() {
        this.tesseract = new Tesseract();
        configureTesseract();
    }

    private void configureTesseract() {
        try {
            // Set Tesseract data path (tessdata)
            // Try multiple common locations
            String[] possiblePaths = {
                    "/usr/share/tesseract-ocr/5/tessdata",
                    "/usr/local/share/tessdata",
                    "/opt/homebrew/share/tessdata",
                    System.getProperty("user.home") + "/tessdata",
                    "./tessdata"
            };

            String tessdataPath = null;
            for (String path : possiblePaths) {
                File dir = new File(path);
                if (dir.exists() && dir.isDirectory()) {
                    tessdataPath = path;
                    log.info("Found tessdata at: {}", path);
                    break;
                }
            }

            if (tessdataPath != null) {
                tesseract.setDatapath(tessdataPath);
            } else {
                log.warn("Tessdata not found in common locations. OCR may not work.");
                log.warn("Install Tesseract: brew install tesseract (Mac) or apt-get install tesseract-ocr (Linux)");
            }

            // Configure OCR settings for better text detection in images
            tesseract.setLanguage("eng"); // English language
            tesseract.setPageSegMode(3); // Fully automatic page segmentation (better for images)
            tesseract.setOcrEngineMode(1); // Neural nets LSTM engine only

            // Additional configuration for better accuracy
            tesseract.setVariable("tessedit_char_whitelist",
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789.,!?:;-()[]{}@#$%&*+=/\\|'\" \n\t");

            log.info("Tesseract OCR initialized successfully with enhanced settings");
        } catch (Exception e) {
            log.error("Failed to initialize Tesseract OCR", e);
        }
    }

    /**
     * Process PDF and extract text using OCR if needed
     */
    public String extractTextFromPDF(InputStream inputStream) throws IOException {
        // Save input stream to temporary file
        Path tempPdfPath = Files.createTempFile("pdf-", ".pdf");
        try {
            Files.copy(inputStream, tempPdfPath, StandardCopyOption.REPLACE_EXISTING);
            return extractTextFromPDF(tempPdfPath.toFile());
        } finally {
            Files.deleteIfExists(tempPdfPath);
        }
    }

    /**
     * Extract text from PDF file, using OCR to capture text from images
     */
    public String extractTextFromPDF(File pdfFile) throws IOException {
        log.info("Processing PDF: {}", pdfFile.getName());

        try (PDDocument document = PDDocument.load(pdfFile)) {
            // First, try to extract text directly
            PDFTextStripper stripper = new PDFTextStripper();
            String extractedText = stripper.getText(document);

            // ALWAYS run OCR to capture text from images within PDFs
            if (ALWAYS_RUN_OCR) {
                log.info("Running OCR on all pages to extract text from images...");
                String ocrText = performOCROnPDF(document);

                // Combine extracted text with OCR text
                StringBuilder combinedText = new StringBuilder();

                if (extractedText != null && !extractedText.trim().isEmpty()) {
                    combinedText.append("=== EXTRACTED TEXT ===\n");
                    combinedText.append(extractedText);
                    combinedText.append("\n\n");
                }

                if (ocrText != null && !ocrText.trim().isEmpty()) {
                    combinedText.append("=== TEXT FROM IMAGES (OCR) ===\n");
                    combinedText.append(ocrText);
                }

                String result = combinedText.toString();
                log.info("Combined extraction: {} characters from text, {} from OCR, total: {}",
                        extractedText.length(), ocrText.length(), result.length());
                return result;
            }

            // Check if PDF contains enough text (is searchable)
            if (isSearchablePDF(extractedText, document.getNumberOfPages())) {
                log.info("PDF is already searchable. Extracted {} characters", extractedText.length());
                return extractedText;
            }

            // PDF is likely scanned/image-based - use OCR
            log.info("PDF appears to be scanned/image-based. Running OCR...");
            return performOCROnPDF(document);

        } catch (Exception e) {
            log.error("Error processing PDF", e);
            throw new IOException("Failed to extract text from PDF", e);
        }
    }

    /**
     * Check if PDF is searchable based on text content
     */
    private boolean isSearchablePDF(String text, int pageCount) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }

        // Remove whitespace and check length
        String cleanText = text.replaceAll("\\s+", "");
        int textLength = cleanText.length();

        // Consider searchable if has reasonable amount of text per page
        int minExpectedLength = MIN_TEXT_LENGTH_FOR_SEARCHABLE_PDF * pageCount;
        boolean isSearchable = textLength >= minExpectedLength;

        log.debug("PDF text length: {}, pages: {}, min expected: {}, searchable: {}",
                textLength, pageCount, minExpectedLength, isSearchable);

        return isSearchable;
    }

    /**
     * Perform OCR on all pages of a PDF
     */
    private String performOCROnPDF(PDDocument document) throws IOException {
        StringBuilder ocrText = new StringBuilder();
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        int totalPages = document.getNumberOfPages();

        log.info("Starting OCR on {} pages...", totalPages);

        for (int page = 0; page < totalPages; page++) {
            try {
                log.debug("Processing page {}/{}", page + 1, totalPages);

                // Render page to image at high DPI for better OCR
                BufferedImage image = pdfRenderer.renderImageWithDPI(page, DPI);

                // Perform OCR on the image
                String pageText = performOCROnImage(image);

                if (pageText != null && !pageText.trim().isEmpty()) {
                    ocrText.append("--- Page ").append(page + 1).append(" ---\n");
                    ocrText.append(pageText);
                    ocrText.append("\n\n");
                }

                log.debug("Page {} OCR completed. Extracted {} characters", page + 1, pageText.length());

            } catch (Exception e) {
                log.error("Error performing OCR on page {}", page + 1, e);
                ocrText.append("--- Page ").append(page + 1).append(" (OCR failed) ---\n\n");
            }
        }

        String result = ocrText.toString();
        log.info("OCR completed. Total extracted text: {} characters", result.length());
        return result;
    }

    /**
     * Perform OCR on a single image with preprocessing
     */
    public String performOCROnImage(BufferedImage image) {
        try {
            // Preprocess image for better OCR accuracy
            BufferedImage processedImage = preprocessImageForOCR(image);

            String text = tesseract.doOCR(processedImage);
            return text != null ? text.trim() : "";
        } catch (TesseractException e) {
            log.error("Tesseract OCR failed", e);
            return "";
        }
    }

    /**
     * Preprocess image to improve OCR accuracy
     */
    private BufferedImage preprocessImageForOCR(BufferedImage original) {
        try {
            int width = original.getWidth();
            int height = original.getHeight();

            // Create a new image with higher contrast
            BufferedImage processed = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = processed.createGraphics();

            // Enable high-quality rendering
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw original image
            g2d.drawImage(original, 0, 0, width, height, null);
            g2d.dispose();

            // Apply contrast enhancement
            processed = enhanceContrast(processed);

            return processed;

        } catch (Exception e) {
            log.warn("Image preprocessing failed, using original image", e);
            return original;
        }
    }

    /**
     * Enhance image contrast for better OCR
     */
    private BufferedImage enhanceContrast(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage enhanced = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);

                // Extract RGB components
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Calculate grayscale
                int gray = (r + g + b) / 3;

                // Apply threshold for better contrast (adaptive)
                int threshold = 128;
                int newGray = gray > threshold ? 255 : 0;

                // If we want to preserve some gray tones for better accuracy
                // Use contrast stretching instead
                newGray = Math.min(255, Math.max(0, (int) ((gray - 128) * 1.5 + 128)));

                // Create new RGB value
                int newRgb = (newGray << 16) | (newGray << 8) | newGray;
                enhanced.setRGB(x, y, newRgb);
            }
        }

        return enhanced;
    }

    /**
     * Perform OCR on an image file
     */
    public String performOCROnImageFile(File imageFile) throws IOException {
        try {
            BufferedImage image = ImageIO.read(imageFile);
            return performOCROnImage(image);
        } catch (Exception e) {
            log.error("Failed to perform OCR on image file", e);
            throw new IOException("OCR failed", e);
        }
    }

    /**
     * Check if Tesseract is properly configured
     */
    public boolean isTesseractAvailable() {
        try {
            // Try a simple OCR operation
            BufferedImage testImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
            tesseract.doOCR(testImage);
            return true;
        } catch (Exception e) {
            log.warn("Tesseract is not available or not properly configured");
            return false;
        }
    }

    /**
     * Get OCR confidence for debugging
     */
    public int getOCRConfidence(BufferedImage image) {
        try {
            return tesseract.doOCR(image).length() > 0 ? 1 : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
