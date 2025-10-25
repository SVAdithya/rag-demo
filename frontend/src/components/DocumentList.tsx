import { FileText, Clock } from 'lucide-react';
import { Document } from '../types';
import './DocumentList.css';

interface Props {
  documentModels: Document[];
  selectedDocument: Document | null;
  onSelectDocument: (doc: Document) => void;
  loading: boolean;
}

function DocumentList({ documentModels, selectedDocument, onSelectDocument, loading }: Props) {
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
    <div className="documentModel-list">
      <h2>Documents ({documentModels.length})</h2>
      
      {loading && documentModels.length === 0 ? (
        <div className="loading-state">
          <div className="spinner"></div>
          <p>Loading documentModels...</p>
        </div>
      ) : documentModels.length === 0 ? (
        <div className="empty-list">
          <p>No documentModels yet</p>
          <span>Upload a documentModel to get started</span>
        </div>
      ) : (
        <div className="documentModels">
          {documentModels.map((doc) => (
            <div
              key={doc.id}
              className={`documentModel-item ${selectedDocument?.id === doc.id ? 'selected' : ''}`}
              onClick={() => onSelectDocument(doc)}
            >
              <div className="documentModel-icon">
                <FileText size={24} />
              </div>
              <div className="documentModel-info">
                <h3>{doc.filename}</h3>
                <div className="documentModel-meta">
                  <span className="meta-item">
                    <Clock size={14} />
                    {formatDate(doc.uploadedAt)}
                  </span>
                  <span className="meta-item">
                    {formatSize(doc.size)}
                  </span>
                </div>
                {doc.summary && (
                  <div className="documentModel-summary">
                    <span className="summary-badge">✨ Summary available</span>
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
