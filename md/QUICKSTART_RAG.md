# Quick Start: RAG Chatbot

Get your RAG (Retrieval Augmented Generation) chatbot running in 5 minutes!

## What This Does

This chatbot **learns from your documents** and answers questions about them using AI. When you upload a document:

1. ✅ The system breaks it into chunks
2. ✅ Creates embeddings (vector representations)
3. ✅ Stores them in Elasticsearch
4. ✅ **Updates the LLM's knowledge base**

When you ask questions:

1. 🔍 Searches your documents for relevant information
2. 🤖 Sends relevant context to Mistral LLM
3. 💬 Returns accurate answers based on YOUR content

## Prerequisites

- Docker and Docker Compose
- Java 17 or higher
- Node.js 18 or higher
- 8GB RAM minimum

## Setup (5 Steps)

### Step 1: Start Infrastructure

```bash
# Start Elasticsearch, Kafka, Ollama, etc.
./start.sh

# Wait 2-3 minutes for services to initialize
```

### Step 2: Install Mistral AI Model

```bash
# Download the Mistral model (one-time setup, ~4GB)
docker exec -it rag-ollama ollama pull mistral

# Verify installation
docker exec -it rag-ollama ollama list
```

### Step 3: Start Backend

```bash
# Build and run Spring Boot application
./mvnw clean install
./mvnw spring-boot:run

# Backend will start on http://localhost:8080
```

### Step 4: Start Frontend

```bash
cd frontend
npm install
npm run dev

# Frontend will start on http://localhost:3000
```

### Step 5: Open Browser

```
Navigate to: http://localhost:3000
```

## Using the Chatbot

### 1. Upload a Document

Click the **"Upload Document"** button and select:

- PDF file (e.g., research paper, manual)
- DOCX file (e.g., report, article)
- TXT file (e.g., notes, transcript)

**What Happens:**

```
Upload → Processing (10-30 sec) → ✅ Document indexed & summarized
```

The document is now part of the LLM's knowledge base!

### 2. Ask Questions

Select your document and type questions like:

- "What is this document about?"
- "Summarize the main points"
- "What does it say about [topic]?"
- "Compare X and Y from the document"

**Example Conversation:**

```
You: What is this document about?
AI: This document discusses artificial intelligence and machine learning...

You: What are the main types of AI mentioned?
AI: Based on the document, there are three main types: narrow AI, general AI...

You: Tell me more about narrow AI
AI: Narrow AI, also called weak AI, is designed for specific tasks...
```

### 3. View Document Summary

Each uploaded document automatically gets an AI-generated summary displayed in the document list.

## Verifying It Works

### Check Services Status

```bash
# Elasticsearch
curl http://localhost:9200/_cluster/health

# Ollama
curl http://localhost:11434/api/tags

# Backend
curl http://localhost:8080/actuator/health

# Kafka
docker ps | grep kafka
```

### Test with Sample Document

Upload the included sample document:

```bash
# The sample-documents/artificial-intelligence.txt file is ready to test
```

Ask it: "What is artificial intelligence?"

Expected behavior: You'll get an answer based on the content of YOUR document, not generic AI training data.

## Architecture Overview (Simple)

```
┌─────────────────┐
│   Your Browser  │
│   (React UI)    │
└────────┬────────┘
         │
         ▼
┌─────────────────┐      ┌─────────────────┐
│  Spring Boot    │◄────►│  Elasticsearch  │
│  (Backend)      │      │  (Vector Store) │
└────────┬────────┘      └─────────────────┘
         │
         ▼
┌─────────────────┐
│  Ollama/Mistral │
│  (AI Model)     │
└─────────────────┘
```

## How RAG Works (Simple Explanation)

### Traditional Chatbot:

```
Question → LLM → Answer (based only on training data)
```

### RAG Chatbot (This System):

```
Question → Search Documents → Find Relevant Info → LLM → Answer (based on YOUR documents)
```

**Example:**

**Without RAG:**

```
Q: "What's in our Q4 report?"
A: "I don't have access to your Q4 report."
```

**With RAG:**

```
Q: "What's in our Q4 report?"
A: "According to your Q4 report, revenue increased 25%..." ✅
```

## Common Issues & Solutions

### Issue: "Ollama connection refused"

```bash
# Solution: Restart Ollama
docker restart rag-ollama

# Wait 30 seconds, then verify
curl http://localhost:11434/api/tags
```

### Issue: "Document upload fails"

```bash
# Solution: Check Elasticsearch
curl http://localhost:9200/_cluster/health

# Restart if needed
docker restart rag-elasticsearch
```

### Issue: "Chat responses are slow"

```bash
# Solution: Enable GPU for Ollama (if available)
# Edit docker-compose.yml and uncomment GPU section

# Or reduce chunk retrieval
# In ChatService.java, change:
# searchRelevantContextInDocument(question, documentId, 5)
# to:
# searchRelevantContextInDocument(question, documentId, 3)
```

### Issue: "Answers don't match document"

**Cause:** Document might not be indexed properly

**Solution:**

```bash
# Check if index exists
curl http://localhost:9200/rag-documents/_search?size=1

# Re-upload the document
```

## Stop Services

```bash
# Stop everything
./stop.sh

# Or manually
docker-compose down
```

## Next Steps

Want to learn more? Check out:

- `RAG_CHATBOT_GUIDE.md` - Comprehensive technical guide
- `ARCHITECTURE.md` - System architecture details
- `RUNBOOK.md` - Operations and troubleshooting

## Key Features

✅ **Automatic Learning** - Upload documents, system learns automatically  
✅ **Semantic Search** - Understands meaning, not just keywords  
✅ **Conversation History** - Maintains context across messages  
✅ **Real-time Processing** - Async document processing with Kafka  
✅ **Production Ready** - Monitoring, logging, error handling

## Example Use Cases

1. **Customer Support:** Upload manuals, answer customer questions
2. **Research:** Upload papers, query findings
3. **Internal Wiki:** Upload company docs, instant Q&A
4. **Learning:** Upload textbooks, study assistant
5. **Legal:** Upload contracts, find clauses

## Performance

- **Document Upload:** 2-10 seconds (depending on size)
- **Question Answering:** 1-3 seconds
- **Supported File Size:** Up to 10MB
- **Concurrent Users:** Scales horizontally

## Security Notes

⚠️ This is a development setup. For production:

- Enable Elasticsearch security
- Add authentication to endpoints
- Use HTTPS
- Implement rate limiting
- Add input validation

## Need Help?

1. Check `RUNBOOK.md` for detailed troubleshooting
2. Review logs: `docker-compose logs -f`
3. Check backend logs for errors
4. Verify all services are running: `docker ps`

---

**You're all set! Upload a document and start chatting! 🚀**
