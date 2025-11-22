#!/bin/bash

echo "🚀 Starting RAG Chatbot Application with OCR Support..."
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

# Step 1: Configure Tesseract OCR (if available)
echo -e "${BLUE}Step 1: Configuring Tesseract OCR...${NC}"
HOMEBREW_PREFIX="/opt/homebrew"
TESSERACT_VERSION=$(ls $HOMEBREW_PREFIX/Cellar/tesseract/ 2>/dev/null | head -1)

if [ -z "$TESSERACT_VERSION" ]; then
    echo -e "${RED}⚠️  Warning: Tesseract not found. OCR features will not be available.${NC}"
    echo "   To enable OCR, install Tesseract: brew install tesseract"
    echo ""
    OCR_ENABLED=false
else
    TESSERACT_LIB="$HOMEBREW_PREFIX/Cellar/tesseract/$TESSERACT_VERSION/lib"
    HOMEBREW_LIB="$HOMEBREW_PREFIX/lib"
    
    echo -e "${GREEN}✓ Found Tesseract version: $TESSERACT_VERSION${NC}"
    echo "  Library path: $TESSERACT_LIB"
    
    # Create local lib directory with symlinks
    LOCAL_LIB_DIR="$PWD/lib"
    mkdir -p "$LOCAL_LIB_DIR"
    
    # Create symlinks to tesseract libraries
    if [ -f "$TESSERACT_LIB/libtesseract.5.dylib" ]; then
        ln -sf "$TESSERACT_LIB/libtesseract.5.dylib" "$LOCAL_LIB_DIR/libtesseract.dylib"
        ln -sf "$TESSERACT_LIB/libtesseract.5.dylib" "$LOCAL_LIB_DIR/libtesseract.5.dylib"
    fi
    
    # Link leptonica (dependency)
    if [ -f "$HOMEBREW_LIB/libleptonica.dylib" ]; then
        ln -sf "$HOMEBREW_LIB/libleptonica.dylib" "$LOCAL_LIB_DIR/libleptonica.dylib"
    fi
    if [ -f "$HOMEBREW_LIB/liblept.dylib" ]; then
        ln -sf "$HOMEBREW_LIB/liblept.dylib" "$LOCAL_LIB_DIR/liblept.dylib"
    fi
    
    # Set comprehensive library paths
    export DYLD_LIBRARY_PATH="$LOCAL_LIB_DIR:$TESSERACT_LIB:$HOMEBREW_LIB:/usr/local/lib:$DYLD_LIBRARY_PATH"
    export LD_LIBRARY_PATH="$LOCAL_LIB_DIR:$TESSERACT_LIB:$HOMEBREW_LIB:/usr/local/lib:$LD_LIBRARY_PATH"
    export JNA_LIBRARY_PATH="$LOCAL_LIB_DIR:$TESSERACT_LIB:$HOMEBREW_LIB:/usr/local/lib"
    export TESSDATA_PREFIX="$HOMEBREW_PREFIX/share/tessdata"
    
    # Export these for Maven to use in pom.xml
    export LOCAL_LIB_DIR
    export TESSERACT_LIB
    export HOMEBREW_LIB
    
    echo -e "${GREEN}✓ OCR environment configured${NC}"
    OCR_ENABLED=true
fi
echo ""

# Step 2: Start Docker services
echo -e "${BLUE}Step 2: Starting Docker services (Elasticsearch, Kafka, Ollama)...${NC}"
docker-compose up -d

if [ $? -ne 0 ]; then
    echo -e "${RED}Failed to start Docker services${NC}"
    exit 1
fi

# Step 3: Wait for services to be ready
echo -e "${BLUE}Step 3: Waiting for services to be ready...${NC}"
check_service "Elasticsearch" "http://localhost:9200/_cluster/health"
check_service "Ollama" "http://localhost:11434/api/tags"

# Step 4: Pull Mistral model if not already available
echo -e "${BLUE}Step 4: Checking Mistral model...${NC}"
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

# Step 5: Build and start backend
echo -e "${BLUE}Step 5: Building and starting Spring Boot backend...${NC}"

# Create .mvn/jvm.config for Maven to use (needed for Spring Boot DevTools)
if [ "$OCR_ENABLED" = true ]; then
    mkdir -p .mvn
    cat > .mvn/jvm.config << EOF
-Djna.library.path=$LOCAL_LIB_DIR:$TESSERACT_LIB:$HOMEBREW_LIB:/usr/local/lib
-Djava.library.path=$LOCAL_LIB_DIR:$TESSERACT_LIB:$HOMEBREW_LIB:/usr/local/lib
EOF
    echo "  Created .mvn/jvm.config with library paths"
fi

mvn clean install -DskipTests

if [ $? -ne 0 ]; then
    echo -e "${RED}Failed to build backend${NC}"
    exit 1
fi

# Start backend in background with OCR support if available
echo "Starting backend server..."
if [ "$OCR_ENABLED" = true ]; then
    # Export Maven options for the forked Spring Boot process
    export MAVEN_OPTS="-Djna.library.path=$LOCAL_LIB_DIR:$TESSERACT_LIB:$HOMEBREW_LIB:/usr/local/lib -Djava.library.path=$LOCAL_LIB_DIR:$TESSERACT_LIB:$HOMEBREW_LIB:/usr/local/lib"
    # Also ensure DYLD_LIBRARY_PATH is exported (already set above, but re-export to be safe)
    export DYLD_LIBRARY_PATH
    export LD_LIBRARY_PATH
    export JNA_LIBRARY_PATH
    mvn spring-boot:run > backend.log 2>&1 &
else
    mvn spring-boot:run > backend.log 2>&1 &
fi
BACKEND_PID=$!
echo $BACKEND_PID > backend.pid

# Wait for backend to be ready
sleep 10
check_service "Backend API" "http://localhost:8080/api/documents"

# Step 6: Setup and start frontend
echo -e "${BLUE}Step 6: Setting up and starting React frontend...${NC}"
cd frontend

if [ ! -d "node_modules" ]; then
    echo "Installing frontend dependencies..."
    npm install
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}Failed to install frontend dependencies${NC}"
        exit 1
    fi
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
if [ "$OCR_ENABLED" = true ]; then
    echo "  - OCR Support: ✓ Enabled"
else
    echo "  - OCR Support: ✗ Disabled (install tesseract to enable)"
fi
echo ""
echo "🌐 Open http://localhost:3000 in your browser to use the application"
echo ""
echo "📝 Logs:"
echo "  - Backend: backend.log"
echo "  - Frontend: frontend.log"
echo ""
echo "🛑 To stop the application, run: ./run/stop.sh"
echo ""
