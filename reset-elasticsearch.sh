#!/bin/bash

# Script to reset Elasticsearch indices
# This will delete all existing data and recreate the indices with correct mappings

echo "🔄 Resetting Elasticsearch indices..."
echo ""

# Check if Elasticsearch is running
if ! curl -s http://localhost:9200/_cluster/health > /dev/null; then
    echo "❌ Error: Elasticsearch is not running at localhost:9200"
    echo "   Please start Elasticsearch with: docker-compose up -d elasticsearch"
    exit 1
fi

echo "✅ Elasticsearch is running"
echo ""

# Delete documents index
echo "🗑️  Deleting 'documents' index..."
curl -X DELETE "http://localhost:9200/documents" 2>/dev/null
echo ""

# Delete chat_messages index
echo "🗑️  Deleting 'chat_messages' index..."
curl -X DELETE "http://localhost:9200/chat_messages" 2>/dev/null
echo ""

# Delete document_chunks index (if it exists)
echo "🗑️  Deleting 'document_chunks' index..."
curl -X DELETE "http://localhost:9200/document_chunks" 2>/dev/null
echo ""

echo ""
echo "✅ All indices deleted!"
echo ""
echo "📝 The indices will be recreated automatically when you:"
echo "   1. Restart the Spring Boot application"
echo "   2. Upload a new document"
echo ""
echo "To restart the application:"
echo "   mvn spring-boot:run"
echo ""
