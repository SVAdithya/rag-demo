import { useState, useRef } from 'react';
import { Upload, FileText } from 'lucide-react';
import { api } from '../api';
import { Document } from '../types';
import './DocumentUpload.css';

interface Props {
  onDocumentUploaded: (doc: Document) => void;
}

function DocumentUpload({ onDocumentUploaded }: Props) {
  const [uploading, setUploading] = useState(false);
  const [showTextInput, setShowTextInput] = useState(false);
  const [textContent, setTextContent] = useState('');
  const [filename, setFilename] = useState('');
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    try {
      setUploading(true);
      const doc = await api.uploadFile(file);
      onDocumentUploaded(doc);
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    } catch (error) {
      console.error('Error uploading file:', error);
      alert('Error uploading file. Please try again.');
    } finally {
      setUploading(false);
    }
  };

  const handleTextUpload = async () => {
    if (!textContent.trim() || !filename.trim()) {
      alert('Please provide both content and filename');
      return;
    }

    try {
      setUploading(true);
      const doc = await api.uploadText({
        content: textContent,
        filename: filename,
        contentType: 'text/plain'
      });
      onDocumentUploaded(doc);
      setTextContent('');
      setFilename('');
      setShowTextInput(false);
    } catch (error) {
      console.error('Error uploading text:', error);
      alert('Error uploading text. Please try again.');
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="documentModel-upload">
      <h2>Upload Document</h2>
      
      <div className="upload-buttons">
        <label className="upload-button">
          <Upload size={20} />
          <span>Upload File</span>
          <input
            ref={fileInputRef}
            type="file"
            accept=".pdf,.docx,.txt"
            onChange={handleFileUpload}
            disabled={uploading}
            style={{ display: 'none' }}
          />
        </label>

        <button
          className="upload-button"
          onClick={() => setShowTextInput(!showTextInput)}
          disabled={uploading}
        >
          <FileText size={20} />
          <span>Enter Text</span>
        </button>
      </div>

      {showTextInput && (
        <div className="text-input-section">
          <input
            type="text"
            placeholder="Filename (e.g., my-documentModel.txt)"
            value={filename}
            onChange={(e) => setFilename(e.target.value)}
            className="filename-input"
          />
          <textarea
            placeholder="Paste or type your content here..."
            value={textContent}
            onChange={(e) => setTextContent(e.target.value)}
            rows={6}
            className="text-area"
          />
          <button
            className="submit-button"
            onClick={handleTextUpload}
            disabled={uploading || !textContent.trim() || !filename.trim()}
          >
            {uploading ? 'Uploading...' : 'Upload Text'}
          </button>
        </div>
      )}

      {uploading && (
        <div className="upload-status">
          <div className="spinner"></div>
          <span>Processing documentModel...</span>
        </div>
      )}
    </div>
  );
}

export default DocumentUpload;
