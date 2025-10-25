# System Architecture

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         User Browser                            │
│                                                                 │
│  ┌─────────────��─────────────────────────────────────────────┐ │
│  │                  React Frontend (Port 3000)               │ │
│  │                                                           │ │
│  │  ┌────────────┐  ┌───────────┐  ┌──────────────────┐   │ │
│  │  │  Document  │  │ Document  │  │      Chat        │   │ │
│  │  │  Upload    │  │   List    │  │   Interface      │   │ │
│  │  └────────────┘  └───────────┘  └──────────────────┘   │ │
│  └────────────────────────┬──────────────────────────────────┘ │
└────────────────────────────┼──────────────────────────────────┘
                             │ HTTP/REST API
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│              Spring Boot Backend (Port 8080)                    │
│                                                                 │
│  ┌────────────────┐      ┌──────────────┐                     │
│  │   Document     │      │    Chat      │                     │
│  │  Controller    │      │ Controller   │                     │
│  └────────┬───────┘      └──────┬───────┘                     │
│           │                     │                              │
│  ┌────────▼────────┐    ┌──────▼──────┐   ┌──────────────┐  │
│  │   Document      │    │    Chat     │   │      AI      │  │
│  │    Service      │◄───┤   Service   │◄──┤   Service    │  │
│  └────────┬────────┘    └─────────────┘   └──────────────┘  │
│           │                                                    │
│           ├─────────────────┐                                 │
│           │                 │                                 │
│  ┌────────▼────────┐   ┌────▼──────────┐                    │
│  │     Kafka       │   │  Elasticsearch│                    │
│  │   Producer      │   │  Repository   │                    │
│  └────────┬────────┘   └───────────────┘                    │
└───────────┼──────────────────────────────────────────────────┘
            │
            ▼
┌─────────────────────────────────────────────────────────────────┐
│                    Message Queue Layer                          │
│                                                                 │
│  ┌─────────────────────────────────────────────────────────┐  │
│  │              Apache Kafka (Port 9092)                   │  │
│  │                                                         │  │
│  │  Topics:                                                │  │
│  │  • documentModel-upload                                      │  │
│  │  • documentModel-processing                                  │  │
│  │  • chat-request                                         │  │
│  └───────────────────────┬─────────────────────────────────┘  │
└────────────────────────────┼──────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                  Document Processing Consumer                   │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │      DocumentProcessingListener (Kafka Consumer)         │ │
│  │                                                          │ │
│  │  1. Receive Document ID                                 │ │
│  │  2. Fetch Document from Elasticsearch                   │ │
│  │  3. Send to AI Service for Summarization                │ │
│  │  4. Update Document with Summary                        │ │
│  └──────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Data & AI Layer                            │
│                                                                 │
│  ┌────────────────────────┐      ┌──────────────────────────┐ │
│  │    Elasticsearch       │      │   Ollama + Mistral       │ │
│  │    (Port 9200)         │      │   (Port 11434)           │ │
│  │                        │      │                          │ │
│  │  Indices:              │      │  Models:                 │ │
│  │  • documentModels           │      │  • mistral:latest        │ │
│  │  • chat_messages       │      │                          │ │
│  └────────────────────────┘      └──────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

## Component Details

### 1. Frontend Layer (React + TypeScript)

**Responsibilities:**

- User interface rendering
- Form handling and validation
- API communication
- State management
- Real-time UI updates

**Key Components:**

#### DocumentUpload Component

```typescript
Features:
- File upload (PDF, DOCX, TXT)
- Text input option
- Progress indication
- Error handling
```

#### DocumentList Component

```typescript
Features:
- Display all uploaded documentModels
- Show documentModel metadata
- Indicate summary availability
- Document selection
```

#### ChatInterface Component

```typescript
Features:
- Display documentModel summary
- Chat message history
- Question input
- Real-time response display
- Typing indicators
```

### 2. API Layer (REST Controllers)

**DocumentController:**

```java
POST   /api/documentModels/upload      - Upload file
POST   /api/documentModels/upload-text - Upload text
GET    /api/documentModels             - List all documentModels
GET    /api/documentModels/{id}        - Get specific documentModel
```

**ChatController:**

```java
POST   /api/chat/ask              - Ask question
GET    /api/chat/history/{id}     - Get chat history
```

### 3. Service Layer (Business Logic)

#### DocumentService

```java
Responsibilities:
- Handle documentModel uploads
- Extract text from various formats (PDF, DOCX)
- Store documentModels in Elasticsearch
- Send processing events to Kafka
- Update documentModel summaries
```

#### AIService

```java
Responsibilities:
- Generate documentModel summaries
- Answer questions based on context
- Manage conversation history
- Prompt engineering and optimization
```

#### ChatService

```java
Responsibilities:
- Process chat requests
- Retrieve documentModel context
- Manage chat history
- Coordinate with AI service
- Store chat messages
```

### 4. Repository Layer (Data Access)

**DocumentRepository:**

```java
Interface: ElasticsearchRepository<Document, String>
Methods:
- save()
- findById()
- findAll()
- findByFilenameContaining()
```

**ChatMessageRepository:**

```java
Interface: ElasticsearchRepository<ChatMessage, String>
Methods:
- save()
- findBySessionIdOrderByTimestampAsc()
- findByDocumentIdOrderByTimestampDesc()
```

### 5. Message Queue Layer (Apache Kafka)

**Topics:**

1. **documentModel-upload**
    - Purpose: Document upload notifications
    - Producer: DocumentService
    - Consumer: (Future use)

2. **documentModel-processing**
    - Purpose: Trigger documentModel processing
    - Producer: DocumentService
    - Consumer: DocumentProcessingListener

3. **chat-request**
    - Purpose: Chat request processing
    - Producer: ChatService
    - Consumer: (Future use for async processing)

**Configuration:**

- Partitions: 3 per topic
- Replication Factor: 1 (development)
- Auto-create: Enabled

### 6. Data Storage Layer

#### Elasticsearch Indices

**documentModels Index:**

```json
{
  "id": "string",
  "filename": "string",
  "content": "text",
  "summary": "text",
  "contentType": "keyword",
  "uploadedAt": "date",
  "size": "long"
}
```

**chat_messages Index:**

```json
{
  "id": "string",
  "sessionId": "keyword",
  "question": "text",
  "answer": "text",
  "timestamp": "date",
  "documentId": "keyword"
}
```

### 7. AI Layer (Ollama + Mistral)

**Mistral Model:**

- Type: Large Language Model
- Purpose: Text generation, summarization, Q&A
- Context Window: ~8k tokens
- Hosting: Local via Ollama

**Prompts:**

1. **Summarization Prompt:**

```
Please provide a comprehensive summary of the following documentModel.
Focus on the main points, key findings, and important details.
Keep the summary concise but informative.

Document: {content}

Summary:
```

2. **Q&A Prompt (without history):**

```
Based on the following context, please answer the question.
If the answer cannot be found in the context, say so clearly.

Context: {context}
Question: {question}

Answer:
```

3. **Q&A Prompt (with history):**

```
Based on the following context and chat history, answer the question.
Use the chat history to maintain conversation continuity.

Context: {context}
Chat History: {history}
Question: {question}

Answer:
```

## Data Flow Diagrams

### Document Upload & Processing Flow

```
┌──────┐
│ User │
└──┬───┘
   │ 1. Upload Document
   ▼
┌────────────────┐
│   Frontend     │
└───────┬────────┘
        │ 2. POST /api/documentModels/upload
        ▼
┌────────────────┐
│ DocumentCtrl   │
└───────┬────────┘
        │ 3. uploadDocument()
        ▼
┌────────────────┐
│ DocumentSvc    │──────┐
└───────┬────────┘      │ 4. Extract Text
        │               │    (PDF/DOCX)
        │               ▼
        │         ┌─────────┐
        │         │ Extract │
        │         └─────────┘
        │
        │ 5. Save to Elasticsearch
        ▼
┌────────────────┐
│ Elasticsearch  │
└────────────────┘
        │
        │ 6. Send to Kafka
        ▼
┌────────────────┐
│     Kafka      │
└───────┬────────┘
        │ 7. Consume Event
        ▼
┌────────────────┐
│DocProcessLstnr │
└───────┬────────┘
        │ 8. Generate Summary
        ▼
┌────────────────┐
│   AI Service   │
└───────┬────────┘
        │ 9. Update Document
        ▼
┌────────────────┐
│ Elasticsearch  │
└────────────────┘
```

### Q&A Flow

```
┌──────┐
│ User │
└──┬───┘
   │ 1. Ask Question
   ▼
┌────────────────┐
│   Frontend     │
└───────┬────────┘
        │ 2. POST /api/chat/ask
        ▼
┌────────────────┐
│   ChatCtrl     │
└───────┬────────┘
        │ 3. askQuestion()
        ▼
┌────────────────┐
│  ChatService   │
└───────┬────────┘
        │ 4. Get Document
        ▼
┌────────────────┐
│ Elasticsearch  │ (Retrieve Document)
└────────────────┘
        │
        │ 5. Get Chat History
        ▼
┌────────────────┐
│ Elasticsearch  │ (Retrieve History)
└────────────────┘
        │
        │ 6. Generate Answer
        ▼
┌────────────────┐
│   AI Service   │ (Call Mistral)
└───────┬────────┘
        │ 7. Save ChatMessage
        ▼
┌────────────────┐
│ Elasticsearch  │
└───────┬────────┘
        │ 8. Return Response
        ���
┌────────────────┐
│   Frontend     │
└───────┬────────┘
        │ 9. Display Answer
        ▼
┌──────┐
│ User │
└──────┘
```

## Scalability Considerations

### Horizontal Scaling

**Frontend:**

- Static files can be served via CDN
- Multiple instances behind load balancer

**Backend:**

- Stateless design allows multiple instances
- Load balancer distributes traffic
- Session affinity not required

**Elasticsearch:**

- Add nodes to cluster
- Distribute shards across nodes
- Configure replicas for high availability

**Kafka:**

- Increase partitions for higher throughput
- Add brokers for replication
- Consumer groups for parallel processing

### Vertical Scaling

**Backend:**

- Increase heap size for Java
- Allocate more CPU cores
- Optimize connection pools

**Elasticsearch:**

- Increase memory for indexing
- Add more disk for storage
- Tune JVM settings

**Ollama:**

- Use GPU acceleration
- Increase model memory allocation
- Use faster SSD storage

## Security Architecture

### Current State (Development)

```
┌─────────┐
│ Browser │ ───────► No authentication
└─────────┘          CORS enabled
     │
     ▼
┌──────────┐
│ Backend  │ ───────► No auth on endpoints
└──────────┘          Open to localhost
     │
     ▼
┌──────────────┐
│Elasticsearch │ ───► No security
└──────────────┘      Open connection
     │
     ▼
┌──────────────┐
│    Kafka     │ ───► No security
└──────────────┘      Plain connection
```

### Recommended Production State

```
┌─────────┐
│ Browser │ ───────► JWT Authentication
└─────────┘          HTTPS only
     │
     ▼
┌──────────┐
│ Backend  │ ───────► Spring Security
└──────────┘          Rate limiting
     │               Input validation
     ▼
┌──────────────┐
│Elasticsearch │ ───► X-Pack Security
└──────────────┘      SSL/TLS enabled
     │               API key auth
     ▼
┌──────────────┐
│    Kafka     │ ───► SASL authentication
└──────────────┘      SSL encryption
```

## Monitoring & Observability

### Recommended Metrics

**Application Metrics:**

- Request count and latency
- Error rates by endpoint
- Document processing time
- AI response time
- Cache hit rates

**Infrastructure Metrics:**

- CPU and memory usage
- Disk I/O and storage
- Network throughput
- Container health

**Business Metrics:**

- Documents uploaded per day
- Questions asked per day
- Average session duration
- Popular documentModel types

### Logging Strategy

**Application Logs:**

- INFO: Normal operations
- WARN: Recoverable errors
- ERROR: Serious issues
- DEBUG: Detailed troubleshooting

**Structured Logging:**

```json
{
  "timestamp": "2024-01-01T12:00:00Z",
  "level": "INFO",
  "service": "documentModel-service",
  "action": "documentModel-uploaded",
  "documentId": "abc123",
  "userId": "user456",
  "duration": 250
}
```

## Disaster Recovery

### Backup Strategy

**Elasticsearch:**

- Snapshot to S3/Azure/GCS
- Daily automated backups
- Retain 30 days of snapshots

**Configuration:**

- Version control for application config
- Document environment variables
- Infrastructure as Code

### Recovery Procedures

**Data Loss:**

1. Restore Elasticsearch snapshot
2. Replay Kafka topics if needed
3. Verify data integrity

**Service Outage:**

1. Check health endpoints
2. Review logs for errors
3. Scale up resources if needed
4. Failover to backup region

## Conclusion

This architecture provides:

- ✅ **Scalability:** Horizontal and vertical scaling options
- ✅ **Reliability:** Asynchronous processing, error handling
- ✅ **Maintainability:** Clean separation of concerns
- ✅ **Observability:** Comprehensive logging and monitoring
- ✅ **Security:** Clear path to production-grade security
- ✅ **Performance:** Optimized data flow and caching
