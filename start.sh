#!/bin/bash

echo "🚀 Starting RAG Chatbot Application..."
echo ""

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Function to check if a service is ready
check_service() {
    local service_name=$1
    local url=$2
    local max_attempts=30
    local attempt=1
    
    echo -e "${BLUE}Waiting for ${service_name} to be ready...${NC}"
    
    while [ $attempt -le $max_attempts ]; do
        if curl -s "$url" > /dev/null 2>&1; then
            echo -e "${GREEN}✓ ${service_name} is ready!${NC}"
            return 0
        fi
        echo "  Attempt $attempt/$max_attempts..."
        sleep 2
        attempt=$((attempt + 1))
    done
    
    echo -e "${RED}✗ ${service_name} failed to start${NC}"
    return 1
}

# Step 1: Start Docker services
echo -e "${BLUE}Step 1: Starting Docker services (Elasticsearch, Kafka, Ollama)...${NC}"
docker-compose up -d

if [ $? -ne 0 ]; then
    echo -e "${RED}Failed to start Docker services${NC}"
    exit 1
fi

# Step 2: Wait for services to be ready
check_service "Elasticsearch" "http://localhost:9200/_cluster/health"
check_service "Ollama" "http://localhost:11434/api/tags"

# Step 3: Pull Mistral model if not already available
echo -e "${BLUE}Step 3: Checking Mistral model...${NC}"
if ! docker exec rag-ollama ollama list | grep -q "mistral"; then
    echo "Pulling Mistral model (this may take a few minutes)..."
    docker exec rag-ollama ollama pull mistral
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ Mistral model ready!${NC}"
    else
        echo -e "${RED}✗ Failed to pull Mistral model${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}✓ Mistral model already available${NC}"
fi

# Step 4: Build and start backend
echo -e "${BLUE}Step 4: Building and starting Spring Boot backend...${NC}"
mvn clean install -DskipTests

if [ $? -ne 0 ]; then
    echo -e "${RED}Failed to build backend${NC}"
    exit 1
fi

# Start backend in background
echo "Starting backend server..."
mvn spring-boot:run > backend.log 2>&1 &
BACKEND_PID=$!
echo $BACKEND_PID > backend.pid

# Wait for backend to be ready
sleep 10
check_service "Backend API" "http://localhost:8080/api/documents"

# Step 5: Setup and start frontend
echo -e "${BLUE}Step 5: Setting up and starting React frontend...${NC}"
cd frontend

if [ ! -d "node_modules" ]; then
    echo "Installing frontend dependencies..."
    npm install
fi

if [ $? -ne 0 ]; then
    echo -e "${RED}Failed to install frontend dependencies${NC}"
    exit 1
fi

# Start frontend in background
echo "Starting frontend development server..."
npm run dev > ../frontend.log 2>&1 &
FRONTEND_PID=$!
echo $FRONTEND_PID > ../frontend.pid

cd ..

# Wait a bit for frontend to start
sleep 5

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}✓ RAG Chatbot Application Started!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "📊 Service Status:"
echo "  - Elasticsearch: http://localhost:9200"
echo "  - Kafka: localhost:9092"
echo "  - Ollama: http://localhost:11434"
echo "  - Backend API: http://localhost:8080"
echo "  - Frontend UI: http://localhost:3000"
echo ""
echo "🌐 Open http://localhost:3000 in your browser to use the application"
echo ""
echo "📝 Logs:"
echo "  - Backend: backend.log"
echo "  - Frontend: frontend.log"
echo ""
echo "🛑 To stop the application, run: ./stop.sh"
echo ""
