# RAG Chatbot - Operations Runbook

This runbook provides step-by-step procedures for deploying, operating, monitoring, and troubleshooting the RAG Chatbot
application.

## 📋 Table of Contents

1. [Prerequisites](#prerequisites)
2. [Initial Deployment](#initial-deployment)
3. [Daily Operations](#daily-operations)
4. [Monitoring & Health Checks](#monitoring--health-checks)
5. [Troubleshooting Guide](#troubleshooting-guide)
6. [Maintenance Procedures](#maintenance-procedures)
7. [Backup & Recovery](#backup--recovery)
8. [Emergency Procedures](#emergency-procedures)
9. [Performance Tuning](#performance-tuning)
10. [Security Hardening](#security-hardening)

---

## Prerequisites

### System Requirements

#### Hardware

- **CPU**: 4+ cores recommended
- **RAM**: 8GB minimum, 16GB recommended
- **Disk**: 50GB free space
- **GPU**: Optional (for faster AI processing)

#### Software

- Docker Desktop 4.x+ or Docker Engine 20.x+
- Docker Compose 2.x+
- Java 17 or higher
- Maven 3.6+
- Node.js 18+
- Git

#### Network

- Ports available: 3000, 6333, 6334, 6379, 8000, 8080, 8090, 9092, 9100, 9200, 11434
- Internet access for downloading dependencies

### Verification Commands

```bash
# Check Docker
docker --version
docker-compose --version

# Check Java
java -version

# Check Maven
mvn -version

# Check Node.js
node --version
npm --version

# Check available ports
lsof -i :8080
lsof -i :9200
lsof -i :9092
```

---

## Initial Deployment

### Step 1: Clone and Setup

```bash
# Clone repository
git clone <repository-url>
cd rag-demo

# Make scripts executable
chmod +x start.sh stop.sh
```

### Step 2: Configuration Review

```bash
# Review application configuration
cat src/main/resources/application.properties

# Review Docker Compose configuration
cat docker-compose.yml
```

**Key Configuration Parameters:**

| Parameter | Default | Description |
|-----------|---------|-------------|
| `server.port` | 8080 | Backend API port |
| `spring.ai.ollama.base-url` | http://localhost:11434 | Ollama service URL |
| `spring.elasticsearch.uris` | http://localhost:9200 | Elasticsearch URL |
| `spring.kafka.bootstrap-servers` | localhost:9092 | Kafka broker URL |
| `spring.data.redis.host` | localhost | Redis host |

### Step 3: Infrastructure Deployment

```bash
# Start all services
docker-compose up -d

# Verify services are starting
docker-compose ps

# Expected output: All services should show "Up" or "Up (healthy)"
```

### Step 4: Service Health Checks

```bash
# Wait for services to be ready (30-60 seconds)
sleep 60

# Check Elasticsearch
curl -s http://localhost:9200/_cluster/health | jq '.status'
# Expected: "green" or "yellow"

# Check Kafka
docker exec -it rag-kafka kafka-broker-api-versions --bootstrap-server localhost:9092 | head -1
# Expected: No errors

# Check Ollama
curl -s http://localhost:11434/api/tags | jq '.models'
# Expected: Empty list (no models yet)

# Check Redis
docker exec -it rag-redis redis-cli ping
# Expected: PONG

# Check Chroma
curl -s http://localhost:8000/api/v1/heartbeat
# Expected: {"nanosecond heartbeat": <number>}

# Check Qdrant
curl -s http://localhost:6333/ | jq '.title'
# Expected: "qdrant - vector search engine"
```

### Step 5: Install AI Model

```bash
# Pull Mistral model (this takes 5-10 minutes)
docker exec -it rag-ollama ollama pull mistral

# Verify model is installed
docker exec -it rag-ollama ollama list
# Expected: mistral should appear in the list

# Test model
docker exec -it rag-ollama ollama run mistral "Hello, respond with OK"
# Expected: AI response containing "OK"
```

### Step 6: Build Backend

```bash
# Clean and build
mvn clean install -DskipTests

# Expected output: BUILD SUCCESS
# Check for the JAR file
ls -lh target/rag-demo-*.jar
```

### Step 7: Deploy Backend

```bash
# Option A: Run in foreground (for testing)
mvn spring-boot:run

# Option B: Run in background (for production)
mvn spring-boot:run > backend.log 2>&1 &
echo $! > backend.pid

# Wait for startup (30-60 seconds)
sleep 30

# Verify backend is running
curl -s http://localhost:8080/api/documents | jq '.'
# Expected: Empty array []
```

### Step 8: Deploy Frontend

```bash
# Navigate to frontend
cd frontend

# Install dependencies
npm install

# Option A: Development mode
npm run dev

# Option B: Production build
npm run build
# Serve with a static file server

# Verify frontend
curl -s http://localhost:3000
# Expected: HTML content
```

### Step 9: Smoke Test

```bash
# Test document upload
curl -X POST http://localhost:8080/api/documents/upload-text \
  -H "Content-Type: application/json" \
  -d '{
    "content": "This is a test document.",
    "filename": "test.txt",
    "contentType": "text/plain"
  }'

# Expected: JSON response with document ID

# Open browser
open http://localhost:3000
# Verify UI loads and document appears
```

---

## Daily Operations

### Starting the Application

#### Automated Start

```bash
./start.sh
```

#### Manual Start

```bash
# 1. Start infrastructure
docker-compose up -d

# 2. Verify services
docker-compose ps

# 3. Start backend
mvn spring-boot:run > backend.log 2>&1 &
echo $! > backend.pid

# 4. Start frontend
cd frontend && npm run dev > ../frontend.log 2>&1 &
echo $! > ../frontend.pid
cd ..
```

### Stopping the Application

#### Automated Stop

```bash
./stop.sh
```

#### Manual Stop

```bash
# 1. Stop backend
kill $(cat backend.pid) 2>/dev/null
rm backend.pid

# 2. Stop frontend
kill $(cat frontend.pid) 2>/dev/null
rm frontend.pid

# 3. Stop infrastructure
docker-compose down

# Optional: Remove volumes
docker-compose down -v
```

### Restarting Services

#### Restart Single Service

```bash
# Restart Elasticsearch
docker-compose restart elasticsearch

# Restart Kafka
docker-compose restart kafka

# Restart Ollama
docker-compose restart ollama

# Restart Backend
kill $(cat backend.pid)
mvn spring-boot:run > backend.log 2>&1 &
echo $! > backend.pid
```

#### Restart All Services

```bash
./stop.sh
./start.sh
```

---

## Monitoring & Health Checks

### Service Health Endpoints

```bash
# Backend Health
curl http://localhost:8080/actuator/health

# Elasticsearch Health
curl http://localhost:9200/_cluster/health?pretty

# Kafka Topics
docker exec -it rag-kafka kafka-topics --bootstrap-server localhost:9092 --list

# Redis Info
docker exec -it rag-redis redis-cli info stats

# Ollama Models
curl http://localhost:11434/api/tags

# Chroma Collections
curl http://localhost:8000/api/v1/collections

# Qdrant Collections
curl http://localhost:6333/collections
```

### Web Dashboards

| Service | URL | Purpose |
|---------|-----|---------|
| **Frontend** | http://localhost:3000 | Main application |
| **Kafka UI** | http://localhost:8090 | Kafka monitoring |
| **ES Head** | http://localhost:9100 | Elasticsearch monitoring |
| **Chroma API** | http://localhost:8000/docs | Vector DB API docs |
| **Qdrant Dashboard** | http://localhost:6333/dashboard | Qdrant monitoring |

### Log Monitoring

```bash
# View all Docker logs
docker-compose logs -f

# View specific service logs
docker-compose logs -f elasticsearch
docker-compose logs -f kafka
docker-compose logs -f ollama

# View backend logs
tail -f backend.log

# View frontend logs
tail -f frontend.log

# Search for errors
grep -i error backend.log
grep -i exception backend.log
```

### Resource Usage

```bash
# Docker container stats
docker stats

# Disk usage
docker system df
du -sh /var/lib/docker

# Check specific volumes
docker volume ls
docker volume inspect rag-demo_es_data
```

### Key Metrics to Monitor

#### System Metrics

- CPU usage < 80%
- Memory usage < 85%
- Disk usage < 80%
- Network latency < 100ms

#### Application Metrics

- Backend response time < 2s
- Document processing time < 30s
- Q&A response time < 10s
- Error rate < 1%

#### Service Metrics

- Elasticsearch cluster status: green
- Kafka lag: < 100 messages
- Redis cache hit rate: > 80%
- Ollama response time: < 5s

---

## Troubleshooting Guide

### Issue: Services Won't Start

#### Symptoms

- `docker-compose up` fails
- Services show "Exited" status

#### Diagnosis

```bash
# Check Docker service
systemctl status docker

# Check logs
docker-compose logs <service-name>

# Check port conflicts
lsof -i :9200  # Elasticsearch
lsof -i :9092  # Kafka
lsof -i :6379  # Redis
```

#### Resolution

```bash
# Restart Docker
sudo systemctl restart docker

# Remove conflicting containers
docker rm -f $(docker ps -aq)

# Clean up volumes if needed
docker-compose down -v
docker-compose up -d

# If port conflict, change port in docker-compose.yml
```

### Issue: Backend Won't Start

#### Symptoms

- `mvn spring-boot:run` fails
- Connection refused errors

#### Diagnosis

```bash
# Check Java version
java -version

# Check if port 8080 is in use
lsof -i :8080

# Check logs
cat backend.log

# Check Elasticsearch connection
curl http://localhost:9200

# Check Kafka connection
docker-compose logs kafka
```

#### Resolution

```bash
# Kill process on port 8080
kill $(lsof -ti:8080)

# Rebuild application
mvn clean install -DskipTests

# Check Elasticsearch is ready
curl http://localhost:9200/_cluster/health

# Restart backend
mvn spring-boot:run
```

### Issue: AI Responses Not Working

#### Symptoms

- Questions don't return answers
- Summarization fails

#### Diagnosis

```bash
# Check if Mistral model is installed
docker exec -it rag-ollama ollama list

# Test Ollama directly
curl http://localhost:11434/api/tags

# Check backend logs for errors
grep -i "ollama" backend.log
grep -i "mistral" backend.log
```

#### Resolution

```bash
# Pull Mistral model
docker exec -it rag-ollama ollama pull mistral

# Restart Ollama
docker-compose restart ollama

# Test model
docker exec -it rag-ollama ollama run mistral "test"

# Restart backend
kill $(cat backend.pid)
mvn spring-boot:run > backend.log 2>&1 &
echo $! > backend.pid
```

### Issue: Documents Not Indexing

#### Symptoms

- Upload succeeds but documents don't appear
- Search returns no results

#### Diagnosis

```bash
# Check Elasticsearch indices
curl http://localhost:9200/_cat/indices?v

# Check Kafka topics
docker exec -it rag-kafka kafka-topics --bootstrap-server localhost:9092 --list

# Check if messages are in Kafka
docker exec -it rag-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic document-processing \
  --from-beginning --max-messages 10
```

#### Resolution

```bash
# Recreate Elasticsearch indices
curl -X DELETE http://localhost:9200/documents
curl -X DELETE http://localhost:9200/chat_messages

# Restart backend (will recreate indices)
kill $(cat backend.pid)
mvn spring-boot:run > backend.log 2>&1 &
echo $! > backend.pid

# Check Kafka consumer group
docker exec -it rag-kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group rag-demo-group \
  --describe
```

### Issue: High Memory Usage

#### Symptoms

- Services running slow
- Out of memory errors

#### Diagnosis

```bash
# Check container memory
docker stats

# Check Java heap usage
jps -l
jmap -heap <pid>

# Check Elasticsearch heap
curl http://localhost:9200/_nodes/stats/jvm?pretty
```

#### Resolution

```bash
# Increase Docker memory
# Docker Desktop > Settings > Resources > Memory: 8GB+

# Adjust Elasticsearch heap
# Edit docker-compose.yml:
# ES_JAVA_OPTS: -Xms1g -Xmx1g

# Restart services
docker-compose down
docker-compose up -d

# Adjust JVM settings in application.properties
# -Xms512m -Xmx2g
```

### Issue: Slow AI Responses

#### Symptoms

- Summarization takes > 60s
- Q&A responses timeout

#### Diagnosis

```bash
# Check Ollama resource usage
docker stats rag-ollama

# Check if GPU is being used
nvidia-smi  # If GPU available

# Test Ollama performance
time docker exec -it rag-ollama ollama run mistral "test"
```

#### Resolution

```bash
# Enable GPU in docker-compose.yml (if available)
# Uncomment GPU configuration

# Use smaller model if needed
docker exec -it rag-ollama ollama pull mistral:7b-instruct

# Adjust chunk size in VectorStoreService
# Reduce chunk size from 500 to 300 tokens

# Enable Redis caching (should be default)
docker exec -it rag-redis redis-cli info stats
```

### Issue: Frontend Not Loading

#### Symptoms

- Blank page
- JavaScript errors
- Can't connect to backend

#### Diagnosis

```bash
# Check if frontend is running
curl http://localhost:3000

# Check browser console for errors
# Open browser DevTools (F12)

# Check if backend is accessible
curl http://localhost:8080/api/documents

# Check CORS settings
curl -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: GET" \
  -X OPTIONS http://localhost:8080/api/documents
```

#### Resolution

```bash
# Rebuild frontend
cd frontend
rm -rf node_modules dist .vite
npm install
npm run dev

# Check proxy configuration in vite.config.ts
cat vite.config.ts

# Verify CORS in backend
grep -r "addCorsMappings" src/

# Clear browser cache
# Chrome: Ctrl+Shift+Del > Clear browsing data
```

---

## Maintenance Procedures

### Daily Maintenance

```bash
# Check service health
docker-compose ps
curl http://localhost:8080/actuator/health
curl http://localhost:9200/_cluster/health

# Check logs for errors
tail -100 backend.log | grep -i error
docker-compose logs --tail=100 | grep -i error

# Check disk space
df -h
docker system df
```

### Weekly Maintenance

```bash
# Backup data
./backup.sh  # See Backup & Recovery section

# Check for updates
docker-compose pull
mvn versions:display-dependency-updates
npm outdated -g

# Clean up logs
find . -name "*.log" -mtime +7 -delete

# Optimize Elasticsearch
curl -X POST "http://localhost:9200/_forcemerge?max_num_segments=1"

# Clean Redis cache if needed
docker exec -it rag-redis redis-cli FLUSHDB
```

### Monthly Maintenance

```bash
# Update dependencies
cd frontend && npm update
mvn versions:use-latest-releases

# Rebuild application
mvn clean install
cd frontend && npm run build

# Clean Docker
docker system prune -a --volumes
docker volume prune

# Review and archive old documents
# Use Elasticsearch curator or manual cleanup

# Performance testing
# Run load tests on key endpoints
```

### Quarterly Maintenance

```bash
# Major version updates
# Review changelog for breaking changes
# Test in staging environment first

# Security audit
npm audit
mvn dependency-check:check

# Capacity planning review
# Analyze usage trends
# Plan for scaling if needed
```

---

## Backup & Recovery

### Backup Procedures

#### Elasticsearch Backup

```bash
# Create snapshot repository
curl -X PUT "http://localhost:9200/_snapshot/my_backup" -H 'Content-Type: application/json' -d '{
  "type": "fs",
  "settings": {
    "location": "/usr/share/elasticsearch/backup"
  }
}'

# Create snapshot
curl -X PUT "http://localhost:9200/_snapshot/my_backup/snapshot_$(date +%Y%m%d)?wait_for_completion=true"

# List snapshots
curl http://localhost:9200/_snapshot/my_backup/_all?pretty
```

#### Volume Backup

```bash
#!/bin/bash
# backup.sh

BACKUP_DIR="./backups/$(date +%Y%m%d_%H%M%S)"
mkdir -p $BACKUP_DIR

# Backup Elasticsearch data
docker run --rm \
  -v rag-demo_es_data:/data \
  -v $(pwd)/$BACKUP_DIR:/backup \
  alpine tar czf /backup/es_data.tar.gz /data

# Backup Ollama models
docker run --rm \
  -v rag-demo_ollama_data:/data \
  -v $(pwd)/$BACKUP_DIR:/backup \
  alpine tar czf /backup/ollama_data.tar.gz /data

# Backup Chroma data
docker run --rm \
  -v rag-demo_chroma_data:/data \
  -v $(pwd)/$BACKUP_DIR:/backup \
  alpine tar czf /backup/chroma_data.tar.gz /data

# Backup Redis data
docker run --rm \
  -v rag-demo_redis_data:/data \
  -v $(pwd)/$BACKUP_DIR:/backup \
  alpine tar czf /backup/redis_data.tar.gz /data

echo "Backup completed: $BACKUP_DIR"
```

#### Configuration Backup

```bash
# Backup configuration files
tar czf config_backup_$(date +%Y%m%d).tar.gz \
  docker-compose.yml \
  src/main/resources/application.properties \
  frontend/package.json \
  frontend/vite.config.ts
```

### Recovery Procedures

#### Restore Elasticsearch

```bash
# Stop services
docker-compose down

# Restore volume
docker run --rm \
  -v rag-demo_es_data:/data \
  -v $(pwd)/backups/20241026_120000:/backup \
  alpine tar xzf /backup/es_data.tar.gz -C /

# Start services
docker-compose up -d elasticsearch

# Verify data
curl http://localhost:9200/_cat/indices?v
```

#### Restore from Snapshot

```bash
# List available snapshots
curl http://localhost:9200/_snapshot/my_backup/_all?pretty

# Restore snapshot
curl -X POST "http://localhost:9200/_snapshot/my_backup/snapshot_20241026/_restore?wait_for_completion=true"

# Verify restoration
curl http://localhost:9200/documents/_count
```

#### Complete System Recovery

```bash
#!/bin/bash
# restore.sh

BACKUP_DIR=$1

if [ -z "$BACKUP_DIR" ]; then
  echo "Usage: ./restore.sh <backup_directory>"
  exit 1
fi

# Stop all services
./stop.sh

# Remove old volumes
docker-compose down -v

# Restore volumes
docker run --rm -v rag-demo_es_data:/data -v $(pwd)/$BACKUP_DIR:/backup alpine tar xzf /backup/es_data.tar.gz -C /
docker run --rm -v rag-demo_ollama_data:/data -v $(pwd)/$BACKUP_DIR:/backup alpine tar xzf /backup/ollama_data.tar.gz -C /
docker run --rm -v rag-demo_chroma_data:/data -v $(pwd)/$BACKUP_DIR:/backup alpine tar xzf /backup/chroma_data.tar.gz -C /
docker run --rm -v rag-demo_redis_data:/data -v $(pwd)/$BACKUP_DIR:/backup alpine tar xzf /backup/redis_data.tar.gz -C /

# Start services
docker-compose up -d

echo "Restoration completed from: $BACKUP_DIR"
```

---

## Emergency Procedures

### System Down - Critical

#### Immediate Actions

1. Check if Docker daemon is running
2. Check system resources (CPU, memory, disk)
3. Check network connectivity
4. Restart Docker service if needed

```bash
# Check system status
sudo systemctl status docker
docker ps
df -h
free -h

# Emergency restart
sudo systemctl restart docker
docker-compose down
docker-compose up -d
```

### Data Corruption

#### Immediate Actions

1. Stop all services immediately
2. Take snapshot of current state
3. Assess extent of corruption
4. Restore from latest backup

```bash
# Stop services
./stop.sh

# Snapshot current state
docker commit rag-elasticsearch es_snapshot_emergency
docker commit rag-kafka kafka_snapshot_emergency

# Restore from backup
./restore.sh backups/<latest_backup>
```

### Security Breach

#### Immediate Actions

1. Isolate affected services
2. Stop all external access
3. Review logs for suspicious activity
4. Change all credentials
5. Notify security team

```bash
# Stop services
docker-compose down

# Review logs
grep -i "unauthorized\|forbidden\|hack\|attack" *.log
docker-compose logs | grep -i "error\|failed\|unauthorized"

# Change passwords
# Update application.properties
# Restart with new credentials
```

### Performance Degradation

#### Immediate Actions

1. Identify bottleneck (CPU, memory, disk, network)
2. Scale affected services
3. Clear caches if safe
4. Restart services in order

```bash
# Identify bottleneck
docker stats
top
iostat
netstat -tunlp

# Clear Redis cache
docker exec -it rag-redis redis-cli FLUSHDB

# Restart services
docker-compose restart <service-name>
```

---

## Performance Tuning

### Elasticsearch Tuning

```yaml
# docker-compose.yml
services:
  elasticsearch:
    environment:
      - "ES_JAVA_OPTS=-Xms2g -Xmx2g"  # Increase heap
      - bootstrap.memory_lock=true
      - indices.memory.index_buffer_size=30%
    ulimits:
      memlock:
        soft: -1
        hard: -1
```

### Kafka Tuning

```yaml
# docker-compose.yml
services:
  kafka:
    environment:
      - KAFKA_NUM_NETWORK_THREADS=8
      - KAFKA_NUM_IO_THREADS=8
      - KAFKA_SOCKET_SEND_BUFFER_BYTES=102400
      - KAFKA_SOCKET_RECEIVE_BUFFER_BYTES=102400
```

### Backend Tuning

```properties
# application.properties

# Connection pool
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5

# Thread pool
server.tomcat.threads.max=200
server.tomcat.threads.min-spare=10

# Timeouts
spring.kafka.consumer.session-timeout-ms=30000
spring.kafka.consumer.request-timeout-ms=60000
```

### Redis Tuning

```bash
# Increase max memory
docker exec -it rag-redis redis-cli CONFIG SET maxmemory 2gb
docker exec -it rag-redis redis-cli CONFIG SET maxmemory-policy allkeys-lru
```

---

## Security Hardening

### Production Security Checklist

#### Infrastructure

- [ ] Enable Elasticsearch X-Pack security
- [ ] Configure Kafka SASL authentication
- [ ] Add Redis password
- [ ] Use HTTPS for all services
- [ ] Configure firewall rules
- [ ] Enable Docker content trust

#### Application

- [ ] Add Spring Security
- [ ] Implement JWT authentication
- [ ] Add rate limiting
- [ ] Enable CSRF protection
- [ ] Sanitize all inputs
- [ ] Add audit logging

#### Network

- [ ] Use internal Docker network
- [ ] Restrict external access
- [ ] Configure TLS for Kafka
- [ ] Use VPN for remote access

### Security Configuration Example

```properties
# application.properties (Production)

# Enable security
spring.security.enabled=true
spring.security.user.name=admin
spring.security.user.password=${ADMIN_PASSWORD}

# Elasticsearch security
spring.elasticsearch.username=elastic
spring.elasticsearch.password=${ES_PASSWORD}

# Kafka security
spring.kafka.security.protocol=SASL_SSL
spring.kafka.properties.sasl.mechanism=PLAIN

# Redis security
spring.data.redis.password=${REDIS_PASSWORD}
```

---

## Contact Information

### Escalation Path

**Level 1**: Team Member → Check runbook, restart services
**Level 2**: Team Lead → Investigate logs, apply fixes
**Level 3**: DevOps/SRE → Infrastructure issues, scaling
**Level 4**: Vendor Support → Platform-specific issues

### Important Links

- Documentation: /docs
- Repository: <git-url>
- CI/CD: <ci-url>
- Monitoring: <monitoring-url>
- Incident Management: <incident-url>

---

## Appendix

### Quick Reference Commands

```bash
# Start everything
./start.sh

# Stop everything
./stop.sh

# View logs
docker-compose logs -f
tail -f backend.log

# Health checks
curl http://localhost:8080/actuator/health
curl http://localhost:9200/_cluster/health

# Restart service
docker-compose restart <service>

# Clean everything
docker-compose down -v
rm -rf target/ frontend/dist/ frontend/node_modules/
```

### File Locations

```
Configuration:
├── docker-compose.yml          # Docker services
├── application.properties      # Backend config
├── frontend/vite.config.ts     # Frontend config
└── .env                        # Environment variables

Logs:
├── backend.log                 # Backend logs
├── frontend.log                # Frontend logs
└── docker-compose logs         # Container logs

Data:
├── Docker volumes              # Persistent data
│   ├── es_data
│   ├── kafka_data
│   ├── ollama_data
│   ├── chroma_data
│   └── redis_data
└── backups/                    # Backup directory
```

### Version Information

```
Application Version: 0.0.1-SNAPSHOT
Spring Boot Version: 3.5.8-SNAPSHOT
Spring AI Version: 1.0.3
React Version: 18.2.0
Docker Compose Version: 3.8
```

---

**Document Version**: 1.0  
**Last Updated**: 2024-10-26  
**Next Review**: 2024-11-26
