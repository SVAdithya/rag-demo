# Troubleshooting Q&A Not Working

This guide helps diagnose and fix Q&A issues in the RAG chatbot.

## Quick Diagnosis

Run this command to check all services:

```bash
./check-services.sh
```

## Common Issues & Solutions

### Issue 1: Frontend Not Accessible

**Symptoms:**

- Cannot access `localhost:3000`
- Port 3000 shows as in use

**Solution:**

```bash
# Step 1: Check what's using port 3000
lsof -i :3000

# Step 2: Kill the process if needed
kill -9 <PID>

# Step 3: Start frontend
cd frontend
npm run dev
```

**Alternative:** Frontend might be running on port 3001 if 3000 is busy. Check the terminal output.

### Issue 2: Ollama/Mistral Not Responding

**Symptoms:**

- Q&A takes forever
- Errors about "connection refused" to Ollama
- No AI responses

**Diagnosis:**

```bash
# Check if Ollama is running
curl http://localhost:11434/api/tags

# Check if Mistral model is installed
docker exec -it rag-ollama ollama list
```

**Solution:**

```bash
# Restart Ollama
docker restart rag-ollama

# Wait 30 seconds for startup
sleep 30

# Pull Mistral model if missing
docker exec -it rag-ollama ollama pull mistral

# Verify
docker exec -it rag-ollama ollama list
```

### Issue 3: Document Not Indexed in Vector Store

**Symptoms:**

- Q&A returns generic answers
- Answers don't match document content
- "No relevant context found" in logs

**Diagnosis:**

```bash
# Check Elasticsearch indices
curl http://localhost:9200/_cat/indices | grep rag

# Check document count
curl http://localhost:9200/rag-documents/_count

# Check if vector embeddings exist
curl http://localhost:9200/rag-documents/_search?size=1 | jq
```

**Solution:**

1. **Re-upload the document:**
    - Delete and re-upload the document through the UI
    - Wait 10-30 seconds for processing

2. **Check Kafka processing:**
   ```bash
   # View Kafka logs
   docker logs rag-kafka --tail 50
   
   # Check Kafka topics
   docker exec -it rag-kafka kafka-topics --list --bootstrap-server localhost:9092
   ```

3. **Reset Elasticsearch (CAUTION: Deletes all data):**
   ```bash
   ./reset-elasticsearch.sh
   ```

### Issue 4: Backend Not Running

**Symptoms:**

- API calls fail with connection errors
- Port 8080 not responding

**Diagnosis:**

```bash
# Check if backend is running
lsof -i :8080

# Check backend health
curl http://localhost:8080/actuator/health
```

**Solution:**

```bash
# Stop any existing backend
pkill -f "spring-boot"

# Start backend
./mvnw spring-boot:run
```

### Issue 5: DocumentId Required Error

**Symptoms:**

- Error: "Document ID is required"
- Q&A button doesn't work
- Chat interface shows error

**Cause:** You must select a document before asking questions.

**Solution:**

1. Go to the Documents list
2. Click on a document to select it
3. The chat interface should load
4. Now try asking questions

### Issue 6: Elasticsearch Connection Failed

**Symptoms:**

- Backend logs show Elasticsearch errors
- Documents don't save
- Search doesn't work

**Diagnosis:**

```bash
# Check Elasticsearch health
curl http://localhost:9200/_cluster/health

# Check if container is running
docker ps | grep elasticsearch
```

**Solution:**

```bash
# Restart Elasticsearch
docker restart rag-elasticsearch

# Wait for green status
watch -n 2 "curl -s http://localhost:9200/_cluster/health | jq '.status'"

# Press Ctrl+C when status is "green" or "yellow"
```

### Issue 7: Slow Q&A Responses

**Symptoms:**

- Responses take 30+ seconds
- UI times out

**Solutions:**

1. **Reduce context retrieval:**
   Edit `src/main/java/com/ai/rag_demo/service/ChatService.java`:
   ```java
   // Change from 5 to 3
   List<Document> relevantDocs = vectorStoreService
       .searchRelevantContextInDocument(question, documentId, 3);
   ```

2. **Enable GPU for Ollama (if available):**
   Edit `docker-compose.yml` and uncomment GPU sections

3. **Check system resources:**
   ```bash
   # Check Docker memory
   docker stats
   
   # Increase Docker memory to 8GB if possible
   ```

## Testing Q&A Manually

### Test 1: Direct API Test

```bash
# Get a document ID
DOCUMENT_ID=$(curl -s http://localhost:8080/api/documents | jq -r '.[0].id')

echo "Testing with document: $DOCUMENT_ID"

# Test Q&A endpoint
curl -X POST http://localhost:8080/api/chat/ask \
  -H "Content-Type: application/json" \
  -d "{
    \"question\": \"What is this document about?\",
    \"sessionId\": \"test-session-$(date +%s)\",
    \"documentId\": \"$DOCUMENT_ID\"
  }" | jq
```

**Expected Response:**

```json
{
  "id": "msg-uuid",
  "question": "What is this document about?",
  "answer": "Based on the document...",
  "timestamp": "2025-11-22T10:00:00",
  "sessionId": "test-session-123"
}
```

### Test 2: Check Vector Store

```bash
# Search for similar content
curl -X POST http://localhost:9200/rag-documents/_search \
  -H "Content-Type: application/json" \
  -d '{
    "size": 5,
    "query": {
      "match_all": {}
    }
  }' | jq '.hits.hits[0]'
```

### Test 3: Test Ollama Directly

```bash
# Test Mistral model
curl -X POST http://localhost:11434/api/generate \
  -H "Content-Type: application/json" \
  -d '{
    "model": "mistral",
    "prompt": "What is artificial intelligence?",
    "stream": false
  }' | jq '.response'
```

## Complete System Check

Run all checks in order:

```bash
# 1. Check Docker containers
echo "=== Checking Docker Containers ==="
docker ps --format "table {{.Names}}\t{{.Status}}"

# 2. Check Elasticsearch
echo -e "\n=== Checking Elasticsearch ==="
curl -s http://localhost:9200/_cluster/health | jq '.status'
curl -s http://localhost:9200/_cat/indices | grep rag

# 3. Check Ollama
echo -e "\n=== Checking Ollama ==="
curl -s http://localhost:11434/api/tags | jq '.models[].name'

# 4. Check Backend
echo -e "\n=== Checking Backend ==="
curl -s http://localhost:8080/actuator/health | jq '.status'

# 5. Check if documents exist
echo -e "\n=== Checking Documents ==="
curl -s http://localhost:8080/api/documents | jq 'length'

# 6. Check Kafka
echo -e "\n=== Checking Kafka ==="
docker exec -it rag-kafka kafka-topics --list --bootstrap-server localhost:9092 2>/dev/null || echo "Kafka check failed"

echo -e "\n=== All checks complete! ==="
```

## Enable Debug Logging

To see detailed logs for troubleshooting:

1. Edit `src/main/resources/application.properties`:
   ```properties
   logging.level.com.ai.rag_demo=DEBUG
   logging.level.org.springframework.ai=DEBUG
   ```

2. Restart backend:
   ```bash
   ./mvnw spring-boot:run
   ```

3. Watch logs in real-time:
   ```bash
   tail -f backend.log
   ```

## Browser Console Debugging

1. Open browser DevTools (F12)
2. Go to Console tab
3. Try asking a question
4. Look for errors in:
    - Network tab (failed API calls)
    - Console tab (JavaScript errors)

Common errors and solutions:

- **CORS error:** Check `WebConfig.java` CORS settings
- **404 error:** Backend not running or wrong API path
- **Network error:** Backend not accessible
- **Timeout:** Ollama/Mistral taking too long

## Getting Help

If Q&A still doesn't work after trying these solutions:

1. **Collect diagnostic info:**
   ```bash
   # Save all logs
   docker logs rag-ollama > ollama.log 2>&1
   docker logs rag-elasticsearch > elasticsearch.log 2>&1
   docker logs rag-kafka > kafka.log 2>&1
   
   # Save backend logs
   tail -100 backend.log > backend-errors.log
   ```

2. **Check the specific error message** in:
    - Browser console (F12)
    - Backend logs
    - Docker logs

3. **Verify the complete workflow:**
    - [ ] All Docker containers running
    - [ ] Ollama has Mistral model
    - [ ] Elasticsearch is healthy
    - [ ] Backend is running on 8080
    - [ ] Frontend is running (3000 or 3001)
    - [ ] Document is uploaded and indexed
    - [ ] Document is selected in UI
    - [ ] Question is typed and sent

## Quick Reset (Nuclear Option)

If nothing works, reset everything:

```bash
# Stop everything
./stop.sh
docker-compose down -v

# Start fresh
docker-compose up -d
sleep 60  # Wait for services

# Pull Mistral
docker exec -it rag-ollama ollama pull mistral

# Start backend
./mvnw spring-boot:run &

# Start frontend
cd frontend && npm run dev
```

Then re-upload your documents and try again.
