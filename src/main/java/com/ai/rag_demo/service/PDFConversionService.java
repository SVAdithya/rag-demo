package com.ai.rag_demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for converting various file formats to searchable PDFs
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PDFConversionService {

    private final OCRService ocrService;

    private static final String CONVERTED_FILES_DIR = "converted-pdfs";
    private static final int DPI = 300;
    private static final float FONT_SIZE = 12f;
    private static final float LINE_HEIGHT = 14f;
    private static final float MARGIN = 50f;

    /**
     * Convert uploaded file to searchable PDF
     * Returns the path to the converted PDF file
     */
    public String convertToSearchablePDF(MultipartFile file, String documentId) throws IOException {
        log.info("Converting file to searchable PDF: {}", file.getOriginalFilename());

        String contentType = file.getContentType();
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        // Create converted files directory if it doesn't exist
        Path convertedDir = Path.of(CONVERTED_FILES_DIR);
        if (!Files.exists(convertedDir)) {
            Files.createDirectories(convertedDir);
        }

        String convertedFileName = documentId + "-converted.pdf";
        Path convertedFilePath = convertedDir.resolve(convertedFileName);

        // Handle different file types
        if (contentType.equals("application/pdf")) {
            convertPDFToSearchable(file, convertedFilePath);
        } else if (contentType.startsWith("image/")) {
            convertImageToSearchablePDF(file, convertedFilePath);
        } else if (contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
            convertDocxToSearchablePDF(file, convertedFilePath);
        } else if (contentType.startsWith("text/")) {
            convertTextToSearchablePDF(file, convertedFilePath);
        } else {
            // For unsupported types, try to extract text and convert
            convertGenericToSearchablePDF(file, convertedFilePath);
        }

        log.info("File converted to searchable PDF: {}", convertedFilePath);
        return convertedFilePath.toString();
    }

    /**
     * Convert existing PDF to searchable PDF (ALWAYS run OCR to capture images)
     */
    private void convertPDFToSearchable(MultipartFile file, Path outputPath) throws IOException {
        log.info("Converting PDF to searchable format with OCR for images");

        // Save input to temp file
        Path tempPdfPath = Files.createTempFile("input-pdf-", ".pdf");
        try {
            Files.copy(file.getInputStream(), tempPdfPath, StandardCopyOption.REPLACE_EXISTING);

            // Just copy the PDF as-is for now
            // The OCR will be applied when extracting text via OCRService
            Files.copy(tempPdfPath, outputPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("PDF copied, OCR will be applied during text extraction");

        } finally {
            Files.deleteIfExists(tempPdfPath);
        }
    }

    /**
     * Create searchable PDF from scanned PDF pages
     */
    private void createSearchablePDFFromScanned(PDDocument sourceDoc, Path outputPath) throws IOException {
        PDDocument outputDoc = new PDDocument();
        PDFRenderer renderer = new PDFRenderer(sourceDoc);

        try {
            int totalPages = sourceDoc.getNumberOfPages();
            log.info("Processing {} pages with OCR", totalPages);

            for (int pageIndex = 0; pageIndex < totalPages; pageIndex++) {
                log.debug("Processing page {}/{}", pageIndex + 1, totalPages);

                // Render page to image
                BufferedImage pageImage = renderer.renderImageWithDPI(pageIndex, DPI);

                // Perform OCR on the image
                String ocrText = ocrService.performOCROnImage(pageImage);

                // Create a new page with the image and OCR text
                addSearchablePageToDocument(outputDoc, pageImage, ocrText);
            }

            // Save the output document
            outputDoc.save(outputPath.toFile());
            log.info("Created searchable PDF with {} pages", totalPages);

        } finally {
            outputDoc.close();
        }
    }

    /**
     * Convert image file to searchable PDF
     */
    private void convertImageToSearchablePDF(MultipartFile file, Path outputPath) throws IOException {
        log.info("Converting image to searchable PDF");

        BufferedImage image = ImageIO.read(file.getInputStream());
        if (image == null) {
            throw new IOException("Could not read image file");
        }

        // Perform OCR on the image
        String ocrText = ocrService.performOCROnImage(image);

        // Create PDF with image and OCR text
        PDDocument document = new PDDocument();
        try {
            addSearchablePageToDocument(document, image, ocrText);
            document.save(outputPath.toFile());
            log.info("Created searchable PDF from image");
        } finally {
            document.close();
        }
    }

    /**
     * Convert DOCX to searchable PDF
     */
    private void convertDocxToSearchablePDF(MultipartFile file, Path outputPath) throws IOException {
        log.info("Converting DOCX to searchable PDF");

        // Extract text from DOCX
        String text = extractTextFromDocx(file.getInputStream());

        // Create PDF with text
        createTextPDF(text, outputPath);
    }

    /**
     * Convert plain text to searchable PDF
     */
    private void convertTextToSearchablePDF(MultipartFile file, Path outputPath) throws IOException {
        log.info("Converting text file to searchable PDF");

        String text = new String(file.getBytes());
        createTextPDF(text, outputPath);
    }

    /**
     * Convert generic/unknown file type to searchable PDF
     */
    private void convertGenericToSearchablePDF(MultipartFile file, Path outputPath) throws IOException {
        log.info("Converting generic file to searchable PDF");

        // Try to read as text
        String text = new String(file.getBytes());

        // If text looks valid (mostly printable characters), create text PDF
        if (isPrintableText(text)) {
            createTextPDF(text, outputPath);
        } else {
            // Create a simple PDF with file info
            createPlaceholderPDF(file.getOriginalFilename(), outputPath);
        }
    }

    /**
     * Create a PDF document from text content
     */
    private void createTextPDF(String text, Path outputPath) throws IOException {
        PDDocument document = new PDDocument();

        try {
            // Split text into lines
            String[] lines = text.split("\n");
            List<String> wrappedLines = new ArrayList<>();

            // Wrap long lines
            for (String line : lines) {
                wrappedLines.addAll(wrapTextToLines(line, 80));
            }

            // Calculate pages needed
            float pageHeight = PDRectangle.A4.getHeight();
            float contentHeight = pageHeight - (2 * MARGIN);
            int linesPerPage = (int) (contentHeight / LINE_HEIGHT);

            int currentLine = 0;
            while (currentLine < wrappedLines.size()) {
                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);

                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA, FONT_SIZE);
                    contentStream.newLineAtOffset(MARGIN, pageHeight - MARGIN);

                    // Write lines for this page
                    int linesToWrite = Math.min(linesPerPage, wrappedLines.size() - currentLine);
                    for (int i = 0; i < linesToWrite; i++) {
                        String line = wrappedLines.get(currentLine++);
                        contentStream.showText(line);
                        contentStream.newLineAtOffset(0, -LINE_HEIGHT);
                    }

                    contentStream.endText();
                }
            }

            document.save(outputPath.toFile());
            log.info("Created text PDF with {} pages", document.getNumberOfPages());

        } finally {
            document.close();
        }
    }

    /**
     * Add a searchable page (image + invisible OCR text) to a PDF document
     */
    private void addSearchablePageToDocument(PDDocument document, BufferedImage image, String ocrText) throws IOException {
        // Create page with same aspect ratio as image
        float imageWidth = image.getWidth();
        float imageHeight = image.getHeight();
        float aspectRatio = imageWidth / imageHeight;

        PDRectangle pageSize = new PDRectangle(
                aspectRatio >= 1 ? PDRectangle.A4.getWidth() : PDRectangle.A4.getWidth() * aspectRatio,
                aspectRatio >= 1 ? PDRectangle.A4.getWidth() / aspectRatio : PDRectangle.A4.getHeight()
        );

        PDPage page = new PDPage(pageSize);
        document.addPage(page);

        // Save image to temp file
        File tempImageFile = File.createTempFile("pdf-image-", ".png");
        try {
            ImageIO.write(image, "PNG", tempImageFile);

            // Add image to PDF
            PDImageXObject pdImage = PDImageXObject.createFromFile(tempImageFile.getAbsolutePath(), document);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // Draw image to fill the page
                contentStream.drawImage(pdImage, 0, 0, pageSize.getWidth(), pageSize.getHeight());

                // Add invisible OCR text for searchability
                // Note: For production, you'd want to position text more accurately
                // This is a simplified version that makes the PDF searchable
                if (ocrText != null && !ocrText.trim().isEmpty()) {
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA, 0.1f); // Very small font
                    contentStream.newLineAtOffset(0, 0);

                    // Write OCR text (invisible due to tiny font size)
                    String[] words = ocrText.split("\\s+");
                    for (String word : words) {
                        contentStream.showText(word + " ");
                    }

                    contentStream.endText();
                }
            }
        } finally {
            tempImageFile.delete();
        }
    }

    /**
     * Create a placeholder PDF for unsupported file types
     */
    private void createPlaceholderPDF(String filename, Path outputPath) throws IOException {
        PDDocument document = new PDDocument();

        try {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA, 14f);
                contentStream.newLineAtOffset(MARGIN, PDRectangle.A4.getHeight() - MARGIN);
                contentStream.showText("Unsupported File Format");
                contentStream.newLineAtOffset(0, -30);
                contentStream.setFont(PDType1Font.HELVETICA, 12f);
                contentStream.showText("Filename: " + filename);
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("This file format could not be converted to a searchable PDF.");
                contentStream.endText();
            }

            document.save(outputPath.toFile());
            log.info("Created placeholder PDF for unsupported file type");

        } finally {
            document.close();
        }
    }

    /**
     * Extract text content from converted PDF using enhanced OCR
     */
    public String extractTextFromConvertedPDF(String convertedFilePath) throws IOException {
        File pdfFile = new File(convertedFilePath);
        if (!pdfFile.exists()) {
            throw new IOException("Converted PDF file not found: " + convertedFilePath);
        }

        // Use OCRService which has ALWAYS_RUN_OCR enabled to extract text from images
        log.info("Extracting text from converted PDF with OCR support: {}", convertedFilePath);
        return ocrService.extractTextFromPDF(pdfFile);
    }

    /**
     * Helper method to check if PDF is searchable
     */
    private boolean isSearchablePDF(String text, int pageCount) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        String cleanText = text.replaceAll("\\s+", "");
        return cleanText.length() >= (100 * pageCount);
    }

    /**
     * Helper method to check if text is mostly printable
     */
    private boolean isPrintableText(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        long printableChars = text.chars()
                .filter(c -> Character.isLetterOrDigit(c) || Character.isWhitespace(c) || ".,!?;:()[]{}\"'".indexOf(c) >= 0)
                .count();

        return (double) printableChars / text.length() > 0.7;
    }

    /**
     * Wrap text to fit within line width
     */
    private List<String> wrapTextToLines(String text, int maxCharsPerLine) {
        List<String> lines = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }

        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            if (currentLine.length() + word.length() + 1 > maxCharsPerLine) {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder();
                }
            }

            if (currentLine.length() > 0) {
                currentLine.append(" ");
            }
            currentLine.append(word);
        }

        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }

        return lines;
    }

    /**
     * Extract text from DOCX
     */
    private String extractTextFromDocx(InputStream inputStream) throws IOException {
        try (org.apache.poi.xwpf.usermodel.XWPFDocument document =
                     new org.apache.poi.xwpf.usermodel.XWPFDocument(inputStream)) {
            StringBuilder content = new StringBuilder();
            for (org.apache.poi.xwpf.usermodel.XWPFParagraph paragraph : document.getParagraphs()) {
                content.append(paragraph.getText()).append("\n");
            }
            return content.toString();
        }
    }

    /**
     * Delete converted PDF file
     */
    public void deleteConvertedPDF(String convertedFilePath) {
        try {
            Files.deleteIfExists(Path.of(convertedFilePath));
            log.info("Deleted converted PDF: {}", convertedFilePath);
        } catch (IOException e) {
            log.error("Error deleting converted PDF: {}", convertedFilePath, e);
        }
    }

    /**
     * Get converted PDF file
     */
    public File getConvertedPDFFile(String convertedFilePath) {
        return new File(convertedFilePath);
    }
}
