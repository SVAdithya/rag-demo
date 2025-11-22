package com.ai.rag_demo.kafka;

import com.ai.rag_demo.model.DocumentModel;
import com.ai.rag_demo.service.AIService;
import com.ai.rag_demo.service.DocumentService;
import com.ai.rag_demo.service.VectorStoreService;
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
    private final VectorStoreService vectorStoreService;

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

            // Index document into vector store for RAG
            // This updates the LLM's knowledge base
            log.info("Indexing document into vector store: {}", documentId);
            vectorStoreService.indexDocument(
                    documentId,
                    documentModel.getFilename(),
                    documentModel.getContent()
            );
            log.info("Document indexed successfully into vector store");

            // Generate summary
            log.info("Generating AI summary for document: {}", documentId);
            String summary = aiService.summarizeDocument(documentModel.getContent());

            // Update document with summary
            documentService.updateDocumentSummary(documentId, summary);

            log.info("Document processed successfully: {} (indexed + summarized)", documentId);
        } catch (Exception e) {
            log.error("Error processing document: {}", documentId, e);
        }
    }
}
