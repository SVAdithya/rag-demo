package com.ai.rag_demo.repository;

import com.ai.rag_demo.model.DocumentChunk;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends ElasticsearchRepository<DocumentChunk, String> {
    List<DocumentChunk> findByDocumentId(String documentId);

    void deleteByDocumentId(String documentId);
}
