package com.ai.rag_demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "chat_messages")
public class ChatMessage {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String sessionId;

    @Field(type = FieldType.Text)
    private String question;

    @Field(type = FieldType.Text)
    private String answer;

    @Field(type = FieldType.Date, format = {}, pattern = "uuuu-MM-dd'T'HH:mm:ss.SSS||uuuu-MM-dd'T'HH:mm:ss||uuuu-MM-dd||epoch_millis")
    private LocalDateTime timestamp;

    @Field(type = FieldType.Keyword)
    private String documentId;
}
