# Tesseract Library Fix - macOS

## Problem

Error:
`Unable to load library 'tesseract': dlopen(libtesseract.dylib, 0x0009): tried: 'libtesseract.dylib' (no such file)`

This happens because Java can't find the Tesseract native library even though Tesseract is installed.

## ✅ Quick Fix

### Option 1: Use the Startup Script (EASIEST)

Stop the current application and use the provided startup script:

```bash
# Stop current application
pkill -f "spring-boot:run"

# Start with library path configured
./start-with-ocr.sh
```

This script automatically sets the correct library paths for Tesseract.

### Option 2: Set Environment Variables Manually

```bash
# Stop current application
pkill -f "spring-boot:run"

# Set library paths
export DYLD_LIBRARY_PATH="/opt/homebrew/Cellar/tesseract/5.5.1/lib:/opt/homebrew/lib:$DYLD_LIBRARY_PATH"
export JNA_LIBRARY_PATH="/opt/homebrew/Cellar/tesseract/5.5.1/lib:/opt/homebrew/lib"

# Start application
mvn spring-boot:run \
    -Djna.library.path="/opt/homebrew/Cellar/tesseract/5.5.1/lib:/opt/homebrew/lib"
```

### Option 3: Add to Shell Profile (PERMANENT)

Add these lines to your `~/.zshrc` or `~/.bash_profile`:

```bash
# Tesseract library paths
export DYLD_LIBRARY_PATH="/opt/homebrew/lib:$DYLD_LIBRARY_PATH"
export JNA_LIBRARY_PATH="/opt/homebrew/lib"
export TESSDATA_PREFIX="/opt/homebrew/share/tessdata"
```

Then:

```bash
# Reload shell config
source ~/.zshrc

# Start application
mvn spring-boot:run
```

## Verification

After starting with the fix, try uploading a document:

```bash
# In another terminal
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@your-file.pdf"
```

Check logs - you should NO LONGER see the `UnsatisfiedLinkError`.

## Why This Happens

1. Tesseract is installed by Homebrew in `/opt/homebrew/Cellar/tesseract/`
2. Java (JNA) looks for native libraries in standard system paths
3. Homebrew's location is not in Java's default search path
4. We need to explicitly tell Java where to find the library

## Troubleshooting

### If the script doesn't work

1. **Find your Tesseract installation:**
   ```bash
   brew list tesseract | grep dylib
   ```

2. **Update the script with your path:**
   Edit `start-with-ocr.sh` and change the `TESSERACT_LIB` path to match your installation.

3. **Check symlinks:**
   ```bash
   ls -la /opt/homebrew/lib/libtesseract.*
   ```

   Make sure symlinks exist. If not:
   ```bash
   brew link tesseract
   ```

### Still not working?

Try creating a symlink in a system path:

```bash
sudo ln -s /opt/homebrew/Cellar/tesseract/5.5.1/lib/libtesseract.5.dylib /usr/local/lib/libtesseract.dylib
```

**Note:** This requires sudo and modifies system directories.

## Alternative: Disable OCR (Not Recommended)

If you can't get Tesseract working and need to run the application:

1. Edit `src/main/java/com/ai/rag_demo/service/OCRService.java`
2. Change: `ALWAYS_RUN_OCR = false`
3. Rebuild: `mvn clean compile`
4. Restart

**WARNING:** This will disable image text extraction!

## Summary

**Use this command to start the application:**

```bash
./start-with-ocr.sh
```

This is the easiest and most reliable way to run the application with OCR support on macOS.

---

**Status after fix:** ✅ Tesseract library will be found and OCR will work!
