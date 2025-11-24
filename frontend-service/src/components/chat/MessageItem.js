// components/chat/MessageItem.js
import React from 'react';

const MessageItem = ({ message, isUser }) => {
  const formatTimestamp = (timestamp) => {
    if (!timestamp) return '';
    const date = new Date(timestamp);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  return (
    <div className={`d-flex mb-3 ${isUser ? 'justify-content-end' : 'justify-content-start'}`}>
      <div 
        className={`message-bubble p-3 rounded ${isUser ? 'bg-primary text-white' : 'bg-light'}`}
        style={{ maxWidth: '75%' }}
      >
        <div className="message-content">{message.content}</div>
        <div className={`message-time small mt-1 ${isUser ? 'text-white-50' : 'text-muted'}`}>
          {formatTimestamp(message.timestamp)}
        </div>
      </div>
    </div>
  );
};

export default MessageItem;