package com.ai.rag_demo.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "document_chunks")
public class DocumentChunk {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String documentId;

    @Field(type = FieldType.Text)
    private String content;

    @Field(type = FieldType.Integer)
    private Integer chunkIndex;

    @Field(type = FieldType.Dense_Vector, dims = 768)
    private float[] embedding;

    @Field(type = FieldType.Keyword)
    private String filename;
}
