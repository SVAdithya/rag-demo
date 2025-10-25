package com.ai.rag_demo.kafka;

import com.ai.rag_demo.model.DocumentModel;
import com.ai.rag_demo.service.AIService;
import com.ai.rag_demo.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import static com.ai.rag_demo.config.KafkaConfig.DOCUMENT_PROCESSING_TOPIC;

@Component
@Slf4j
@RequiredArgsConstructor
public class DocumentProcessingListener {

    private final DocumentService documentService;
    private final AIService aiService;

    @KafkaListener(topics = DOCUMENT_PROCESSING_TOPIC, groupId = "document-processing-group")
    public void processDocument(String documentId) {
        log.info("Processing document from Kafka: {}", documentId);

        try {
            // Retrieve document
            DocumentModel documentModel = documentService.getDocumentById(documentId);
            if (documentModel == null) {
                log.error("Document not found: {}", documentId);
                return;
            }

            // Generate summary
            String summary = aiService.summarizeDocument(documentModel.getContent());

            // Update document with summary
            documentService.updateDocumentSummary(documentId, summary);

            log.info("Document processed successfully: {}", documentId);
        } catch (Exception e) {
            log.error("Error processing document: {}", documentId, e);
        }
    }
}
