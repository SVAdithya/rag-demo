#!/bin/bash

# OCR Extraction Test Script
# Usage: ./test-ocr.sh your-file.pdf

if [ -z "$1" ]; then
    echo "Usage: $0 <pdf-file>"
    echo "Example: $0 document-with-mo-number.pdf"
    exit 1
fi

if [ ! -f "$1" ]; then
    echo "Error: File '$1' not found"
    exit 1
fi

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo "================================================"
echo "   OCR Extraction Test"
echo "================================================"
echo ""

# Check Tesseract
echo -n "1. Checking Tesseract: "
if command -v tesseract &> /dev/null; then
    echo -e "${GREEN}✓ Installed${NC}"
    TESS_VERSION=$(tesseract --version 2>&1 | head -1)
    echo "   Version: $TESS_VERSION"
else
    echo -e "${RED}✗ Not installed${NC}"
    echo "   Install with: brew install tesseract"
    exit 1
fi
echo ""

# Check application
echo -n "2. Checking Application: "
if curl -s http://localhost:8080/actuator/health &> /dev/null; then
    echo -e "${GREEN}✓ Running${NC}"
elif lsof -i :8080 &> /dev/null; then
    echo -e "${YELLOW}⚠ Port 8080 in use but health check failed${NC}"
else
    echo -e "${RED}✗ Not running${NC}"
    echo "   Start with: mvn spring-boot:run"
    exit 1
fi
echo ""

# Upload file
echo "3. Uploading file: $1"
RESPONSE=$(curl -s -X POST http://localhost:8080/api/documents/upload \
    -F "file=@$1" 2>&1)

if [ $? -ne 0 ]; then
    echo -e "${RED}✗ Upload failed${NC}"
    echo "$RESPONSE"
    exit 1
fi

DOC_ID=$(echo "$RESPONSE" | jq -r '.id' 2>/dev/null)

if [ -z "$DOC_ID" ] || [ "$DOC_ID" = "null" ]; then
    echo -e "${RED}✗ Failed to get document ID${NC}"
    echo "Response: $RESPONSE"
    exit 1
fi

echo -e "${GREEN}✓ Uploaded successfully${NC}"
echo "   Document ID: $DOC_ID"
echo ""

# Wait for OCR processing
echo "4. Waiting for OCR processing (15 seconds)..."
for i in {15..1}; do
    echo -n "   $i..."
    sleep 1
done
echo " Done!"
echo ""

# Check logs for OCR activity
echo "5. Checking OCR activity in logs..."
if [ -f "backend.log" ]; then
    OCR_LINES=$(grep "$DOC_ID" backend.log 2>/dev/null | grep -c "OCR" || echo "0")
    if [ "$OCR_LINES" -gt 0 ]; then
        echo -e "${GREEN}✓ OCR activity found ($OCR_LINES log entries)${NC}"
        
        # Show relevant log lines
        echo "   Recent OCR logs:"
        grep "$DOC_ID" backend.log 2>/dev/null | grep "OCR" | tail -3 | sed 's/^/   /'
    else
        echo -e "${YELLOW}⚠ No OCR activity found in logs${NC}"
    fi
else
    echo -e "${YELLOW}⚠ Log file not found${NC}"
fi
echo ""

# Check extracted content
echo "6. Checking extracted content..."
CONTENT=$(curl -s "http://localhost:8080/api/documents/$DOC_ID" 2>/dev/null | jq -r '.content' 2>/dev/null)

if [ -z "$CONTENT" ] || [ "$CONTENT" = "null" ]; then
    echo -e "${RED}✗ Could not retrieve content${NC}"
else
    CONTENT_LENGTH=${#CONTENT}
    echo -e "${GREEN}✓ Content extracted${NC}"
    echo "   Length: $CONTENT_LENGTH characters"
    
    # Check for OCR section
    if echo "$CONTENT" | grep -q "=== TEXT FROM IMAGES ==="; then
        echo -e "${GREEN}✓ OCR section found in content${NC}"
        
        # Show sample of OCR text
        echo ""
        echo "   Sample of OCR extracted text:"
        echo "   ----------------------------"
        echo "$CONTENT" | sed -n '/=== TEXT FROM IMAGES ===/,/^$/p' | head -10 | sed 's/^/   /'
        echo "   ..."
    else
        echo -e "${YELLOW}⚠ No OCR section found${NC}"
        echo "   (Document may not have images or OCR failed)"
    fi
    
    # Search for MO Number
    echo ""
    echo "   Searching for MO Number keywords..."
    if echo "$CONTENT" | grep -i "MO\|consignment" &> /dev/null; then
        echo -e "${GREEN}✓ Found MO/Consignment references${NC}"
        echo "$CONTENT" | grep -i "MO\|consignment" | head -3 | sed 's/^/   /'
    else
        echo -e "${YELLOW}⚠ No MO/Consignment keywords found${NC}"
    fi
fi
echo ""

# Test RAG query
echo "7. Testing RAG query..."
QUERY_RESPONSE=$(curl -s -X POST http://localhost:8080/api/chat/ask \
    -H "Content-Type: application/json" \
    -d '{"question": "What is the MO Number or Consignment Number in this document?", "sessionId": "test-'$RANDOM'"}' 2>/dev/null)

ANSWER=$(echo "$QUERY_RESPONSE" | jq -r '.answer' 2>/dev/null)

if [ -z "$ANSWER" ] || [ "$ANSWER" = "null" ]; then
    echo -e "${RED}✗ Query failed${NC}"
else
    echo -e "${GREEN}✓ Query successful${NC}"
    echo "   Question: What is the MO Number or Consignment Number?"
    echo "   Answer:"
    echo "$ANSWER" | sed 's/^/   /'
    
    # Check if answer indicates success or failure
    if echo "$ANSWER" | grep -iq "not.*mentioned\|not.*found\|not.*available\|no.*information"; then
        echo ""
        echo -e "${YELLOW}⚠ RAG could not find the information${NC}"
        echo "   This might indicate:"
        echo "   - OCR didn't extract the text"
        echo "   - Vector similarity threshold too high"
        echo "   - Text format not matching query"
    else
        echo ""
        echo -e "${GREEN}✓ RAG appears to have found relevant information${NC}"
    fi
fi
echo ""

# Summary
echo "================================================"
echo "   Summary"
echo "================================================"
echo ""

# Count issues
ISSUES=0

if ! command -v tesseract &> /dev/null; then
    echo -e "${RED}✗ Tesseract not installed${NC}"
    ((ISSUES++))
fi

if [ -z "$DOC_ID" ]; then
    echo -e "${RED}✗ Document upload failed${NC}"
    ((ISSUES++))
fi

if [ ! -f "backend.log" ] || [ "$OCR_LINES" -eq 0 ]; then
    echo -e "${YELLOW}⚠ No OCR activity detected${NC}"
    echo "   Check: ALWAYS_RUN_OCR = true in OCRService.java"
    ((ISSUES++))
fi

if [ -n "$CONTENT" ] && ! echo "$CONTENT" | grep -q "=== TEXT FROM IMAGES ==="; then
    echo -e "${YELLOW}⚠ No OCR text section in extracted content${NC}"
    ((ISSUES++))
fi

if [ "$ISSUES" -eq 0 ]; then
    echo -e "${GREEN}✓ All checks passed!${NC}"
    echo ""
    echo "OCR extraction appears to be working correctly."
else
    echo -e "${YELLOW}Found $ISSUES potential issue(s)${NC}"
    echo ""
    echo "Troubleshooting:"
    echo "1. Run: ./run/check-ocr-status.sh"
    echo "2. Check logs: tail -50 backend.log | grep OCR"
    echo "3. Verify ALWAYS_RUN_OCR = true in OCRService.java"
    echo "4. Restart application after code changes"
    echo "5. See: TEST_OCR_EXTRACTION.md for detailed guide"
fi

echo ""
echo "Document ID: $DOC_ID"
echo "View details: curl http://localhost:8080/api/documents/$DOC_ID | jq"
echo "Download PDF: curl http://localhost:8080/api/documents/$DOC_ID/download-converted-pdf -o converted.pdf"
echo ""
echo "================================================"
