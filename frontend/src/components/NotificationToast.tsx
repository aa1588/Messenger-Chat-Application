import React, { useState, useEffect } from 'react';

interface NotificationToastProps {
  message: string;
  sender: string;
  onClose: () => void;
}

const NotificationToast: React.FC<NotificationToastProps> = ({ message, sender, onClose }) => {
  useEffect(() => {
    const timer = setTimeout(() => {
      onClose();
    }, 4000);

    return () => clearTimeout(timer);
  }, [onClose]);

  return (
    <div className="notification-toast">
      <div className="notification-header">
        <strong>{sender}</strong>
        <button className="notification-close" onClick={onClose}>×</button>
      </div>
      <div className="notification-content">
        {message.length > 50 ? `${message.substring(0, 50)}...` : message}
      </div>
    </div>
  );
};

export default NotificationToast;