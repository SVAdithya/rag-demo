#!/bin/bash

echo "🛑 Stopping RAG Chatbot Application..."
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Stop backend
if [ -f backend.pid ]; then
    BACKEND_PID=$(cat backend.pid)
    echo "Stopping backend (PID: $BACKEND_PID)..."
    kill $BACKEND_PID 2>/dev/null
    rm backend.pid
    echo -e "${GREEN}✓ Backend stopped${NC}"
else
    echo "No backend PID file found"
fi

# Stop frontend
if [ -f frontend.pid ]; then
    FRONTEND_PID=$(cat frontend.pid)
    echo "Stopping frontend (PID: $FRONTEND_PID)..."
    kill $FRONTEND_PID 2>/dev/null
    rm frontend.pid
    echo -e "${GREEN}✓ Frontend stopped${NC}"
else
    echo "No frontend PID file found"
fi

# Stop Docker services
echo "Stopping Docker services..."
docker-compose down

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Docker services stopped${NC}"
else
    echo -e "${RED}✗ Failed to stop Docker services${NC}"
fi

echo ""
echo -e "${GREEN}Application stopped successfully!${NC}"
