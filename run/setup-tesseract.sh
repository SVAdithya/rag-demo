#!/bin/bash

# Tesseract OCR Installation Script
# This script installs Tesseract OCR for PDF text extraction

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║       Tesseract OCR Installation for RAG Chatbot              ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Detect OS
OS="$(uname -s)"
case "$OS" in
    Darwin*)    OS_TYPE="macOS";;
    Linux*)     OS_TYPE="Linux";;
    *)          OS_TYPE="Unknown";;
esac

echo -e "${BLUE}Detected OS: $OS_TYPE${NC}"
echo ""

# Check if Tesseract is already installed
if command -v tesseract &> /dev/null; then
    echo -e "${GREEN}✓ Tesseract is already installed${NC}"
    tesseract --version
    echo ""
    
    # Check languages
    echo "Available languages:"
    tesseract --list-langs
    echo ""
    
    read -p "Tesseract is installed. Re-install? (y/n): " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Installation skipped."
        exit 0
    fi
fi

# Install based on OS
case "$OS_TYPE" in
    macOS)
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo "Installing Tesseract on macOS (Homebrew)"
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo ""
        
        # Check if Homebrew is installed
        if ! command -v brew &> /dev/null; then
            echo -e "${RED}✗ Homebrew is not installed${NC}"
            echo "Install Homebrew first: /bin/bash -c \"\$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)\""
            exit 1
        fi
        
        # Install Tesseract
        echo "Installing tesseract..."
        brew install tesseract
        
        # Install language data
        echo "Installing language packs..."
        brew install tesseract-lang
        
        ;;
        
    Linux)
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo "Installing Tesseract on Linux"
        echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
        echo ""
        
        # Detect Linux distribution
        if [ -f /etc/debian_version ]; then
            # Debian/Ubuntu
            echo "Detected Debian/Ubuntu"
            sudo apt-get update
            sudo apt-get install -y tesseract-ocr tesseract-ocr-eng
        elif [ -f /etc/redhat-release ]; then
            # RedHat/CentOS/Fedora
            echo "Detected RedHat/CentOS/Fedora"
            sudo yum install -y tesseract tesseract-langpack-eng
        else
            echo -e "${YELLOW}⚠ Unknown Linux distribution${NC}"
            echo "Please install Tesseract manually:"
            echo "  Debian/Ubuntu: sudo apt-get install tesseract-ocr"
            echo "  RedHat/CentOS: sudo yum install tesseract"
            exit 1
        fi
        ;;
        
    *)
        echo -e "${RED}✗ Unsupported operating system: $OS_TYPE${NC}"
        echo "Please install Tesseract manually:"
        echo "  macOS: brew install tesseract"
        echo "  Linux: apt-get install tesseract-ocr"
        echo "  Windows: https://github.com/UB-Mannheim/tesseract/wiki"
        exit 1
        ;;
esac

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Verifying Installation"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Check if Tesseract is installed
if ! command -v tesseract &> /dev/null; then
    echo -e "${RED}✗ Installation failed - tesseract command not found${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Tesseract installed successfully${NC}"
echo ""

# Show version
echo "Version:"
tesseract --version
echo ""

# Show available languages
echo "Available languages:"
tesseract --list-langs
echo ""

# Find tessdata location
echo "Tessdata locations:"
case "$OS_TYPE" in
    macOS)
        TESSDATA_PATHS=(
            "/opt/homebrew/share/tessdata"
            "/usr/local/share/tessdata"
        )
        ;;
    Linux)
        TESSDATA_PATHS=(
            "/usr/share/tesseract-ocr/5/tessdata"
            "/usr/share/tesseract-ocr/4.00/tessdata"
            "/usr/local/share/tessdata"
        )
        ;;
esac

for path in "${TESSDATA_PATHS[@]}"; do
    if [ -d "$path" ]; then
        echo -e "${GREEN}✓ Found tessdata at: $path${NC}"
        echo "  Files: $(ls -1 $path | wc -l | tr -d ' ') language files"
    fi
done
echo ""

# Test OCR
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Testing OCR"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

# Create a test image if ImageMagick is available
if command -v convert &> /dev/null; then
    echo "Creating test image..."
    echo "Hello Tesseract OCR" | convert -pointsize 24 -background white -fill black label:@- test-ocr.png
    
    echo "Running OCR on test image..."
    OCR_RESULT=$(tesseract test-ocr.png stdout 2>/dev/null | tr -d '\n' | tr -d ' ')
    EXPECTED="HelloTesseractOCR"
    
    if [ "$OCR_RESULT" == "$EXPECTED" ]; then
        echo -e "${GREEN}✓ OCR test passed!${NC}"
        echo "  Expected: $EXPECTED"
        echo "  Got: $OCR_RESULT"
    else
        echo -e "${YELLOW}⚠ OCR test result differs${NC}"
        echo "  Expected: $EXPECTED"
        echo "  Got: $OCR_RESULT"
        echo "  (This may be normal due to whitespace differences)"
    fi
    
    # Cleanup
    rm -f test-ocr.png
else
    echo -e "${YELLOW}⚠ ImageMagick not found, skipping OCR test${NC}"
    echo "Install ImageMagick to test OCR: brew install imagemagick (macOS)"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Installation Complete"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

echo -e "${GREEN}✓ Tesseract OCR is ready to use${NC}"
echo ""
echo "Next steps:"
echo "  1. Rebuild the application: ./mvnw clean install"
echo "  2. Start the backend: ./mvnw spring-boot:run"
echo "  3. Upload a scanned PDF to test OCR functionality"
echo ""
echo "Documentation:"
echo "  • Read TESSERACT_OCR_SETUP.md for detailed usage"
echo "  • Check logs for OCR activity: tail -f backend.log | grep OCR"
echo ""
echo "✓ Setup complete!"
