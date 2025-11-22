package com.ai.rag_demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class AIService {

    private final ChatModel chatModel;

    public AIService(@Qualifier("ollamaChatModel") ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public String summarizeDocument(String content) {
        log.info("Summarizing document with {} characters", content.length());

        String promptText = """
                Please provide a comprehensive summary of the following document.
                Focus on the main points, key findings, and important details.
                Keep the summary concise but informative.
                
                Document:
                {content}
                
                Summary:
                """;

        PromptTemplate promptTemplate = new PromptTemplate(promptText);
        Prompt prompt = promptTemplate.create(Map.of("content", truncateContent(content, 4000)));

        ChatResponse response = chatModel.call(prompt);
        String summary = response.getResult().getOutput().getText();
        log.info("Summary generated successfully");
        return summary;
    }

    public String answerQuestion(String question, String context) {
        log.info("Answering question: {}", question);

        String promptText = """
                Based on the following context, please answer the question.
                If the answer cannot be found in the context, say so clearly.
                Provide a clear, accurate, and concise answer.
                
                Context:
                {context}
                
                Question: {question}
                
                Answer:
                """;

        PromptTemplate promptTemplate = new PromptTemplate(promptText);
        Prompt prompt = promptTemplate.create(Map.of(
                "context", truncateContent(context, 3000),
                "question", question
        ));

        ChatResponse response = chatModel.call(prompt);
        String answer = response.getResult().getOutput().getText();
        log.info("Answer generated successfully");
        return answer;
    }

    public String answerWithHistory(String question, String context, String chatHistory) {
        log.info("Answering question with history: {}", question);

        String promptText = """
                Based on the following context and chat history, please answer the question.
                Use the chat history to maintain conversation continuity.
                If the answer cannot be found in the context, say so clearly.
                
                Context:
                {context}
                
                Chat History:
                {history}
                
                Question: {question}
                
                Answer:
                """;

        PromptTemplate promptTemplate = new PromptTemplate(promptText);
        Prompt prompt = promptTemplate.create(Map.of(
                "context", truncateContent(context, 2500),
                "history", truncateContent(chatHistory, 1000),
                "question", question
        ));

        ChatResponse response = chatModel.call(prompt);
        String answer = response.getResult().getOutput().getText();
        log.info("Answer with history generated successfully");
        return answer;
    }

    /**
     * Answer question using RAG (Retrieval Augmented Generation)
     * The context is retrieved from vector store and contains only relevant chunks
     */
    public String answerQuestionWithRAG(String question, String retrievedContext) {
        log.info("Answering question with RAG: {}", question);

        String promptText = """
                You are a helpful AI assistant that answers questions based on provided context.
                Use the context below to answer the question accurately and concisely.
                If the information is not in the context, clearly state that you cannot find the answer in the provided information.
                
                Retrieved Context:
                {context}
                
                Question: {question}
                
                Answer:
                """;

        PromptTemplate promptTemplate = new PromptTemplate(promptText);
        Prompt prompt = promptTemplate.create(Map.of(
                "context", retrievedContext,
                "question", question
        ));

        ChatResponse response = chatModel.call(prompt);
        String answer = response.getResult().getOutput().getText();
        log.info("RAG answer generated successfully");
        return answer;
    }

    /**
     * Answer question using RAG with conversation history
     */
    public String answerWithHistoryAndRAG(String question, String retrievedContext, String chatHistory) {
        log.info("Answering question with RAG and history: {}", question);

        String promptText = """
                You are a helpful AI assistant that answers questions based on provided context and conversation history.
                Use both the retrieved context and the chat history to provide a coherent, contextual answer.
                If the information is not in the context, clearly state that you cannot find the answer in the provided information.
                
                Retrieved Context:
                {context}
                
                Previous Conversation:
                {history}
                
                Current Question: {question}
                
                Answer:
                """;

        PromptTemplate promptTemplate = new PromptTemplate(promptText);
        Prompt prompt = promptTemplate.create(Map.of(
                "context", retrievedContext,
                "history", truncateContent(chatHistory, 1500),
                "question", question
        ));

        ChatResponse response = chatModel.call(prompt);
        String answer = response.getResult().getOutput().getText();
        log.info("RAG answer with history generated successfully");
        return answer;
    }

    private String truncateContent(String content, int maxLength) {
        if (content == null) {
            return "";
        }
        return content.length() > maxLength ? content.substring(0, maxLength) + "..." : content;
    }
}
