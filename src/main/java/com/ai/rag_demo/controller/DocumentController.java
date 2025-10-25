package com.ai.rag_demo.controller;

import com.ai.rag_demo.dto.DocumentResponse;
import com.ai.rag_demo.dto.DocumentUploadRequest;
import com.ai.rag_demo.model.DocumentModel;
import com.ai.rag_demo.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping("/upload")
    public ResponseEntity<DocumentResponse> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Received file upload request: {}", file.getOriginalFilename());
            DocumentModel documentModel = documentService.uploadDocument(file);
            return ResponseEntity.ok(toResponse(documentModel));
        } catch (IOException e) {
            log.error("Error uploading file", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/upload-text")
    public ResponseEntity<DocumentResponse> uploadTextContent(@RequestBody DocumentUploadRequest request) {
        log.info("Received text upload request: {}", request.getFilename());
        DocumentModel documentModel = documentService.uploadTextContent(request.getContent(), request.getFilename());
        return ResponseEntity.ok(toResponse(documentModel));
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getAllDocuments() {
        List<DocumentModel> documentModels = documentService.getAllDocuments();
        List<DocumentResponse> responses = documentModels.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable String id) {
        DocumentModel documentModel = documentService.getDocumentById(id);
        if (documentModel == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toResponse(documentModel));
    }

    private DocumentResponse toResponse(DocumentModel documentModel) {
        DocumentResponse response = new DocumentResponse();
        response.setId(documentModel.getId());
        response.setFilename(documentModel.getFilename());
        response.setSummary(documentModel.getSummary());
        response.setContentType(documentModel.getContentType());
        response.setUploadedAt(documentModel.getUploadedAt());
        response.setSize(documentModel.getSize());
        return response;
    }
}
