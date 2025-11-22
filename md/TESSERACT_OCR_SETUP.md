# Tesseract OCR Setup Guide

This guide explains how to set up and use Tesseract OCR for making scanned PDFs searchable before training the model.

## What This Does

The OCR (Optical Character Recognition) system:

✅ **Automatically detects** if a PDF is searchable or scanned  
✅ **Applies OCR** to scanned/image-based PDFs  
✅ **Extracts text** from images (JPG, PNG, etc.)  
✅ **Makes documents searchable** before indexing into the vector database  
✅ **Improves Q&A accuracy** for scanned documents

## How It Works

```
Upload PDF
    ↓
Is PDF searchable?
    ├─ Yes → Extract text directly (fast)
    └─ No  → Convert to images → Run OCR → Extract text (slower but accurate)
         ↓
Text content available
    ↓
Index into vector database
    ↓
Ready for Q&A
```

## Installation

### macOS

```bash
# Install Tesseract
brew install tesseract

# Verify installation
tesseract --version

# Install language data (English is included by default)
brew install tesseract-lang
```

### Linux (Ubuntu/Debian)

```bash
# Install Tesseract
sudo apt-get update
sudo apt-get install tesseract-ocr

# Install language packs
sudo apt-get install tesseract-ocr-eng

# Verify installation
tesseract --version
```

### Windows

1. Download installer from: https://github.com/UB-Mannheim/tesseract/wiki
2. Run the installer
3. Add to PATH: `C:\Program Files\Tesseract-OCR`
4. Verify: `tesseract --version`

## Configuration

### 1. Verify Tessdata Location

The application looks for Tesseract data in these locations (in order):

```
/usr/share/tesseract-ocr/5/tessdata    (Linux)
/usr/local/share/tessdata               (macOS/Linux)
/opt/homebrew/share/tessdata            (macOS Homebrew)
~/tessdata                              (User home)
./tessdata                              (Project root)
```

### 2. Check Installation

```bash
# Find tessdata location
tesseract --list-langs

# Output should show available languages:
# List of available languages (3):
# eng
# osd
```

### 3. Test OCR

Create a test script:

```bash
# Create test image with text
echo "Test OCR" | convert -pointsize 48 label:@- test.png

# Run OCR
tesseract test.png stdout

# Should output: "Test OCR"
```

## Using OCR in the Application

### Automatic OCR Processing

The system automatically uses OCR when needed:

**1. Upload a scanned PDF:**

```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@scanned-document.pdf"
```

**2. Upload an image (JPG, PNG):**

```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@document-scan.jpg"
```

**3. The system will:**

- Detect if the file needs OCR
- Process it automatically
- Extract searchable text
- Index into vector database

### Manual Testing

Test OCR functionality:

```java
// In your application or test
@Autowired
private OCRService ocrService;

// Check if Tesseract is available
boolean available = ocrService.isTesseractAvailable();
System.out.println("Tesseract available: " + available);

// Test OCR on an image
File imageFile = new File("test-image.png");
String text = ocrService.performOCROnImageFile(imageFile);
System.out.println("Extracted text: " + text);
```

## Features

### 1. Automatic Detection

The system automatically detects if a PDF needs OCR:

```java
// PDFs with < 100 characters per page → OCR needed
// PDFs with sufficient text → Use direct extraction (faster)
```

### 2. High-Quality OCR

- **DPI:** 300 (high resolution for accuracy)
- **Language:** English (configurable)
- **Engine:** LSTM neural network

### 3. Supported File Types

- ✅ PDF (searchable and scanned)
- ✅ JPG/JPEG
- ✅ PNG
- ✅ TIFF
- ✅ BMP

### 4. Progress Logging

Monitor OCR progress in logs:

```
INFO  - PDF is already searchable. Extracted 5000 characters
INFO  - PDF appears to be scanned/image-based. Running OCR...
INFO  - Starting OCR on 10 pages...
DEBUG - Processing page 1/10
DEBUG - Page 1 OCR completed. Extracted 450 characters
INFO  - OCR completed. Total extracted text: 4500 characters
```

## Performance

### Processing Time

| Document Type | Pages | Time | Notes |
|--------------|-------|------|-------|
| Searchable PDF | 10 | 1-2s | Direct text extraction |
| Scanned PDF | 10 | 30-60s | OCR needed |
| Image (1 page) | 1 | 3-5s | Single page OCR |
| High-res scan | 10 | 90-120s | 300+ DPI images |

### Optimization Tips

1. **Pre-process images:** Convert to grayscale for faster OCR
2. **Reduce DPI:** Lower DPI (150-200) for faster processing
3. **Batch processing:** OCR runs asynchronously via Kafka
4. **Cache results:** Extracted text is stored in Elasticsearch

## Troubleshooting

### Issue 1: "Tesseract is not available"

**Symptoms:**

```
WARN  - Tesseract is not available or not properly configured
```

**Solutions:**

```bash
# 1. Check installation
tesseract --version

# 2. Find tessdata location
find /usr -name "tessdata" 2>/dev/null

# 3. Set environment variable (if needed)
export TESSDATA_PREFIX=/usr/local/share/tessdata

# 4. Restart application
```

### Issue 2: Poor OCR Quality

**Symptoms:**

- Gibberish text extracted
- Missing characters
- Low accuracy

**Solutions:**

1. **Increase image quality:**

```java
// In OCRService.java, increase DPI
private static final int DPI = 400; // Change from 300 to 400
```

2. **Pre-process images:**

- Convert to grayscale
- Increase contrast
- Remove noise

3. **Use language-specific training data:**

```bash
# Install language pack
brew install tesseract-lang

# Configure in OCRService
tesseract.setLanguage("eng+spa"); // English + Spanish
```

### Issue 3: Slow Processing

**Symptoms:**

- OCR takes too long
- System runs out of memory

**Solutions:**

1. **Reduce DPI:**

```java
private static final int DPI = 150; // Lower DPI
```

2. **Process fewer pages at once:**

```java
// Process in batches
for (int page = 0; page < totalPages; page += 5) {
    // Process 5 pages at a time
}
```

3. **Increase memory:**

```bash
# Start with more memory
./mvnw spring-boot:run -Xmx4g
```

### Issue 4: Language Not Supported

**Symptoms:**

```
Error: Language 'fra' not found
```

**Solutions:**

```bash
# Download language data
# macOS
brew install tesseract-lang

# Linux
sudo apt-get install tesseract-ocr-fra tesseract-ocr-deu

# List installed languages
tesseract --list-langs
```

## API Usage Examples

### Test OCR Endpoint

```bash
# Upload scanned PDF
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@scanned-document.pdf" \
  -H "Content-Type: multipart/form-data"

# Response includes extracted text
{
  "id": "doc-123",
  "filename": "scanned-document.pdf",
  "content": "Extracted text from OCR...",
  "contentType": "application/pdf"
}
```

### Check OCR Status

```bash
# Check if Tesseract is configured
curl http://localhost:8080/actuator/health

# View logs
tail -f backend.log | grep OCR
```

## Advanced Configuration

### Custom Language Models

```java
@Service
public class OCRService {
    
    private void configureTesseract() {
        tesseract.setLanguage("eng+fra+deu"); // Multiple languages
        tesseract.setPageSegMode(3); // Fully automatic
        tesseract.setOcrEngineMode(1); // LSTM only
    }
}
```

### Custom Processing

```java
// Preprocess image before OCR
private BufferedImage preprocessImage(BufferedImage original) {
    // Convert to grayscale
    BufferedImage grayscale = new BufferedImage(
        original.getWidth(), 
        original.getHeight(), 
        BufferedImage.TYPE_BYTE_GRAY
    );
    Graphics2D g = grayscale.createGraphics();
    g.drawImage(original, 0, 0, null);
    g.dispose();
    return grayscale;
}
```

### OCR Quality Assessment

```java
// Check OCR confidence
public class OCRResult {
    private String text;
    private double confidence;
    private boolean needsManualReview;
}
```

## Integration with RAG System

### Complete Flow

```
1. User uploads scanned PDF
   ↓
2. OCRService detects it needs OCR
   ↓
3. Render PDF pages to images (300 DPI)
   ↓
4. Run Tesseract OCR on each page
   ↓
5. Extract text and combine pages
   ↓
6. Save to Elasticsearch
   ↓
7. VectorStoreService chunks the text
   ↓
8. Generate embeddings
   ↓
9. Index into vector database
   ↓
10. Document ready for Q&A!
```

### Performance Metrics

Monitor OCR performance:

```bash
# Check processing times
grep "OCR completed" backend.log

# Example output:
INFO  - OCR completed. Total extracted text: 4500 characters (took 45s)
```

## Best Practices

1. ✅ **Upload high-quality scans** (300 DPI minimum)
2. ✅ **Use clean, well-lit documents**
3. ✅ **Avoid skewed or rotated pages**
4. ✅ **Pre-process images** for better accuracy
5. ✅ **Monitor logs** for OCR performance
6. ✅ **Test with sample documents** before production

## Summary

**Installation:**

```bash
# macOS
brew install tesseract

# Linux
apt-get install tesseract-ocr

# Verify
tesseract --version
```

**Usage:**

- Upload any PDF → System automatically detects and applies OCR if needed
- No manual intervention required
- Extracted text is searchable via Q&A

**Benefits:**

- ✅ Handles scanned documents
- ✅ Processes images (JPG, PNG)
- ✅ Automatic detection
- ✅ High accuracy (300 DPI)
- ✅ Integrated with RAG pipeline

Your RAG system now supports both searchable and scanned PDFs! 🎉
