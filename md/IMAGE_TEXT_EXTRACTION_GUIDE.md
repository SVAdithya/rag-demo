# Image Text Extraction Guide - Enhanced OCR

## 🎯 What Was Fixed

The system now **always runs OCR on all PDF pages** to extract text from images, even if the PDF already has regular
text. This ensures that:

✅ Text from charts, diagrams, and infographics is captured  
✅ Screenshots and embedded images are processed  
✅ Mixed PDFs (text + images) are fully indexed  
✅ Image preprocessing improves OCR accuracy

## 🔧 Key Enhancements

### 1. Always-On OCR Mode

**Previous Behavior:**

- Only ran OCR if PDF appeared to be "scanned"
- Skipped OCR for PDFs with regular text
- Missed text in images within "searchable" PDFs

**New Behavior:**

```java
ALWAYS_RUN_OCR = true  // Always process images
```

- OCR runs on **every page** of every PDF
- Extracts both regular text AND text from images
- Combines results for complete content extraction

### 2. Image Preprocessing

**Enhancements for Better OCR:**

1. **High-Quality Rendering**
    - Bicubic interpolation
    - Anti-aliasing enabled
    - Quality rendering hints

2. **Contrast Enhancement**
    - Adaptive contrast stretching
    - Grayscale optimization
    - Better text detection

3. **Optimized Tesseract Settings**
    - Page segmentation mode 3 (fully automatic)
    - Character whitelist for better accuracy
    - LSTM neural network engine

### 3. Combined Text Extraction

The system now provides:

```
=== EXTRACTED TEXT ===
[Regular text from PDF]

=== TEXT FROM IMAGES (OCR) ===
[Text extracted from images via OCR]
```

Both sections are indexed and searchable via RAG.

## 📊 Testing Your PDFs

### Quick Test

1. **Upload a PDF with images containing text:**

```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@pdf-with-images.pdf"
```

2. **Check the logs to see OCR in action:**

```bash
tail -f backend.log | grep -E "OCR|extracting"
```

You should see:

```
Running OCR on all pages to extract text from images...
Starting OCR on X pages...
Processing page 1/X
OCR completed. Total extracted text: XXXX characters
Combined extraction: XXX characters from text, XXX from OCR
```

3. **Query about content in images:**

```bash
curl -X POST http://localhost:8080/api/chat/ask \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What information is shown in the charts/images?",
    "sessionId": "test-123"
  }'
```

## 🧪 Test Cases

### Test Case 1: PDF with Embedded Images

**Scenario:** PDF has regular text + images with text (charts, diagrams)

**Test:**

```bash
# Upload
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@report-with-charts.pdf"

# Query about chart data
curl -X POST http://localhost:8080/api/chat/ask \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What are the values shown in the chart?",
    "sessionId": "test"
  }'
```

**Expected:** Should extract and answer from chart text

### Test Case 2: Screenshots

**Scenario:** PDF contains screenshots with text

**Test:**

```bash
# Upload
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@tutorial-screenshots.pdf"

# Query about screenshot content
curl -X POST http://localhost:8080/api/chat/ask \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What steps are shown in the screenshots?",
    "sessionId": "test"
  }'
```

**Expected:** Should extract text from screenshots

### Test Case 3: Mixed Content PDF

**Scenario:** PDF with paragraphs, tables, and images

**Test:**

```bash
# Upload
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@mixed-content.pdf"

# Get document to see extracted content
curl -X GET http://localhost:8080/api/documents/{id}
```

**Expected:** Both regular text and image text in content

### Test Case 4: Pure Image PDF (Scanned)

**Scenario:** Completely scanned document

**Test:**

```bash
# Upload
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@scanned-document.pdf"

# Query content
curl -X POST http://localhost:8080/api/chat/ask \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What is this document about?",
    "sessionId": "test"
  }'
```

**Expected:** OCR extracts all text successfully

## 🔍 Verification Steps

### 1. Check Extracted Content

After uploading, retrieve the document to see what was extracted:

```bash
# Upload and get ID
RESP=$(curl -s -X POST http://localhost:8080/api/documents/upload \
  -F "file=@your-pdf.pdf")
DOC_ID=$(echo $RESP | jq -r '.id')

# Wait for processing
sleep 10

# Get document details
curl -X GET http://localhost:8080/api/documents/$DOC_ID | jq '.content' | head -100
```

Look for:

- `=== EXTRACTED TEXT ===` section
- `=== TEXT FROM IMAGES (OCR) ===` section
- Actual content from images

### 2. Check Logs for OCR Activity

```bash
# Watch OCR processing in real-time
tail -f backend.log | grep -A5 "Running OCR"

# Check for successful extraction
grep "OCR completed" backend.log | tail -5

# Look for combined extraction stats
grep "Combined extraction" backend.log | tail -5
```

### 3. Test RAG Query Results

```bash
# Query specific content that should be in images
curl -X POST http://localhost:8080/api/chat/ask \
  -H "Content-Type: application/json" \
  -d '{
    "question": "[Ask about content you know is in an image]",
    "sessionId": "verification"
  }' | jq '.answer'
```

## 🛠️ Troubleshooting

### Problem: No Text from Images

**Symptoms:**

- Upload succeeds but can't query image content
- Logs show `OCR completed. Total extracted text: 0 characters`

**Solutions:**

1. **Check Tesseract Installation:**

```bash
tesseract --version
```

Should show version 4.0+

2. **Verify Tessdata:**

```bash
# Check common locations
ls -la /usr/local/share/tessdata/
ls -la /opt/homebrew/share/tessdata/
ls -la /usr/share/tesseract-ocr/5/tessdata/
```

Should contain `eng.traineddata`

3. **Install/Reinstall Tesseract:**

```bash
# macOS
brew uninstall tesseract
brew install tesseract

# Ubuntu/Debian
sudo apt-get remove tesseract-ocr
sudo apt-get install tesseract-ocr tesseract-ocr-eng

# Verify installation
tesseract --list-langs
```

4. **Check Application Logs:**

```bash
grep "Tesseract" backend.log
```

Should show: `Tesseract OCR initialized successfully with enhanced settings`

### Problem: Poor OCR Quality

**Symptoms:**

- Text is garbled or incomplete
- Many errors in extracted text

**Solutions:**

1. **Increase DPI (in OCRService.java):**

```java
private static final int DPI = 400; // Increase from 300
```

2. **Use Better Quality Source:**

- Ensure PDF images are high resolution
- Scan at 300+ DPI for paper documents
- Use PNG instead of JPEG for screenshots

3. **Check Image Quality:**

```bash
# Extract images from PDF to inspect
pdfimages -all your-pdf.pdf output-dir/
```

4. **Adjust Preprocessing:**
   Modify the contrast enhancement threshold in `OCRService.java`:

```java
// More aggressive thresholding
int threshold = 140; // Increase from 128
```

### Problem: OCR is Too Slow

**Symptoms:**

- Upload takes very long
- Multiple minutes per page

**Solutions:**

1. **Reduce DPI for Faster Processing:**

```java
private static final int DPI = 200; // Lower from 300
```

2. **Disable Preprocessing (if needed):**

```java
// In performOCROnImage(), skip preprocessing:
String text = tesseract.doOCR(image); // Use original image
```

3. **Process Only Image-Heavy PDFs:**

```java
// Adjust ALWAYS_RUN_OCR flag
private static final boolean ALWAYS_RUN_OCR = false;
// Lower threshold to catch more image PDFs
private static final int MIN_TEXT_LENGTH_FOR_SEARCHABLE_PDF = 200;
```

4. **Increase Heap Size:**

```bash
export MAVEN_OPTS="-Xmx4g"
mvn spring-boot:run
```

### Problem: Out of Memory

**Symptoms:**

- Application crashes during OCR
- `OutOfMemoryError` in logs

**Solutions:**

1. **Increase JVM Memory:**

```bash
# Set in environment
export JAVA_OPTS="-Xmx4g -Xms2g"

# Or in application startup
java -Xmx4g -Xms2g -jar rag-demo.jar
```

2. **Process Pages Sequentially:**
   Already implemented - pages are processed one at a time

3. **Lower Image Resolution:**

```java
private static final int DPI = 150; // Much faster, less memory
```

### Problem: Special Characters Not Recognized

**Symptoms:**

- Numbers/symbols missing
- Only letters extracted

**Solutions:**

1. **Expand Character Whitelist:**

```java
tesseract.setVariable("tessedit_char_whitelist", 
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz" +
    "0123456789" +
    ".,!?:;-()[]{}@#$%&*+=/\\|'\" \n\t" +
    "™®©€£¥¢" + // Add currency/symbols
    "αβγδεζηθικλμνξοπρστυφχψω"); // Add Greek letters if needed
```

2. **Or Remove Whitelist Completely:**

```java
// Comment out or remove this line:
// tesseract.setVariable("tessedit_char_whitelist", ...);
```

## 📈 Performance Optimization

### For Best Results

1. **High-Quality Source Documents:**
    - 300+ DPI scans
    - Clear, high-contrast images
    - Minimal noise/artifacts

2. **Optimal Tesseract Settings:**
   ```java
   DPI = 300              // Good balance
   PageSegMode = 3        // Fully automatic
   OcrEngineMode = 1      // LSTM (neural net)
   ```

3. **Image Preprocessing:**
    - Contrast enhancement: ON
    - Anti-aliasing: ON
    - High-quality rendering: ON

### For Faster Processing

1. **Lower Quality Settings:**
   ```java
   DPI = 150-200          // Faster
   Preprocessing = OFF    // Skip enhancement
   ```

2. **Selective OCR:**
   ```java
   ALWAYS_RUN_OCR = false
   MIN_TEXT_LENGTH = 200  // Only OCR image-heavy PDFs
   ```

## 🎯 Configuration Reference

### OCRService Settings

```java
// Location: src/main/java/com/ai/rag_demo/service/OCRService.java

// OCR Behavior
ALWAYS_RUN_OCR = true                    // Always run OCR (true for images)
MIN_TEXT_LENGTH_FOR_SEARCHABLE_PDF = 50  // Threshold for searchable detection

// Image Quality
DPI = 300                                 // Resolution for rendering

// Tesseract Configuration
Language = "eng"                          // OCR language
PageSegMode = 3                           // Fully automatic segmentation
OcrEngineMode = 1                         // LSTM neural network
```

### Testing Configuration Changes

1. **Modify settings in OCRService.java**
2. **Recompile:**
   ```bash
   mvn clean compile
   ```
3. **Restart application**
4. **Test with sample PDF:**
   ```bash
   curl -X POST http://localhost:8080/api/documents/upload \
     -F "file=@test-pdf.pdf"
   ```
5. **Check logs for results**

## 📊 Monitoring OCR Performance

### Key Metrics to Track

1. **Extraction Success Rate:**

```bash
grep "OCR completed" backend.log | wc -l
grep "OCR failed" backend.log | wc -l
```

2. **Processing Time:**

```bash
grep "OCR completed" backend.log | grep -o "Extracted [0-9]* characters"
```

3. **Content Quality:**

```bash
# Check if both sections exist
grep "=== TEXT FROM IMAGES ===" backend.log | wc -l
```

### Log Analysis Script

```bash
#!/bin/bash
# analyze-ocr.sh

echo "=== OCR Performance Analysis ==="
echo ""

echo "Total OCR operations:"
grep "Starting OCR" backend.log | wc -l

echo ""
echo "Successful extractions:"
grep "OCR completed" backend.log | wc -l

echo ""
echo "Failed extractions:"
grep "OCR failed" backend.log | wc -l

echo ""
echo "Average characters extracted:"
grep "OCR completed" backend.log | \
  grep -o "[0-9]* characters" | \
  grep -o "[0-9]*" | \
  awk '{sum+=$1; count++} END {if(count>0) print sum/count}'

echo ""
echo "Recent OCR operations:"
grep "Starting OCR" backend.log | tail -5
```

## 🚀 Production Recommendations

### Essential Settings

1. **Enable Always-On OCR:**
   ```java
   ALWAYS_RUN_OCR = true
   ```

2. **Use High DPI:**
   ```java
   DPI = 300
   ```

3. **Enable Preprocessing:**
   ```java
   // Keep preprocessImageForOCR() enabled
   ```

4. **Adequate Memory:**
   ```bash
   -Xmx4g -Xms2g
   ```

### Optional Enhancements

1. **Multi-Language Support:**
   ```bash
   # Install additional languages
   brew install tesseract-lang  # macOS
   apt-get install tesseract-ocr-fra  # French
   
   # Configure in code
   tesseract.setLanguage("eng+fra+deu");
   ```

2. **Async OCR Processing:**
    - Consider moving OCR to background queue
    - Use thread pool for parallel page processing
    - Implement progress tracking

3. **Caching:**
    - Cache OCR results by image hash
    - Avoid re-processing same images

## 📚 Additional Resources

- **Tesseract Documentation:** https://tesseract-ocr.github.io/
- **PDFBox Documentation:** https://pdfbox.apache.org/
- **OCR Best Practices:** See `TESSERACT_OCR_SETUP.md`

## ✅ Success Checklist

Before using in production:

- [ ] Tesseract installed and working
- [ ] Test with sample PDFs containing images
- [ ] Verify OCR extraction in logs
- [ ] Test RAG queries for image content
- [ ] Configure appropriate DPI for your needs
- [ ] Set adequate memory allocation
- [ ] Monitor performance and adjust settings

---

**The system is now optimized to extract text from images within PDFs!** 🎉

If you still can't get answers from image content:

1. Check Tesseract is installed
2. Verify logs show OCR processing
3. Review extracted content
4. Contact support with specific PDF example
