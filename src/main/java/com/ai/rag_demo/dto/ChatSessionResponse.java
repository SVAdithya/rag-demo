package com.ai.rag_demo.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChatSessionResponse {
    private String id;
    private String title;
    private List<String> documentIds;
    private List<DocumentInfo> documents;
    private LocalDateTime createdAt;
    private LocalDateTime lastActivity;
    private int messageCount;

    @Data
    public static class DocumentInfo {
        private String id;
        private String filename;
        private String contentType;
    }
}
