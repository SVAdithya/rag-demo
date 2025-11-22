#!/bin/bash

# OCR Status Check Script
# This script helps diagnose OCR configuration issues

echo "================================================"
echo "   RAG Demo - OCR Status Check"
echo "================================================"
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check Tesseract installation
echo "1. Checking Tesseract Installation..."
if command -v tesseract &> /dev/null; then
    echo -e "${GREEN}✓${NC} Tesseract is installed"
    tesseract --version | head -1
else
    echo -e "${RED}✗${NC} Tesseract is NOT installed"
    echo "   Install with:"
    echo "   - macOS: brew install tesseract"
    echo "   - Ubuntu: sudo apt-get install tesseract-ocr"
    exit 1
fi
echo ""

# Check Tesseract languages
echo "2. Checking Tesseract Languages..."
if tesseract --list-langs &> /dev/null; then
    LANGS=$(tesseract --list-langs 2>&1 | grep -v "List of available languages")
    if echo "$LANGS" | grep -q "eng"; then
        echo -e "${GREEN}✓${NC} English language data is available"
    else
        echo -e "${RED}✗${NC} English language data is missing"
        echo "   Install with: brew install tesseract-lang (macOS)"
    fi
    echo "   Available languages:"
    echo "$LANGS" | head -10
else
    echo -e "${YELLOW}⚠${NC} Could not list languages"
fi
echo ""

# Check tessdata directory
echo "3. Checking Tessdata Directory..."
TESSDATA_PATHS=(
    "/usr/share/tesseract-ocr/5/tessdata"
    "/usr/local/share/tessdata"
    "/opt/homebrew/share/tessdata"
    "$HOME/tessdata"
    "./tessdata"
)

FOUND_TESSDATA=false
for path in "${TESSDATA_PATHS[@]}"; do
    if [ -d "$path" ]; then
        echo -e "${GREEN}✓${NC} Found tessdata at: $path"
        if [ -f "$path/eng.traineddata" ]; then
            echo "   - eng.traineddata: Present"
            FOUND_TESSDATA=true
        else
            echo -e "   ${RED}✗${NC} eng.traineddata: Missing"
        fi
        break
    fi
done

if [ "$FOUND_TESSDATA" = false ]; then
    echo -e "${RED}✗${NC} No valid tessdata directory found"
    echo "   Check installation or set TESSDATA_PREFIX environment variable"
fi
echo ""

# Check Java installation
echo "4. Checking Java Installation..."
if command -v java &> /dev/null; then
    echo -e "${GREEN}✓${NC} Java is installed"
    java -version 2>&1 | head -1
    
    # Check Java version
    JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 17 ]; then
        echo -e "${GREEN}✓${NC} Java version is sufficient (17+)"
    else
        echo -e "${YELLOW}⚠${NC} Java version might be too old (need 17+)"
    fi
else
    echo -e "${RED}✗${NC} Java is NOT installed"
    exit 1
fi
echo ""

# Check if application is running
echo "5. Checking Application Status..."
if curl -s http://localhost:8080/actuator/health &> /dev/null; then
    echo -e "${GREEN}✓${NC} Application is running on port 8080"
elif lsof -i :8080 &> /dev/null; then
    echo -e "${YELLOW}⚠${NC} Port 8080 is in use but health check failed"
else
    echo -e "${YELLOW}⚠${NC} Application does not appear to be running"
    echo "   Start with: mvn spring-boot:run"
fi
echo ""

# Check recent OCR activity in logs
echo "6. Checking Recent OCR Activity..."
if [ -f "backend.log" ]; then
    echo -e "${GREEN}✓${NC} Log file found: backend.log"
    
    OCR_INIT=$(grep "Tesseract OCR initialized" backend.log | tail -1)
    if [ -n "$OCR_INIT" ]; then
        echo -e "${GREEN}✓${NC} Tesseract initialized in application"
    else
        echo -e "${RED}✗${NC} No Tesseract initialization found in logs"
    fi
    
    OCR_COUNT=$(grep -c "Starting OCR" backend.log 2>/dev/null || echo "0")
    echo "   Total OCR operations: $OCR_COUNT"
    
    if [ "$OCR_COUNT" -gt 0 ]; then
        RECENT_OCR=$(grep "Starting OCR" backend.log | tail -1)
        echo "   Most recent: $(echo $RECENT_OCR | cut -d' ' -f1-3)"
        
        SUCCESS_COUNT=$(grep -c "OCR completed" backend.log 2>/dev/null || echo "0")
        FAIL_COUNT=$(grep -c "OCR failed" backend.log 2>/dev/null || echo "0")
        echo "   Successful: $SUCCESS_COUNT, Failed: $FAIL_COUNT"
    fi
else
    echo -e "${YELLOW}⚠${NC} Log file not found: backend.log"
    echo "   Start application to generate logs"
fi
echo ""

# Check converted-pdfs directory
echo "7. Checking Converted PDFs Directory..."
if [ -d "converted-pdfs" ]; then
    PDF_COUNT=$(ls -1 converted-pdfs/*.pdf 2>/dev/null | wc -l)
    echo -e "${GREEN}✓${NC} Directory exists: converted-pdfs/"
    echo "   Converted PDFs: $PDF_COUNT"
    
    if [ "$PDF_COUNT" -gt 0 ]; then
        TOTAL_SIZE=$(du -sh converted-pdfs/ 2>/dev/null | cut -f1)
        echo "   Total size: $TOTAL_SIZE"
    fi
else
    echo -e "${YELLOW}⚠${NC} Directory does not exist: converted-pdfs/"
    echo "   Will be created on first document upload"
fi
echo ""

# Test OCR functionality
echo "8. Testing OCR Functionality..."
echo "   Creating test image..."

# Create a simple test image with ImageMagick if available
if command -v convert &> /dev/null; then
    convert -size 300x100 xc:white \
        -font Arial -pointsize 24 -fill black \
        -draw "text 20,50 'Test OCR Text'" \
        /tmp/test-ocr.png 2>/dev/null
    
    if [ -f "/tmp/test-ocr.png" ]; then
        echo -e "${GREEN}✓${NC} Test image created: /tmp/test-ocr.png"
        
        # Try OCR on test image
        OCR_OUTPUT=$(tesseract /tmp/test-ocr.png stdout 2>/dev/null)
        if echo "$OCR_OUTPUT" | grep -q "Test OCR Text"; then
            echo -e "${GREEN}✓${NC} OCR test successful!"
            echo "   Extracted: $OCR_OUTPUT"
        else
            echo -e "${YELLOW}⚠${NC} OCR test completed but text not recognized"
            echo "   Extracted: $OCR_OUTPUT"
        fi
        
        rm /tmp/test-ocr.png
    else
        echo -e "${YELLOW}⚠${NC} Could not create test image"
    fi
else
    echo -e "${YELLOW}⚠${NC} ImageMagick not available for testing"
    echo "   Install with: brew install imagemagick (macOS)"
fi
echo ""

# Summary and recommendations
echo "================================================"
echo "   Summary"
echo "================================================"
echo ""

ISSUES=0

if ! command -v tesseract &> /dev/null; then
    echo -e "${RED}✗ CRITICAL:${NC} Tesseract is not installed"
    ((ISSUES++))
fi

if [ "$FOUND_TESSDATA" = false ]; then
    echo -e "${RED}✗ CRITICAL:${NC} Tessdata directory not found"
    ((ISSUES++))
fi

if [ "$ISSUES" -eq 0 ]; then
    echo -e "${GREEN}✓ All checks passed!${NC}"
    echo ""
    echo "Your OCR setup appears to be working correctly."
    echo ""
    echo "Next steps:"
    echo "1. Upload a PDF with images: curl -X POST http://localhost:8080/api/documents/upload -F 'file=@your-pdf.pdf'"
    echo "2. Check logs: tail -f backend.log | grep OCR"
    echo "3. Query the content: curl -X POST http://localhost:8080/api/chat/ask -H 'Content-Type: application/json' -d '{\"question\":\"...\", \"sessionId\":\"test\"}'"
else
    echo -e "${RED}Found $ISSUES critical issue(s)${NC}"
    echo ""
    echo "Please fix the issues above before using OCR functionality."
fi

echo ""
echo "================================================"
echo "For more help, see: IMAGE_TEXT_EXTRACTION_GUIDE.md"
echo "================================================"
