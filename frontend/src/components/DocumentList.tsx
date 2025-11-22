import { FileText, Clock } from 'lucide-react';
import { Document } from '../types';
import './DocumentList.css';

interface Props {
  documents: Document[];
  selectedDocumentId: string | null;
  onSelectDocumentId: (id: string) => void;
  loading: boolean;
}

function DocumentList({ documents, selectedDocumentId, onSelectDocumentId, loading }: Props) {
  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  const formatSize = (bytes: number) => {
    if (bytes < 1024) return bytes + ' B';
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
    return (bytes / (1024 * 1024)).toFixed(1) + ' MB';
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
              className={`document-item ${selectedDocumentId === doc.id ? 'selected' : ''}`}
              onClick={() => onSelectDocumentId(doc.id)}
            >
              <div className="document-icon">
                <FileText size={24} />
              </div>
              <div className="document-info">
                <h3>{doc.filename}</h3>
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
