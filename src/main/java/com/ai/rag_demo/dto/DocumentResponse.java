package com.ai.rag_demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {
    private String id;
    private String filename;
    private String summary;
    private String contentType;
    private LocalDateTime uploadedAt;
    private Long size;
}
