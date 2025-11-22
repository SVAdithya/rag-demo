# Docker Compose Services Guide

This documentModel explains all the services included in the `docker-compose.yml` and how to use them.

## 📋 Services Overview

### Core Services (Required)

| Service | Port | Purpose | Status Endpoint |
|---------|------|---------|----------------|
| **Elasticsearch** | 9200 | Document storage & search | http://localhost:9200/_cluster/health |
| **Kafka** | 9092 | Message queue | - |
| **Zookeeper** | 2181 | Kafka coordination | - |
| **Ollama** | 11434 | AI model hosting (Mistral) | http://localhost:11434/api/tags |

### Vector Databases (Choose One)

| Service | Port | Purpose | Dashboard |
|---------|------|---------|-----------|
| **Chroma** | 8000 | Vector embeddings storage | http://localhost:8000/api/v1 |
| **Qdrant** | 6333, 6334 | Alternative vector DB | http://localhost:6333/dashboard |

### Supporting Services

| Service | Port | Purpose | Access |
|---------|------|---------|--------|
| **Redis** | 6379 | Caching layer | redis-cli |
| **Kafka UI** | 8090 | Kafka monitoring | http://localhost:8090 |
| **ES Head** | 9100 | Elasticsearch monitoring | http://localhost:9100 |

## 🚀 Quick Start

### Start All Services

```bash
docker-compose up -d
```

### Start Specific Services

```bash
# Only core services
docker-compose up -d elasticsearch kafka zookeeper ollama

# With Chroma vector database
docker-compose up -d elasticsearch kafka zookeeper ollama chroma redis

# With Qdrant vector database
docker-compose up -d elasticsearch kafka zookeeper ollama qdrant redis
```

### Stop All Services

```bash
docker-compose down
```

### Stop and Remove Volumes (Clean Slate)

```bash
docker-compose down -v
```

## 📊 Service Details

### 1. Elasticsearch

**Purpose:** Stores documentModels, chat messages, and provides full-text search.

**Configuration:**

- Single-node cluster (development)
- No security (development)
- 512MB heap size

**Health Check:**

```bash
curl http://localhost:9200/_cluster/health
```

**View Indices:**

```bash
curl http://localhost:9200/_cat/indices?v
```

**Query Documents:**

```bash
curl http://localhost:9200/documentModels/_search?pretty
```

**Access via UI:**
Open http://localhost:9100 (Elasticsearch Head)

### 2. Apache Kafka

**Purpose:** Asynchronous message processing for documentModel summarization.

**Configuration:**

- 3 partitions per topic
- Single broker (development)
- Auto-create topics enabled
- 7-day log retention

**Topics Created:**

- `documentModel-upload`
- `documentModel-processing`
- `chat-request`

**Monitor via Kafka UI:**
Open http://localhost:8090

**View Topics:**

```bash
docker exec -it rag-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

**View Messages:**

```bash
docker exec -it rag-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic documentModel-processing \
  --from-beginning
```

### 3. Ollama + Mistral

**Purpose:** Local AI model for summarization and Q&A.

**Configuration:**

- Mistral model (7B parameters)
- GPU support enabled (if available)
- Persistent model storage

**Install Mistral Model:**

```bash
docker exec -it rag-ollama ollama pull mistral
```

**List Installed Models:**

```bash
docker exec -it rag-ollama ollama list
```

**Test AI:**

```bash
docker exec -it rag-ollama ollama run mistral "Hello, how are you?"
```

**GPU Support:**

- Requires NVIDIA Docker runtime
- Comment out GPU config if not available
- Falls back to CPU (slower)

### 4. Chroma Vector Database

**Purpose:** Store vector embeddings for semantic search and RAG.

**Configuration:**

- Persistent storage
- Token authentication (test-token)
- REST API access

**Health Check:**

```bash
curl http://localhost:8000/api/v1/heartbeat
```

**List Collections:**

```bash
curl http://localhost:8000/api/v1/collections
```

**API Documentation:**
Open http://localhost:8000/docs

**Use Cases:**

- Semantic documentModel search
- Similar question matching
- Context retrieval for Q&A

### 5. Qdrant Vector Database

**Purpose:** Alternative high-performance vector database.

**Configuration:**

- gRPC port: 6334
- REST API port: 6333
- Persistent storage

**Health Check:**

```bash
curl http://localhost:6333/
```

**Dashboard:**
Open http://localhost:6333/dashboard

**List Collections:**

```bash
curl http://localhost:6333/collections
```

**When to Use:**

- Need better performance
- Large-scale vector operations
- Production deployments

### 6. Redis

**Purpose:** Caching layer for embeddings and API responses.

**Configuration:**

- Persistent storage (AOF)
- Default database
- No authentication (development)

**Connect:**

```bash
docker exec -it rag-redis redis-cli
```

**Monitor Cache:**

```bash
docker exec -it rag-redis redis-cli INFO stats
```

**View Keys:**

```bash
docker exec -it rag-redis redis-cli KEYS *
```

**Clear Cache:**

```bash
docker exec -it rag-redis redis-cli FLUSHALL
```

### 7. Kafka UI

**Purpose:** Web interface for Kafka monitoring.

**Features:**

- View topics and messages
- Consumer group monitoring
- Broker statistics
- Message publishing

**Access:**
Open http://localhost:8090

### 8. Elasticsearch Head

**Purpose:** Web interface for Elasticsearch.

**Features:**

- Index browser
- Query interface
- Cluster overview
- Document CRUD

**Access:**
Open http://localhost:9100

## 🔧 Configuration

### Environment Variables

You can override defaults in `docker-compose.yml`:

```yaml
services:
  elasticsearch:
    environment:
      - "ES_JAVA_OPTS=-Xms1g -Xmx1g"  # Increase heap
```

### Resource Limits

Add resource constraints:

```yaml
services:
  elasticsearch:
    deploy:
      resources:
        limits:
          memory: 2G
        reservations:
          memory: 1G
```

## 🐛 Troubleshooting

### Elasticsearch Won't Start

**Issue:** Insufficient memory

**Solution:**

```bash
# Increase Docker Desktop memory to 4GB+
# Or reduce ES heap in docker-compose.yml:
- "ES_JAVA_OPTS=-Xms256m -Xmx256m"
```

**Issue:** Port already in use

**Solution:**

```bash
# Find process using port 9200
lsof -i :9200
# Kill process or change port in docker-compose.yml
```

### Kafka Connection Errors

**Issue:** Can't connect to Kafka

**Solution:**

```bash
# Check Zookeeper is running first
docker-compose ps zookeeper

# View Kafka logs
docker-compose logs kafka
```

### Ollama Model Not Found

**Issue:** Model not downloaded

**Solution:**

```bash
# Pull the model
docker exec -it rag-ollama ollama pull mistral

# Verify installation
docker exec -it rag-ollama ollama list
```

### Out of Disk Space

**Issue:** Docker volumes consuming too much space

**Solution:**

```bash
# Check disk usage
docker system df

# Clean up unused volumes
docker volume prune

# Remove specific volume
docker volume rm rag-demo_es_data
```

### Container Keeps Restarting

**Check Logs:**

```bash
docker-compose logs [service-name]
```

**Common Causes:**

- Insufficient memory
- Port conflicts
- Configuration errors
- Missing dependencies

## 📈 Monitoring

### View All Container Stats

```bash
docker stats
```

### View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f elasticsearch

# Last 100 lines
docker-compose logs --tail=100 kafka
```

### Health Checks

```bash
# Check all services
docker-compose ps

# View health status
docker inspect rag-elasticsearch | grep -A 10 Health
```

## 🔐 Production Considerations

### Security

**Enable Elasticsearch Security:**

```yaml
environment:
  - xpack.security.enabled=true
  - ELASTIC_PASSWORD=your-secure-password
```

**Enable Kafka Security:**

```yaml
environment:
  - KAFKA_SECURITY_PROTOCOL=SASL_SSL
  - KAFKA_SASL_MECHANISM=PLAIN
```

**Add Redis Password:**

```yaml
command: redis-server --requirepass your-password
```

### Persistence

All data is persisted in Docker volumes:

- `es_data`: Elasticsearch indices
- `ollama_data`: AI models
- `chroma_data`: Vector embeddings
- `qdrant_data`: Vector embeddings
- `redis_data`: Cache data
- `kafka_data`: Message logs

### Backup Strategy

```bash
# Backup Elasticsearch
docker exec rag-elasticsearch curl -X PUT \
  "localhost:9200/_snapshot/my_backup/snapshot_1?wait_for_completion=true"

# Backup volumes
docker run --rm -v rag-demo_es_data:/data -v $(pwd):/backup \
  alpine tar czf /backup/es_backup.tar.gz /data
```

## 🎯 Best Practices

### Development

1. **Use all services** for full feature set
2. **Monitor logs** during development
3. **Clear cache** when testing changes
4. **Regular backups** of important data

### Production

1. **Use managed services** (AWS, GCP, Azure)
2. **Enable security** on all services
3. **Set resource limits** appropriately
4. **Use external volumes** for data
5. **Implement monitoring** (Prometheus, Grafana)
6. **Set up alerts** for failures

## 🔗 Service Dependencies

```
┌─────────────┐
│  Zookeeper  │
└──────┬──────┘
       │
       ▼
┌─────────────┐     ┌──────────────┐
│    Kafka    │────▶│   Kafka UI   │
└─────────────┘     └──────────────┘

┌─────────────┐     ┌──────────────┐
│Elasticsearch│────▶│   ES Head    │
└─────────────┘     └──────────────┘

┌─────────────┐
│   Ollama    │ (Independent)
└─────────────┘

┌─────────────┐
│   Chroma    │ (Independent)
└─────────────┘

┌─────────────┐
│   Qdrant    │ (Independent)
└─────────────┘

┌─────────────┐
│    Redis    │ (Independent)
└─────────────┘
```

## 📚 Additional Resources

- [Elasticsearch Documentation](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [Kafka Documentation](https://kafka.apache.org/documentation/)
- [Ollama Documentation](https://ollama.ai/docs)
- [Chroma Documentation](https://docs.trychroma.com/)
- [Qdrant Documentation](https://qdrant.tech/documentation/)
- [Redis Documentation](https://redis.io/documentation)

## 🆘 Getting Help

If you encounter issues:

1. Check service logs: `docker-compose logs [service]`
2. Verify health: `docker-compose ps`
3. Review this guide
4. Check official documentation
5. Open an issue on GitHub

---

**Happy Developing! 🚀**
