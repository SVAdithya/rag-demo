import { useState, useEffect, useRef } from 'react';
import { Send, Sparkles, MessageCircle } from 'lucide-react';
import { Document, ChatMessage } from '../types';
import { api } from '../api';
import './ChatInterface.css';

interface Props {
  documentModel: Document;
  sessionId: string;
}

function ChatInterface({ documentModel, sessionId }: Props) {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [question, setQuestion] = useState('');
  const [loading, setLoading] = useState(false);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    loadChatHistory();
  }, [sessionId]);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const loadChatHistory = async () => {
    try {
      setLoadingHistory(true);
      const history = await api.getChatHistory(sessionId);
      setMessages(history);
    } catch (error) {
      console.error('Error loading chat history:', error);
    } finally {
      setLoadingHistory(false);
    }
  };

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const handleAskQuestion = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!question.trim() || loading) return;

    const currentQuestion = question;
    setQuestion('');
    setLoading(true);

    try {
      const response = await api.askQuestion({
        question: currentQuestion,
        sessionId,
        documentId: documentModel.id
      });
      setMessages(prev => [...prev, response]);
    } catch (error) {
      console.error('Error asking question:', error);
      alert('Error processing your question. Please try again.');
      setQuestion(currentQuestion);
    } finally {
      setLoading(false);
    }
  };

  const formatTimestamp = (timestamp: string) => {
    const date = new Date(timestamp);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  return (
    <div className="chat-interface">
      <div className="chat-header">
        <div className="header-content">
          <h2>{documentModel.filename}</h2>
          <span className="documentModel-type">{documentModel.contentType}</span>
        </div>
      </div>

      {documentModel.summary && (
        <div className="summary-section">
          <div className="summary-header">
            <Sparkles size={20} />
            <h3>AI Summary</h3>
          </div>
          <p className="summary-text">{documentModel.summary}</p>
        </div>
      )}

      <div className="chat-messages">
        {loadingHistory ? (
          <div className="loading-history">
            <div className="spinner"></div>
            <p>Loading conversation...</p>
          </div>
        ) : messages.length === 0 ? (
          <div className="empty-chat">
            <MessageCircle size={48} />
            <h3>Start a Conversation</h3>
            <p>Ask questions about the documentModel content</p>
            <div className="suggestions">
              <p>Try asking:</p>
              <button onClick={() => setQuestion("What is this documentModel about?")}>
                What is this documentModel about?
              </button>
              <button onClick={() => setQuestion("What are the key points?")}>
                What are the key points?
              </button>
              <button onClick={() => setQuestion("Can you summarize the main ideas?")}>
                Can you summarize the main ideas?
              </button>
            </div>
          </div>
        ) : (
          <>
            {messages.map((msg) => (
              <div key={msg.id} className="message-group">
                <div className="message question-message">
                  <div className="message-header">
                    <span className="message-label">You</span>
                    <span className="message-time">{formatTimestamp(msg.timestamp)}</span>
                  </div>
                  <p>{msg.question}</p>
                </div>
                <div className="message answer-message">
                  <div className="message-header">
                    <span className="message-label">🤖 AI Assistant</span>
                    <span className="message-time">{formatTimestamp(msg.timestamp)}</span>
                  </div>
                  <p>{msg.answer}</p>
                </div>
              </div>
            ))}
            {loading && (
              <div className="message answer-message loading-message">
                <div className="message-header">
                  <span className="message-label">🤖 AI Assistant</span>
                </div>
                <div className="typing-indicator">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
              </div>
            )}
          </>
        )}
        <div ref={messagesEndRef} />
      </div>

      <form className="chat-input-form" onSubmit={handleAskQuestion}>
        <input
          type="text"
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
          placeholder="Ask a question about the documentModel..."
          disabled={loading}
          className="chat-input"
        />
        <button
          type="submit"
          disabled={!question.trim() || loading}
          className="send-button"
        >
          <Send size={20} />
        </button>
      </form>
    </div>
  );
}

export default ChatInterface;
