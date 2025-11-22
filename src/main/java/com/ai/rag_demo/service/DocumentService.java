package com.ai.rag_demo.service;

import com.ai.rag_demo.model.DocumentModel;
import com.ai.rag_demo.repository.DocumentRepository;
import com.ai.rag_demo.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.ai.rag_demo.config.KafkaConfig.DOCUMENT_PROCESSING_TOPIC;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OCRService ocrService;
    private final PDFConversionService pdfConversionService;
    private final ChatMessageRepository chatMessageRepository;

    public DocumentModel uploadDocument(MultipartFile file) throws IOException {
        log.info("Uploading document: {}", file.getOriginalFilename());

        String documentId = UUID.randomUUID().toString();

        // Convert file to searchable PDF first
        String convertedPdfPath = null;
        String content = null;

        try {
            log.info("Converting file to searchable PDF: {}", file.getOriginalFilename());
            convertedPdfPath = pdfConversionService.convertToSearchablePDF(file, documentId);

            // Extract content from converted PDF for RAG processing
            content = pdfConversionService.extractTextFromConvertedPDF(convertedPdfPath);
            log.info("Extracted {} characters from converted PDF", content.length());

        } catch (Exception e) {
            log.error("Error converting file to searchable PDF, falling back to direct extraction", e);
            // Fallback: extract content directly from original file
            content = extractContent(file);
        }

        DocumentModel documentModel = new DocumentModel();
        documentModel.setId(documentId);
        documentModel.setFilename(file.getOriginalFilename());
        documentModel.setContent(content);
        documentModel.setContentType(file.getContentType());
        documentModel.setSize(file.getSize());
        documentModel.setUploadedAt(LocalDateTime.now());
        documentModel.setConvertedPdfPath(convertedPdfPath);
        documentModel.setIsConverted(convertedPdfPath != null);

        DocumentModel savedDoc = documentRepository.save(documentModel);

        // Send to Kafka for processing
        kafkaTemplate.send(DOCUMENT_PROCESSING_TOPIC, savedDoc.getId());
        log.info("Document saved and sent to Kafka for processing: {}", savedDoc.getId());

        return savedDoc;
    }

    public DocumentModel uploadTextContent(String content, String filename) {
        log.info("Uploading text content: {}", filename);

        DocumentModel documentModel = new DocumentModel();
        documentModel.setId(UUID.randomUUID().toString());
        documentModel.setFilename(filename);
        documentModel.setContent(content);
        documentModel.setContentType("text/plain");
        documentModel.setSize((long) content.length());
        documentModel.setUploadedAt(LocalDateTime.now());

        DocumentModel savedDoc = documentRepository.save(documentModel);

        // Send to Kafka for processing
        kafkaTemplate.send(DOCUMENT_PROCESSING_TOPIC, savedDoc.getId());
        log.info("Text content saved and sent to Kafka for processing: {}", savedDoc.getId());

        return savedDoc;
    }

    public List<DocumentModel> getAllDocuments() {
        Iterable<DocumentModel> iterable = documentRepository.findAll();
        List<DocumentModel> documents = new java.util.ArrayList<>();
        iterable.forEach(documents::add);
        return documents;
    }

    public DocumentModel getDocumentById(String id) {
        return documentRepository.findById(id).orElse(null);
    }

    public void updateDocumentSummary(String documentId, String summary) {
        documentRepository.findById(documentId).ifPresent(doc -> {
            doc.setSummary(summary);
            documentRepository.save(doc);
            log.info("Updated summary for document: {}", documentId);
        });
    }

    private String extractContent(MultipartFile file) throws IOException {
        String contentType = file.getContentType();

        if (contentType == null) {
            return new String(file.getBytes());
        }

        if (contentType.equals("application/pdf")) {
            return extractPdfContentWithOCR(file);
        } else if (contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
            return extractDocxContent(file);
        } else if (contentType.startsWith("text/")) {
            return new String(file.getBytes());
        } else if (contentType.startsWith("image/")) {
            // Handle image files with OCR
            return extractImageContentWithOCR(file);
        } else {
            return new String(file.getBytes());
        }
    }

    /**
     * Extract PDF content with OCR support for scanned PDFs
     */
    private String extractPdfContentWithOCR(MultipartFile file) throws IOException {
        log.info("Extracting PDF content with OCR support: {}", file.getOriginalFilename());

        try {
            // Use OCR service which automatically detects if PDF needs OCR
            String content = ocrService.extractTextFromPDF(file.getInputStream());

            if (content == null || content.trim().isEmpty()) {
                log.warn("No text extracted from PDF: {}", file.getOriginalFilename());
                return "";
            }

            log.info("Successfully extracted {} characters from PDF", content.length());
            return content;

        } catch (Exception e) {
            log.error("Error extracting PDF content with OCR", e);
            // Fallback to basic extraction
            return extractPdfContentBasic(file);
        }
    }

    /**
     * Basic PDF extraction (fallback method)
     */
    private String extractPdfContentBasic(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * Extract text from image files using OCR
     */
    private String extractImageContentWithOCR(MultipartFile file) throws IOException {
        log.info("Extracting text from image with OCR: {}", file.getOriginalFilename());

        try {
            BufferedImage image = javax.imageio.ImageIO.read(file.getInputStream());
            if (image == null) {
                log.warn("Could not read image file: {}", file.getOriginalFilename());
                return "";
            }

            String content = ocrService.performOCROnImage(image);
            log.info("Extracted {} characters from image", content.length());
            return content;

        } catch (Exception e) {
            log.error("Error performing OCR on image", e);
            return "";
        }
    }

    private String extractDocxContent(MultipartFile file) throws IOException {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream())) {
            StringBuilder content = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                content.append(paragraph.getText()).append("\n");
            }
            return content.toString();
        }
    }

    /**
     * Get converted PDF file for download
     */
    public java.io.File getConvertedPDF(String documentId) {
        DocumentModel document = getDocumentById(documentId);
        if (document == null || document.getConvertedPdfPath() == null) {
            return null;
        }
        return pdfConversionService.getConvertedPDFFile(document.getConvertedPdfPath());
    }

    /**
     * Delete document and its converted PDF
     */
    public void deleteDocument(String documentId) {
        DocumentModel document = getDocumentById(documentId);
        if (document != null) {
            // Delete converted PDF file if exists
            if (document.getConvertedPdfPath() != null) {
                pdfConversionService.deleteConvertedPDF(document.getConvertedPdfPath());
            }
            // Delete from repository
            documentRepository.deleteById(documentId);
            log.info("Deleted document and its converted PDF: {}", documentId);
        }
    }

    public void deleteDocumentAndHistory(String documentId) {
        documentRepository.deleteById(documentId);
        chatMessageRepository.deleteAll(chatMessageRepository.findByDocumentIdOrderByTimestampAsc(documentId));
    }
}
