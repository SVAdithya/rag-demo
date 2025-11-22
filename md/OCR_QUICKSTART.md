# OCR Quick Start - Convert Scanned PDFs to Searchable Documents

## TL;DR - 3 Steps

```bash
# 1. Install Tesseract OCR
./setup-tesseract.sh

# 2. Rebuild application  
./mvnw clean install

# 3. Restart backend
pkill -f spring-boot && ./mvnw spring-boot:run
```

**That's it!** Now upload any scanned PDF and the system will automatically extract text using OCR.

## What Changed

✅ **Added Tesseract OCR** - Automatic text extraction from scanned PDFs and images  
✅ **Auto-detection** - System automatically detects if PDF needs OCR  
✅ **High accuracy** - 300 DPI rendering for better text recognition  
✅ **Supports images** - JPG, PNG, TIFF files now work  
✅ **Seamless integration** - No changes needed to your upload process

## How It Works

### Before OCR

```
Upload scanned PDF → ❌ No text extracted → ❌ Can't answer questions
```

### With OCR (Now)

```
Upload scanned PDF → ✅ OCR extracts text → ✅ Index into vector DB → ✅ Can answer questions!
```

## Testing

### 1. Upload a scanned PDF

```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@scanned-document.pdf"
```

### 2. Check logs for OCR activity

```bash
tail -f backend.log | grep OCR

# You'll see:
# INFO - PDF is already searchable. Extracted 5000 characters
# OR
# INFO - PDF appears to be scanned. Running OCR...
# INFO - Starting OCR on 10 pages...
# INFO - OCR completed. Total extracted text: 4500 characters
```

### 3. Ask questions about the document

The scanned PDF is now searchable via Q&A!

## Performance

| Document Type | Processing Time |
|--------------|----------------|
| Searchable PDF (10 pages) | 1-2 seconds |
| Scanned PDF (10 pages) | 30-60 seconds |
| Image file (1 page) | 3-5 seconds |

## Supported Formats

- ✅ PDF (searchable and scanned)
- ✅ JPG/JPEG images
- ✅ PNG images
- ✅ TIFF images
- ✅ DOCX (no change, still works)
- ✅ TXT (no change, still works)

## Configuration

OCR settings are in `application.properties`:

```properties
ocr.enabled=true          # Enable/disable OCR
ocr.language=eng          # Language (eng, fra, deu, etc.)
ocr.dpi=300              # Image quality (higher = better but slower)
ocr.min-text-length=100  # Threshold for searchable PDFs
```

## Troubleshooting

### "Tesseract not found"

```bash
# Install Tesseract
brew install tesseract  # macOS
# OR
sudo apt-get install tesseract-ocr  # Linux

# Verify
tesseract --version
```

### OCR too slow?

Reduce DPI in `OCRService.java`:

```java
private static final int DPI = 200; // Change from 300
```

### Poor text quality?

Increase DPI for better accuracy:

```java
private static final int DPI = 400; // Higher quality
```

## Code Changes Summary

### New Files Created

- `src/main/java/com/ai/rag_demo/service/OCRService.java` - OCR functionality
- `TESSERACT_OCR_SETUP.md` - Detailed setup guide
- `setup-tesseract.sh` - Automated installation script

### Modified Files

- `pom.xml` - Added Tess4J and PDFBox Tools dependencies
- `DocumentService.java` - Integrated OCR for PDF and image processing
- `application.properties` - Added OCR configuration
- `docker-compose.yml` - Added Tesseract installation notes

## Architecture

```
Upload PDF/Image
      ↓
DocumentService.extractContent()
      ↓
Is it a PDF? → Yes → OCRService.extractTextFromPDF()
                           ↓
                     Is PDF searchable?
                           ├─ Yes → Direct text extraction (fast)
                           └─ No  → Render to images → OCR → Extract text
      ↓
Is it an image? → Yes → OCRService.performOCROnImage()
      ↓
Text content ready
      ↓
Save to Elasticsearch
      ↓
VectorStoreService indexes chunks
      ↓
Ready for Q&A!
```

## API Examples

### Upload scanned PDF

```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@scanned-invoice.pdf"
```

### Upload image with text

```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@screenshot.png"
```

### Check if document was OCR'd

```bash
# View logs
grep "OCR" backend.log

# Get document
curl http://localhost:8080/api/documents/{id} | jq '.content'
```

## Benefits

1. **Handle any PDF** - Searchable or scanned, both work
2. **Process images** - Screenshots, photos of documents
3. **Improve accuracy** - Extract text that was previously inaccessible
4. **Automatic** - No manual intervention needed
5. **Integrated** - Works seamlessly with existing RAG pipeline

## Next Steps

For more details, see:

- **TESSERACT_OCR_SETUP.md** - Complete setup and troubleshooting guide
- **SPEED_UP_MISTRAL.md** - Optimize Q&A performance
- **TROUBLESHOOTING_QA.md** - Fix common issues

## Summary

**Before:** Only searchable PDFs worked  
**Now:** All PDFs and images work with automatic OCR

Upload any document → System extracts text → Index and search → Answer questions! 🎉
