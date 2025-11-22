# Quick Start: Extracting MO Numbers from Images in PDFs

## 🎯 Your Use Case

You have PDFs where the MO Number (or Consignment Number) appears as an **image**, and you want the RAG system to be
able to find and answer questions about it.

## ✅ Solution Status

**FIXED!** The system now extracts text from ALL images in PDFs using OCR.

## 🚀 Quick Start (3 Steps)

### Step 1: Restart the Application

**CRITICAL:** You must restart to pick up the latest OCR fixes!

```bash
# Stop if running (Ctrl+C or)
pkill -f "spring-boot:run"

# Start (the build is already done)
mvn spring-boot:run
```

### Step 2: Verify OCR is Working

```bash
./check-ocr-status.sh
```

Look for these ✓ checkmarks:

- ✓ Tesseract is installed
- ✓ English language data is available
- ✓ Tesseract OCR initialized successfully

### Step 3: Test with Your PDF

```bash
./test-ocr.sh your-pdf-with-mo-number.pdf
```

This will:

1. Upload your PDF
2. Wait for OCR processing
3. Check if text was extracted from images
4. Query for the MO Number
5. Show you the results

## 📝 Expected Results

### In the Test Output

```
✓ Tesseract: Installed
✓ Application: Running
✓ Uploaded successfully
   Document ID: abc123...
✓ OCR activity found (X log entries)
✓ Content extracted
   Length: XXXX characters
✓ OCR section found in content
   Sample of OCR extracted text:
   === TEXT FROM IMAGES (OCR) ===
   MO Number: YOUR_NUMBER_HERE
   ...
✓ Found MO/Consignment references
✓ Query successful
   Answer: The MO Number is YOUR_NUMBER_HERE
```

### In Your Logs

```bash
tail -f backend.log | grep OCR
```

You should see:

```
Running OCR on all pages to extract text from images...
Starting OCR on 1 pages...
Processing page 1/1
Page 1 OCR completed. Extracted 234 characters
OCR completed. Total extracted text: 234 characters
Combined extraction: 50 from text, 234 from OCR, total: 284
```

## 🔍 Manual Testing

If you prefer manual testing:

### 1. Upload Your PDF

```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@your-file.pdf" \
  -o response.json

# Get the document ID
cat response.json | jq -r '.id'
```

### 2. Wait for Processing

Wait 15-20 seconds for OCR and Kafka processing.

### 3. Check Extracted Content

```bash
# Replace DOC_ID with your actual document ID
curl "http://localhost:8080/api/documents/DOC_ID" | jq '.content' > content.txt

# View the content
cat content.txt

# Look for your MO Number
grep -i "MO\|consignment" content.txt
```

You should see both sections:

```
=== EXTRACTED TEXT ===
[Regular text from PDF]

=== TEXT FROM IMAGES (OCR) ===
[Text from images including MO Number]
```

### 4. Query via RAG

```bash
curl -X POST http://localhost:8080/api/chat/ask \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What is the MO Number?",
    "sessionId": "test"
  }' | jq '.answer'
```

Expected response:

```
"The MO Number is [YOUR NUMBER]"
```

## 🐛 Troubleshooting

### Issue: Test Script Says "No OCR activity detected"

**Solution:**

```bash
# 1. Verify the fix is in place
grep "ocrService.extractTextFromPDF" src/main/java/com/ai/rag_demo/service/PDFConversionService.java

# Should show the line using ocrService

# 2. Rebuild
mvn clean compile

# 3. Restart
pkill -f "spring-boot:run"
mvn spring-boot:run

# 4. Test again
./test-ocr.sh your-file.pdf
```

### Issue: "Tesseract is not installed"

**Solution:**

```bash
# macOS
brew install tesseract

# Verify
tesseract --version

# Test again
./check-ocr-status.sh
```

### Issue: OCR Runs But Extracts 0 Characters

**Possible causes:**

1. **Image quality too low** - Try increasing DPI:
   ```bash
   # Edit OCRService.java
   # Change: private static final int DPI = 300;
   # To: private static final int DPI = 400;
   
   mvn clean compile
   # Restart application
   ```

2. **Text is rotated** - Rotate PDF first before uploading

3. **Character whitelist too restrictive** - Remove or expand it:
   ```bash
   # Edit OCRService.java
   # Comment out or remove the whitelist line:
   # tesseract.setVariable("tessedit_char_whitelist", ...);
   
   mvn clean compile
   # Restart application
   ```

### Issue: RAG Says "Not mentioned" Even Though OCR Extracted It

**Solution:**

```bash
# Try more specific queries
curl -X POST http://localhost:8080/api/chat/ask \
  -H "Content-Type: application/json" \
  -d '{
    "question": "What numbers or codes are visible in this document?",
    "sessionId": "test2"
  }'

# Or broader queries
curl -X POST http://localhost:8080/api/chat/ask \
  -H "Content-Type: application/json" \
  -d '{
    "question": "Tell me about all the information in this document",
    "sessionId": "test3"
  }'
```

If still not working, lower the similarity threshold:

```java
// In VectorStoreService.java
.similarityThreshold(0.3)  // Lower from 0.5
```

## 📊 Verification Checklist

Before reporting issues:

- [ ] Application restarted after fix
- [ ] `./check-ocr-status.sh` shows ✓ Tesseract installed
- [ ] Logs show "Running OCR on all pages"
- [ ] Logs show "OCR completed" with >0 characters
- [ ] Extracted content has "=== TEXT FROM IMAGES ===" section
- [ ] MO Number appears in extracted content
- [ ] Waited 20+ seconds after upload for Kafka processing
- [ ] Tried multiple query variations

## 🎯 Success Criteria

You'll know it's working when:

1. ✅ Test script shows "All checks passed"
2. ✅ Extracted content includes your MO Number
3. ✅ RAG query returns the MO Number in the answer
4. ✅ Logs show successful OCR with character counts

## 📞 Need Help?

1. **Run diagnostics:**
   ```bash
   ./check-ocr-status.sh > diagnostic.txt
   ./test-ocr.sh your-file.pdf > test-results.txt
   ```

2. **Collect logs:**
   ```bash
   tail -200 backend.log > backend-logs.txt
   ```

3. **Check documentation:**
    - [TEST_OCR_EXTRACTION.md](TEST_OCR_EXTRACTION.md) - Detailed testing guide
    - [IMAGE_TEXT_EXTRACTION_GUIDE.md](IMAGE_TEXT_EXTRACTION_GUIDE.md) - Complete OCR guide
    - [OCR_FIX_SUMMARY.md](OCR_FIX_SUMMARY.md) - What was fixed

## 💡 Tips for Best Results

1. **High-Quality Scans:**
    - 300+ DPI for scanned documents
    - Clear, high-contrast images
    - Minimal noise

2. **Upright Text:**
    - Ensure text is not rotated
    - OCR works best on upright text

3. **Good Lighting:**
    - For photos of documents
    - Avoid shadows and glare

4. **Simple Backgrounds:**
    - Plain backgrounds work best
    - Avoid complex patterns behind text

## 🎉 Summary

**What Changed:**

- Fixed: System now uses OCR service for text extraction
- Result: Text from images (including MO Numbers) is now extracted

**What You Need to Do:**

1. Restart the application (REQUIRED)
2. Run `./test-ocr.sh your-file.pdf`
3. Verify MO Number is found

**Expected Outcome:**
Your RAG system will now be able to find and answer questions about MO Numbers that appear as images in PDFs!

---

**Ready to test!** Just restart and run `./test-ocr.sh your-file.pdf` 🚀
