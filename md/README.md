# RAG Chatbot - Document Q&A System with LLM Knowledge Updates

A full-stack application that enables users to upload documents, get AI-powered summaries, and interact with document
content through a Q&A chatbot interface. The system **automatically updates the LLM's knowledge base** by indexing
documents into a vector store, enabling intelligent Q&A about YOUR specific content.

## 🎯 What's New: RAG Implementation

This system now features a **complete RAG (Retrieval Augmented Generation) implementation** that:

- ✅ **Automatically indexes documents** into Elasticsearch vector store
- ✅ **Updates LLM knowledge** by creating searchable embeddings
- ✅ **Retrieves relevant context** using semantic search
- ✅ **Provides accurate answers** based on YOUR documents

### How It Works (Simple)

```
1. Upload Document → Split into chunks → Generate embeddings → Store in vector DB
2. Ask Question → Search vectors → Retrieve relevant chunks → Send to LLM → Get answer
```

**Result:** The LLM answers questions using YOUR documents, not just its training data! 🚀

## 📚 Complete Documentation

### Quick Start

- **[QUICKSTART_RAG.md](QUICKSTART_RAG.md)** - Get running in 5 minutes
- **[DEMO_WALKTHROUGH.md](DEMO_WALKTHROUGH.md)** - Step-by-step demo with real examples

### Understanding RAG

- **[HOW_LLM_UPDATES_WORK.md](HOW_LLM_UPDATES_WORK.md)** - How the system updates LLM knowledge
- **[RAG_SYSTEM_SUMMARY.md](RAG_SYSTEM_SUMMARY.md)** - Complete system overview

### PDF Conversion Feature 🆕

- **[PDF_CONVERSION_FEATURE.md](PDF_CONVERSION_FEATURE.md)** - Technical guide for PDF conversion
- **[USAGE_EXAMPLE_PDF_CONVERSION.md](USAGE_EXAMPLE_PDF_CONVERSION.md)** - Usage examples and integration guides
- **[IMAGE_TEXT_EXTRACTION_GUIDE.md](IMAGE_TEXT_EXTRACTION_GUIDE.md)** - Enhanced OCR for extracting text from images 🆕
- **[OCR_FIX_SUMMARY.md](OCR_FIX_SUMMARY.md)** - Critical fix for image text extraction 🔥
- **[TEST_OCR_EXTRACTION.md](TEST_OCR_EXTRACTION.md)** - Testing and verification guide

### Technical Details

- **[RAG_CHATBOT_GUIDE.md](RAG_CHATBOT_GUIDE.md)** - Comprehensive technical guide
- **[RAG_IMPLEMENTATION_COMPLETE.md](RAG_IMPLEMENTATION_COMPLETE.md)** - Implementation details
- **[CHANGES_SUMMARY.md](CHANGES_SUMMARY.md)** - All changes made

### Operations

- **[RUNBOOK.md](RUNBOOK.md)** - Operations and troubleshooting
- **[ARCHITECTURE.md](ARCHITECTURE.md)** - System architecture

## 🚀 Features

- **Document Upload**: Support for PDF, DOCX, and text files
- **🆕 PDF Conversion**: Automatic conversion of all files to searchable PDFs with OCR support
- **🆕 Enhanced OCR**: Always-on OCR extracts text from images within PDFs (charts, diagrams, screenshots)
- **OCR Support**: Automatic text extraction from scanned PDFs and images using Tesseract with image preprocessing
- **AI Summarization**: Automatic documentModel summarization using Mistral AI
- **Q&A Chatbot**: Interactive chat interface for asking questions about documentModels
- **Semantic Search**: Vector embeddings for intelligent documentModel retrieval
- **Elasticsearch Integration**: Fast documentModel storage and retrieval
- **Vector Databases**: Support for Chroma and Qdrant for vector storage
- **Kafka Messaging**: Asynchronous documentModel processing
- **Redis Caching**: Fast embedding and response caching
- **Modern Web UI**: Beautiful, responsive React-based interface
- **Chat History**: Persistent conversation tracking per session
- **🆕 Download Converted PDFs**: Get searchable PDF versions of uploaded documents

## 🛠️ Technology Stack

### Backend
- **Spring Boot 3.5.8**: Main application framework
- **Spring AI**: Integration with AI models and vector stores
- **Ollama + Mistral**: Local AI model for summarization and Q&A
- **Elasticsearch**: Document storage and search
- **Vector Databases**: Chroma or Qdrant for embeddings
- **Apache Kafka**: Message queue for asynchronous processing
- **Redis**: Caching layer for performance
- **Apache PDFBox**: PDF text extraction
- **Apache POI**: DOCX text extraction
- **Tess4J**: OCR for scanned documents

### Frontend

- **React 18**: UI framework
- **TypeScript**: Type-safe JavaScript
- **Vite**: Build tool and dev server
- **Axios**: HTTP client
- **Lucide React**: Icon library

## 📋 Prerequisites

- **Java 17** or higher
- **Maven 3.6+**
- **Node.js 18+** and npm
- **Docker & Docker Compose** (for running all infrastructure services)
- **4GB+ RAM** for Docker containers

## 🔧 Installation & Setup

### 1. Clone the Repository

```bash
git clone <repository-url>
cd rag-demo
```

### 2. Start Required Services with Docker Compose

The `docker-compose.yml` includes:

- ✅ Elasticsearch (documentModel storage)
- ✅ Kafka + Zookeeper (message queue)
- ✅ Ollama (AI model hosting)
- ✅ Chroma (vector database)
- ✅ Qdrant (alternative vector database)
- ✅ Redis (caching)
- ✅ Kafka UI (monitoring)
- ✅ Elasticsearch Head (monitoring)

Start all services:

```bash
docker-compose up -d
```

Or start only core services:

```bash
docker-compose up -d elasticsearch kafka zookeeper ollama chroma redis
```

### 3. Install Mistral Model in Ollama

Once Ollama is running, pull the Mistral model:

```bash
docker exec -it rag-ollama ollama pull mistral
```

### 4. Configure Application

The application is pre-configured in `src/main/resources/application.properties`. Key settings:

```properties
# Ollama Configuration
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.model=mistral

# Elasticsearch Configuration
spring.elasticsearch.uris=http://localhost:9200

# Kafka Configuration
spring.kafka.bootstrap-servers=localhost:9092

# Vector Store - Chroma
spring.ai.vectorstore.chroma.url=http://localhost:8000

# Redis Cache
spring.data.redis.host=localhost
```

### 5. Build and Run Backend

```bash
# Build the project
mvn clean install

# Run the Spring Boot application WITH OCR support (macOS)
./start-with-ocr.sh

# OR run without OCR script (may have library path issues)
# mvn spring-boot:run
```

The backend will start on `http://localhost:8080`

**Note for macOS users:** Use `./start-with-ocr.sh` to ensure Tesseract library is found correctly.

### 6. Setup and Run Frontend

```bash
# Navigate to frontend directory
cd frontend

# Install dependencies
npm install

# Start development server
npm run dev
```

The frontend will start on `http://localhost:3000`

## 📱 Usage

1. **Access the Application**: Open `http://localhost:3000` in your browser

2. **Upload a Document**:
    - Click "Upload File" to upload PDF, DOCX, or text files
    - Or click "Enter Text" to paste content directly
    - The documentModel will be processed and automatically summarized
    - Vector embeddings are created for semantic search

3. **View Summary**:
    - Select a documentModel from the list
    - The AI-generated summary will appear at the top

4. **Ask Questions**:
    - Type your question in the chat input
    - Press Enter or click the send button
    - The AI will answer based on documentModel content using:
        - Vector similarity search for relevant context
        - Chat history for conversation continuity
        - Mistral AI for intelligent responses

## 🏗️ Architecture

### Document Processing Flow

1. **Upload**: User uploads documentModel via REST API
2. **Storage**: Document saved to Elasticsearch
3. **Chunking**: Content split into chunks
4. **Embedding**: Vector embeddings generated via Mistral
5. **Vector Storage**: Embeddings stored in Chroma/Qdrant
6. **Kafka Event**: Document ID sent to Kafka processing topic
7. **AI Processing**: Kafka listener consumes event and generates summary
8. **Update**: Summary saved back to Elasticsearch

### Q&A Flow with Vector Search

1. **Question**: User submits question via chat interface
2. **Embedding**: Question converted to vector embedding
3. **Vector Search**: Find similar documentModel chunks using cosine similarity
4. **Context Retrieval**: Top-K relevant chunks retrieved
5. **History**: Previous chat messages loaded for context
6. **AI Query**: Question + context + history sent to Mistral
7. **Response**: AI-generated answer returned and saved
8. **Display**: Answer displayed in chat interface

## 📂 Project Structure

```
rag-demo/
├── src/main/java/com/ai/rag_demo/
│   ├── config/          # Configuration classes (Kafka, Vector Store, Web)
│   ├── controller/      # REST controllers
│   ├── dto/             # Data transfer objects
│   ├── kafka/           # Kafka listeners
│   ├── model/           # Entity models
│   ├── repository/      # Elasticsearch repositories
│   └── service/         # Business logic services
│       ├── AIService.java
│       ├── ChatService.java
│       ├── DocumentService.java
│       └── VectorStoreService.java
├── frontend/
│   ├── src/
│   │   ├── components/  # React components
│   │   ├── api.ts       # API service
│   │   ├── types.ts     # TypeScript types
│   │   └── App.tsx      # Main app component
│   └── package.json
├── docker-compose.yml   # All infrastructure services
├── pom.xml
└── README.md
```

## 🔌 API Endpoints

### Documents
- `POST /api/documents/upload` - Upload a file (automatically converted to searchable PDF)
- `POST /api/documents/upload-text` - Upload text content
- `GET /api/documents` - Get all documents
- `GET /api/documents/{id}` - Get specific document
- `GET /api/documents/{id}/download-converted-pdf` - Download searchable PDF version 🆕
- `DELETE /api/documents/{id}` - Delete document and converted PDF 🆕

### Chat

- `POST /api/chat/ask` - Ask a question
- `GET /api/chat/history/{sessionId}` - Get chat history

## 🧪 Testing with cURL

### Document API Tests

#### 1. Upload a Text File

```bash
# Create a sample text file
echo "Artificial Intelligence is transforming the world. Machine learning is a subset of AI that enables computers to learn from data." > sample.txt

# Upload the file
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@sample.txt" \
  -H "Content-Type: multipart/form-data"
```

Expected response:

```json
{
  "id": "uuid-here",
  "filename": "sample.txt",
  "contentType": "text/plain",
  "size": 123,
  "uploadedAt": "2024-01-15T10:30:00",
  "summary": null
}
```

#### 2. Upload Text Content Directly

```bash
curl -X POST http://localhost:8080/api/documents/upload-text \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Spring Boot is a powerful framework for building Java applications. It simplifies the development process with auto-configuration and embedded servers.",
    "filename": "spring-boot-intro.txt"
  }'
```

Expected response:

```json
{
  "id": "uuid-here",
  "filename": "spring-boot-intro.txt",
  "contentType": "text/plain",
  "size": 150,
  "uploadedAt": "2024-01-15T10:35:00",
  "summary": null
}
```

#### 3. Get All Documents

```bash
curl -X GET http://localhost:8080/api/documents \
  -H "Accept: application/json"
```

Expected response:

```json
[
  {
    "id": "uuid-1",
    "filename": "sample.txt",
    "contentType": "text/plain",
    "size": 123,
    "uploadedAt": "2024-01-15T10:30:00",
    "summary": "This document discusses artificial intelligence and machine learning..."
  },
  {
    "id": "uuid-2",
    "filename": "spring-boot-intro.txt",
    "contentType": "text/plain",
    "size": 150,
    "uploadedAt": "2024-01-15T10:35:00",
    "summary": "An introduction to Spring Boot framework..."
  }
]
```

#### 4. Get Specific Document by ID

```bash
# Replace {document-id} with actual document ID from previous responses
curl -X GET http://localhost:8080/api/documents/{document-id} \
  -H "Accept: application/json"
```

Example with actual ID:

```bash
curl -X GET http://localhost:8080/api/documents/550e8400-e29b-41d4-a716-446655440000 \
  -H "Accept: application/json"
```

Expected response:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "filename": "sample.txt",
  "contentType": "text/plain",
  "size": 123,
  "uploadedAt": "2024-01-15T10:30:00",
  "summary": "This document discusses artificial intelligence and machine learning..."
}
```

#### 5. Upload a PDF File

```bash
# If you have a PDF file
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@document.pdf" \
  -H "Content-Type: multipart/form-data"
```

#### 6. Upload a DOCX File

```bash
# If you have a Word document
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@document.docx" \
  -H "Content-Type: multipart/form-data"
```

### Chat API Tests

#### 7. Ask a Question About Uploaded Documents

```bash
curl -X POST http://localhost:8080/api/chat/ask \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What is artificial intelligence?",
    "sessionId": "test-session-123"
  }'
```

Expected response:

```json
{
  "id": "chat-msg-id",
  "question": "What is artificial intelligence?",
  "answer": "Based on the documents, Artificial Intelligence is transforming the world. It encompasses various technologies including machine learning, which is a subset of AI that enables computers to learn from data.",
  "sessionId": "test-session-123",
  "timestamp": "2024-01-15T10:40:00"
}
```

#### 8. Ask a Follow-up Question

```bash
curl -X POST http://localhost:8080/api/chat/ask \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Can you tell me more about machine learning?",
    "sessionId": "test-session-123"
  }'
```

#### 9. Ask About Spring Boot

```bash
curl -X POST http://localhost:8080/api/chat/ask \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What are the benefits of Spring Boot?",
    "sessionId": "test-session-456"
  }'
```

#### 10. Get Chat History for a Session

```bash
# Get history for session test-session-123
curl -X GET http://localhost:8080/api/chat/history/test-session-123 \
  -H "Accept: application/json"
```

Expected response:

```json
[
  {
    "id": "chat-msg-1",
    "question": "What is artificial intelligence?",
    "answer": "Based on the documents, Artificial Intelligence is...",
    "sessionId": "test-session-123",
    "timestamp": "2024-01-15T10:40:00"
  },
  {
    "id": "chat-msg-2",
    "question": "Can you tell me more about machine learning?",
    "answer": "Machine learning is a subset of AI that...",
    "sessionId": "test-session-123",
    "timestamp": "2024-01-15T10:42:00"
  }
]
```

### Complete Test Workflow

Here's a complete workflow to test the entire system:

```bash
# Step 1: Upload a document with technical content
curl -X POST http://localhost:8080/api/documents/upload-text \
  -H "Content-Type: application/json" \
  -d '{
    "content": "Docker is a platform for developing, shipping, and running applications in containers. Containers are lightweight, portable, and provide consistent environments across different systems. Docker Compose is a tool for defining and running multi-container Docker applications.",
    "filename": "docker-basics.txt"
  }'

# Wait 5-10 seconds for processing and summarization
sleep 10

# Step 2: Verify document was uploaded and processed
curl -X GET http://localhost:8080/api/documents

# Step 3: Ask a question about the document
curl -X POST http://localhost:8080/api/chat/ask \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What is Docker?",
    "sessionId": "workflow-test-001"
  }'

# Step 4: Ask a follow-up question
curl -X POST http://localhost:8080/api/chat/ask \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What is Docker Compose?",
    "sessionId": "workflow-test-001"
  }'

# Step 5: View the chat history
curl -X GET http://localhost:8080/api/chat/history/workflow-test-001
```

### Pretty-Print JSON Responses

Add `| jq` to any curl command to format JSON output (requires `jq` to be installed):

```bash
# Install jq first (if not already installed)
# macOS: brew install jq
# Ubuntu/Debian: sudo apt-get install jq

# Example with pretty-printing
curl -X GET http://localhost:8080/api/documents | jq '.'
```

### Save Response to File

```bash
# Save document list to file
curl -X GET http://localhost:8080/api/documents > documents.json

# Save specific document details
curl -X GET http://localhost:8080/api/documents/{document-id} > document-details.json

# Save chat history
curl -X GET http://localhost:8080/api/chat/history/test-session-123 > chat-history.json
```

### Error Testing

#### Test with Invalid Document ID

```bash
curl -X GET http://localhost:8080/api/documents/invalid-id-12345 \
  -H "Accept: application/json"
```

Expected: 404 Not Found

#### Test with Empty Content

```bash
curl -X POST http://localhost:8080/api/documents/upload-text \
  -H "Content-Type: application/json" \
  -d '{
    "content": "",
    "filename": "empty.txt"
  }'
```

#### Test with Missing Fields

```bash
curl -X POST http://localhost:8080/api/chat/ask \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What is AI?"
  }'
```

### Advanced Testing Scripts

#### Batch Upload Multiple Documents

```bash
#!/bin/bash
# save as batch-upload.sh

documents=(
  "AI:Artificial Intelligence is the simulation of human intelligence by machines."
  "ML:Machine Learning is a method of data analysis that automates analytical model building."
  "DL:Deep Learning is a subset of machine learning based on artificial neural networks."
)

for doc in "${documents[@]}"; do
  IFS=':' read -r filename content <<< "$doc"
  echo "Uploading $filename..."
  curl -X POST http://localhost:8080/api/documents/upload-text \
    -H "Content-Type: application/json" \
    -d "{\"content\": \"$content\", \"filename\": \"$filename.txt\"}"
  echo -e "\n"
  sleep 2
done

echo "All documents uploaded!"
```

#### Interactive Chat Session

```bash
#!/bin/bash
# save as interactive-chat.sh

SESSION_ID="interactive-$(date +%s)"
echo "Chat Session: $SESSION_ID"
echo "Type 'exit' to quit"

while true; do
  read -p "Your question: " question
  if [ "$question" = "exit" ]; then
    break
  fi
  
  echo "Asking AI..."
  curl -X POST http://localhost:8080/api/chat/ask \
    -H "Content-Type: application/json" \
    -d "{\"question\": \"$question\", \"sessionId\": \"$SESSION_ID\"}" \
    | jq -r '.answer'
  echo ""
done

echo "Session ended. View history with:"
echo "curl http://localhost:8080/api/chat/history/$SESSION_ID | jq '.'"
```

Make scripts executable:

```bash
chmod +x batch-upload.sh interactive-chat.sh
./batch-upload.sh
./interactive-chat.sh
```

## 🎯 Vector Database Features

### Semantic Search

- Document chunks stored as vector embeddings
- Questions converted to vectors for similarity search
- Cosine similarity for relevance ranking

### Supported Vector Stores

**Chroma** (Default)

- Lightweight and easy to use
- REST API access
- Good for development

**Qdrant** (Alternative)

- High performance
- gRPC and REST support
- Better for production

Switch between them in `application.properties`:

```properties
# Use Chroma
spring.ai.vectorstore.chroma.url=http://localhost:8000

# Or use Qdrant
#spring.ai.vectorstore.qdrant.url=http://localhost:6333
```

## 📊 Monitoring & Management

### Service Dashboards

- **Elasticsearch**: http://localhost:9100
- **Kafka UI**: http://localhost:8090
- **Chroma API**: http://localhost:8000/docs
- **Qdrant Dashboard**: http://localhost:6333/dashboard
- **Backend API**: http://localhost:8080
- **Frontend UI**: http://localhost:3000

### View Service Status

```bash
docker-compose ps
```

### View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f elasticsearch
```

## 🐛 Troubleshooting

### OCR Not Working / Can't Extract Text from Images

- **Run diagnostic script**: `./check-ocr-status.sh`
- **Check Tesseract**: `tesseract --version` (should be 4.0+)
- **Install Tesseract**:
    - macOS: `brew install tesseract`
    - Ubuntu: `sudo apt-get install tesseract-ocr tesseract-ocr-eng`
- **See detailed guide**: [IMAGE_TEXT_EXTRACTION_GUIDE.md](IMAGE_TEXT_EXTRACTION_GUIDE.md)

### Elasticsearch not connecting
- Ensure Elasticsearch is running: `docker ps`
- Check health: `curl http://localhost:9200/_cluster/health`
- Increase Docker memory to 4GB+

### Kafka topics not created
- Topics are auto-created on first use
- Verify Kafka: `docker logs rag-kafka`

### Ollama/Mistral not responding
- Check Ollama: `curl http://localhost:11434/api/tags`
- Verify Mistral model is installed: `docker exec -it rag-ollama ollama list`

### Vector database connection failed

- Check Chroma: `curl http://localhost:8000/api/v1/heartbeat`
- Check Qdrant: `curl http://localhost:6333/`

### Frontend can't connect to backend
- Ensure backend is running on port 8080
- Check CORS configuration in `WebConfig.java`

## 📚 Documentation

- [Quick Start Guide](QUICKSTART.md) - Get started in minutes
- [Docker Compose Guide](DOCKER_COMPOSE_GUIDE.md) - Detailed service information
- [Architecture Documentation](ARCHITECTURE.md) - System design details
- [Implementation Summary](IMPLEMENTATION_SUMMARY.md) - Technical overview

## 🚀 Production Deployment

For production deployment:

1. Update Elasticsearch security settings
2. Configure Kafka with proper replication
3. Use production-grade vector database (Qdrant recommended)
4. Enable Redis authentication
5. Use managed cloud services where possible
6. Build frontend for production: `npm run build`
7. Configure proper CORS origins
8. Set up monitoring and logging
9. Implement rate limiting
10. Enable HTTPS

## 💡 Advanced Features

### Vector Embeddings

- Automatic chunking of large documentModels
- Embedding caching for performance
- Similarity-based context retrieval

### Caching Layer

- Redis caching for embeddings
- API response caching
- Configurable TTL

### Async Processing

- Kafka-based documentModel processing
- Non-blocking summarization
- Scalable architecture

## 📄 License

This project is open source and available under the MIT License.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📧 Support

For issues and questions, please open an issue in the repository.

---
