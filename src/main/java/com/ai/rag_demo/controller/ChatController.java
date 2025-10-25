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
    public ResponseEntity<ChatResponse> askQuestion(@RequestBody ChatRequest request) {
        try {
            log.info("Received question: {}", request.getQuestion());
            ChatMessage chatMessage = chatService.askQuestion(
                    request.getQuestion(),
                    request.getSessionId(),
                    request.getDocumentId()
            );
            return ResponseEntity.ok(toResponse(chatMessage));
        } catch (Exception e) {
            log.error("Error processing question", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/history/{sessionId}")
    public ResponseEntity<List<ChatResponse>> getChatHistory(@PathVariable String sessionId) {
        List<ChatMessage> history = chatService.getChatHistory(sessionId);
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
        response.setSessionId(chatMessage.getSessionId());
        return response;
    }
}
