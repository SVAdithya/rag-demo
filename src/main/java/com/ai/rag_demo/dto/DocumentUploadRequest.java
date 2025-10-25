package com.ai.rag_demo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadRequest {
    private String content;
    private String filename;
    private String contentType;
}
