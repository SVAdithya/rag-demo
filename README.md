# RAG Chatbot - Document Q&A System

A full-stack application that enables users to upload documentModels, get AI-powered summaries, and interact with documentModel
content through a Q&A chatbot interface. Features **vector embeddings** for semantic search and advanced RAG
capabilities.

## 🚀 Features

- **Document Upload**: Support for PDF, DOCX, and text files
- **AI Summarization**: Automatic documentModel summarization using Mistral AI
- **Q&A Chatbot**: Interactive chat interface for asking questions about documentModels
- **Semantic Search**: Vector embeddings for intelligent documentModel retrieval
- **Elasticsearch Integration**: Fast documentModel storage and retrieval
- **Vector Databases**: Support for Chroma and Qdrant for vector storage
- **Kafka Messaging**: Asynchronous documentModel processing
- **Redis Caching**: Fast embedding and response caching
- **Modern Web UI**: Beautiful, responsive React-based interface
- **Chat History**: Persistent conversation tracking per session

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

# Run the Spring Boot application
mvn spring-boot:run
```

The backend will start on `http://localhost:8080`

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
- `POST /api/documentModels/upload` - Upload a file
- `POST /api/documentModels/upload-text` - Upload text content
- `GET /api/documentModels` - Get all documentModels
- `GET /api/documentModels/{id}` - Get specific documentModel

### Chat
- `POST /api/chat/ask` - Ask a question
- `GET /api/chat/history/{sessionId}` - Get chat history

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
