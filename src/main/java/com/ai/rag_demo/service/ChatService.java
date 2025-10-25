package com.ai.rag_demo.service;

import com.ai.rag_demo.model.ChatMessage;
import com.ai.rag_demo.model.DocumentModel;
import com.ai.rag_demo.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final AIService aiService;
    private final DocumentService documentService;

    public ChatMessage askQuestion(String question, String sessionId, String documentId) {
        log.info("Processing question for session: {} and document: {}", sessionId, documentId);

        // Get document content
        DocumentModel documentModel = documentService.getDocumentById(documentId);
        if (documentModel == null) {
            throw new RuntimeException("Document not found: " + documentId);
        }

        // Get chat history
        List<ChatMessage> history = chatMessageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
        String chatHistory = formatChatHistory(history);

        // Generate answer
        String answer;
        if (chatHistory.isEmpty()) {
            answer = aiService.answerQuestion(question, documentModel.getContent());
        } else {
            answer = aiService.answerWithHistory(question, documentModel.getContent(), chatHistory);
        }

        // Save chat message
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setId(UUID.randomUUID().toString());
        chatMessage.setSessionId(sessionId);
        chatMessage.setQuestion(question);
        chatMessage.setAnswer(answer);
        chatMessage.setTimestamp(LocalDateTime.now());
        chatMessage.setDocumentId(documentId);

        ChatMessage saved = chatMessageRepository.save(chatMessage);
        log.info("Chat message saved: {}", saved.getId());

        return saved;
    }

    public List<ChatMessage> getChatHistory(String sessionId) {
        return chatMessageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
    }

    private String formatChatHistory(List<ChatMessage> history) {
        return history.stream()
                .map(msg -> "Q: " + msg.getQuestion() + "\nA: " + msg.getAnswer())
                .collect(Collectors.joining("\n\n"));
    }
}
