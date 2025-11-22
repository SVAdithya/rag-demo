# RAG Chatbot with LLM Knowledge Updates

This guide explains the complete RAG (Retrieval Augmented Generation) chatbot system that automatically updates the
LLM's knowledge base when new documents are uploaded.

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [How RAG Works](#how-rag-works)
3. [System Components](#system-components)
4. [How to Use](#how-to-use)
5. [API Endpoints](#api-endpoints)
6. [Technical Details](#technical-details)

## Architecture Overview

The system uses a sophisticated RAG architecture that combines:

```
┌───────────────────────────────────────────────────���─────────────┐
│                         RAG Chatbot System                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────┐      ┌──────────────┐      ┌──────────────┐  │
│  │   Frontend   │ ───▶ │   Backend    │ ───▶ │  Ollama LLM  │  │
│  │  (React UI)  │      │ (Spring Boot)│      │  (Mistral)   │  │
│  └──────────────┘      └──────────────┘      └──────────────┘  │
│         │                      │                      ▲          │
│         │                      ▼                      │          │
│         │              ┌──────────────┐              │          │
│         │              │    Kafka     │              │          │
│         │              │ (Message Bus)│              │          │
│         │              └──────────────┘              │          │
│         │                      │                      │          │
│         │                      ▼                      │          │
│         │      ┌───────────────────────────┐         │          │
│         └─────▶│  Elasticsearch Vector DB  │─────────┘          │
│                │  (Document Embeddings)     │                    │
│                └───────────────────────────┘                    │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

## How RAG Works

### 1. Document Upload & Indexing (Updating LLM Knowledge)

When you upload a new document:

```
User Upload → Backend → Kafka Queue → Document Processing
                                            ↓
                        ┌───────────────────────────────┐
                        │  1. Split document into chunks │
                        │  2. Generate embeddings        │
                        │  3. Store in vector database   │
                        │  4. Generate AI summary        │
                        └───────────────────────────────┘
                                            ↓
                                    Knowledge Updated!
```

**Key Points:**

- Documents are split into 500-character chunks with 50-character overlap
- Each chunk is converted to a 768-dimensional vector using embeddings
- Vectors are stored in Elasticsearch with metadata (document ID, filename, chunk index)
- This process **updates the LLM's knowledge base** with new information

### 2. Question Answering with RAG

When you ask a question:

```
User Question → Vector Search → Retrieve Relevant Chunks → Send to LLM
                     ↓                     ↓                     ↓
              Elasticsearch        Top 5 Most Similar      Context + Question
              Similarity           Document Chunks         = Informed Answer
              Search (Cosine)
```

**Example:**

```
Question: "What is artificial intelligence?"

Step 1: Convert question to vector
Step 2: Search vector database for similar content
Step 3: Retrieve top 5 most relevant chunks from documents
Step 4: Send chunks + question to Mistral LLM
Step 5: LLM generates answer based on YOUR documents
```

## System Components

### 1. VectorStoreService

**Purpose:** Manages document indexing and semantic search

**Key Methods:**

- `indexDocument()` - Splits document and stores embeddings in vector database
- `searchRelevantContextInDocument()` - Finds relevant chunks for a question
- `splitIntoChunks()` - Divides text into manageable pieces

### 2. ChatService

**Purpose:** Handles user questions with RAG

**Process:**

1. Receives user question and document ID
2. Searches vector store for relevant context
3. Passes context + question to AIService
4. Saves conversation history
5. Returns AI-generated answer

### 3. AIService

**Purpose:** Communicates with Ollama/Mistral LLM

**RAG Methods:**

- `answerQuestionWithRAG()` - Answers using retrieved context
- `answerWithHistoryAndRAG()` - Answers with context + conversation history
- `summarizeDocument()` - Generates document summaries

### 4. DocumentProcessingListener

**Purpose:** Async document processing via Kafka

**Workflow:**

```java
@KafkaListener
public void processDocument(String documentId) {
    // 1. Retrieve document
    DocumentModel doc = documentService.getDocumentById(documentId);
    
    // 2. Index into vector store (UPDATE LLM KNOWLEDGE)
    vectorStoreService.indexDocument(doc.getId(), doc.getFilename(), doc.getContent());
    
    // 3. Generate summary
    String summary = aiService.summarizeDocument(doc.getContent());
    
    // 4. Update document metadata
    documentService.updateDocumentSummary(documentId, summary);
}
```

## How to Use

### Prerequisites

1. **Start Infrastructure:**

```bash
# Start all services (Elasticsearch, Kafka, Ollama, etc.)
./start.sh

# Or manually with Docker Compose
docker-compose up -d
```

2. **Pull Mistral Model:**

```bash
docker exec -it rag-ollama ollama pull mistral
```

### Running the Application

1. **Start Backend:**

```bash
./mvnw spring-boot:run
```

2. **Start Frontend:**

```bash
cd frontend
npm install
npm run dev
```

3. **Access Application:**

- Frontend: http://localhost:3000
- Backend API: http://localhost:8080
- Elasticsearch: http://localhost:9200
- Kibana: http://localhost:5601

### Using the Chatbot

1. **Upload a Document:**
    - Click "Upload Document" button
    - Select a PDF, DOCX, or TXT file
    - Wait for processing (document is indexed automatically)

2. **Ask Questions:**
    - Select the uploaded document
    - Type your question in the chat interface
    - The system will:
        * Search the vector database
        * Find relevant content from YOUR document
        * Generate an answer using the Mistral LLM

3. **View Document Summary:**
    - Each uploaded document gets an AI-generated summary
    - Summary appears in the document details

## API Endpoints

### Document Management

#### Upload Document

```http
POST /api/documents/upload
Content-Type: multipart/form-data

Form Data:
- file: [your document file]

Response: 200 OK
{
  "id": "doc-uuid",
  "filename": "example.pdf",
  "contentType": "application/pdf",
  "size": 1024576,
  "uploadedAt": "2025-01-26T10:30:00",
  "summary": "AI generated summary..."
}
```

#### Upload Text Content

```http
POST /api/documents/upload-text
Content-Type: application/json

{
  "content": "Your text content here...",
  "filename": "example.txt"
}

Response: 200 OK
{
  "id": "doc-uuid",
  "filename": "example.txt",
  ...
}
```

#### Get All Documents

```http
GET /api/documents

Response: 200 OK
[
  {
    "id": "doc-uuid",
    "filename": "document1.pdf",
    "summary": "Summary...",
    ...
  },
  ...
]
```

#### Get Document by ID

```http
GET /api/documents/{id}

Response: 200 OK
{
  "id": "doc-uuid",
  "filename": "document.pdf",
  "content": "Full document text...",
  ...
}
```

### Chat Operations

#### Ask Question

```http
POST /api/chat/ask
Content-Type: application/json

{
  "question": "What is this document about?",
  "sessionId": "session-123",
  "documentId": "doc-uuid"
}

Response: 200 OK
{
  "id": "msg-uuid",
  "question": "What is this document about?",
  "answer": "Based on the document, it discusses...",
  "timestamp": "2025-01-26T10:30:00",
  "sessionId": "session-123"
}
```

#### Get Chat History

```http
GET /api/chat/history/{sessionId}

Response: 200 OK
[
  {
    "id": "msg-1",
    "question": "First question?",
    "answer": "First answer...",
    "timestamp": "2025-01-26T10:30:00"
  },
  ...
]
```

## Technical Details

### Vector Embeddings

**Model:** Mistral (via Ollama)  
**Dimensions:** 768  
**Similarity:** Cosine Similarity  
**Threshold:** 0.5

### Document Chunking Strategy

```
Chunk Size: 500 characters
Overlap: 50 characters
Purpose: Maintain context between chunks
```

**Example:**

```
Original Text: "Spring Boot is a framework... [5000 chars] ...and makes development easy."

Chunks:
1. "Spring Boot is a framework that..." (chars 0-500)
2. "framework that simplifies..." (chars 450-950) [50 char overlap]
3. "simplifies application development..." (chars 900-1400)
...
```

### Semantic Search Process

1. **Query Embedding:**

```java
String query = "What is Spring Boot?";
float[] queryVector = embeddingModel.embed(query);
// Result: [0.123, -0.456, 0.789, ..., 0.321] (768 dimensions)
```

2. **Similarity Search:**

```java
SearchRequest request = SearchRequest.builder()
    .query(query)
    .topK(5)  // Get top 5 results
    .similarityThreshold(0.5)  // Minimum 50% similarity
    .filterExpression("document_id == 'doc-uuid'")
    .build();
```

3. **Cosine Similarity Calculation:**

```
similarity = (A · B) / (||A|| × ||B||)

Where:
- A = query vector
- B = document chunk vector
- · = dot product
- ||x|| = vector magnitude
```

### Conversation Context

The system maintains conversation history:

- Stores last 5 messages per session
- Includes history in LLM prompts for context
- Enables follow-up questions
- Maintains coherent conversations

### Example Prompt to LLM

```
You are a helpful AI assistant that answers questions based on provided context.

Retrieved Context:
Context 1:
Spring Boot is a powerful framework for building Java applications...

Context 2:
The framework provides auto-configuration and embedded servers...

Previous Conversation:
Q: What frameworks does this document discuss?
A: The document primarily discusses Spring Boot framework.

Current Question: How does Spring Boot simplify development?

Answer:
```

## Performance Considerations

### Indexing Performance

- Async processing via Kafka
- Batch embedding generation
- Parallel chunk processing
- Typical upload time: 2-5 seconds for 100-page PDF

### Query Performance

- Vector search: < 100ms
- LLM generation: 1-3 seconds
- Total response time: 1.5-3.5 seconds
- Caching for embeddings enabled (Redis)

### Scalability

- Horizontal scaling via Kafka partitions
- Elasticsearch clustering support
- Stateless backend design
- Redis for distributed caching

## Troubleshooting

### Document Not Indexed

**Problem:** Questions don't return relevant answers

**Solution:**

1. Check Elasticsearch status: `curl http://localhost:9200/_cluster/health`
2. Verify index exists: `curl http://localhost:9200/rag-documents`
3. Check Kafka logs for processing errors
4. Ensure Ollama is running with Mistral model

### LLM Not Responding

**Problem:** Chatbot returns errors

**Solution:**

1. Check Ollama status: `curl http://localhost:11434/api/tags`
2. Verify Mistral model: `docker exec -it rag-ollama ollama list`
3. Check backend logs for connection errors
4. Restart Ollama if needed

### Slow Response Times

**Problem:** Answers take too long

**Solution:**

1. Reduce `topK` in search (fewer chunks)
2. Enable GPU for Ollama (if available)
3. Use smaller document chunks
4. Check system resources

## Advanced Configuration

### Adjusting RAG Parameters

**In ChatService.java:**

```java
// Retrieve more/fewer chunks
List<Document> relevantDocs = vectorStoreService
    .searchRelevantContextInDocument(question, documentId, 5);  // Change 5 to desired number
```

**In VectorStoreService.java:**

```java
// Adjust chunk size and overlap
List<String> chunks = splitIntoChunks(text, 500, 50);  // Change 500 and 50
```

**In SearchRequest:**

```java
// Adjust similarity threshold
SearchRequest searchRequest = SearchRequest.builder()
    .similarityThreshold(0.5)  // 0.0 = all results, 1.0 = exact match
    .build();
```

### Switching Vector Stores

The system supports multiple vector stores:

**Elasticsearch (Default):**

```properties
spring.ai.vectorstore.elasticsearch.index-name=rag-documents
spring.ai.vectorstore.elasticsearch.initialize-schema=true
```

**Chroma:**

```properties
spring.ai.vectorstore.chroma.url=http://localhost:8000
spring.ai.vectorstore.chroma.collection-name=rag-documents
```

**Qdrant:**

```properties
spring.ai.vectorstore.qdrant.url=http://localhost:6333
spring.ai.vectorstore.qdrant.collection-name=rag-documents
```

## Conclusion

This RAG chatbot system provides:
✅ Automatic LLM knowledge updates via document indexing  
✅ Semantic search for accurate answers  
✅ Conversation history maintenance  
✅ Scalable architecture with Kafka  
✅ Production-ready with monitoring  
✅ Beautiful React UI

The system continuously learns from uploaded documents, enabling the LLM to answer questions based on YOUR specific
content rather than just its training data.
