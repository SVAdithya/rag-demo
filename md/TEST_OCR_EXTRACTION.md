# Testing OCR Text Extraction - Verification Guide

## 🎯 Purpose

This guide helps you verify that the system is correctly extracting text from images in PDFs using OCR.

## ✅ Prerequisites

Before testing:

1. **Check Tesseract is installed:**
   ```bash
   ./check-ocr-status.sh
   ```
   Should show: ✓ Tesseract is installed

2. **Restart the application** to pick up the latest changes:
   ```bash
   # Stop if running
   pkill -f "spring-boot:run"
   
   # Rebuild
   mvn clean package -DskipTests
   
   # Start
   mvn spring-boot:run
   ```

## 🧪 Test Procedure

### Step 1: Upload Your PDF

```bash
# Upload the PDF with MO Number as image
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@your-file.pdf" \
  -o response.json

# Save the document ID
DOC_ID=$(cat response.json | jq -r '.id')
echo "Document ID: $DOC_ID"
```

### Step 2: Monitor Logs for OCR Activity

Open another terminal and watch the logs:

```bash
tail -f backend.log | grep -E "OCR|extracting|Processing PDF"
```

You should see output like:

```
Processing PDF: your-file.pdf
Running OCR on all pages to extract text from images...
Starting OCR on X pages...
Processing page 1/X
Processing page 2/X
...
OCR completed. Total extracted text: XXXX characters
Combined extraction: XXX characters from text, XXX from OCR, total: XXXX
```

### Step 3: Wait for Processing

Wait 10-30 seconds for the document to be processed via Kafka:

```bash
# Check Kafka processing
tail -f backend.log | grep "Processing document from Kafka"
```

### Step 4: Check Extracted Content

Retrieve the document and check what was extracted:

```bash
# Get document details
curl -X GET "http://localhost:8080/api/documents/$DOC_ID" | jq '.content' > extracted_content.txt

# View the content
cat extracted_content.txt

# Search for your MO Number
grep -i "MO\|consignment\|number" extracted_content.txt
```

You should see:

- `=== EXTRACTED TEXT ===` section (if there was regular text)
- `=== TEXT FROM IMAGES (OCR) ===` section (with your MO Number)

### Step 5: Query via RAG

Now test if the RAG system can find the information:

```bash
curl -X POST http://localhost:8080/api/chat/ask \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What is the MO Number or Consignment Number?",
    "sessionId": "test-mo-number"
  }' | jq '.answer'
```

Expected: Should return the MO Number from the image

### Step 6: Try Specific Queries

```bash
# Query 1: Direct question
curl -X POST http://localhost:8080/api/chat/ask \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What is the MO Number in the document?",
    "sessionId": "test-1"
  }' | jq '.answer'

# Query 2: Broader question
curl -X POST http://localhost:8080/api/chat/ask \
  -H "Content-Type: application/json" \
  -d '{
    "question": "List all numbers mentioned in the document",
    "sessionId": "test-2"
  }' | jq '.answer'

# Query 3: Context-based
curl -X POST http://localhost:8080/api/chat/ask \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What identifying numbers or codes are in this document?",
    "sessionId": "test-3"
  }' | jq '.answer'
```

## 🔍 Detailed Verification

### Check Vector Store Indexing

The extracted text should be indexed in Elasticsearch:

```bash
# Check if document is indexed
curl -X GET "http://localhost:9200/documents/_doc/$DOC_ID" | jq '.found'

# Search for MO Number in Elasticsearch
curl -X GET "http://localhost:9200/documents/_search" \
  -H "Content-Type: application/json" \
  -d '{
    "query": {
      "match": {
        "content": "MO"
      }
    }
  }' | jq '.hits.hits[]._source.filename'
```

### Check Converted PDF

Download and verify the converted PDF:

```bash
# Download converted PDF
curl -X GET "http://localhost:8080/api/documents/$DOC_ID/download-converted-pdf" \
  -o converted.pdf

# Extract text from converted PDF to verify
pdftotext converted.pdf -
```

You should see the MO Number in the extracted text.

### Check Application Logs in Detail

```bash
# Search for specific document processing
grep "$DOC_ID" backend.log

# Check OCR character count
grep "OCR completed" backend.log | tail -1

# Check combined extraction stats
grep "Combined extraction" backend.log | tail -1
```

## 🐛 Troubleshooting

### Issue: No OCR Activity in Logs

**Symptoms:**

- Logs don't show "Running OCR on all pages"
- Only see "Extracting PDF content"

**Solution:**

```bash
# 1. Check ALWAYS_RUN_OCR flag
grep "ALWAYS_RUN_OCR" src/main/java/com/ai/rag_demo/service/OCRService.java

# Should show: ALWAYS_RUN_OCR = true

# 2. Rebuild and restart
mvn clean compile
pkill -f "spring-boot:run"
mvn spring-boot:run
```

### Issue: OCR Runs But No Text Extracted

**Symptoms:**

- Logs show "OCR completed. Total extracted text: 0 characters"
- Or very few characters extracted

**Possible Causes & Solutions:**

1. **Tesseract Not Properly Configured:**
   ```bash
   ./check-ocr-status.sh
   
   # If issues, reinstall:
   brew reinstall tesseract  # macOS
   sudo apt-get install --reinstall tesseract-ocr tesseract-ocr-eng  # Ubuntu
   ```

2. **Image Quality Too Low:**
    - Check the image resolution in your PDF
    - Increase DPI in OCRService.java:
      ```java
      private static final int DPI = 400; // Increase from 300
      ```

3. **Image is Rotated:**
    - OCR works best on upright text
    - Try manually rotating the PDF first

4. **Complex Background:**
    - The contrast enhancement might not be enough
    - Try increasing contrast threshold in OCRService.java

### Issue: Text Extracted But RAG Can't Find It

**Symptoms:**

- Extracted content shows MO Number
- But queries return "not found"

**Solution:**

1. **Check Vector Indexing:**
   ```bash
   # Wait for Kafka processing
   sleep 20
   
   # Check Elasticsearch
   curl "http://localhost:9200/documents/_doc/$DOC_ID" | jq '._source.content'
   ```

2. **Try Different Query Phrasing:**
   ```bash
   # More specific
   curl -X POST http://localhost:8080/api/chat/ask \
     -H "Content-Type: application/json" \
     -d '{"question": "Find the text that contains MO or consignment", "sessionId": "test"}'
   
   # Broader
   curl -X POST http://localhost:8080/api/chat/ask \
     -H "Content-Type: application/json" \
     -d '{"question": "What information is available in this document?", "sessionId": "test"}'
   ```

3. **Check Similarity Threshold:**
    - Lower the similarity threshold in VectorStoreService.java:
      ```java
      .similarityThreshold(0.3)  // Lower from 0.5
      ```

### Issue: Partial Text Extracted

**Symptoms:**

- Some text extracted but MO Number specifically is missing
- OCR seems to work but misses certain parts

**Solutions:**

1. **Check Character Whitelist:**
   Edit OCRService.java to remove or expand whitelist:
   ```java
   // Option 1: Remove whitelist (allow all characters)
   // Comment out this line:
   // tesseract.setVariable("tessedit_char_whitelist", ...);
   
   // Option 2: Add specific characters you need
   tesseract.setVariable("tessedit_char_whitelist", 
       "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz" +
       "0123456789/-_" + // Add dashes, slashes, underscores
       ".,!?:;()[]{}@#$%&*+=/\\|'\" \n\t");
   ```

2. **Try Different Page Segmentation Mode:**
   ```java
   // In OCRService.java configureTesseract()
   tesseract.setPageSegMode(6);  // Try mode 6 (single uniform block)
   // Or
   tesseract.setPageSegMode(11); // Try mode 11 (sparse text)
   ```

3. **Increase OCR Quality:**
   ```java
   private static final int DPI = 400; // Higher quality
   ```

## 📊 Expected Outcomes

### Successful OCR Extraction

When working correctly, you should see:

1. **In Logs:**
   ```
   Processing PDF: your-file.pdf
   Running OCR on all pages to extract text from images...
   Starting OCR on 1 pages...
   Processing page 1/1
   Page 1 OCR completed. Extracted 234 characters
   OCR completed. Total extracted text: 234 characters
   Combined extraction: 100 characters from text, 234 from OCR, total: 334
   ```

2. **In Extracted Content:**
   ```json
   {
     "content": "=== EXTRACTED TEXT ===\n[any regular text]\n\n=== TEXT FROM IMAGES (OCR) ===\nMO Number: YOUR_NUMBER_HERE\n[other image text]"
   }
   ```

3. **In RAG Queries:**
   ```
   "The MO Number is: YOUR_NUMBER_HERE"
   ```

### Failed OCR Extraction

If OCR is not working:

1. **In Logs:**
   ```
   Tesseract OCR failed
   OCR completed. Total extracted text: 0 characters
   ```

2. **In Extracted Content:**
   ```json
   {
     "content": "=== EXTRACTED TEXT ===\n[only regular text, no image text]"
   }
   ```

3. **In RAG Queries:**
   ```
   "The MO Number is not mentioned in the provided context"
   ```

## 🔧 Advanced Debugging

### Enable Debug Logging

Add to `application.properties`:

```properties
logging.level.com.ai.rag_demo.service.OCRService=DEBUG
logging.level.com.ai.rag_demo.service.PDFConversionService=DEBUG
```

### Extract Images from PDF for Manual Inspection

```bash
# Install pdfimages if not available
brew install poppler  # macOS
sudo apt-get install poppler-utils  # Ubuntu

# Extract images
mkdir pdf-images
pdfimages -all your-file.pdf pdf-images/img

# View images
ls -lh pdf-images/
open pdf-images/  # macOS
```

### Test Tesseract Directly

```bash
# Extract an image and test Tesseract directly
tesseract pdf-images/img-000.png stdout

# If this doesn't work, Tesseract itself has issues
```

### Test with Simple Image

Create a test image with known text:

```bash
# Using ImageMagick
convert -size 600x100 xc:white \
  -font Arial -pointsize 36 -fill black \
  -draw "text 20,60 'MO Number: 12345'" \
  test-mo.png

# Upload as image
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@test-mo.png"
```

If this works but your PDF doesn't, the issue is with the PDF image quality or format.

## 📋 Verification Checklist

Before reporting issues, verify:

- [ ] Tesseract installed and working (`./check-ocr-status.sh`)
- [ ] Application restarted after code changes
- [ ] ALWAYS_RUN_OCR = true in OCRService.java
- [ ] Logs show "Running OCR on all pages"
- [ ] OCR completes with >0 characters extracted
- [ ] Extracted content includes "=== TEXT FROM IMAGES ===" section
- [ ] MO Number appears in extracted content
- [ ] Document is indexed in Elasticsearch
- [ ] Waited sufficient time for Kafka processing (20+ seconds)
- [ ] Tried multiple query variations

## 🎯 Quick Test Script

Save this as `test-ocr.sh`:

```bash
#!/bin/bash

echo "OCR Extraction Test"
echo "==================="

# Check Tesseract
echo -n "1. Tesseract: "
if command -v tesseract &> /dev/null; then
    echo "✓ Installed"
else
    echo "✗ Not installed"
    exit 1
fi

# Upload file
echo "2. Uploading file..."
RESPONSE=$(curl -s -X POST http://localhost:8080/api/documents/upload -F "file=@$1")
DOC_ID=$(echo $RESPONSE | jq -r '.id')
echo "   Document ID: $DOC_ID"

# Wait for processing
echo "3. Waiting for OCR processing..."
sleep 15

# Check extracted content
echo "4. Checking extracted content..."
CONTENT=$(curl -s "http://localhost:8080/api/documents/$DOC_ID" | jq -r '.content')

if echo "$CONTENT" | grep -q "=== TEXT FROM IMAGES ==="; then
    echo "   ✓ OCR section found"
    echo "   Sample: $(echo "$CONTENT" | grep -A5 "TEXT FROM IMAGES" | head -6)"
else
    echo "   ✗ No OCR section found"
fi

# Test query
echo "5. Testing RAG query..."
ANSWER=$(curl -s -X POST http://localhost:8080/api/chat/ask \
    -H "Content-Type: application/json" \
    -d "{\"question\": \"What is the MO Number?\", \"sessionId\": \"test\"}" | jq -r '.answer')
echo "   Answer: $ANSWER"

echo ""
echo "Test complete!"
```

Usage:

```bash
chmod +x test-ocr.sh
./test-ocr.sh your-file.pdf
```

## 📞 Getting Help

If OCR still isn't working after following this guide:

1. **Collect Debug Information:**
   ```bash
   # Run diagnostic
   ./check-ocr-status.sh > diagnostic-output.txt
   
   # Get recent logs
   tail -100 backend.log > recent-logs.txt
   
   # Get extracted content
   curl "http://localhost:8080/api/documents/$DOC_ID" > document-details.json
   ```

2. **Provide Details:**
    - PDF characteristics (size, number of pages, image resolution)
    - Sample of MO Number format
    - Diagnostic output
    - Recent logs showing OCR attempt
    - Extracted content (if any)

---

**With these tests, you can verify that OCR is working correctly and extracting text from images in your PDFs!**
