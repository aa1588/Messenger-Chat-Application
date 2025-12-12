import React, { useEffect, useRef } from 'react';
import { Message, User } from '../services/chatService';
import TypingIndicator from './TypingIndicator';

interface MessageListProps {
  messages: Message[];
  currentUser: User;
  typingUsers?: string[];
  onMessageVisible?: (messageId: number) => void;
}

const MessageList: React.FC<MessageListProps> = ({ messages, currentUser, typingUsers = [], onMessageVisible }) => {
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const observerRef = useRef<IntersectionObserver | null>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
    
    // Fallback: Mark messages as visible after a short delay
    if (onMessageVisible && messages.length > 0) {
      const timer = setTimeout(() => {
        messages.forEach(message => {
          if (message.sender.id !== currentUser.id && !message.isRead) {
            console.log('Marking message as visible (fallback):', message.id);
            onMessageVisible(message.id);
          }
        });
      }, 1000); // 1 second delay to ensure messages are rendered and visible
      
      return () => clearTimeout(timer);
    }
  }, [messages, onMessageVisible, currentUser.id]);

  // Set up intersection observer for read receipts
  useEffect(() => {
    if (!onMessageVisible) return;

    // Clean up previous observer
    if (observerRef.current) {
      observerRef.current.disconnect();
    }

    observerRef.current = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            const messageId = parseInt(entry.target.getAttribute('data-message-id') || '0');
            if (messageId) {
              console.log('Message visible via intersection observer:', messageId);
              onMessageVisible(messageId);
            }
          }
        });
      },
      {
        threshold: 0.5, // Message is considered visible when 50% is in view
        rootMargin: '0px 0px -50px 0px' // Trigger when message is well within view
      }
    );

    // Use a timeout to ensure DOM is updated
    const timer = setTimeout(() => {
      const messageElements = document.querySelectorAll('[data-message-id]');
      console.log('Setting up intersection observer for', messageElements.length, 'messages');
      messageElements.forEach((element) => {
        observerRef.current?.observe(element);
      });
    }, 100);

    return () => {
      clearTimeout(timer);
      observerRef.current?.disconnect();
    };
  }, [messages, onMessageVisible]);

  const formatTime = (dateString: string) => {
    return new Date(dateString).toLocaleTimeString([], { 
      hour: '2-digit', 
      minute: '2-digit' 
    });
  };

  const getMessageStatusIcon = (message: Message) => {
    if (message.isRead) {
      return <span className="message-status read" title="Read">✓✓</span>;
    } else if (message.isDelivered) {
      return <span className="message-status delivered" title="Delivered">✓</span>;
    }
    return <span className="message-status" title="Sending">⏱</span>;
  };

  return (
    <div className="chat-messages">
      {messages.map((message) => {
        const isOwnMessage = message.sender.id === currentUser.id;
        const isSystemMessage = message.type === 'JOIN' || message.type === 'LEAVE';
        
        if (isSystemMessage) {
          return (
            <div key={message.id} className="message system-message">
              <div className="system-message-content">
                <em>{message.content}</em>
              </div>
            </div>
          );
        }
        
        return (
          <div 
            key={message.id} 
            className={`message ${isOwnMessage ? 'own-message' : 'other-message'}`}
            data-message-id={message.id}
          >
            <div className={`message-bubble ${isOwnMessage ? 'sent' : 'received'}`}>
              <div className="message-content">
                {message.content}
              </div>
              <div className="message-footer">
                <span className="message-time">
                  {formatTime(message.createdAt)}
                </span>
                {isOwnMessage && getMessageStatusIcon(message)}
              </div>
            </div>
            {!isOwnMessage && (
              <div className="message-sender">
                {message.sender.username}
              </div>
            )}
          </div>
        );
      })}
      
      {/* Typing Indicator */}
      {typingUsers.length > 0 && (
        <div className="message other-message">
          <TypingIndicator typingUsers={typingUsers} />
        </div>
      )}
      
      <div ref={messagesEndRef} />
    </div>
  );
};

export default MessageList;