# PDF Conversion Quick Reference

## 🚀 Quick Start

### Upload and Convert

```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@document.pdf"
```

### Download Converted PDF

```bash
curl -X GET http://localhost:8080/api/documents/{id}/download-converted-pdf \
  -o converted.pdf
```

### Delete Document

```bash
curl -X DELETE http://localhost:8080/api/documents/{id}
```

## 📋 Supported File Types

| Type | Extension | OCR | Notes |
|------|-----------|-----|-------|
| PDF (searchable) | .pdf | No | Copied as-is |
| PDF (scanned) | .pdf | Yes | OCR applied |
| Images | .jpg, .png, .gif | Yes | OCR + image layer |
| Word Docs | .docx | No | Text extracted |
| Text | .txt | No | Formatted to PDF |

## 🔧 Key Features

✅ Automatic conversion on upload  
✅ OCR for scanned documents  
✅ Searchable text layer  
✅ Download converted PDFs  
✅ Integrated with RAG pipeline  
✅ Fallback on errors

## 📊 API Response

```json
{
  "id": "uuid",
  "filename": "document.pdf",
  "isConverted": true,
  "convertedPdfDownloadUrl": "/api/documents/uuid/download-converted-pdf",
  "summary": "AI-generated summary..."
}
```

## ⚙️ Configuration

**Location:** `src/main/java/com/ai/rag_demo/service/PDFConversionService.java`

```java
DPI = 300                     // OCR resolution
FONT_SIZE = 12f               // Text PDF font size
LINE_HEIGHT = 14f             // Line spacing
MARGIN = 50f                  // Page margins
CONVERTED_FILES_DIR = "converted-pdfs"
```

## 🔍 Troubleshooting

### OCR Not Working

```bash
# Check Tesseract
tesseract --version

# Install if missing
brew install tesseract        # macOS
apt-get install tesseract-ocr # Ubuntu
```

### Check Logs

```bash
# View conversion activity
tail -f backend.log | grep "Converting"

# Check for errors
grep "ERROR" backend.log | grep -i "pdf\|ocr"
```

### Verify Conversion

```bash
# Upload
RESPONSE=$(curl -s -X POST http://localhost:8080/api/documents/upload \
  -F "file=@test.txt")

# Get document ID
DOC_ID=$(echo $RESPONSE | jq -r '.id')

# Download
curl -X GET http://localhost:8080/api/documents/$DOC_ID/download-converted-pdf \
  -o test-converted.pdf

# Verify text is searchable
pdftotext test-converted.pdf -
```

## 📈 Performance

| File Type | Processing Time |
|-----------|----------------|
| Text (100KB) | < 1 second |
| DOCX (1MB) | 1-2 seconds |
| Searchable PDF (5MB) | < 1 second |
| Scanned PDF (10 pages) | 20-50 seconds |
| Image (2MB) | 1-3 seconds |

## 🗂️ File Storage

**Location:** `converted-pdfs/` directory  
**Format:** `{documentId}-converted.pdf`  
**Cleanup:** Automatic on document deletion

```bash
# Check storage
du -sh converted-pdfs/

# List files
ls -lh converted-pdfs/

# Clean up all
rm -rf converted-pdfs/*
```

## 🔐 Security Notes

- File type validation included
- Safe file path handling
- Error handling prevents info leakage
- Recommend: Add size limits, rate limiting, malware scanning

## 📚 Documentation

- **[PDF_CONVERSION_FEATURE.md](PDF_CONVERSION_FEATURE.md)** - Full technical guide
- **[USAGE_EXAMPLE_PDF_CONVERSION.md](USAGE_EXAMPLE_PDF_CONVERSION.md)** - Usage examples
- **[PDF_CONVERSION_IMPLEMENTATION_SUMMARY.md](PDF_CONVERSION_IMPLEMENTATION_SUMMARY.md)** - Implementation details

## 🧪 Test Commands

### Basic Upload

```bash
echo "Test content" > test.txt
curl -X POST http://localhost:8080/api/documents/upload -F "file=@test.txt"
```

### Full Workflow

```bash
# 1. Upload
RESP=$(curl -s -X POST http://localhost:8080/api/documents/upload -F "file=@doc.pdf")
ID=$(echo $RESP | jq -r '.id')

# 2. Wait for processing
sleep 5

# 3. Download converted
curl -X GET http://localhost:8080/api/documents/$ID/download-converted-pdf -o converted.pdf

# 4. Query via RAG
curl -X POST http://localhost:8080/api/chat/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What is in the document?", "sessionId": "test"}'

# 5. Clean up
curl -X DELETE http://localhost:8080/api/documents/$ID
```

## 🎯 Common Use Cases

### 1. Digitize Paper Documents

```bash
# Scan to PDF, upload, get searchable version
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@scanned-receipt.pdf"
```

### 2. Process Screenshots

```bash
# Upload image, OCR applied, searchable PDF created
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@screenshot.png"
```

### 3. Convert Legacy Files

```bash
# Old text files → modern searchable PDFs
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@legacy-doc.txt"
```

## 💻 Frontend Integration

### React Example

```typescript
const uploadFile = async (file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  
  const response = await axios.post('/api/documents/upload', formData);
  
  if (response.data.isConverted) {
    // Download button
    const url = response.data.convertedPdfDownloadUrl;
    window.open(`http://localhost:8080${url}`, '_blank');
  }
};
```

### Python Example

```python
import requests

# Upload
files = {'file': open('document.pdf', 'rb')}
response = requests.post('http://localhost:8080/api/documents/upload', files=files)
doc = response.json()

# Download converted
if doc['isConverted']:
    pdf_url = f"http://localhost:8080{doc['convertedPdfDownloadUrl']}"
    pdf_response = requests.get(pdf_url)
    with open('converted.pdf', 'wb') as f:
        f.write(pdf_response.content)
```

## 🔄 RAG Integration

**Automatic Flow:**

```
Upload → Convert → Extract Text → Index in Vector DB → Ready for Q&A
```

**No additional steps needed** - RAG automatically uses converted PDF content!

## ✅ Checklist

Before using:

- [ ] Tesseract installed
- [ ] Services running (Elasticsearch, Kafka, Ollama)
- [ ] Backend started
- [ ] `converted-pdfs/` directory writable

For production:

- [ ] Add file size limits
- [ ] Implement rate limiting
- [ ] Configure monitoring
- [ ] Set up backup strategy
- [ ] Enable security features

## 🆘 Quick Fixes

### "Tesseract not found"

```bash
brew install tesseract  # macOS
```

### "Directory not writable"

```bash
chmod 755 converted-pdfs/
```

### "Conversion taking too long"

```bash
# Check if OCR is needed (slower)
# For faster processing, use already-searchable PDFs
```

### "Out of memory"

```bash
# Increase heap size
export JAVA_OPTS="-Xmx2g"
mvn spring-boot:run
```

## 📞 Support

- GitHub Issues: Report bugs
- Documentation: Check guides above
- Logs: `backend.log` for debugging

---

**Quick Links:**

- API Docs: [README.md](README.md#-api-endpoints)
- Full Guide: [PDF_CONVERSION_FEATURE.md](PDF_CONVERSION_FEATURE.md)
- Examples: [USAGE_EXAMPLE_PDF_CONVERSION.md](USAGE_EXAMPLE_PDF_CONVERSION.md)
