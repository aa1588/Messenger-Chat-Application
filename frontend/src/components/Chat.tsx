import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { chatService, ChatRoom, Message } from '../services/chatService';
import { websocketService } from '../services/websocketService';
import { formatLastSeen } from '../utils/timeUtils';
import ChatRoomList from './ChatRoomList';
import MessageList from './MessageList';
import MessageInput from './MessageInput';
import CreateChatModal from './CreateChatModal';
import NotificationToast from './NotificationToast';
import ConfirmDialog from './ConfirmDialog';

const Chat: React.FC = () => {
  const { user, logout } = useAuth();
  const [chatRooms, setChatRooms] = useState<ChatRoom[]>([]);
  const [selectedRoom, setSelectedRoom] = useState<ChatRoom | null>(null);
  const [messages, setMessages] = useState<Message[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [notifications, setNotifications] = useState<Array<{id: number, message: Message}>>([]);
  const [deleteConfirm, setDeleteConfirm] = useState<{
    isOpen: boolean;
    chatRoom: ChatRoom | null;
  }>({ isOpen: false, chatRoom: null });
  const [currentTime, setCurrentTime] = useState(new Date());
  const [typingUsers, setTypingUsers] = useState<string[]>([]);
  
  // Use ref to track current room to avoid closure issues
  const selectedRoomRef = React.useRef<ChatRoom | null>(null);

  useEffect(() => {
    if (user) {
      initializeChat();
      
      // Handle page visibility changes
      const handleVisibilityChange = () => {
        // Only update status if user is still authenticated
        if (localStorage.getItem('token')) {
          if (document.hidden) {
            chatService.updateOnlineStatus(false);
          } else {
            chatService.updateOnlineStatus(true);
            // Mark messages as read when user comes back to the page
            if (selectedRoomRef.current) {
              markCurrentRoomMessagesAsRead();
            }
          }
        }
      };

      // Handle page unload
      const handleBeforeUnload = () => {
        // Only update status if user is still authenticated
        if (localStorage.getItem('token')) {
          chatService.updateOnlineStatus(false);
        }
      };

      document.addEventListener('visibilitychange', handleVisibilityChange);
      window.addEventListener('beforeunload', handleBeforeUnload);
      
      // Set user as offline when component unmounts
      return () => {
        try {
          // Only update status if user is still authenticated (token exists)
          if (localStorage.getItem('token')) {
            chatService.updateOnlineStatus(false);
          }
        } catch (error) {
          console.error('Failed to update offline status on unmount:', error);
        }
        document.removeEventListener('visibilitychange', handleVisibilityChange);
        window.removeEventListener('beforeunload', handleBeforeUnload);
      };
    }
  }, [user]);

  useEffect(() => {
    selectedRoomRef.current = selectedRoom; // Update ref when selectedRoom changes
    
    if (selectedRoom) {
      loadMessages();
      
      // Clear typing users when switching rooms
      setTypingUsers([]);
    }
  }, [selectedRoom]);

  // Subscribe to all chat rooms for messages and notifications
  useEffect(() => {
    if (chatRooms.length > 0) {
      // Clear existing room subscriptions to prevent duplicates
      websocketService.clearSubscriptions();
      
      // Subscribe to all rooms for messages
      chatRooms.forEach(room => {
        websocketService.subscribeToRoom(room.id, (message) => {
          const currentRoom = selectedRoomRef.current;
          
          // Only add message to current view if it's for the selected room
          if (currentRoom && message.chatRoom.id === currentRoom.id) {
            setMessages(prev => {
              // Check if message already exists to avoid duplicates
              if (prev.find(m => m.id === message.id)) {
                return prev;
              }
              return [...prev, message];
            });
            
            // Auto-mark incoming messages as read if not from current user
            if (message.sender.id !== user?.id && !message.isRead) {
              // Mark as read with a small delay to ensure message is visible
              setTimeout(async () => {
                try {
                  await chatService.markMessageAsRead(currentRoom.id, message.id);
                  
                  // Update local state immediately
                  setMessages(prev => 
                    prev.map(m => 
                      m.id === message.id 
                        ? { ...m, isRead: true, readAt: new Date().toISOString() }
                        : m
                    )
                  );
                } catch (error) {
                  console.warn('Failed to auto-mark message as read:', error);
                }
              }, 500); // 500ms delay
            }
          }
          
          // Always update chat room list to show latest message (for all rooms)
          setChatRooms(prevRooms => 
            prevRooms.map(r => 
              r.id === message.chatRoom.id 
                ? { 
                    ...r, 
                    lastMessage: message, 
                    lastMessageTime: message.createdAt,
                    // Increment unread count if message is not from current user and not for current room
                    unreadCount: (message.sender.id !== user?.id && (!currentRoom || currentRoom.id !== message.chatRoom.id)) 
                      ? (r.unreadCount || 0) + 1 
                      : r.unreadCount
                  }
                : r
            )
          );
          
          // Show notification if message is not from current room and not from current user
          if ((!currentRoom || message.chatRoom.id !== currentRoom.id) && message.sender.id !== user?.id) {
            const notificationId = Date.now();
            setNotifications(prev => [...prev, { id: notificationId, message }]);
          }
        });
      });
      
      // Subscribe to typing indicators and message status for the selected room
      if (selectedRoom) {
        console.log('Setting up typing and status subscriptions for room:', selectedRoom.id);
        websocketService.subscribeToTypingIndicators(selectedRoom.id, handleTypingUpdate);
        websocketService.subscribeToMessageStatus(selectedRoom.id, handleMessageStatusUpdate);
      }
    }
  }, [chatRooms, selectedRoom]);

  // Update current time every minute for real-time "last seen" updates
  useEffect(() => {
    const interval = setInterval(() => {
      setCurrentTime(new Date());
    }, 60000); // Update every minute

    return () => clearInterval(interval);
  }, []);



  const initializeChat = async () => {
    try {
      // Connect to WebSocket (without global message handler to avoid duplicates)
      await websocketService.connect(user!.username, () => {});
      
      // Subscribe to new chat room notifications
      websocketService.subscribeToNewChatRooms(user!.username, handleNewChatRoom);
      
      // Subscribe to user status updates
      websocketService.subscribeToUserStatus(handleUserStatusUpdate);
      
      // Set user as online first, then load chat rooms
      await chatService.updateOnlineStatus(true);
      
      // Small delay to allow status update to propagate
      await new Promise(resolve => setTimeout(resolve, 500));
      
      // Load chat rooms
      const rooms = await chatService.getChatRooms();
      
      // Load last message for each room
      const roomsWithLastMessages = await Promise.all(
        rooms.map(async (room) => {
          const lastMessage = await chatService.getLastMessage(room.id);
          return {
            ...room,
            lastMessage,
            lastMessageTime: lastMessage?.createdAt
          };
        })
      );
      
      setChatRooms(roomsWithLastMessages);
      
      // Don't auto-select a room - let user choose
    } catch (error) {
      console.error('Failed to initialize chat:', error);
    } finally {
      setLoading(false);
    }
  };

  const loadMessages = async () => {
    if (selectedRoom) {
      try {
        const roomMessages = await chatService.getChatRoomMessages(selectedRoom.id);
        setMessages(roomMessages);
        
        // Mark unread messages as read
        await markMessagesAsRead(roomMessages);
      } catch (error) {
        console.error('Failed to load messages:', error);
      }
    }
  };

  const markMessagesAsRead = async (messages: Message[]) => {
    if (!selectedRoom || !user) return;
    
    // Find messages that are not from current user and not yet read
    const unreadMessages = messages.filter(msg => 
      msg.sender.id !== user.id && !msg.isRead
    );
    
    // Mark each unread message as read
    for (const message of unreadMessages) {
      try {
        await chatService.markMessageAsRead(selectedRoom.id, message.id);
        
        // Update local state immediately for better UX
        setMessages(prev => 
          prev.map(m => 
            m.id === message.id 
              ? { ...m, isRead: true, readAt: new Date().toISOString() }
              : m
          )
        );
      } catch (error) {
        console.warn('Failed to mark message as read:', error);
      }
    }
    
    // Refresh chat room list to update unread counts
    if (unreadMessages.length > 0) {
      refreshChatRooms();
    }
  };

  const markCurrentRoomMessagesAsRead = async () => {
    const currentRoom = selectedRoomRef.current;
    if (!currentRoom || !user) return;
    
    // Get current messages and mark unread ones as read
    const currentMessages = messages.filter(msg => 
      msg.sender.id !== user.id && !msg.isRead
    );
    
    for (const message of currentMessages) {
      try {
        await chatService.markMessageAsRead(currentRoom.id, message.id);
        
        // Update local state immediately
        setMessages(prev => 
          prev.map(m => 
            m.id === message.id 
              ? { ...m, isRead: true, readAt: new Date().toISOString() }
              : m
          )
        );
      } catch (error) {
        console.warn('Failed to mark message as read:', error);
      }
    }
    
    // Refresh chat room list to update unread counts
    if (currentMessages.length > 0) {
      refreshChatRooms();
    }
  };

  const handleMessageVisible = async (messageId: number) => {
    const currentRoom = selectedRoomRef.current;
    if (!currentRoom || !user) return;
    
    // Find the message and check if it needs to be marked as read
    const message = messages.find(m => m.id === messageId);
    
    if (message && message.sender.id !== user.id && !message.isRead) {
      console.log('Marking message as read:', messageId);
      try {
        await chatService.markMessageAsRead(currentRoom.id, messageId);
        
        // Update local state immediately
        setMessages(prev => 
          prev.map(m => 
            m.id === messageId 
              ? { ...m, isRead: true, readAt: new Date().toISOString() }
              : m
          )
        );
        
        // Refresh chat room list to update unread count
        refreshChatRooms();
      } catch (error) {
        console.warn('Failed to mark message as read:', error);
      }
    }
  };

  const refreshChatRooms = async () => {
    try {
      const rooms = await chatService.getChatRooms();
      
      // Load last message for each room
      const roomsWithLastMessages = await Promise.all(
        rooms.map(async (room) => {
          const lastMessage = await chatService.getLastMessage(room.id);
          return {
            ...room,
            lastMessage,
            lastMessageTime: lastMessage?.createdAt
          };
        })
      );
      
      setChatRooms(roomsWithLastMessages);
    } catch (error) {
      console.error('Failed to refresh chat rooms:', error);
    }
  };



  const handleSendMessage = (content: string) => {
    if (selectedRoom && user) {
      websocketService.sendMessage(selectedRoom.id, content, user.username);
    }
  };

  const handleTypingChange = async (isTyping: boolean) => {
    console.log('Typing change:', isTyping, 'Room:', selectedRoom?.id);
    if (selectedRoom && localStorage.getItem('token') && user) {
      try {
        await chatService.sendTypingIndicator(selectedRoom.id, isTyping);
        console.log('Typing indicator sent successfully');
      } catch (error: any) {
        console.warn('Failed to send typing indicator:', error);
        
        // If it's a 403 error, the token might be invalid
        if (error.response?.status === 403) {
          console.warn('Authentication failed for typing indicator - token might be expired');
        }
        
        // Don't show error to user - typing indicators are not critical
      }
    } else {
      console.log('Cannot send typing indicator - missing requirements');
    }
  };

  const handleTypingUpdate = (typingUpdate: any) => {
    console.log('Received typing update:', typingUpdate);
    
    // Don't show typing indicator for current user
    if (typingUpdate.userId === user?.id) {
      console.log('Ignoring typing update from current user');
      return;
    }
    
    setTypingUsers(prev => {
      console.log('Current typing users:', prev);
      if (typingUpdate.typing) {
        // Add user to typing list if not already there
        if (!prev.includes(typingUpdate.username)) {
          console.log('Adding user to typing list:', typingUpdate.username);
          return [...prev, typingUpdate.username];
        }
        return prev;
      } else {
        // Remove user from typing list
        console.log('Removing user from typing list:', typingUpdate.username);
        return prev.filter(username => username !== typingUpdate.username);
      }
    });
  };

  const handleMessageStatusUpdate = (statusUpdate: any) => {
    setMessages(prev => 
      prev.map(message => 
        message.id === statusUpdate.messageId 
          ? { 
              ...message, 
              isRead: statusUpdate.statusType === 'READ' ? true : message.isRead,
              readAt: statusUpdate.readAt || message.readAt
            }
          : message
      )
    );
  };

  const handleRoomSelect = (room: ChatRoom) => {
    setSelectedRoom(room);
    selectedRoomRef.current = room; // Update ref as well
    setMessages([]);
    setTypingUsers([]); // Clear typing users when switching rooms
    
    // Reset unread count for selected room
    setChatRooms(prevRooms => 
      prevRooms.map(r => 
        r.id === room.id 
          ? { ...r, unreadCount: 0 }
          : r
      )
    );
  };

  const handleDeleteChat = (chatRoom: ChatRoom) => {
    setDeleteConfirm({ isOpen: true, chatRoom });
  };

  const confirmDeleteChat = async () => {
    if (!deleteConfirm.chatRoom) return;
    
    const chatRoomToDelete = deleteConfirm.chatRoom;
    
    try {
      // Call API to delete chat for current user
      await chatService.deleteChatForUser(chatRoomToDelete.id);
      
      // Remove chat from local state
      setChatRooms(prev => prev.filter(room => room.id !== chatRoomToDelete.id));
      
      // If the deleted chat was selected, clear selection
      if (selectedRoom?.id === chatRoomToDelete.id) {
        setSelectedRoom(null);
        setMessages([]);
      }
      
      // Show success notification
      const notificationId = Date.now();
      setNotifications(prev => [...prev, { 
        id: notificationId, 
        message: {
          id: 0,
          content: `Chat "${chatRoomToDelete.name}" deleted`,
          sender: { username: 'System', id: 0, email: '' },
          createdAt: new Date().toISOString(),
          chatRoom: chatRoomToDelete,
          type: 'LEAVE' as Message['type']
        }
      }]);
    } catch (error) {
      console.error('Failed to delete chat:', error);
      
      // Show error notification
      const notificationId = Date.now();
      setNotifications(prev => [...prev, { 
        id: notificationId, 
        message: {
          id: 0,
          content: 'Failed to delete chat. Please try again.',
          sender: { username: 'System', id: 0, email: '' },
          createdAt: new Date().toISOString(),
          chatRoom: chatRoomToDelete,
          type: 'LEAVE' as Message['type']
        }
      }]);
    } finally {
      setDeleteConfirm({ isOpen: false, chatRoom: null });
    }
  };

  const cancelDeleteChat = () => {
    setDeleteConfirm({ isOpen: false, chatRoom: null });
  };

  const handleUserStatusUpdate = (statusUpdate: any) => {
    // Update online status in chat rooms
    setChatRooms(prevRooms => 
      prevRooms.map(room => ({
        ...room,
        members: room.members.map(member => 
          member.id === statusUpdate.userId 
            ? { ...member, isOnline: statusUpdate.isOnline, lastSeen: statusUpdate.lastSeen }
            : member
        )
      }))
    );
  };

  const handleNewChatRoom = async (newChatRoom: ChatRoom) => {
    console.log('Processing new chat room notification:', newChatRoom);
    
    // Validate the newChatRoom object
    if (!newChatRoom || !newChatRoom.id) {
      console.error('Invalid chat room data received:', newChatRoom);
      return;
    }
    
    try {
      // Load last message for the new chat room
      const lastMessage = await chatService.getLastMessage(newChatRoom.id);
      const chatRoomWithLastMessage = {
        ...newChatRoom,
        lastMessage,
        lastMessageTime: lastMessage?.createdAt
      };

      // Add the new chat room to the list
      setChatRooms(prev => {
        // Check if chat room already exists to avoid duplicates
        if (prev.find(room => room.id === newChatRoom.id)) {
          console.log('Chat room already exists, skipping');
          return prev;
        }
        console.log('Adding new chat room to list');
        return [chatRoomWithLastMessage, ...prev];
      });

      // Check if current user is the creator (don't show notification to creator)
      const isCreator = newChatRoom.createdBy && user && newChatRoom.createdBy.id === user.id;
      
      if (!isCreator) {
        // Show notification about new chat only to non-creators
        const notificationId = Date.now();
        const creatorName = newChatRoom.createdBy?.username || 'Someone';
        console.log('Showing notification for new chat from:', creatorName);
        setNotifications(prev => [...prev, { 
          id: notificationId, 
          message: {
            id: 0,
            content: newChatRoom.type === 'DIRECT' 
              ? `${creatorName} started a conversation with you`
              : `${creatorName} added you to "${newChatRoom.name}"`,
            sender: { username: 'System', id: 0, email: '' },
            createdAt: new Date().toISOString(),
            chatRoom: newChatRoom,
            type: 'JOIN' as Message['type']
          }
        }]);
      } else {
        console.log('User is the creator, not showing notification');
      }
    } catch (error) {
      console.error('Error processing new chat room:', error);
    }
  };

  const handleChatCreated = async () => {
    // The new chat room will be automatically added via WebSocket notification
    // We just need to reload to ensure we have the latest state
    try {
      const rooms = await chatService.getChatRooms();
      
      // Load last message for each room
      const roomsWithLastMessages = await Promise.all(
        rooms.map(async (room) => {
          const lastMessage = await chatService.getLastMessage(room.id);
          return {
            ...room,
            lastMessage,
            lastMessageTime: lastMessage?.createdAt
          };
        })
      );
      
      setChatRooms(roomsWithLastMessages);
      
      // Select the newly created room (usually the first one after sorting)
      if (roomsWithLastMessages.length > 0) {
        const newRoom = roomsWithLastMessages[0];
        setSelectedRoom(newRoom);
      }
    } catch (error) {
      console.error('Failed to reload chat rooms:', error);
    }
  };

  if (loading) {
    return <div>Loading...</div>;
  }

  return (
    <div className="chat-container">
      <div className="sidebar">
        <div style={{ marginBottom: '20px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '10px' }}>
            <h3>Chat Rooms</h3>
            <button 
              onClick={(e) => {
                e.preventDefault();
                logout();
              }} 
              style={{ background: 'none', border: 'none', color: 'white', cursor: 'pointer' }}
            >
              Logout
            </button>
          </div>
          <div style={{ 
            padding: '8px 12px', 
            backgroundColor: 'rgba(255, 255, 255, 0.1)', 
            borderRadius: '5px',
            marginBottom: '10px',
            fontSize: '14px',
            color: '#e0e0e0'
          }}>
            Logged in as: <strong style={{ color: 'white' }}>{user?.username}</strong>
          </div>
        </div>
        
        <button 
          className="create-chat-btn"
          onClick={() => setShowCreateModal(true)}
          style={{ 
            width: '100%', 
            padding: '10px', 
            marginBottom: '15px',
            backgroundColor: '#007bff',
            color: 'white',
            border: 'none',
            borderRadius: '5px',
            cursor: 'pointer'
          }}
        >
          + New Chat
        </button>
        
        <ChatRoomList 
          chatRooms={chatRooms}
          selectedRoom={selectedRoom}
          onRoomSelect={handleRoomSelect}
          onDeleteChat={handleDeleteChat}
          currentUser={user!}
          currentTime={currentTime}
        />
      </div>
      
      <div className="main-chat">
        {selectedRoom ? (
          <>
            <div className="chat-header">
              <h2>
                {selectedRoom.type === 'DIRECT' 
                  ? selectedRoom.members.find(member => member.id !== user?.id)?.username || selectedRoom.name
                  : selectedRoom.name
                }
              </h2>
              <p className={selectedRoom.type === 'DIRECT' ? 
                  (() => {
                    const otherUser = selectedRoom.members.find(member => member.id !== user?.id);
                    return otherUser?.isOnline ? 'status-online' : 'status-offline';
                  })() : ''
                }>
                {selectedRoom.type === 'DIRECT' 
                  ? (() => {
                      const otherUser = selectedRoom.members.find(member => member.id !== user?.id);
                      return otherUser 
                        ? formatLastSeen(otherUser.lastSeen, otherUser.isOnline, currentTime)
                        : `${selectedRoom.members.length} members`;
                    })()
                  : `${selectedRoom.members.length} members`
                }
              </p>
            </div>
            
            <MessageList 
              messages={messages} 
              currentUser={user!} 
              typingUsers={typingUsers}
              onMessageVisible={handleMessageVisible}
            />
            
            <MessageInput 
              onSendMessage={handleSendMessage} 
              onTypingChange={handleTypingChange}
            />
            

          </>
        ) : (
          <div className="chat-header">
            <h2>Select a chat room to start messaging</h2>
          </div>
        )}
      </div>

      <CreateChatModal
        isOpen={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        onChatCreated={handleChatCreated}
        existingChatRooms={chatRooms}
        currentUser={user!}
        onExistingChatSelected={handleRoomSelect}
      />

      {/* Notifications */}
      <div className="notification-container">
        {notifications.map(({ id, message }) => (
          <NotificationToast
            key={id}
            message={message.content}
            sender={message.sender.username}
            onClose={() => setNotifications(prev => prev.filter(n => n.id !== id))}
          />
        ))}
      </div>

      {/* Delete Confirmation Dialog */}
      <ConfirmDialog
        isOpen={deleteConfirm.isOpen}
        title="Delete Chat"
        message={`Are you sure you want to delete "${deleteConfirm.chatRoom?.name}"? This will only remove it from your chat list. Other participants will still have access to the chat.`}
        onConfirm={confirmDeleteChat}
        onCancel={cancelDeleteChat}
      />
    </div>
  );
};

export default Chat;