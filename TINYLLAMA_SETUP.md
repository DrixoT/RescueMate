# TinyLlama Model Setup Guide

This app uses **TinyLlama 1.1B** for on-device health analysis via the **Llamatik** library.

## 📥 Download the Model

You need to download the TinyLlama GGUF model and place it in your project.

### Option 1: Download from Hugging Face (Recommended)

1. Visit: https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF
2. Download: `tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf` (~630 MB)
3. Rename to: `TinyLlama-1.1B-Chat-v0.4-Q4_K_M.gguf`

### Option 2: Using Command Line

```bash
# Install Hugging Face CLI
pip install huggingface_hub

# Download the model
huggingface-cli download TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF \
  tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf \
  --local-dir ./models

# Rename the file
mv ./models/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf ./models/TinyLlama-1.1B-Chat-v0.4-Q4_K_M.gguf
```

### Option 3: Using Ollama

```bash
# Pull the model
ollama pull tinyllama

# Export to GGUF
ollama export tinyllama TinyLlama-1.1B-Chat-v0.4-Q4_K_M.gguf
```

## 📁 Place the Model in Your Project

1. Create the directory (if it doesn't exist):
   ```
   app/src/main/assets/models/
   ```

2. Copy the downloaded model file:
   ```
   TinyLlama-1.1B-Chat-v0.4-Q4_K_M.gguf
   ```
   to:
   ```
   app/src/main/assets/models/TinyLlama-1.1B-Chat-v0.4-Q4_K_M.gguf
   ```

## 📦 Final Directory Structure

```
RescueMate-2.0/
├── app/
│   └── src/
│       └── main/
│           ├── assets/
│           │   └── models/
│           │       └── TinyLlama-1.1B-Chat-v0.4-Q4_K_M.gguf  ← Place model here
│           ├── java/
│           └── AndroidManifest.xml
```

## ⚠️ Important Notes

1. **File Size**: The model is ~630 MB. Make sure you have enough space.
2. **Git Ignore**: The model file should be in `.gitignore` (already configured).
3. **First Run**: On first run, the app will copy the model from assets to internal storage (~1-2 seconds).
4. **Build Time**: Having a large file in assets may increase build time slightly.

## 🔍 Verify Installation

After placing the model file, the app will log:

```
TinyLlama model loaded successfully via Llamatik from: /data/user/0/com.rescuemate/app_models/TinyLlama-1.1B-Chat-v0.4-Q4_K_M.gguf
```

If the model is not found, you'll see:

```
Model file not found in assets/models/TinyLlama-1.1B-Chat-v0.4-Q4_K_M.gguf
```

## 🚀 Alternative: Download on First Run (Future Enhancement)

Currently, you must manually place the model in assets. A future update could add:

- Automatic download from Hugging Face on first run
- Model selection UI (different sizes: 1.1B, 3B, etc.)
- Progress indicator during download

## 📚 More Information

- **TinyLlama**: https://github.com/jzhang38/TinyLlama
- **Llamatik Library**: https://github.com/ferranpons/llamatik
- **Hugging Face Models**: https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF

## 🐛 Troubleshooting

### "Model file not found in assets"
- Verify the file exists at: `app/src/main/assets/models/TinyLlama-1.1B-Chat-v0.4-Q4_K_M.gguf`
- Check the filename matches exactly (case-sensitive)

### "Failed to load TinyLlama model"
- Model file may be corrupted - re-download
- Check available device storage
- Review logcat for detailed error messages

### Build fails with "Out of memory"
- Increase Gradle memory in `gradle.properties`:
  ```
  org.gradle.jvmargs=-Xmx4096m
  ```

## ✅ Fallback Behavior

If TinyLlama is unavailable, the app automatically falls back to:

1. **GPT-4 (Optional)**: If OpenAI API key is provided
2. **Rule-based Analysis**: Always available as final fallback

This ensures health monitoring continues even without the local model.

