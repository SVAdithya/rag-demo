package com.ai.rag_demo.controller;

import com.ai.rag_demo.dto.ChatRequest;
import com.ai.rag_demo.dto.ChatResponse;
import com.ai.rag_demo.model.ChatMessage;
import com.ai.rag_demo.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/ask")
    public ResponseEntity<?> askQuestion(@RequestBody ChatRequest request) {
        try {
            // Validate request
            if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
                log.error("Question is empty");
                return ResponseEntity.badRequest().body("Question cannot be empty");
            }
            if (request.getDocumentId() == null || request.getDocumentId().trim().isEmpty()) {
                log.error("DocumentId is empty");
                return ResponseEntity.badRequest().body("Document ID is required");
            }

            log.info("Received question: {} for document: {}", request.getQuestion(), request.getDocumentId());
            ChatMessage chatMessage = chatService.askQuestion(request.getQuestion(), request.getDocumentId());
            return ResponseEntity.ok(toResponse(chatMessage));
        } catch (RuntimeException e) {
            log.error("Error processing question: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error processing question", e);
            return ResponseEntity.internalServerError().body("An unexpected error occurred");
        }
    }

    @GetMapping("/history/{documentId}")
    public ResponseEntity<List<ChatResponse>> getChatHistory(@PathVariable String documentId) {
        List<ChatMessage> history = chatService.getChatHistory(documentId);
        List<ChatResponse> responses = history.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    private ChatResponse toResponse(ChatMessage chatMessage) {
        ChatResponse response = new ChatResponse();
        response.setId(chatMessage.getId());
        response.setQuestion(chatMessage.getQuestion());
        response.setAnswer(chatMessage.getAnswer());
        response.setTimestamp(chatMessage.getTimestamp());
        return response;
    }
}
