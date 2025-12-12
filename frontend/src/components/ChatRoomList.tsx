import React, { useState } from 'react';
import { ChatRoom, User } from '../services/chatService';
import { formatLastSeen, formatMessageTime } from '../utils/timeUtils';
import ContextMenu from './ContextMenu';

interface ChatRoomListProps {
  chatRooms: ChatRoom[];
  selectedRoom: ChatRoom | null;
  onRoomSelect: (room: ChatRoom) => void;
  onDeleteChat: (chatRoom: ChatRoom) => void;
  currentUser: User;
  currentTime: Date;
}

const ChatRoomList: React.FC<ChatRoomListProps> = ({ 
  chatRooms, 
  selectedRoom, 
  onRoomSelect,
  onDeleteChat,
  currentUser,
  currentTime
}) => {
  const [contextMenu, setContextMenu] = useState<{
    x: number;
    y: number;
    chatRoom: ChatRoom;
  } | null>(null);

  const handleRightClick = (event: React.MouseEvent, chatRoom: ChatRoom) => {
    event.preventDefault();
    setContextMenu({
      x: event.clientX,
      y: event.clientY,
      chatRoom
    });
  };

  const handleCloseContextMenu = () => {
    setContextMenu(null);
  };

  const handleDeleteChat = () => {
    if (contextMenu) {
      onDeleteChat(contextMenu.chatRoom);
    }
  };
  return (
    <ul className="chat-room-list">
      {chatRooms.map((room) => {
        const otherUser = room.type === 'DIRECT' 
          ? room.members.find(member => member.id !== currentUser.id)
          : null;
        
        return (
          <li
            key={room.id}
            className={`chat-room-item ${selectedRoom?.id === room.id ? 'active' : ''}`}
            onClick={() => onRoomSelect(room)}
            onContextMenu={(e) => handleRightClick(e, room)}
          >
            <div className="chat-room-avatar">
              {room.type === 'DIRECT' && otherUser 
                ? otherUser.username.charAt(0).toUpperCase()
                : room.name.charAt(0).toUpperCase()
              }
            </div>
            
            <div style={{ flex: 1 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <strong>
                  {room.type === 'DIRECT' && otherUser 
                    ? otherUser.username 
                    : room.name
                  }
                </strong>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                  {room.lastMessageTime && (
                    <span className="message-time">
                      {formatMessageTime(room.lastMessageTime)}
                    </span>
                  )}
                  {room.unreadCount && room.unreadCount > 0 && (
                    <div className="unread-badge">
                      {room.unreadCount > 99 ? '99+' : room.unreadCount}
                    </div>
                  )}
                </div>
              </div>
              
              <div style={{ fontSize: '12px', opacity: 0.7, marginTop: '2px' }}>
                {room.lastMessage && room.lastMessage !== null ? (
                  <span>
                    {room.lastMessage.type === 'CHAT' 
                      ? `${room.lastMessage.sender.username}: ${room.lastMessage.content.substring(0, 30)}${room.lastMessage.content.length > 30 ? '...' : ''}`
                      : room.lastMessage.content
                    }
                  </span>
                ) : (
                  <span>{room.type === 'DIRECT' ? 'Start a conversation' : `Group • ${room.members.length} members`}</span>
                )}
              </div>
              
              {room.type === 'DIRECT' && otherUser && !room.lastMessage && (
                <div className="last-seen">
                  {formatLastSeen(otherUser.lastSeen, otherUser.isOnline, currentTime)}
                </div>
              )}
            </div>
          </li>
        );
      })}
      
      {contextMenu && (
        <ContextMenu
          x={contextMenu.x}
          y={contextMenu.y}
          onClose={handleCloseContextMenu}
          onDelete={handleDeleteChat}
        />
      )}
    </ul>
  );
};

export default ChatRoomList;