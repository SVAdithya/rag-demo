export interface Document {
  id: string;
  filename: string;
  summary: string | null;
  contentType: string;
  uploadedAt: string;
  size: number;
}

export interface ChatMessage {
  id: string;
  question: string;
  answer: string;
  timestamp: string;
  sessionId: string;
}

export interface ChatRequest {
  question: string;
  sessionId: string;
  documentId: string;
}

export interface DocumentUploadRequest {
  content: string;
  filename: string;
  contentType: string;
}
