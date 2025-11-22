package com.ai.rag_demo.controller;

import com.ai.rag_demo.dto.DocumentResponse;
import com.ai.rag_demo.dto.DocumentUploadRequest;
import com.ai.rag_demo.model.DocumentModel;
import com.ai.rag_demo.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
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

    @GetMapping("/{id}/download-converted-pdf")
    public ResponseEntity<Resource> downloadConvertedPDF(@PathVariable String id) {
        log.info("Request to download converted PDF for document: {}", id);

        File convertedPdf = documentService.getConvertedPDF(id);
        if (convertedPdf == null || !convertedPdf.exists()) {
            log.warn("Converted PDF not found for document: {}", id);
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(convertedPdf);
        DocumentModel documentModel = documentService.getDocumentById(id);

        String filename = documentModel.getFilename();
        if (!filename.toLowerCase().endsWith(".pdf")) {
            // Replace extension with .pdf
            int lastDot = filename.lastIndexOf('.');
            if (lastDot > 0) {
                filename = filename.substring(0, lastDot) + ".pdf";
            } else {
                filename = filename + ".pdf";
            }
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable String id) {
        log.info("Request to delete document: {}", id);
        documentService.deleteDocument(id);
        return ResponseEntity.ok().build();
    }

    private DocumentResponse toResponse(DocumentModel documentModel) {
        DocumentResponse response = new DocumentResponse();
        response.setId(documentModel.getId());
        response.setFilename(documentModel.getFilename());
        response.setSummary(documentModel.getSummary());
        response.setContentType(documentModel.getContentType());
        response.setUploadedAt(documentModel.getUploadedAt());
        response.setSize(documentModel.getSize());
        response.setIsConverted(documentModel.getIsConverted());

        // Add download URL if converted PDF exists
        if (documentModel.getIsConverted() != null && documentModel.getIsConverted()) {
            response.setConvertedPdfDownloadUrl("/api/documents/" + documentModel.getId() + "/download-converted-pdf");
        }

        return response;
    }
}
