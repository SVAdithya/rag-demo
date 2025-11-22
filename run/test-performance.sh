#!/bin/bash

# Performance Test Script for RAG Chatbot
# Measures Q&A response times

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║       RAG Chatbot Performance Test                            ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check if backend is running
if ! curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo -e "${RED}✗ Backend is not running on port 8080${NC}"
    echo "Start it with: ./mvnw spring-boot:run"
    exit 1
fi

echo -e "${GREEN}✓ Backend is running${NC}"
echo ""

# Get first document
echo "Fetching test document..."
DOC_ID=$(curl -s http://localhost:8080/api/documents | jq -r '.[0].id' 2>/dev/null)

if [ -z "$DOC_ID" ] || [ "$DOC_ID" == "null" ]; then
    echo -e "${RED}✗ No documents found${NC}"
    echo "Upload a document first through the UI or API"
    exit 1
fi

DOC_NAME=$(curl -s http://localhost:8080/api/documents | jq -r '.[0].filename' 2>/dev/null)
echo -e "${GREEN}✓ Using document: $DOC_NAME${NC}"
echo -e "  Document ID: $DOC_ID"
echo ""

# Test questions
declare -a QUESTIONS=(
    "What is the main topic of this document?"
    "Summarize the key points."
    "What are the most important details?"
)

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Running Performance Tests (3 questions)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

TOTAL_TIME=0
TEST_COUNT=0

for i in "${!QUESTIONS[@]}"; do
    question="${QUESTIONS[$i]}"
    test_num=$((i + 1))
    
    echo -e "${BLUE}Test $test_num/${#QUESTIONS[@]}${NC}"
    echo "Question: \"$question\""
    echo -n "Processing... "
    
    # Measure time
    START=$(date +%s)
    
    RESPONSE=$(curl -s -X POST http://localhost:8080/api/chat/ask \
      -H "Content-Type: application/json" \
      -d "{
        \"question\": \"$question\",
        \"sessionId\": \"perf-test-$(date +%s)\",
        \"documentId\": \"$DOC_ID\"
      }" 2>&1)
    
    END=$(date +%s)
    DURATION=$((END - START))
    TOTAL_TIME=$((TOTAL_TIME + DURATION))
    TEST_COUNT=$((TEST_COUNT + 1))
    
    # Check if successful
    if echo "$RESPONSE" | grep -q '"answer"'; then
        ANSWER=$(echo "$RESPONSE" | jq -r '.answer' 2>/dev/null | head -c 100)
        echo -e "${GREEN}✓ Success${NC}"
        echo "  Time: ${DURATION}s"
        echo "  Answer preview: ${ANSWER}..."
        
        # Performance rating
        if [ $DURATION -lt 10 ]; then
            echo -e "  Rating: ${GREEN}EXCELLENT${NC}"
        elif [ $DURATION -lt 20 ]; then
            echo -e "  Rating: ${GREEN}GOOD${NC}"
        elif [ $DURATION -lt 30 ]; then
            echo -e "  Rating: ${YELLOW}ACCEPTABLE${NC}"
        else
            echo -e "  Rating: ${RED}SLOW${NC}"
        fi
    else
        echo -e "${RED}✗ Failed${NC}"
        echo "  Error: $(echo $RESPONSE | head -c 100)"
    fi
    
    echo ""
    
    # Wait between tests
    if [ $test_num -lt ${#QUESTIONS[@]} ]; then
        sleep 2
    fi
done

# Calculate average
AVG_TIME=$((TOTAL_TIME / TEST_COUNT))

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Performance Summary"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Tests completed: $TEST_COUNT"
echo "Total time: ${TOTAL_TIME}s"
echo "Average time: ${AVG_TIME}s per question"
echo ""

# Overall rating
if [ $AVG_TIME -lt 10 ]; then
    echo -e "Overall Performance: ${GREEN}EXCELLENT ⭐⭐⭐⭐⭐${NC}"
    echo "Your system is very fast!"
elif [ $AVG_TIME -lt 20 ]; then
    echo -e "Overall Performance: ${GREEN}GOOD ⭐⭐⭐⭐${NC}"
    echo "Performance is solid. Consider further optimizations for even better speed."
elif [ $AVG_TIME -lt 30 ]; then
    echo -e "Overall Performance: ${YELLOW}ACCEPTABLE ⭐⭐⭐${NC}"
    echo "Performance is adequate but could be improved."
    echo "Run: ./run/optimize-speed.sh"
else
    echo -e "Overall Performance: ${RED}SLOW ⭐⭐${NC}"
    echo "Significant improvements needed."
    echo ""
    echo "Recommendations:"
    echo "  1. Run: ./run/optimize-speed.sh"
    echo "  2. Read: SPEED_UP_MISTRAL.md"
    echo "  3. Consider using a faster model or cloud LLM"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
