# How LLM Knowledge Updates Work in This RAG System

This document explains exactly how the system "updates" the LLM's knowledge when you upload documents.

## Important Clarification

**The LLM model itself is NOT retrained or modified.** Instead, we use **RAG (Retrieval Augmented Generation)** to
provide the LLM with relevant context from your documents at query time.

## The Process

### Phase 1: Document Upload & Indexing (Knowledge Update)

```
┌──────────────────────────────────────────────────────────────────┐
│                    DOCUMENT UPLOAD FLOW                          │
└──────────────────────────────────────────────────────────────────┘

Step 1: User Uploads Document
   │
   ├─▶ Frontend: User selects file
   │
   └─▶ Backend: Receives file via /api/documents/upload
        │
        ├─▶ Extract text content (PDF, DOCX, TXT)
        │
        ├─▶ Save to Elasticsearch (document repository)
        │
        └─▶ Send documentId to Kafka queue

Step 2: Async Processing (Kafka Listener)
   │
   ├─▶ DocumentProcessingListener picks up message
   │
   └─▶ Calls vectorStoreService.indexDocument()
        │
        ├─▶ Split document into chunks (500 chars, 50 overlap)
        │   Example:
        │   Original: "Spring Boot is a framework... [5000 chars]"
        │   →  Chunk 1: "Spring Boot is a framework..." (0-500)
        │   →  Chunk 2: "framework that simplifies..." (450-950)
        │   →  Chunk 3: "simplifies application dev..." (900-1400)
        │
        ├─▶ Generate embeddings for each chunk
        │   Uses: Mistral embedding model via Ollama
        │   Input:  "Spring Boot is a framework..."
        │   Output: [0.123, -0.456, 0.789, ..., 0.321] (768 dimensions)
        │
        └─▶ Store in Elasticsearch Vector Index
            ┌─────────────────────────────────────────┐
            │ Document ID: doc-123                    │
            │ Filename: spring-boot-guide.pdf         │
            │ Chunk Index: 0                          │
            │ Text: "Spring Boot is a framework..."   │
            │ Embedding: [0.123, -0.456, ...]        │
            └─────────────────────────────────────────┘

Step 3: Generate Summary
   │
   └─▶ AIService.summarizeDocument()
        │
        ├─▶ Send document to Mistral LLM
        │
        └─▶ Store summary in document metadata

✅ Knowledge Base Updated!
```

### Phase 2: Question Answering (Using Updated Knowledge)

```
┌──────────────────────────────────────────────────────────────────┐
│                    QUESTION ANSWERING FLOW                       │
└──────────────────────────────────────────────────────────────────┘

Step 1: User Asks Question
   │
   └─▶ "What is Spring Boot?"

Step 2: Vector Search
   │
   ├─▶ Convert question to embedding
   │   Input:  "What is Spring Boot?"
   │   Output: [0.145, -0.432, 0.876, ..., 0.234] (768 dimensions)
   │
   └─▶ Search Elasticsearch for similar vectors
       ┌─────────────────────────────────────────────────────────┐
       │ Cosine Similarity Calculation:                          │
       │                                                          │
       │ Query Vector:    [0.145, -0.432, 0.876, ...]           │
       │         ↓                                                │
       │ Compare with all document chunk vectors:                │
       │                                                          │
       │ Chunk 1 Vector:  [0.123, -0.456, 0.789, ...]  → 0.92   │ ← High similarity!
       │ Chunk 2 Vector:  [0.234, -0.543, 0.678, ...]  → 0.87   │ ← High similarity!
       │ Chunk 3 Vector:  [0.001, -0.023, 0.045, ...]  → 0.34   │ ← Low similarity
       │                                                          │
       │ Return top 5 chunks with similarity > 0.5               │
       └─────────────────────────────────────────────────────────┘

Step 3: Build Context
   │
   └─▶ Combine retrieved chunks:
       ┌─────────────────────────────────────────────────────────┐
       │ Context 1:                                              │
       │ Spring Boot is a framework that simplifies Java        │
       │ development by providing auto-configuration...          │
       │                                                          │
       │ Context 2:                                              │
       │ The framework eliminates boilerplate code and          │
       │ provides embedded servers...                            │
       │                                                          │
       │ Context 3:                                              │
       │ Spring Boot makes it easy to create stand-alone,       │
       │ production-grade applications...                        │
       └─────────────────────────────────────────────────────────┘

Step 4: Send to LLM with Context
   │
   └─▶ Create enhanced prompt:
       ┌─────────────────────────────────────────────────────────┐
       │ System: You are a helpful AI assistant.                 │
       │                                                          │
       │ Retrieved Context:                                      │
       │ [Context 1]                                             │
       │ [Context 2]                                             │
       │ [Context 3]                                             │
       │                                                          │
       │ Question: What is Spring Boot?                          │
       │                                                          │
       │ Answer based on the context above:                      │
       └─────────────────────────────────────────────────────────┘

Step 5: LLM Generates Answer
   │
   └─▶ Mistral processes context + question
       │
       └─▶ Returns: "Based on the provided context, Spring Boot 
                     is a framework that simplifies Java 
                     development by providing auto-configuration 
                     and embedded servers..."

✅ Answer based on YOUR documents!
```

## Key Concepts Explained

### 1. Embeddings (Vector Representations)

**What are they?**

- Numerical representations of text
- Capture semantic meaning
- Similar meanings = Similar vectors

**Example:**

```
Text: "car"          →  Embedding: [0.8, 0.2, 0.1, ...]
Text: "automobile"   →  Embedding: [0.7, 0.3, 0.1, ...]  ← Similar!
Text: "banana"       →  Embedding: [0.1, 0.1, 0.9, ...]  ← Different!
```

### 2. Cosine Similarity

**How it works:**

```
similarity = dot_product(A, B) / (magnitude(A) * magnitude(B))

Result: -1.0 to 1.0
- 1.0  = Identical meaning
- 0.5  = Somewhat similar
- 0.0  = Unrelated
- -1.0 = Opposite meaning
```

**Example:**

```
Query: "What is AI?"
Doc 1: "Artificial Intelligence is..."  → Similarity: 0.95 ✅
Doc 2: "Machine learning techniques..."  → Similarity: 0.78 ✅
Doc 3: "Best pizza recipes in Italy..." → Similarity: 0.12 ❌
```

### 3. Why Chunking?

**Problem:** Documents are too long for context windows

**Solution:** Break into smaller, overlapping pieces

**Benefits:**

- Fits in LLM context window
- More precise retrieval
- Better relevance matching

**Example:**

```
Document (10,000 words) → 20 chunks (500 words each)

Question: "What is Spring Boot?"
↓
Only retrieve 5 most relevant chunks (2,500 words)
↓
Send to LLM (fits comfortably in context)
```

## Comparison: Traditional vs RAG

### Traditional LLM (No RAG)

```
┌────────────────────────────────────────┐
│ User: "What's in our Q4 report?"       │
│   ↓                                    │
│ LLM: "I don't have access to your     │
│      specific Q4 report."             │
└────────────────────────────────────────┘

Limitations:
❌ Only knows training data (cutoff date)
❌ No access to private documents
❌ Can't answer company-specific questions
❌ Static knowledge
```

### RAG System (This Application)

```
┌────────────────────────────────────────┐
│ User: "What's in our Q4 report?"       │
│   ↓                                    │
│ System:                                │
│ 1. Search vector database              │
│ 2. Find Q4 report chunks               │
│ 3. Retrieve relevant sections          │
│   ↓                                    │
│ LLM: "According to your Q4 report,    │
│      revenue increased 25% and..."    │
└────────────────────────────────────────┘

Advantages:
✅ Accesses YOUR documents
✅ Always up-to-date (add new docs anytime)
✅ Answers company-specific questions
✅ Dynamic, growing knowledge base
```

## Technical Implementation

### Code Flow

#### 1. Indexing (VectorStoreService.java)

```java
public void indexDocument(String documentId, String filename, String content) {
    // Split into chunks
    List<String> chunks = splitIntoChunks(content, 500, 50);
    
    // Create Document objects with metadata
    List<Document> documents = new ArrayList<>();
    for (int i = 0; i < chunks.size(); i++) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("document_id", documentId);
        metadata.put("filename", filename);
        metadata.put("chunk_index", i);
        
        Document doc = new Document(chunks.get(i), metadata);
        documents.add(doc);
    }
    
    // Store in vector database
    vectorStore.add(documents);  // ← LLM knowledge updated!
}
```

#### 2. Searching (VectorStoreService.java)

```java
public List<Document> searchRelevantContextInDocument(String query, String documentId, int topK) {
    // Build search request
    SearchRequest searchRequest = SearchRequest.builder()
        .query(query)  // User's question
        .topK(topK)    // Get top 5 results
        .similarityThreshold(0.5)  // Min 50% similarity
        .filterExpression("document_id == '" + documentId + "'")  // Filter by document
        .build();
    
    // Search vector store
    return vectorStore.similaritySearch(searchRequest);
}
```

#### 3. Answering (ChatService.java)

```java
public ChatMessage askQuestion(String question, String sessionId, String documentId) {
    // 1. Retrieve relevant chunks
    List<Document> relevantDocs = vectorStoreService
        .searchRelevantContextInDocument(question, documentId, 5);
    
    // 2. Format as context string
    String context = vectorStoreService.formatContext(relevantDocs);
    
    // 3. Send to LLM with context
    String answer = aiService.answerQuestionWithRAG(question, context);
    
    // 4. Save and return
    return saveChatMessage(question, answer, sessionId, documentId);
}
```

#### 4. LLM Prompt (AIService.java)

```java
public String answerQuestionWithRAG(String question, String retrievedContext) {
    String promptText = """
        You are a helpful AI assistant that answers questions based on provided context.
        
        Retrieved Context:
        {context}
        
        Question: {question}
        
        Answer:
        """;
    
    // Send to Mistral LLM
    Prompt prompt = promptTemplate.create(Map.of(
        "context", retrievedContext,
        "question", question
    ));
    
    return chatModel.call(prompt).getResult().getOutput().getText();
}
```

## Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                       COMPLETE DATA FLOW                        │
└─────────────────────────────────────────────────────────────────┘

                    USER UPLOADS DOCUMENT
                            │
                            ▼
                    ┌───────────────┐
                    │   Backend     │
                    │  (REST API)   │
                    └───────┬───────┘
                            │
            ┌───────────────┼───────────────┐
            │               │               │
            ▼               ▼               ▼
    ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
    │ Elasticsearch│ │    Kafka     │ │   Redis      │
    │  (Document)  │ │   (Queue)    │ │  (Cache)     │
    └──────────────┘ └──────┬───────┘ └──────────────┘
                            │
                            ▼
                ┌───────────────────────┐
                │  Kafka Listener       │
                │ (Async Processing)    │
                └───────────┬───────────┘
                            │
            ┌───────────────┼───────────────┐
            │               │               │
            ▼               ▼               ▼
    ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
    │ Split Chunks │ │   Generate   │ │  Generate    │
    │              │→│  Embeddings  │→│   Summary    │
    └──────────────┘ └──────────────┘ └──────────────┘
                            │
                            ▼
                ┌───────────────────────┐
                │  Elasticsearch        │
                │  (Vector Store)       │
                │                       │
                │  ┌─────────────────┐ │
                │  │ Chunk 1 + Vec   │ │
                │  │ Chunk 2 + Vec   │ │
                │  │ Chunk 3 + Vec   │ │
                │  │     ...         │ │
                │  └─────────────────┘ │
                └───────────────────────┘
                            │
                            ▼
                ✅ KNOWLEDGE BASE UPDATED

                    USER ASKS QUESTION
                            │
                            ▼
                    ┌───────────────┐
                    │   ChatService │
                    └───────┬───────┘
                            │
                            ▼
                ┌───────────────────────┐
                │  Vector Search        │
                │  (Similarity Search)  │
                └───────────┬───────────┘
                            │
                            ▼
                    ┌───────────────┐
                    │  Top 5 Chunks │
                    └───────┬───────┘
                            │
                            ▼
                    ┌───────────────┐
                    │   AIService   │
                    │ (Format Prompt│
                    └───────┬───────┘
                            │
                            ▼
                    ┌───────────────┐
                    │ Ollama/Mistral│
                    │     (LLM)     │
                    └───────┬───────┘
                            │
                            ▼
                        ANSWER
```

## Summary

**How LLM Knowledge Updates Work:**

1. **Document Upload** → Text extracted from file
2. **Chunking** → Split into 500-char pieces with overlap
3. **Embedding** → Convert chunks to 768-dim vectors
4. **Indexing** → Store vectors in Elasticsearch
5. **Query Time** → Search vectors for relevant chunks
6. **Context Injection** → Add chunks to LLM prompt
7. **Answer Generation** → LLM uses YOUR context

**Key Point:** The LLM doesn't learn permanently. Instead, we provide it with relevant context from your documents at
query time. This approach:

- ✅ Works with any LLM
- ✅ No retraining needed
- ✅ Instant updates (upload → indexed → queryable)
- ✅ Cost-effective
- ✅ Scalable

This is the power of RAG! 🚀
