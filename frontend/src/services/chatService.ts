import axios from 'axios';
import { authService } from './authService';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

export interface ChatRoom {
  id: number;
  name: string;
  type: 'DIRECT' | 'GROUP';
  createdAt: string;
  members: User[];
  createdBy?: User;
  lastMessage?: Message | null;
  lastMessageTime?: string;
  unreadCount?: number;
}

export interface Message {
  id: number;
  content: string;
  createdAt: string;
  sender: User;
  chatRoom: ChatRoom;
  type: 'CHAT' | 'JOIN' | 'LEAVE';
  isDelivered?: boolean;
  isRead?: boolean;
  deliveredAt?: string;
  readAt?: string;
}

export interface User {
  id: number;
  username: string;
  email: string;
  isOnline?: boolean;
  lastSeen?: string;
}

class ChatService {
  async getChatRooms(): Promise<ChatRoom[]> {
    const response = await axios.get(`${API_URL}/api/chatrooms`, {
      headers: authService.getAuthHeader()
    });
    return response.data;
  }

  async createChatRoom(name: string, type: 'DIRECT' | 'GROUP', memberIds: number[]): Promise<ChatRoom> {
    const response = await axios.post(`${API_URL}/api/chatrooms`, {
      name,
      type,
      memberIds
    }, {
      headers: authService.getAuthHeader()
    });
    return response.data;
  }

  async getChatRoomMessages(chatRoomId: number): Promise<Message[]> {
    const response = await axios.get(`${API_URL}/api/chatrooms/${chatRoomId}/messages`, {
      headers: authService.getAuthHeader()
    });
    return response.data;
  }

  async joinChatRoom(chatRoomId: number): Promise<void> {
    await axios.post(`${API_URL}/api/chatrooms/${chatRoomId}/join`, {}, {
      headers: authService.getAuthHeader()
    });
  }

  async leaveChatRoom(chatRoomId: number): Promise<void> {
    await axios.post(`${API_URL}/api/chatrooms/${chatRoomId}/leave`, {}, {
      headers: authService.getAuthHeader()
    });
  }

  async getLastMessage(chatRoomId: number): Promise<Message | null> {
    try {
      const response = await axios.get(`${API_URL}/api/chatrooms/${chatRoomId}/last-message`, {
        headers: authService.getAuthHeader()
      });
      return response.data;
    } catch (error) {
      return null;
    }
  }

  async deleteChatForUser(chatRoomId: number): Promise<void> {
    await axios.delete(`${API_URL}/api/chatrooms/${chatRoomId}`, {
      headers: authService.getAuthHeader()
    });
  }

  async searchUsers(query: string): Promise<User[]> {
    const response = await axios.get(`${API_URL}/api/users/search?query=${encodeURIComponent(query)}`, {
      headers: authService.getAuthHeader()
    });
    return response.data;
  }

  async getAllUsers(): Promise<User[]> {
    const response = await axios.get(`${API_URL}/api/users`, {
      headers: authService.getAuthHeader()
    });
    return response.data;
  }

  async updateOnlineStatus(isOnline: boolean): Promise<void> {
    await axios.post(`${API_URL}/api/users/status?isOnline=${isOnline}`, {}, {
      headers: authService.getAuthHeader()
    });
  }

  async markMessageAsRead(chatRoomId: number, messageId: number): Promise<void> {
    await axios.post(`${API_URL}/api/chatrooms/${chatRoomId}/messages/${messageId}/read`, {}, {
      headers: authService.getAuthHeader()
    });
  }

  async sendTypingIndicator(chatRoomId: number, isTyping: boolean): Promise<void> {
    try {
      await axios.post(`${API_URL}/api/chatrooms/${chatRoomId}/typing?isTyping=${isTyping}`, {}, {
        headers: authService.getAuthHeader()
      });
    } catch (error) {
      console.warn('Failed to send typing indicator:', error);
      // Don't throw error - typing indicators are not critical
    }
  }
}

export const chatService = new ChatService();