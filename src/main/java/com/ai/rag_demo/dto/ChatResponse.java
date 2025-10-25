package com.ai.rag_demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    private String id;
    private String question;
    private String answer;
    private LocalDateTime timestamp;
    private String sessionId;
}
