# Quick Start Guide

Get up and running with the RAG Chatbot in minutes!

## Prerequisites

Make sure you have installed:

- Docker & Docker Compose
- Java 17+
- Maven 3.6+
- Node.js 18+

## Quick Start (Automated)

### 1. Start Everything with One Command

```bash
./start.sh
```

This script will:

- Start Elasticsearch, Kafka, and Ollama using Docker Compose
- Download the Mistral AI model
- Build and start the Spring Boot backend
- Install dependencies and start the React frontend

### 2. Access the Application

Once the script completes, open your browser to:

**http://localhost:3000**

### 3. Stop Everything

```bash
./stop.sh
```

---

## Manual Setup (Alternative)

If you prefer to start services manually:

### 1. Start Docker Services

```bash
docker-compose up -d
```

### 2. Install Mistral Model

```bash
docker exec -it rag-ollama ollama pull mistral
```

### 3. Start Backend

```bash
mvn spring-boot:run
```

### 4. Start Frontend

In a new terminal:

```bash
cd frontend
npm install
npm run dev
```

### 5. Open Application

Navigate to **http://localhost:3000**

---

## First Steps

### Upload Your First Document

1. Click the **"Upload File"** button to upload a PDF, DOCX, or text file
2. Or click **"Enter Text"** to paste content directly
3. Wait a few seconds while the AI generates a summary

### Ask Questions

1. Select a documentModel from the list
2. View the AI-generated summary at the top
3. Type your question in the chat input at the bottom
4. Press Enter or click the send button
5. Get instant answers based on the documentModel content!

### Example Documents

Try uploading:

- Research papers or articles
- Meeting notes or documentation
- Books or reports
- Any text-based documentModel

### Example Questions

- "What is this documentModel about?"
- "What are the main points discussed?"
- "Can you summarize the key findings?"
- "What conclusions were drawn?"

---

## Troubleshooting

### Services Not Starting?

Check Docker:

```bash
docker ps
```

You should see containers for:

- rag-elasticsearch
- rag-kafka
- rag-zookeeper
- rag-ollama

### Backend Fails to Start?

Check the logs:

```bash
tail -f backend.log
```

Common issues:

- Elasticsearch not ready (wait a bit longer)
- Port 8080 already in use

### Frontend Not Loading?

Check the logs:

```bash
tail -f frontend.log
```

Common issues:

- Port 3000 already in use
- npm dependencies not installed properly

### AI Not Responding?

Verify Mistral model is installed:

```bash
docker exec -it rag-ollama ollama list
```

You should see `mistral` in the list.

---

## What's Next?

- Check out [README.md](README.md) for detailed architecture information
- Explore the API endpoints
- Customize the AI prompts in the service layer
- Add more documentModel types support

## Need Help?

Open an issue on the repository with:

- What you were trying to do
- Error messages or logs
- Your environment (OS, versions, etc.)

Happy chatting! 🤖
