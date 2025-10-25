import axios from 'axios';
import { Document, ChatMessage, ChatRequest, DocumentUploadRequest } from './types';

const API_BASE_URL = '/api';

export const api = {
  // Document endpoints
  uploadFile: async (file: File): Promise<Document> => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await axios.post(`${API_BASE_URL}/documentModels/upload`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    return response.data;
  },

  uploadText: async (request: DocumentUploadRequest): Promise<Document> => {
    const response = await axios.post(`${API_BASE_URL}/documentModels/upload-text`, request);
    return response.data;
  },

  getAllDocuments: async (): Promise<Document[]> => {
    const response = await axios.get(`${API_BASE_URL}/documentModels`);
    return response.data;
  },

  getDocument: async (id: string): Promise<Document> => {
    const response = await axios.get(`${API_BASE_URL}/documentModels/${id}`);
    return response.data;
  },

  // Chat endpoints
  askQuestion: async (request: ChatRequest): Promise<ChatMessage> => {
    const response = await axios.post(`${API_BASE_URL}/chat/ask`, request);
    return response.data;
  },

  getChatHistory: async (sessionId: string): Promise<ChatMessage[]> => {
    const response = await axios.get(`${API_BASE_URL}/chat/history/${sessionId}`);
    return response.data;
  }
};
