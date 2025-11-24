// components/chat/ChatInterface.js
import React, { useState, useEffect, useRef } from 'react';
import { useParams, Link } from 'react-router-dom';
import { getMessages, sendMessage } from '../../services/chatService';
import { getCourseById } from '../../services/courseService';
import MessageItem from './MessageItem';
import { FaArrowLeft, FaPaperPlane } from 'react-icons/fa';

const ChatInterface = () => {
  const { id } = useParams();
  const [course, setCourse] = useState(null);
  const [messages, setMessages] = useState([]);
  const [newMessage, setNewMessage] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState('');
  const messagesEndRef = useRef(null);

  useEffect(() => {
    fetchCourseAndMessages();
  }, [id]);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const fetchCourseAndMessages = async () => {
    try {
      const [courseResponse, messagesResponse] = await Promise.all([
        getCourseById(id),
        getMessages(id)
      ]);
      setCourse(courseResponse.data);
      setMessages(messagesResponse.data);
    } catch (err) {
      setError('Failed to load chat data');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const handleSendMessage = async (e) => {
    e.preventDefault();
    if (!newMessage.trim()) return;
    
    setSending(true);
    setError('');
    
    try {
      const response = await sendMessage(id, newMessage);
      setMessages(response.data);
      setNewMessage('');
    } catch (err) {
      setError('Failed to send message');
      console.error(err);
    } finally {
      setSending(false);
    }
  };

  if (loading) return <div className="text-center mt-5">Loading chat...</div>;
  if (!course) return <div className="alert alert-warning mt-3">Course not found</div>;

  return (
    <div className="container mt-4">
      <div className="card">
        <div className="card-header bg-primary text-white d-flex justify-content-between align-items-center">
          <div>
            <Link to={`/courses/${id}`} className="btn btn-sm btn-light me-2">
              <FaArrowLeft />
            </Link>
            <span className="fs-5">{course.title}</span>
          </div>
          <small>Chat with course materials</small>
        </div>
        
        <div className="card-body chat-container" style={{ height: '70vh', overflowY: 'auto' }}>
          {error && <div className="alert alert-danger">{error}</div>}
          
          {messages.length === 0 ? (
            <div className="text-center text-muted my-5">
              <p>No messages yet. Start the conversation by asking something about the course materials.</p>
            </div>
          ) : (
            <div className="messages-list">
              {messages.map((message, index) => (
                <MessageItem 
                  key={index} 
                  message={message} 
                  isUser={message.role === 'user'} 
                />
              ))}
              <div ref={messagesEndRef} />
            </div>
          )}
        </div>
        
        <div className="card-footer">
          <form onSubmit={handleSendMessage}>
            <div className="input-group">
              <input
                type="text"
                className="form-control"
                placeholder="Type your message..."
                value={newMessage}
                onChange={(e) => setNewMessage(e.target.value)}
                disabled={sending}
              />
              <button 
                type="submit" 
                className="btn btn-primary"
                disabled={sending || !newMessage.trim()}
              >
                {sending ? 'Sending...' : <FaPaperPlane />}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default ChatInterface;