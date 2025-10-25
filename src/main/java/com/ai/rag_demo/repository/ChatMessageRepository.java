package com.ai.rag_demo.repository;

import com.ai.rag_demo.model.ChatMessage;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends ElasticsearchRepository<ChatMessage, String> {
    List<ChatMessage> findBySessionIdOrderByTimestampAsc(String sessionId);

    List<ChatMessage> findByDocumentIdOrderByTimestampDesc(String documentId);
}
