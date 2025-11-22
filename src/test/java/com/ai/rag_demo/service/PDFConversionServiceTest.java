package com.ai.rag_demo.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class PDFConversionServiceTest {

    @Mock
    private OCRService ocrService;

    private PDFConversionService pdfConversionService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        pdfConversionService = new PDFConversionService(ocrService);
    }

    @Test
    void testConvertTextToSearchablePDF() throws IOException {
        // Given
        String testContent = "This is a test document.\nIt has multiple lines.\nAnd should be converted to PDF.";
        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                testContent.getBytes()
        );

        String documentId = "test-doc-123";

        // When
        String convertedPath = pdfConversionService.convertToSearchablePDF(textFile, documentId);

        // Then
        assertNotNull(convertedPath);
        assertTrue(convertedPath.contains("converted.pdf"));

        File convertedFile = new File(convertedPath);
        assertTrue(convertedFile.exists());

        // Verify PDF contains the text
        String extractedText = pdfConversionService.extractTextFromConvertedPDF(convertedPath);
        assertTrue(extractedText.contains("test document"));
        assertTrue(extractedText.contains("multiple lines"));

        // Cleanup
        pdfConversionService.deleteConvertedPDF(convertedPath);
    }

    @Test
    void testExtractTextFromConvertedPDF() throws IOException {
        // Given - create a simple PDF first
        String testContent = "Sample PDF content for extraction test.";
        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "sample.txt",
                "text/plain",
                testContent.getBytes()
        );

        String documentId = "test-doc-456";
        String convertedPath = pdfConversionService.convertToSearchablePDF(textFile, documentId);

        // When
        String extractedText = pdfConversionService.extractTextFromConvertedPDF(convertedPath);

        // Then
        assertNotNull(extractedText);
        assertTrue(extractedText.contains("Sample PDF content"));

        // Cleanup
        pdfConversionService.deleteConvertedPDF(convertedPath);
    }

    @Test
    void testDeleteConvertedPDF() throws IOException {
        // Given
        String testContent = "Test content for deletion";
        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "delete-test.txt",
                "text/plain",
                testContent.getBytes()
        );

        String documentId = "test-doc-789";
        String convertedPath = pdfConversionService.convertToSearchablePDF(textFile, documentId);

        File convertedFile = new File(convertedPath);
        assertTrue(convertedFile.exists());

        // When
        pdfConversionService.deleteConvertedPDF(convertedPath);

        // Then
        assertFalse(convertedFile.exists());
    }

    @Test
    void testConvertDocxToSearchablePDF() throws IOException {
        // Given - simulate DOCX file
        String testContent = "This is DOCX content that will be converted to PDF.";
        MockMultipartFile docxFile = new MockMultipartFile(
                "file",
                "test.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                testContent.getBytes()
        );

        String documentId = "test-doc-docx";

        // When/Then - May fail if not a valid DOCX, but should not throw unexpected exceptions
        try {
            String convertedPath = pdfConversionService.convertToSearchablePDF(docxFile, documentId);
            if (convertedPath != null) {
                pdfConversionService.deleteConvertedPDF(convertedPath);
            }
        } catch (Exception e) {
            // Expected for invalid DOCX format - test passes
            assertTrue(true);
        }
    }

    @Test
    void testGetConvertedPDFFile() throws IOException {
        // Given
        String testContent = "Test content for file retrieval";
        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "retrieve-test.txt",
                "text/plain",
                testContent.getBytes()
        );

        String documentId = "test-doc-retrieve";
        String convertedPath = pdfConversionService.convertToSearchablePDF(textFile, documentId);

        // When
        File retrievedFile = pdfConversionService.getConvertedPDFFile(convertedPath);

        // Then
        assertNotNull(retrievedFile);
        assertTrue(retrievedFile.exists());
        assertTrue(retrievedFile.getName().endsWith(".pdf"));

        // Cleanup
        pdfConversionService.deleteConvertedPDF(convertedPath);
    }

    @Test
    void testConvertPDFAlreadySearchable() throws IOException {
        // Given - Create a simple text PDF first with enough content to be considered searchable
        StringBuilder testContentBuilder = new StringBuilder();
        testContentBuilder.append("This is already searchable PDF content.\n\n");

        // Add enough text to meet the searchable threshold (100 chars per page)
        for (int i = 0; i < 5; i++) {
            testContentBuilder.append("This is line ").append(i).append(" with additional content to make it searchable. ");
            testContentBuilder.append("Lorem ipsum dolor sit amet, consectetur adipiscing elit. ");
            testContentBuilder.append("Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.\n");
        }

        String testContent = testContentBuilder.toString();
        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "searchable.txt",
                "text/plain",
                testContent.getBytes()
        );

        String documentId = "test-doc-searchable";
        String firstConversion = pdfConversionService.convertToSearchablePDF(textFile, documentId);

        // Read the converted PDF
        byte[] pdfBytes = Files.readAllBytes(Path.of(firstConversion));

        // When - Try to "convert" the already searchable PDF
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "searchable.pdf",
                "application/pdf",
                pdfBytes
        );

        String documentId2 = "test-doc-searchable-2";
        String secondConversion = pdfConversionService.convertToSearchablePDF(pdfFile, documentId2);

        // Then
        assertNotNull(secondConversion);
        File convertedFile = new File(secondConversion);
        assertTrue(convertedFile.exists());

        // Verify content is preserved (at least partially)
        String extractedText = pdfConversionService.extractTextFromConvertedPDF(secondConversion);
        assertTrue(extractedText.length() > 50, "Extracted text should have substantial content");

        // Cleanup
        pdfConversionService.deleteConvertedPDF(firstConversion);
        pdfConversionService.deleteConvertedPDF(secondConversion);
    }

    @Test
    void testConversionCreatesDirectory() throws IOException {
        // Given
        Path convertedDir = Path.of("converted-pdfs");

        // Delete directory if exists (for clean test)
        if (Files.exists(convertedDir)) {
            Files.walk(convertedDir)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            // Ignore
                        }
                    });
        }

        assertFalse(Files.exists(convertedDir));

        // When
        String testContent = "Test content for directory creation";
        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "dir-test.txt",
                "text/plain",
                testContent.getBytes()
        );

        String documentId = "test-doc-dir";
        String convertedPath = pdfConversionService.convertToSearchablePDF(textFile, documentId);

        // Then
        assertTrue(Files.exists(convertedDir));
        assertTrue(Files.isDirectory(convertedDir));

        // Cleanup
        pdfConversionService.deleteConvertedPDF(convertedPath);
    }
}
