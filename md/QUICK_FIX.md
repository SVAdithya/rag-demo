# Quick Fix - Tesseract Library Error

## ⚡ TLDR

**Problem:** `UnsatisfiedLinkError: Unable to load library 'tesseract'`

**Solution:**

```bash
# Stop current app
pkill -f "spring-boot:run"

# Start with fix
./start-with-ocr.sh
```

**Done!** ✅

## ✅ Verification

After starting, you should see:

```
✓ Found Tesseract version: 5.5.1
✓ Created local library symlinks
```

And **NO** `UnsatisfiedLinkError` when uploading files.

## 🧪 Test It

```bash
# Upload PDF with MO number
curl -X POST http://localhost:8080/api/documents/upload -F "file=@your-file.pdf"

# Query for MO number
curl -X POST http://localhost:8080/api/chat/ask \
  -H "Content-Type: application/json" \
  -d '{"question":"What is the MO Number?","sessionId":"test"}'
```

## 📝 Remember

**Always start with:**

```bash
./start-with-ocr.sh
```

**NOT:**

```bash
mvn spring-boot:run  # ❌ This will fail!
```

## 🆘 Still Not Working?

1. **Check Tesseract is installed:**
   ```bash
   brew list tesseract
   ```
   If not: `brew install tesseract`

2. **Check symlinks exist:**
   ```bash
   ls -la lib/
   ```
   Should show 3 symlinks.

3. **Recreate symlinks:**
   ```bash
   rm -rf lib/
   ./start-with-ocr.sh
   ```

4. **See full guide:**
    - [TESSERACT_FIX_FINAL.md](TESSERACT_FIX_FINAL.md) - Complete troubleshooting

---

**That's it!** Use `./start-with-ocr.sh` every time. 🚀
