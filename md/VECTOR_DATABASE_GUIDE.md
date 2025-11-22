# Vector Database Integration Guide

This guide explains how vector databases are integrated into the RAG chatbot for semantic search and intelligent
documentModel retrieval.

## 🎯 What are Vector Databases?

Vector databases store **embeddings** (numerical representations of text) that enable:

- **Semantic search**: Find similar content based on meaning, not just keywords
- **Context retrieval**: Get the most relevant documentModel chunks for Q&A
- **Similarity matching**: Find related documentModels or questions

## 🏗️ Architecture Overview

```
┌─────────────┐
│  Document   │
│   Upload    │
└──────┬──────┘
       │
       ▼
┌─────────────────┐
│ Text Extraction │
└──────┬──────────┘
       │
       ▼
┌─────────────────┐
│  Text Chunking  │ (Split into 500-token chunks)
└──────┬──────────┘
       │
       ▼
┌─────────────────┐
│    Mistral      │ (Generate embeddings)
│   Embeddings    │
└──────┬──────────┘
       │
       ├─────────────────┐
       │                 │
       ▼                 ▼
┌─────────────┐   ┌──────────────┐
│Elasticsearch│   │Vector DB     │
│(Full text)  │   │(Chroma/Qdrant)│
└─────────────┘   └──────────────┘
```

## 📊 Supported Vector Databases

### 1. Chroma (Default)

**Best for:**

- Development and prototyping
- Small to medium datasets
- Quick setup

**Features:**

- REST API
- Python client
- Easy to use
- Lightweight

**Configuration:**

```properties
spring.ai.vectorstore.chroma.url=http://localhost:8000
spring.ai.vectorstore.chroma.collection-name=rag-documentModels
spring.ai.vectorstore.chroma.initialize-schema=true
```

**Docker:**

```yaml
chroma:
  image: chromadb/chroma:latest
  ports:
    - "8000:8000"
  volumes:
    - chroma_data:/chroma/chroma
```

### 2. Qdrant

**Best for:**

- Production deployments
- Large-scale applications
- High performance needs

**Features:**

- gRPC and REST APIs
- Advanced filtering
- Horizontal scaling
- Web dashboard

**Configuration:**

```properties
spring.ai.vectorstore.qdrant.url=http://localhost:6333
spring.ai.vectorstore.qdrant.collection-name=rag-documentModels
spring.ai.vectorstore.qdrant.initialize-schema=true
```

**Docker:**

```yaml
qdrant:
  image: qdrant/qdrant:latest
  ports:
    - "6333:6333"
    - "6334:6334"
  volumes:
    - qdrant_data:/qdrant/storage
```

## 🔧 How It Works

### 1. Document Chunking

When a documentModel is uploaded:

```java
// Split documentModel into chunks of ~500 tokens
TextSplitter textSplitter = new TokenTextSplitter(
    500,  // chunk size
    100,  // chunk overlap
    5,    // min chunk size
    1000, // max chunk size
    true  // preserve structure
);

List<Document> chunks = textSplitter.apply(documentModels);
```

**Why chunking?**

- AI models have token limits
- Smaller chunks = more precise retrieval
- Overlap ensures context continuity

### 2. Embedding Generation

Each chunk is converted to a vector:

```java
// Generate embedding using Mistral
List<Double> embedding = embeddingModel.embed(chunkText);
```

**Embedding dimensions:** ~4096 for Mistral

**What's an embedding?**

- A list of numbers representing text meaning
- Similar texts have similar vectors
- Used for semantic comparison

### 3. Vector Storage

Embeddings are stored with metadata:

```json
{
  "id": "chunk-123",
  "vector": [0.123, -0.456, 0.789, ...],
  "metadata": {
    "documentId": "doc-456",
    "chunkIndex": 0,
    "totalChunks": 5,
    "filename": "report.pdf"
  }
}
```

### 4. Semantic Search

When a user asks a question:

```java
// 1. Convert question to vector
List<Double> queryEmbedding = generateEmbedding(question);

// 2. Find similar vectors using cosine similarity
double similarity = cosineSimilarity(queryEmbedding, documentEmbedding);

// 3. Return top-K most similar chunks
List<Document> relevant = findTopK(queryEmbedding, 5);
```

## 🚀 Implementation Details

### VectorStoreService

The `VectorStoreService` provides key functionality:

**Chunking:**

```java
List<Document> chunks = vectorStoreService.splitDocument(
    content,
    documentId,
    metadata
);
```

**Embedding:**

```java
List<Double> embedding = vectorStoreService.generateEmbedding(text);
```

**Similarity Search:**

```java
List<Document> relevant = vectorStoreService.findRelevantChunks(
    query,
    documentModels,
    topK
);
```

### Caching with Redis

Embeddings are cached to improve performance:

```java
@Cacheable(value = "embeddings", key = "#text.hashCode()")
public List<Double> generateEmbedding(String text) {
    return embeddingModel.embed(text);
}
```

**Benefits:**

- Faster response times
- Reduced AI model calls
- Lower resource usage

## 📐 Similarity Metrics

### Cosine Similarity

```java
public double cosineSimilarity(List<Double> v1, List<Double> v2) {
    double dotProduct = 0.0;
    double norm1 = 0.0;
    double norm2 = 0.0;
    
    for (int i = 0; i < v1.size(); i++) {
        dotProduct += v1.get(i) * v2.get(i);
        norm1 += Math.pow(v1.get(i), 2);
        norm2 += Math.pow(v2.get(i), 2);
    }
    
    return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
}
```

**Range:** -1 to 1

- 1 = identical vectors
- 0 = orthogonal (unrelated)
- -1 = opposite vectors

## 🎨 Use Cases

### 1. Document Q&A

```
User: "What are the main findings?"
  ↓
Generate embedding for question
  ↓
Find similar chunks in vector DB
  ↓
Retrieve top 5 relevant chunks
  ↓
Send to AI with chunks as context
  ↓
Return intelligent answer
```

### 2. Similar Document Search

Find documentModels similar to a given documentModel:

```java
List<Double> docEmbedding = generateEmbedding(documentContent);
List<Document> similar = findSimilarDocuments(docEmbedding, 10);
```

### 3. Question Suggestions

Suggest questions based on documentModel content:

```java
// Find chunks with high information density
List<Document> keyChunks = findImportantChunks(documentId);

// Generate questions from key chunks
List<String> suggestions = generateQuestions(keyChunks);
```

## 🔍 Querying Vector Databases

### Chroma

**REST API:**

```bash
# Query collection
curl -X POST http://localhost:8000/api/v1/collections/rag-documentModels/query \
  -H "Content-Type: application/json" \
  -d '{
    "query_embeddings": [[0.1, 0.2, 0.3, ...]],
    "n_results": 5
  }'
```

**Python Client:**

```python
import chromadb

client = chromadb.HttpClient(host="localhost", port=8000)
collection = client.get_collection("rag-documentModels")

results = collection.query(
    query_embeddings=[[0.1, 0.2, 0.3, ...]],
    n_results=5
)
```

### Qdrant

**REST API:**

```bash
# Search for similar vectors
curl -X POST http://localhost:6333/collections/rag-documentModels/points/search \
  -H "Content-Type: application/json" \
  -d '{
    "vector": [0.1, 0.2, 0.3, ...],
    "limit": 5
  }'
```

**Dashboard:**
Open http://localhost:6333/dashboard

## 🎯 Best Practices

### Chunking Strategy

**Good:**

- 300-700 token chunks
- 50-150 token overlap
- Preserve sentence boundaries

**Avoid:**

- Too small chunks (< 100 tokens) - lose context
- Too large chunks (> 1000 tokens) - less precise
- No overlap - miss context between chunks

### Embedding Quality

**Improve quality:**

- Clean text before embedding
- Remove excessive whitespace
- Handle special characters
- Normalize text encoding

### Search Optimization

**Top-K selection:**

- Use 3-5 chunks for short answers
- Use 5-10 chunks for detailed answers
- Too many chunks confuse the AI

**Filtering:**

- Filter by documentModel ID
- Filter by date
- Filter by documentModel type

## 📊 Performance Optimization

### 1. Batch Embedding

```java
// Instead of:
for (String text : texts) {
    embeddings.add(generateEmbedding(text));
}

// Use:
List<List<Double>> embeddings = generateEmbeddings(texts);
```

### 2. Cache Embeddings

```java
@Cacheable(value = "embeddings", key = "#text.hashCode()")
public List<Double> generateEmbedding(String text) {
    // Only computed once, then cached
    return embeddingModel.embed(text);
}
```

### 3. Async Processing

```java
@Async
public CompletableFuture<List<Double>> generateEmbeddingAsync(String text) {
    return CompletableFuture.completedFuture(
        embeddingModel.embed(text)
    );
}
```

## 🔐 Security Considerations

### Production Settings

**Chroma:**

```yaml
environment:
  - CHROMA_SERVER_AUTH_CREDENTIALS=secure-token-here
  - CHROMA_SERVER_AUTH_PROVIDER=chromadb.auth.token.TokenAuthServerProvider
```

**Qdrant:**

```yaml
environment:
  - QDRANT__SERVICE__API_KEY=your-secure-api-key
```

**Redis:**

```yaml
command: redis-server --requirepass your-password
```

## 🐛 Troubleshooting

### Embeddings Taking Too Long

**Solutions:**

- Enable Redis caching
- Batch embed multiple texts
- Use GPU for Ollama
- Reduce chunk size

### Out of Memory

**Solutions:**

- Reduce chunk size
- Process documentModels in batches
- Increase Docker memory
- Use disk-based vector storage

### Poor Search Results

**Solutions:**

- Adjust chunk size and overlap
- Clean documentModel text
- Increase top-K value
- Check embedding model quality

### Vector DB Connection Issues

**Check:**

```bash
# Chroma
curl http://localhost:8000/api/v1/heartbeat

# Qdrant
curl http://localhost:6333/
```

## 📈 Monitoring

### Chroma Metrics

```bash
# Collection stats
curl http://localhost:8000/api/v1/collections/rag-documentModels
```

### Qdrant Metrics

```bash
# Collection info
curl http://localhost:6333/collections/rag-documentModels
```

### Cache Hit Rate

```bash
# Redis stats
docker exec -it rag-redis redis-cli INFO stats | grep hits
```

## 🔄 Migration Between Vector DBs

### Chroma to Qdrant

1. Export from Chroma
2. Transform format
3. Import to Qdrant

```python
# Export from Chroma
chroma_collection = chroma_client.get_collection("rag-documentModels")
data = chroma_collection.get(include=["embeddings", "metadatas"])

# Import to Qdrant
qdrant_client.upsert(
    collection_name="rag-documentModels",
    points=[
        {
            "id": data["ids"][i],
            "vector": data["embeddings"][i],
            "payload": data["metadatas"][i]
        }
        for i in range(len(data["ids"]))
    ]
)
```

## 📚 Additional Resources

- [Chroma Documentation](https://docs.trychroma.com/)
- [Qdrant Documentation](https://qdrant.tech/documentation/)
- [Vector Embeddings Explained](https://www.pinecone.io/learn/vector-embeddings/)
- [RAG Best Practices](https://www.anthropic.com/index/contextual-retrieval)

## 💡 Future Enhancements

- [ ] Hybrid search (vector + keyword)
- [ ] Multi-modal embeddings (text + images)
- [ ] Fine-tuned embedding models
- [ ] Advanced filtering and metadata search
- [ ] Vector compression for storage efficiency

---

**Vector databases are the backbone of modern RAG systems!** 🚀
