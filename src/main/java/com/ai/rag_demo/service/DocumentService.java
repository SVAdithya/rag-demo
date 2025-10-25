package com.ai.rag_demo.service;

import com.ai.rag_demo.model.DocumentModel;
import com.ai.rag_demo.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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

    public DocumentModel uploadDocument(MultipartFile file) throws IOException {
        log.info("Uploading document: {}", file.getOriginalFilename());

        String content = extractContent(file);

        DocumentModel documentModel = new DocumentModel();
        documentModel.setId(UUID.randomUUID().toString());
        documentModel.setFilename(file.getOriginalFilename());
        documentModel.setContent(content);
        documentModel.setContentType(file.getContentType());
        documentModel.setSize(file.getSize());
        documentModel.setUploadedAt(LocalDateTime.now());

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
        return (List<DocumentModel>) documentRepository.findAll();
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
            return extractPdfContent(file);
        } else if (contentType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")) {
            return extractDocxContent(file);
        } else if (contentType.startsWith("text/")) {
            return new String(file.getBytes());
        } else {
            return new String(file.getBytes());
        }
    }

    private String extractPdfContent(MultipartFile file) throws IOException {
        try (PDDocument document = PDDocument.load(file.getInputStream())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
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
}
