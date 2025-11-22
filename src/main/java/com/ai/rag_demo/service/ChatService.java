package com.ai.rag_demo.service;

import com.ai.rag_demo.model.ChatMessage;
import com.ai.rag_demo.model.DocumentModel;
import com.ai.rag_demo.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
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
    private final VectorStoreService vectorStoreService;

    /**
     * Ask a question using RAG (Retrieval Augmented Generation)
     * This retrieves relevant context from the provided document
     */
    public ChatMessage askQuestion(String question, String documentId) {
        log.info("Processing RAG question for document: {}", documentId);

        String answer;
        try {
            // Use RAG to retrieve relevant context from the document
            List<Document> relevantDocs = vectorStoreService.searchRelevantContextInDocument(
                    question, documentId, 3
            );
            String relevantContext = vectorStoreService.formatContext(relevantDocs);
            log.info("Retrieved {} relevant chunks from vector store in document",
                    relevantDocs.size());

            // Get chat history
            List<ChatMessage> history = chatMessageRepository.findByDocumentIdOrderByTimestampAsc(documentId);
            String chatHistory = formatChatHistory(history);

            // Generate answer using RAG context
            if (relevantDocs.isEmpty()) {
                // Fallback if no relevant context found
                log.warn("No relevant context found in vector store");
                DocumentModel doc = documentService.getDocumentById(documentId);
                answer = aiService.answerQuestion(question, doc.getContent());
            } else if (chatHistory.isEmpty()) {
                answer = aiService.answerQuestionWithRAG(question, relevantContext);
            } else {
                answer = aiService.answerWithHistoryAndRAG(question, relevantContext, chatHistory);
            }
        } catch (Exception e) {
            // Fallback to basic answer without RAG if vector store fails
            log.error("Error using vector store, falling back to basic answer", e);
            DocumentModel doc = documentService.getDocumentById(documentId);
            answer = aiService.answerQuestion(question, doc.getContent());
        }

        // Save chat message
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setId(UUID.randomUUID().toString());
        chatMessage.setQuestion(question);
        chatMessage.setAnswer(answer);
        chatMessage.setTimestamp(LocalDateTime.now());
        chatMessage.setDocumentId(documentId);

        ChatMessage saved = chatMessageRepository.save(chatMessage);

        log.info("Chat message saved with RAG: {}", saved.getId());

        return saved;
    }

    public List<ChatMessage> getChatHistory(String documentId) {
        // Fetch chat messages by documentId ordered by timestamp
        return chatMessageRepository.findByDocumentIdOrderByTimestampAsc(documentId);
    }

    private String formatChatHistory(List<ChatMessage> history) {
        // Only include last 5 messages to avoid context overflow
        return history.stream()
                .skip(Math.max(0, history.size() - 5))
                .map(msg -> "Q: " + msg.getQuestion() + "\nA: " + msg.getAnswer())
                .collect(Collectors.joining("\n\n"));
    }
}
