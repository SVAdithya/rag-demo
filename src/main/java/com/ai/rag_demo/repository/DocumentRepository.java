package com.ai.rag_demo.repository;

import com.ai.rag_demo.model.DocumentModel;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentRepository extends ElasticsearchRepository<DocumentModel, String> {
    List<DocumentModel> findByFilenameContaining(String filename);
}
