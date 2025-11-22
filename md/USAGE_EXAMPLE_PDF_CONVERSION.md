# PDF Conversion Feature - Usage Examples

## Quick Start

### 1. Upload a Document

Upload any supported file type and it will be automatically converted to a searchable PDF:

```bash
# Upload a text file
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@my-document.txt" \
  -H "Content-Type: multipart/form-data"

# Upload a scanned PDF
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@scanned-invoice.pdf" \
  -H "Content-Type: multipart/form-data"

# Upload an image with text
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@screenshot.png" \
  -H "Content-Type: multipart/form-data"

# Upload a Word document
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@report.docx" \
  -H "Content-Type: multipart/form-data"
```

### 2. Response

The API returns document information including conversion status:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "filename": "my-document.txt",
  "contentType": "text/plain",
  "size": 12345,
  "uploadedAt": "2025-11-22T15:30:00",
  "isConverted": true,
  "convertedPdfDownloadUrl": "/api/documents/550e8400-e29b-41d4-a716-446655440000/download-converted-pdf",
  "summary": "This document discusses artificial intelligence and machine learning..."
}
```

### 3. Download Converted PDF

Download the searchable PDF version of your uploaded document:

```bash
curl -X GET http://localhost:8080/api/documents/550e8400-e29b-41d4-a716-446655440000/download-converted-pdf \
  -o converted-document.pdf
```

### 4. Query the Document with RAG

Once uploaded, the document is automatically indexed and available for AI queries:

```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "What are the key points in the document I uploaded?"
  }'
```

## Frontend Integration

### Upload Component (React/TypeScript)

```typescript
import React, { useState } from 'react';
import axios from 'axios';

interface DocumentResponse {
  id: string;
  filename: string;
  contentType: string;
  size: number;
  uploadedAt: string;
  isConverted: boolean;
  convertedPdfDownloadUrl?: string;
  summary?: string;
}

const DocumentUploader: React.FC = () => {
  const [file, setFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [uploadedDoc, setUploadedDoc] = useState<DocumentResponse | null>(null);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setFile(e.target.files[0]);
    }
  };

  const handleUpload = async () => {
    if (!file) return;

    setUploading(true);
    const formData = new FormData();
    formData.append('file', file);

    try {
      const response = await axios.post<DocumentResponse>(
        'http://localhost:8080/api/documents/upload',
        formData,
        {
          headers: {
            'Content-Type': 'multipart/form-data',
          },
        }
      );

      setUploadedDoc(response.data);
      alert('Document uploaded and converted successfully!');
    } catch (error) {
      console.error('Upload failed:', error);
      alert('Failed to upload document');
    } finally {
      setUploading(false);
    }
  };

  const handleDownloadPDF = () => {
    if (!uploadedDoc || !uploadedDoc.convertedPdfDownloadUrl) return;

    window.open(
      `http://localhost:8080${uploadedDoc.convertedPdfDownloadUrl}`,
      '_blank'
    );
  };

  return (
    <div className="document-uploader">
      <h2>Upload Document</h2>
      
      <input
        type="file"
        onChange={handleFileChange}
        accept=".pdf,.docx,.txt,.jpg,.jpeg,.png"
      />
      
      <button
        onClick={handleUpload}
        disabled={!file || uploading}
      >
        {uploading ? 'Uploading...' : 'Upload'}
      </button>

      {uploadedDoc && (
        <div className="upload-result">
          <h3>Upload Successful!</h3>
          <p><strong>Filename:</strong> {uploadedDoc.filename}</p>
          <p><strong>Size:</strong> {(uploadedDoc.size / 1024).toFixed(2)} KB</p>
          <p><strong>Converted:</strong> {uploadedDoc.isConverted ? 'Yes' : 'No'}</p>
          
          {uploadedDoc.isConverted && (
            <button onClick={handleDownloadPDF}>
              Download Searchable PDF
            </button>
          )}

          {uploadedDoc.summary && (
            <div className="summary">
              <h4>AI Summary:</h4>
              <p>{uploadedDoc.summary}</p>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default DocumentUploader;
```

## Use Cases

### 1. Digitizing Paper Documents

**Scenario:** You have scanned invoices, receipts, or contracts.

```bash
# Upload scanned invoice
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@scanned-invoice-2025-001.pdf"

# Now you can query it
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "What is the total amount on the invoice?"
  }'

# Download searchable version for archival
curl -X GET http://localhost:8080/api/documents/{id}/download-converted-pdf \
  -o invoice-searchable.pdf
```

### 2. Processing Screenshots

**Scenario:** You have screenshots of documentation or error messages.

```bash
# Upload screenshot
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@error-screenshot.png"

# Query about the error
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "What error message is shown in the screenshot?"
  }'
```

### 3. Converting Legacy Documents

**Scenario:** Convert old text files or Word documents to searchable PDFs.

```bash
# Upload old document
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@legacy-report-1995.txt"

# Download modern PDF version
curl -X GET http://localhost:8080/api/documents/{id}/download-converted-pdf \
  -o legacy-report-1995.pdf
```

### 4. Building a Knowledge Base

**Scenario:** Create a searchable knowledge base from various document types.

```bash
# Upload multiple documents
curl -X POST http://localhost:8080/api/documents/upload -F "file=@manual.pdf"
curl -X POST http://localhost:8080/api/documents/upload -F "file=@guide.docx"
curl -X POST http://localhost:8080/api/documents/upload -F "file=@notes.txt"
curl -X POST http://localhost:8080/api/documents/upload -F "file=@diagram.png"

# Query across all documents
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "How do I configure the system according to the documentation?"
  }'

# List all documents
curl -X GET http://localhost:8080/api/documents
```

## Batch Processing Script

Process multiple files at once:

```bash
#!/bin/bash

# batch-upload.sh
# Usage: ./batch-upload.sh /path/to/documents/

UPLOAD_URL="http://localhost:8080/api/documents/upload"
DOCUMENTS_DIR="$1"

if [ -z "$DOCUMENTS_DIR" ]; then
  echo "Usage: $0 <documents_directory>"
  exit 1
fi

for file in "$DOCUMENTS_DIR"/*; do
  if [ -f "$file" ]; then
    echo "Uploading: $file"
    
    response=$(curl -s -X POST "$UPLOAD_URL" \
      -F "file=@$file" \
      -H "Content-Type: multipart/form-data")
    
    document_id=$(echo "$response" | jq -r '.id')
    is_converted=$(echo "$response" | jq -r '.isConverted')
    
    echo "  ✓ Uploaded with ID: $document_id"
    echo "  ✓ Converted: $is_converted"
    echo ""
    
    # Optional: Download converted PDF
    if [ "$is_converted" = "true" ]; then
      filename=$(basename "$file")
      output_file="${filename%.*}-converted.pdf"
      
      curl -s -X GET "http://localhost:8080/api/documents/$document_id/download-converted-pdf" \
        -o "converted/$output_file"
      
      echo "  ✓ Downloaded: converted/$output_file"
      echo ""
    fi
  fi
done

echo "Batch upload complete!"
```

## Python Integration

```python
import requests
import json
from pathlib import Path

class RAGDocumentClient:
    def __init__(self, base_url="http://localhost:8080"):
        self.base_url = base_url
    
    def upload_document(self, file_path):
        """Upload a document and get it converted to searchable PDF"""
        url = f"{self.base_url}/api/documents/upload"
        
        with open(file_path, 'rb') as f:
            files = {'file': f}
            response = requests.post(url, files=files)
            
        response.raise_for_status()
        return response.json()
    
    def download_converted_pdf(self, document_id, output_path):
        """Download the converted searchable PDF"""
        url = f"{self.base_url}/api/documents/{document_id}/download-converted-pdf"
        
        response = requests.get(url)
        response.raise_for_status()
        
        with open(output_path, 'wb') as f:
            f.write(response.content)
    
    def query_documents(self, question):
        """Query the RAG system about uploaded documents"""
        url = f"{self.base_url}/api/chat"
        
        payload = {"message": question}
        response = requests.post(url, json=payload)
        response.raise_for_status()
        
        return response.json()
    
    def list_documents(self):
        """List all uploaded documents"""
        url = f"{self.base_url}/api/documents"
        
        response = requests.get(url)
        response.raise_for_status()
        
        return response.json()

# Usage example
if __name__ == "__main__":
    client = RAGDocumentClient()
    
    # Upload a document
    print("Uploading document...")
    doc = client.upload_document("my-document.pdf")
    print(f"✓ Uploaded: {doc['filename']}")
    print(f"✓ Document ID: {doc['id']}")
    print(f"✓ Converted: {doc['isConverted']}")
    
    if doc['summary']:
        print(f"✓ Summary: {doc['summary']}")
    
    # Download converted PDF
    if doc['isConverted']:
        print("\nDownloading converted PDF...")
        client.download_converted_pdf(doc['id'], "converted-output.pdf")
        print("✓ Downloaded: converted-output.pdf")
    
    # Query the document
    print("\nQuerying document...")
    result = client.query_documents("What is this document about?")
    print(f"✓ Answer: {result['response']}")
    
    # List all documents
    print("\nListing all documents...")
    docs = client.list_documents()
    print(f"✓ Total documents: {len(docs)}")
    for doc in docs:
        print(f"  - {doc['filename']} (ID: {doc['id']})")
```

## Monitoring Conversion Status

Check logs for conversion progress:

```bash
# Follow application logs
tail -f backend.log

# Look for conversion messages
grep "Converting file to searchable PDF" backend.log

# Check for OCR processing
grep "OCR" backend.log

# Monitor PDF creation
grep "Created.*PDF" backend.log
```

## Performance Tips

### 1. For Large Batches

- Upload files in parallel (max 5-10 concurrent)
- Use async processing patterns
- Monitor system resources

### 2. For Large Files

- Consider splitting large PDFs before upload
- Increase heap size if needed: `-Xmx2g`
- Monitor memory usage

### 3. For Better OCR Quality

- Use high-resolution scans (300 DPI or higher)
- Ensure good contrast and lighting
- Use clean, uncluttered images

## Troubleshooting

### Upload Fails

```bash
# Check if service is running
curl http://localhost:8080/actuator/health

# Check disk space
df -h

# Check logs
tail -n 100 backend.log
```

### Conversion Takes Too Long

```bash
# Check if OCR is being used (slower)
grep "Running OCR" backend.log

# Monitor system resources
top

# Check Java heap usage
jstat -gc <pid>
```

### PDF Not Searchable

```bash
# Download the PDF
curl -X GET http://localhost:8080/api/documents/{id}/download-converted-pdf \
  -o test.pdf

# Check if text is present
pdftotext test.pdf - | head -n 20

# Verify OCR is installed
tesseract --version
```

## Best Practices

1. **File Naming**: Use descriptive filenames that will be meaningful in the RAG context
2. **File Organization**: Upload related documents together for better context
3. **Quality Control**: Review AI summaries to ensure conversion quality
4. **Backup**: Keep original files; converted PDFs are stored separately
5. **Cleanup**: Delete old documents when no longer needed to save storage

## Next Steps

- Explore the [PDF Conversion Feature Guide](PDF_CONVERSION_FEATURE.md) for technical details
- Check [RAG Implementation Guide](RAG_IMPLEMENTATION_COMPLETE.md) for RAG usage
- See [API Documentation](README.md#api-endpoints) for full API reference
