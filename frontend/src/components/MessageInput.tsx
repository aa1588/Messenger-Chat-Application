import React, { useState, useEffect, useRef } from 'react';

interface MessageInputProps {
  onSendMessage: (content: string) => void;
  onTypingChange?: (isTyping: boolean) => void;
}

const MessageInput: React.FC<MessageInputProps> = ({ onSendMessage, onTypingChange }) => {
  const [message, setMessage] = useState('');
  const [isTyping, setIsTyping] = useState(false);
  const typingTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const lastTypingCallRef = useRef<number>(0);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (message.trim()) {
      onSendMessage(message.trim());
      setMessage('');
      handleStopTyping();
    }
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setMessage(value);
    console.log('Input changed:', value, 'Current typing state:', isTyping);

    if (value.trim()) {
      if (!isTyping) {
        console.log('Starting typing indicator');
        setIsTyping(true);
        onTypingChange?.(true);
      }
      
      // Clear existing timeout
      if (typingTimeoutRef.current) {
        clearTimeout(typingTimeoutRef.current);
      }

      // Set new timeout to stop typing indicator
      typingTimeoutRef.current = setTimeout(() => {
        console.log('Typing timeout reached, stopping typing indicator');
        handleStopTyping();
      }, 2000); // Stop typing after 2 seconds of inactivity
    } else {
      // If input is empty, stop typing immediately
      if (isTyping) {
        console.log('Input is empty, stopping typing indicator');
        handleStopTyping();
      }
    }
  };

  const handleStopTyping = () => {
    if (isTyping) {
      console.log('Stopping typing indicator');
      setIsTyping(false);
      onTypingChange?.(false);
    }
    if (typingTimeoutRef.current) {
      clearTimeout(typingTimeoutRef.current);
      typingTimeoutRef.current = null;
    }
  };

  // Also stop typing when input becomes empty
  useEffect(() => {
    if (!message.trim() && isTyping) {
      handleStopTyping();
    }
  }, [message, isTyping]);

  useEffect(() => {
    return () => {
      if (typingTimeoutRef.current) {
        clearTimeout(typingTimeoutRef.current);
      }
    };
  }, []);

  return (
    <div className="message-input-container">
      <form className="message-input-form" onSubmit={handleSubmit}>
        <input
          type="text"
          className="message-input"
          placeholder="Type a message..."
          value={message}
          onChange={handleInputChange}
        />
        <button type="submit" className="send-btn">
          Send
        </button>
      </form>
    </div>
  );
};

export default MessageInput;