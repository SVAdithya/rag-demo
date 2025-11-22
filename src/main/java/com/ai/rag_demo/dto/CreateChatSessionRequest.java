package com.ai.rag_demo.dto;

import lombok.Data;

@Data
public class CreateChatSessionRequest {
    private String title;
    private String documentId; // Optional: initial document to add
}
