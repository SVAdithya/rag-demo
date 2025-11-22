package com.ai.rag_demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class VectorStoreService {

    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;

    public VectorStoreService(@Qualifier("ollamaEmbeddingModel") EmbeddingModel embeddingModel,
                              VectorStore vectorStore) {
        this.embeddingModel = embeddingModel;
        this.vectorStore = vectorStore;
    }

    /**
     * Index document chunks into vector store for RAG
     */
    public void indexDocument(String documentId, String filename, String content) {
        log.info("Indexing document {} into vector store", documentId);

        // Split content into chunks
        List<String> chunks = splitIntoChunks(content, 500, 50);
        log.info("Split document into {} chunks", chunks.size());

        // Create Document objects with metadata
        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("document_id", documentId);
            metadata.put("filename", filename);
            metadata.put("chunk_index", i);
            metadata.put("total_chunks", chunks.size());

            Document doc = new Document(chunks.get(i), metadata);
            documents.add(doc);
        }

        // Add to vector store
        vectorStore.add(documents);
        log.info("Successfully indexed {} chunks for document {}", chunks.size(), documentId);
    }

    /**
     * Search for relevant document chunks using RAG
     */
    public List<Document> searchRelevantContext(String query, int topK) {
        log.info("Searching for relevant context with query: {}", query);

        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(0.5)
                .build();

        List<Document> results = vectorStore.similaritySearch(searchRequest);
        log.info("Found {} relevant document chunks", results.size());

        return results;
    }

    /**
     * Search for relevant context within a specific document
     */
    public List<Document> searchRelevantContextInDocument(String query, String documentId, int topK) {
        log.info("Searching for relevant context in document {} with query: {}", documentId, query);

        // Search with filter for specific document
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(0.5)
                .filterExpression("document_id == '" + documentId + "'")
                .build();

        List<Document> results = vectorStore.similaritySearch(searchRequest);
        log.info("Found {} relevant chunks in document {}", results.size(), documentId);

        return results;
    }

    /**
     * Format retrieved documents into context string
     */
    public String formatContext(List<Document> documents) {
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            context.append("Context ").append(i + 1).append(":\n");
            context.append(doc.getText()).append("\n\n");
        }
        return context.toString();
    }

    /**
     * Generate embedding for text
     */
    @Cacheable(value = "embeddings", key = "#text.hashCode()")
    public List<Double> generateEmbedding(String text) {
        log.debug("Generating embedding for text of length {}", text.length());

        // Call embedding model
        EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));
        float[] embedding = response.getResults().get(0).getOutput();

        // Convert float[] to List<Double>
        List<Double> result = new ArrayList<>(embedding.length);
        for (float value : embedding) {
            result.add((double) value);
        }

        return result;
    }

    /**
     * Generate embeddings for multiple documents
     */
    public List<List<Double>> generateEmbeddings(List<String> texts) {
        log.info("Generating embeddings for {} texts", texts.size());
        return texts.stream()
                .map(this::generateEmbedding)
                .collect(Collectors.toList());
    }

    /**
     * Calculate cosine similarity between two vectors
     */
    public double cosineSimilarity(List<Double> vector1, List<Double> vector2) {
        if (vector1.size() != vector2.size()) {
            throw new IllegalArgumentException("Vectors must have the same dimension");
        }

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < vector1.size(); i++) {
            dotProduct += vector1.get(i) * vector2.get(i);
            norm1 += Math.pow(vector1.get(i), 2);
            norm2 += Math.pow(vector2.get(i), 2);
        }

        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    /**
     * Split text into chunks for embedding
     */
    public List<String> splitIntoChunks(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return chunks;
        }

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            start += (chunkSize - overlap);

            // Prevent infinite loop
            if (start >= text.length() - overlap) {
                break;
            }
        }

        log.info("Split text into {} chunks", chunks.size());
        return chunks;
    }

    /**
     * Delete document from vector store
     */
    public void deleteDocument(String documentId) {
        log.info("Deleting document {} from vector store", documentId);
        // Note: VectorStore delete by ID is implementation-specific
        // This is a placeholder - actual implementation depends on the vector store
        vectorStore.delete(List.of(documentId));
    }
}
