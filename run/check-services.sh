#!/bin/bash

# RAG Chatbot Service Checker
# This script checks all services and diagnoses Q&A issues

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║          RAG Chatbot Service Health Check                     ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check functions
check_pass() {
    echo -e "${GREEN}✓${NC} $1"
}

check_fail() {
    echo -e "${RED}✗${NC} $1"
}

check_warn() {
    echo -e "${YELLOW}⚠${NC} $1"
}

# Initialize counters
PASS_COUNT=0
FAIL_COUNT=0
WARN_COUNT=0

# 1. Check Docker Containers
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "1. Docker Containers"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

CONTAINERS=("rag-elasticsearch" "rag-kafka" "rag-zookeeper" "rag-ollama" "rag-redis" "rag-chroma")

for container in "${CONTAINERS[@]}"; do
    if docker ps --format '{{.Names}}' | grep -q "^${container}$"; then
        STATUS=$(docker inspect -f '{{.State.Health.Status}}' $container 2>/dev/null)
        if [ "$STATUS" == "healthy" ]; then
            check_pass "$container is running and healthy"
            ((PASS_COUNT++))
        elif [ "$STATUS" == "starting" ]; then
            check_warn "$container is starting..."
            ((WARN_COUNT++))
        elif [ -z "$STATUS" ]; then
            check_pass "$container is running (no health check)"
            ((PASS_COUNT++))
        else
            check_warn "$container is running but unhealthy"
            ((WARN_COUNT++))
        fi
    else
        check_fail "$container is not running"
        ((FAIL_COUNT++))
    fi
done
echo ""

# 2. Check Elasticsearch
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "2. Elasticsearch"
echo "━━━━━━━━━━━━━━━━━━━━━━━━��━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if curl -s http://localhost:9200/_cluster/health > /dev/null 2>&1; then
    ES_STATUS=$(curl -s http://localhost:9200/_cluster/health | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
    if [ "$ES_STATUS" == "green" ]; then
        check_pass "Elasticsearch cluster is healthy (green)"
        ((PASS_COUNT++))
    elif [ "$ES_STATUS" == "yellow" ]; then
        check_warn "Elasticsearch cluster is operational but degraded (yellow)"
        ((WARN_COUNT++))
    else
        check_fail "Elasticsearch cluster is unhealthy (red)"
        ((FAIL_COUNT++))
    fi
    
    # Check for RAG index
    if curl -s "http://localhost:9200/_cat/indices" | grep -q "rag-documents"; then
        DOC_COUNT=$(curl -s "http://localhost:9200/rag-documents/_count" | grep -o '"count":[0-9]*' | cut -d':' -f2)
        check_pass "RAG documents index exists with $DOC_COUNT documents"
        ((PASS_COUNT++))
    else
        check_warn "RAG documents index not found (no documents uploaded yet)"
        ((WARN_COUNT++))
    fi
else
    check_fail "Elasticsearch is not accessible at http://localhost:9200"
    ((FAIL_COUNT++))
fi
echo ""

# 3. Check Ollama
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "3. Ollama & Mistral AI"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if curl -s http://localhost:11434/api/tags > /dev/null 2>&1; then
    check_pass "Ollama service is running"
    ((PASS_COUNT++))
    
    if curl -s http://localhost:11434/api/tags | grep -q "mistral"; then
        check_pass "Mistral model is installed"
        ((PASS_COUNT++))
    else
        check_fail "Mistral model is NOT installed"
        echo "   Run: docker exec -it rag-ollama ollama pull mistral"
        ((FAIL_COUNT++))
    fi
else
    check_fail "Ollama is not accessible at http://localhost:11434"
    ((FAIL_COUNT++))
fi
echo ""

# 4. Check Backend
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "4. Spring Boot Backend"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if lsof -i :8080 > /dev/null 2>&1; then
    check_pass "Backend process is running on port 8080"
    ((PASS_COUNT++))
    
    if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
        HEALTH_STATUS=$(curl -s http://localhost:8080/actuator/health | grep -o '"status":"[^"]*"' | cut -d'"' -f4)
        if [ "$HEALTH_STATUS" == "UP" ]; then
            check_pass "Backend health check: UP"
            ((PASS_COUNT++))
        else
            check_warn "Backend health check: $HEALTH_STATUS"
            ((WARN_COUNT++))
        fi
    else
        check_warn "Backend health endpoint not responding"
        ((WARN_COUNT++))
    fi
    
    # Check documents endpoint
    if curl -s http://localhost:8080/api/documents > /dev/null 2>&1; then
        DOC_COUNT=$(curl -s http://localhost:8080/api/documents | grep -o '"id"' | wc -l | tr -d ' ')
        check_pass "Documents API is accessible ($DOC_COUNT documents)"
        ((PASS_COUNT++))
    else
        check_fail "Documents API is not responding"
        ((FAIL_COUNT++))
    fi
else
    check_fail "Backend is NOT running on port 8080"
    echo "   Run: ./mvnw spring-boot:run"
    ((FAIL_COUNT++))
fi
echo ""

# 5. Check Frontend
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "5. React Frontend"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if lsof -i :3000 > /dev/null 2>&1; then
    check_pass "Frontend is running on port 3000"
    echo "   Access at: http://localhost:3000"
    ((PASS_COUNT++))
elif lsof -i :3001 > /dev/null 2>&1; then
    check_warn "Frontend is running on port 3001 (3000 was busy)"
    echo "   Access at: http://localhost:3001"
    ((WARN_COUNT++))
else
    check_fail "Frontend is NOT running"
    echo "   Run: cd frontend && npm run dev"
    ((FAIL_COUNT++))
fi
echo ""

# 6. Check Kafka
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "6. Kafka Message Queue"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if docker ps --format '{{.Names}}' | grep -q "rag-kafka"; then
    check_pass "Kafka is running"
    ((PASS_COUNT++))
    
    TOPICS=$(docker exec -it rag-kafka kafka-topics --list --bootstrap-server localhost:9092 2>/dev/null | wc -l | tr -d ' ')
    if [ "$TOPICS" -gt 0 ]; then
        check_pass "Kafka has $TOPICS topics configured"
        ((PASS_COUNT++))
    else
        check_warn "No Kafka topics found (will auto-create on first use)"
        ((WARN_COUNT++))
    fi
else
    check_fail "Kafka is not running"
    ((FAIL_COUNT++))
fi
echo ""

# 7. Test Q&A Functionality
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "7. Q&A Functionality Test"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Only test if backend is running
if curl -s http://localhost:8080/api/documents > /dev/null 2>&1; then
    FIRST_DOC=$(curl -s http://localhost:8080/api/documents | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
    
    if [ -n "$FIRST_DOC" ]; then
        check_pass "Documents available for testing"
        
        # Try a test question
        TEST_RESPONSE=$(curl -s -X POST http://localhost:8080/api/chat/ask \
            -H "Content-Type: application/json" \
            -d "{\"question\":\"Test\",\"sessionId\":\"health-check\",\"documentId\":\"$FIRST_DOC\"}" 2>&1)
        
        if echo "$TEST_RESPONSE" | grep -q '"answer"'; then
            check_pass "Q&A endpoint is working correctly"
            ((PASS_COUNT++))
        else
            check_fail "Q&A endpoint returned an error"
            echo "   Response: $(echo $TEST_RESPONSE | head -c 100)..."
            ((FAIL_COUNT++))
        fi
    else
        check_warn "No documents uploaded yet - upload a document to test Q&A"
        ((WARN_COUNT++))
    fi
else
    check_fail "Cannot test Q&A - backend is not responding"
    ((FAIL_COUNT++))
fi
echo ""

# Summary
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Summary"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo -e "${GREEN}✓ Passed:${NC} $PASS_COUNT"
echo -e "${YELLOW}⚠ Warnings:${NC} $WARN_COUNT"
echo -e "${RED}✗ Failed:${NC} $FAIL_COUNT"
echo ""

if [ $FAIL_COUNT -eq 0 ]; then
    if [ $WARN_COUNT -eq 0 ]; then
        echo -e "${GREEN}╔════════════════════════════════════════╗${NC}"
        echo -e "${GREEN}║  All systems operational! 🚀          ║${NC}"
        echo -e "${GREEN}╚════════════════════════════════════════╝${NC}"
    else
        echo -e "${YELLOW}╔════════════════════════════════════════╗${NC}"
        echo -e "${YELLOW}║  System is functional with warnings   ║${NC}"
        echo -e "${YELLOW}╚════════════════════════════════════════╝${NC}"
    fi
    echo ""
    echo "Next steps:"
    echo "  1. Access frontend at http://localhost:3000 (or 3001)"
    echo "  2. Upload a document"
    echo "  3. Ask questions about your document"
    exit 0
else
    echo -e "${RED}╔════════════════════════════════════════╗${NC}"
    echo -e "${RED}║  Issues detected - see failures above ║${NC}"
    echo -e "${RED}╚════════════════════════════════════════╝${NC}"
    echo ""
    echo "Troubleshooting:"
    echo "  • Read: TROUBLESHOOTING_QA.md"
    echo "  • Start services: docker-compose up -d"
    echo "  • Start backend: ./mvnw spring-boot:run"
    echo "  • Start frontend: cd frontend && npm run dev"
    exit 1
fi
