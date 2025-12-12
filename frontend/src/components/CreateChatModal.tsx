import React, { useState, useEffect } from 'react';
import { chatService, User, ChatRoom } from '../services/chatService';

interface CreateChatModalProps {
  isOpen: boolean;
  onClose: () => void;
  onChatCreated: () => void;
  existingChatRooms: ChatRoom[];
  currentUser: User;
  onExistingChatSelected: (room: ChatRoom) => void;
}

const CreateChatModal: React.FC<CreateChatModalProps> = ({ 
  isOpen, 
  onClose, 
  onChatCreated, 
  existingChatRooms, 
  currentUser, 
  onExistingChatSelected 
}) => {
  const [chatType, setChatType] = useState<'DIRECT' | 'GROUP'>('DIRECT');
  const [chatName, setChatName] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [users, setUsers] = useState<User[]>([]);
  const [selectedUsers, setSelectedUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string>('');

  useEffect(() => {
    if (isOpen) {
      loadUsers();
    }
  }, [isOpen]);

  useEffect(() => {
    if (searchQuery.trim()) {
      searchUsers();
    } else {
      loadUsers();
    }
  }, [searchQuery]);

  const loadUsers = async () => {
    try {
      const allUsers = await chatService.getAllUsers();
      setUsers(allUsers);
    } catch (error) {
      console.error('Failed to load users:', error);
    }
  };

  const searchUsers = async () => {
    try {
      const searchResults = await chatService.searchUsers(searchQuery);
      setUsers(searchResults);
    } catch (error) {
      console.error('Failed to search users:', error);
    }
  };

  const findExistingDirectChat = (otherUserId: number): ChatRoom | null => {
    return existingChatRooms.find(room => 
      room.type === 'DIRECT' && 
      room.members.length === 2 &&
      room.members.some(member => member.id === currentUser.id) &&
      room.members.some(member => member.id === otherUserId)
    ) || null;
  };

  const toggleUserSelection = (user: User) => {
    setSelectedUsers(prev => {
      const isSelected = prev.find(u => u.id === user.id);
      if (isSelected) {
        return prev.filter(u => u.id !== user.id);
      } else {
        if (chatType === 'DIRECT' && prev.length >= 1) {
          return [user]; // For direct chat, only one user can be selected
        }
        return [...prev, user];
      }
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (selectedUsers.length === 0) return;

    setLoading(true);
    try {
      // Check for existing direct chat before creating a new one
      if (chatType === 'DIRECT' && selectedUsers.length === 1) {
        const existingChat = findExistingDirectChat(selectedUsers[0].id);
        if (existingChat) {
          console.log('Found existing direct chat:', existingChat);
          // Navigate to existing chat instead of creating new one
          onExistingChatSelected(existingChat);
          handleClose();
          return;
        }
      }
      
      const name = chatType === 'DIRECT' 
        ? selectedUsers[0].username // Backend will handle the naming
        : chatName || `Group with ${selectedUsers.map(u => u.username).join(', ')}`;
      
      await chatService.createChatRoom(name, chatType, selectedUsers.map(u => u.id));
      onChatCreated();
      handleClose();
    } catch (error) {
      console.error('Failed to create chat:', error);
      setError('Failed to create chat. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleClose = () => {
    setChatName('');
    setSearchQuery('');
    setSelectedUsers([]);
    setChatType('DIRECT');
    setError('');
    onClose();
  };

  if (!isOpen) return null;

  return (
    <div className="modal-overlay" onClick={handleClose}>
      <div className="modal-content" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h3>Create New Chat</h3>
          <button className="close-btn" onClick={handleClose}>×</button>
        </div>

        <form onSubmit={handleSubmit}>
          {error && (
            <div className="error-message" style={{ margin: '20px' }}>
              {error}
            </div>
          )}
          
          <div className="form-group">
            <label>Chat Type:</label>
            <select 
              value={chatType} 
              onChange={(e) => {
                setChatType(e.target.value as 'DIRECT' | 'GROUP');
                setSelectedUsers([]);
              }}
            >
              <option value="DIRECT">Direct Message</option>
              <option value="GROUP">Group Chat</option>
            </select>
          </div>

          {chatType === 'GROUP' && (
            <div className="form-group">
              <label>Group Name:</label>
              <input
                type="text"
                value={chatName}
                onChange={(e) => setChatName(e.target.value)}
                placeholder="Enter group name"
              />
            </div>
          )}

          <div className="form-group">
            <label>Search Users:</label>
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search by username or email"
            />
          </div>

          <div className="user-list">
            <h4>
              Select {chatType === 'DIRECT' ? 'User' : 'Users'} 
              {chatType === 'DIRECT' && ' (1 max)'} 
              ({selectedUsers.length} selected)
            </h4>
            <div className="users-grid">
              {users.map(user => (
                <div
                  key={user.id}
                  className={`user-item ${selectedUsers.find(u => u.id === user.id) ? 'selected' : ''}`}
                  onClick={() => toggleUserSelection(user)}
                >
                  <div className="user-info">
                    <span className="username">{user.username}</span>
                    <span className={`status ${user.isOnline ? 'online' : 'offline'}`}>
                      {user.isOnline ? '🟢' : '⚫'} {user.isOnline ? 'Online' : 'Offline'}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="modal-actions">
            <button type="button" onClick={handleClose}>Cancel</button>
            <button 
              type="submit" 
              disabled={selectedUsers.length === 0 || loading}
            >
              {loading ? 'Creating...' : 'Create Chat'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default CreateChatModal;