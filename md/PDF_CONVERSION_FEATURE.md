# PDF Conversion Feature

## Overview

This feature automatically converts all uploaded files to searchable PDFs on upload. The RAG (Retrieval-Augmented
Generation) process then uses the converted, searchable PDF for all text extraction and processing.

## Key Benefits

1. **Consistent Format**: All documents are stored in a standardized, searchable PDF format
2. **OCR Support**: Scanned PDFs and images are automatically processed with OCR to make them searchable
3. **Better RAG Results**: Searchable PDFs provide cleaner, more accurate text extraction for vector embeddings
4. **Download Capability**: Users can download the converted searchable PDF version of their documents

## How It Works

### Upload Flow

```
1. User uploads file (PDF, image, DOCX, TXT, etc.)
   ↓
2. PDFConversionService converts file to searchable PDF
   ↓
3. Text is extracted from converted PDF
   ↓
4. Document saved with both original metadata and converted PDF path
   ↓
5. Kafka message sent for RAG processing
   ↓
6. RAG process uses the extracted text from converted PDF
   ↓
7. Document indexed in vector store & summary generated
```

### Supported File Types

- **PDF files**:
    - Already searchable PDFs are kept as-is
    - Scanned/image-based PDFs are processed with OCR to create searchable version

- **Image files** (JPG, PNG, etc.):
    - OCR is performed to extract text
    - New searchable PDF created with image and invisible OCR text layer

- **DOCX files**:
    - Text extracted and converted to PDF format

- **Text files**:
    - Content wrapped and formatted into PDF

- **Other formats**:
    - Best-effort conversion or placeholder PDF created

## API Endpoints

### Upload Document

```http
POST /api/documents/upload
Content-Type: multipart/form-data

Parameters:
- file: MultipartFile (the document to upload)

Response:
{
  "id": "document-uuid",
  "filename": "original-filename.ext",
  "contentType": "application/pdf",
  "size": 12345,
  "uploadedAt": "2025-11-22T14:30:00",
  "isConverted": true,
  "convertedPdfDownloadUrl": "/api/documents/{id}/download-converted-pdf",
  "summary": "AI-generated summary..."
}
```

### Download Converted PDF

```http
GET /api/documents/{id}/download-converted-pdf

Response:
- Content-Type: application/pdf
- Content-Disposition: attachment; filename="document.pdf"
- Body: PDF file binary data
```

### Delete Document

```http
DELETE /api/documents/{id}

Response: 200 OK
```

This endpoint deletes both the document metadata and the converted PDF file.

## Architecture

### New Components

#### 1. PDFConversionService

Located at: `src/main/java/com/ai/rag_demo/service/PDFConversionService.java`

**Key Methods:**

- `convertToSearchablePDF()`: Main conversion method
- `extractTextFromConvertedPDF()`: Extract text from converted PDF
- `deleteConvertedPDF()`: Clean up converted files

**Conversion Strategies:**

- PDF → Searchable PDF (with OCR if needed)
- Image → Searchable PDF (with OCR)
- DOCX → PDF (text extraction)
- Text → PDF (formatted)
- Generic → PDF (best effort)

#### 2. Enhanced DocumentModel

Added fields:

- `convertedPdfPath`: Path to the converted PDF file
- `isConverted`: Boolean flag indicating successful conversion

#### 3. Enhanced DocumentService

- Integrates PDFConversionService
- Extracts content from converted PDFs
- Manages converted file lifecycle

#### 4. Updated DocumentController

- New endpoint for downloading converted PDFs
- Delete endpoint removes both metadata and converted files
- Response includes conversion status

## Storage

Converted PDF files are stored in the `converted-pdfs/` directory at the application root.

**File naming:** `{documentId}-converted.pdf`

**Directory structure:**

```
rag-demo/
├── converted-pdfs/
│   ├── uuid-1-converted.pdf
│   ├── uuid-2-converted.pdf
│   └── uuid-3-converted.pdf
├── src/
└── ...
```

The `converted-pdfs/` directory is automatically created if it doesn't exist and is excluded from version control via
`.gitignore`.

## Configuration

### OCR Configuration

The OCR functionality uses Tesseract, configured in `OCRService.java`. Tesseract looks for data files in these
locations (in order):

1. `/usr/share/tesseract-ocr/5/tessdata`
2. `/usr/local/share/tessdata`
3. `/opt/homebrew/share/tessdata`
4. `~/tessdata`
5. `./tessdata`

### PDF Conversion Settings

Configurable parameters in `PDFConversionService.java`:

- `DPI`: 300 (resolution for rendering PDF pages to images for OCR)
- `FONT_SIZE`: 12f (font size for text PDFs)
- `LINE_HEIGHT`: 14f (line spacing for text PDFs)
- `MARGIN`: 50f (page margins for text PDFs)

## RAG Integration

The converted PDF is fully integrated into the RAG pipeline:

1. **Text Extraction**: Content is extracted from the converted, searchable PDF
2. **Vector Indexing**: The extracted text is chunked and indexed into Elasticsearch
3. **Summarization**: AI generates a summary from the extracted text
4. **Search**: Vector similarity search uses the indexed content from converted PDFs

### Flow in DocumentProcessingListener

```java
@KafkaListener(topics = DOCUMENT_PROCESSING_TOPIC)
public void processDocument(String documentId) {
    DocumentModel doc = documentService.getDocumentById(documentId);
    
    // Content already extracted from converted PDF
    String content = doc.getContent();
    
    // Index into vector store for RAG
    vectorStoreService.indexDocument(documentId, doc.getFilename(), content);
    
    // Generate AI summary
    String summary = aiService.summarizeDocument(content);
    
    // Update document
    documentService.updateDocumentSummary(documentId, summary);
}
```

## Error Handling

If PDF conversion fails:

1. Error is logged
2. System falls back to direct content extraction from original file
3. Document is still saved and processed
4. `isConverted` flag is set to `false`
5. `convertedPdfPath` is `null`

This ensures the system remains resilient even if conversion fails.

## Testing

### Test Image Upload with OCR

```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@/path/to/scanned-document.jpg" \
  -H "Content-Type: multipart/form-data"
```

### Test Scanned PDF Upload

```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@/path/to/scanned-document.pdf" \
  -H "Content-Type: multipart/form-data"
```

### Download Converted PDF

```bash
curl -X GET http://localhost:8080/api/documents/{document-id}/download-converted-pdf \
  -o converted-document.pdf
```

### Verify RAG with Converted Content

```bash
# Upload a document
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@sample.pdf"

# Query the chatbot about the document content
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "What is this document about?"}'
```

## Dependencies

Already included in `pom.xml`:

- `org.apache.pdfbox:pdfbox:2.0.29` - PDF manipulation
- `org.apache.pdfbox:pdfbox-tools:2.0.29` - PDF rendering
- `net.sourceforge.tess4j:tess4j:5.9.0` - OCR via Tesseract
- `org.apache.poi:poi-ooxml:5.2.3` - DOCX processing

## Performance Considerations

### Conversion Time

- **Text files**: <1 second
- **DOCX files**: 1-2 seconds
- **Searchable PDFs**: <1 second (copy only)
- **Scanned PDFs with OCR**: 2-5 seconds per page
- **Images with OCR**: 1-3 seconds per image

### Storage

- Converted PDFs are stored as separate files
- Storage overhead: Approximately same size as original or smaller
- Clean up: Converted PDFs are deleted when documents are deleted

### Async Processing

The conversion happens synchronously during upload, but the RAG processing (indexing and summarization) happens
asynchronously via Kafka, so the upload response is fast.

## Future Enhancements

Potential improvements:

1. **Async Conversion**: Move PDF conversion to background task for very large files
2. **Cloud Storage**: Store converted PDFs in S3/Cloud Storage instead of local filesystem
3. **Advanced OCR**: Support for multiple languages, handwriting recognition
4. **PDF Optimization**: Compress converted PDFs to reduce storage
5. **Batch Conversion**: Convert multiple documents in parallel
6. **Progress Tracking**: Real-time conversion progress updates via WebSocket

## Troubleshooting

### OCR Not Working

**Problem**: "Tesseract is not available or not properly configured"

**Solution**:

1. Install Tesseract:
   ```bash
   # macOS
   brew install tesseract
   
   # Ubuntu/Debian
   sudo apt-get install tesseract-ocr
   ```

2. Verify installation:
   ```bash
   tesseract --version
   ```

3. Check tessdata location:
   ```bash
   ls /usr/local/share/tessdata/eng.traineddata
   ```

### Conversion Fails for Certain Files

**Problem**: Some files fail to convert

**Solution**:

- Check logs for specific error
- Verify file is not corrupted
- System falls back to direct extraction automatically
- File is still processed for RAG

### Converted PDFs Not Searchable

**Problem**: Converted PDFs don't contain searchable text

**Solution**:

1. Verify OCR is working (see above)
2. Check DPI setting (increase for better OCR accuracy)
3. Verify original document has sufficient quality

### Storage Space Issues

**Problem**: `converted-pdfs/` directory growing too large

**Solution**:

1. Implement cleanup job for old documents
2. Move to cloud storage
3. Compress PDFs after conversion
4. Delete unused documents via API

## Summary

The PDF conversion feature ensures all documents in the RAG system are in a consistent, searchable format. This improves
the quality of text extraction, vector embeddings, and ultimately the accuracy of AI-generated responses. The feature is
transparent to users and handles various file types with appropriate conversion strategies.
