import { FileText, Clock, Trash2 } from 'lucide-react';
import { useState } from 'react';
import { Document } from '../types';
import './DocumentList.css';

interface Props {
  documents: Document[];
  selectedDocumentId: string | null;
  onSelectDocumentId: (id: string) => void;
  onRemoveDocument: (id: string) => void;
  loading: boolean;
}

function DocumentList({ documents, selectedDocumentId, onSelectDocumentId, onRemoveDocument, loading }: Props) {
  const [removingId, setRemovingId] = useState<string | null>(null);

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  const formatSize = (bytes: number) => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
  };

  const handleRemove = (docId: string) => {
    setRemovingId(docId);
    setTimeout(() => {
      onRemoveDocument(docId);
      setRemovingId(null);
    }, 400); // matches CSS transition
  };

  return (
    <div className="document-list">
      <h2>Documents ({documents.length})</h2>
      
      {loading && documents.length === 0 ? (
        <div className="loading-state">
          <div className="spinner"></div>
          <p>Loading documents...</p>
        </div>
      ) : documents.length === 0 ? (
        <div className="empty-list">
          <p>No documents yet</p>
          <span>Upload a document to get started</span>
        </div>
      ) : (
        <div className="documents">
          {documents.map((doc) => (
            <div
              key={doc.id}
              className={`document-item ${selectedDocumentId === doc.id ? 'selected' : ''}${removingId === doc.id ? ' removing' : ''}`}
              onClick={() => onSelectDocumentId(doc.id)}
            >
              <div className="document-icon">
                <FileText size={24} />
              </div>
              <div className="document-info">
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  <h3>{doc.filename}</h3>
                  <button
                    className="remove-document-btn"
                    onClick={e => { e.stopPropagation(); if (window.confirm('Remove this document and its history?')) handleRemove(doc.id); }}
                    title="Remove from history"
                  >
                    <Trash2 size={18} />
                  </button>
                </div>
                <div className="document-meta">
                  <span className="meta-item">
                    <Clock size={14} />
                    {formatDate(doc.uploadedAt)}
                  </span>
                  <span className="meta-item">
                    {formatSize(doc.size)}
                  </span>
                </div>
                {doc.summary && (
                  <div className="document-summary">
                    <span className="summary-badge">Summary available</span>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default DocumentList;
