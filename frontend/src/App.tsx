import { useState, useEffect } from 'react';
import { Document } from './types';
import { api } from './api';
import DocumentUpload from './components/DocumentUpload';
import DocumentList from './components/DocumentList';
import ChatInterface from './components/ChatInterface';
import './App.css';

function App() {
  const [documents, setDocuments] = useState<Document[]>([]);
  const [selectedDocumentId, setSelectedDocumentId] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadDocuments();
  }, []);

  const loadDocuments = async () => {
    try {
      setLoading(true);
      const docs = await api.getAllDocuments();
      setDocuments(docs);
    } catch (error) {
      console.error('Error loading documents:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleDocumentUploaded = (doc: Document) => {
    setDocuments(prev => [doc, ...prev]);
    setSelectedDocumentId(doc.id);
  };

  return (
    <div className="app">
      <header className="app-header">
        <h1>🤖 RAG Chatbot - Document Q&A</h1>
        <p>Upload documents, get AI-powered summaries, and ask questions</p>
      </header>

      <div className="app-container">
        <aside className="sidebar">
          <DocumentUpload onDocumentUploaded={handleDocumentUploaded} />
          <DocumentList
            documents={documents}
            selectedDocumentId={selectedDocumentId}
            onSelectDocumentId={setSelectedDocumentId}
            loading={loading}
          />
        </aside>

        <main className="main-content">
          {selectedDocumentId ? (
            <ChatInterface
              document={documents.find(d => d.id === selectedDocumentId)!}
              documentId={selectedDocumentId}
            />
          ) : (
            <div className="empty-state">
              <h2>Welcome to RAG Chatbot</h2>
              <p>Upload a document or select an existing one to get started</p>
              <div className="features">
                <div className="feature">
                  <span className="icon">📄</span>
                  <h3>Upload Documents</h3>
                  <p>Support for PDF, DOCX, and text files</p>
                </div>
                <div className="feature">
                  <span className="icon">✨</span>
                  <h3>AI Summarization</h3>
                  <p>Automatic document summarization using Mistral</p>
                </div>
                <div className="feature">
                  <span className="icon">💬</span>
                  <h3>Q&A Chat</h3>
                  <p>Ask questions about your documents</p>
                </div>
              </div>
            </div>
          )}
        </main>
      </div>
    </div>
  );
}

export default App;
