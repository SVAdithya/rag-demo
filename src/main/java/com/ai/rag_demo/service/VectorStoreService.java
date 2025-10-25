package com.ai.rag_demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class VectorStoreService {

    private final EmbeddingModel embeddingModel;

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
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());
            chunks.add(text.substring(start, end));
            start += (chunkSize - overlap);
        }

        log.info("Split text into {} chunks", chunks.size());
        return chunks;
    }
}
