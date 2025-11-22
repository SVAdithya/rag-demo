import axios from 'axios';
import { Document, ChatMessage, ChatRequest, DocumentUploadRequest } from './types';

const API_BASE_URL = '/api';

export const api = {
  // Document endpoints
  uploadFile: async (file: File): Promise<Document> => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await axios.post(`${API_BASE_URL}/documents/upload`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    return response.data;
  },

  uploadText: async (request: DocumentUploadRequest): Promise<Document> => {
    const response = await axios.post(`${API_BASE_URL}/documents/upload-text`, request);
    return response.data;
  },

  getAllDocuments: async (): Promise<Document[]> => {
    const response = await axios.get(`${API_BASE_URL}/documents`);
    return response.data;
  },

  getDocument: async (id: string): Promise<Document> => {
    const response = await axios.get(`${API_BASE_URL}/documents/${id}`);
    return response.data;
  },

  deleteDocument: async (documentId: string): Promise<void> => {
    await axios.delete(`${API_BASE_URL}/documents/${documentId}`);
  },

  // Chat endpoints
  askQuestion: async ({ documentId, question }: { documentId: string; question: string }): Promise<ChatMessage> => {
    const response = await axios.post(`${API_BASE_URL}/chat/ask`, {
      documentId,
      question,
    });
    return response.data;
  },

  getChatHistory: async (documentId: string): Promise<ChatMessage[]> => {
    const response = await axios.get(`${API_BASE_URL}/chat/history/${documentId}`);
    return response.data;
  }
};
