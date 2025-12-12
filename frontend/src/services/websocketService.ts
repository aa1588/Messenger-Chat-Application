import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { Message } from './chatService';

class WebSocketService {
  private client: Client | null = null;
  private connected = false;
  private subscriptions: any[] = [];
  private globalSubscriptions: any[] = []; // For user status, new chat rooms, etc.

  connect(username: string, onMessageReceived: (message: Message) => void): Promise<void> {
    return new Promise((resolve, reject) => {
      const socket = new SockJS('http://localhost:8080/ws');
      const token = localStorage.getItem('token');
      
      this.client = new Client({
        webSocketFactory: () => socket,
        connectHeaders: {
          'Authorization': `Bearer ${token}`
        },
        debug: (str) => {
          console.log('STOMP: ' + str);
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
      });

      this.client.onConnect = () => {
        console.log('Connected to WebSocket');
        this.connected = true;
        resolve();
      };

      this.client.onStompError = (frame) => {
        console.error('Broker reported error: ' + frame.headers['message']);
        console.error('Additional details: ' + frame.body);
        reject(new Error('WebSocket connection failed'));
      };

      this.client.activate();
    });
  }

  subscribeToRoom(roomId: number, onMessageReceived: (message: Message) => void) {
    if (this.client && this.connected) {
      const subscription = this.client.subscribe(`/topic/chatroom/${roomId}`, (message) => {
        const receivedMessage = JSON.parse(message.body);
        onMessageReceived(receivedMessage);
      });
      this.subscriptions.push(subscription);
    }
  }

  subscribeToNewChatRooms(username: string, onNewChatRoom: (chatRoom: any) => void) {
    if (this.client && this.connected) {
      console.log('Subscribing to new chat rooms for user:', username);
      const subscription = this.client.subscribe('/topic/chatroom-created', (message) => {
        console.log('Received new chat room notification:', message.body);
        const notification = JSON.parse(message.body);
        
        // Check if current user is a member of this chat room
        if (notification.memberUsernames && notification.memberUsernames.includes(username)) {
          console.log('User is a member of this chat room, processing notification');
          onNewChatRoom(notification.room); // Changed from notification.chatRoom to notification.room
        } else {
          console.log('User is not a member of this chat room, ignoring notification');
        }
      });
      this.globalSubscriptions.push(subscription); // Use global subscriptions
    }
  }

  subscribeToUserStatus(onStatusUpdate: (statusUpdate: any) => void) {
    if (this.client && this.connected) {
      const subscription = this.client.subscribe('/topic/user-status', (message) => {
        const statusUpdate = JSON.parse(message.body);
        onStatusUpdate(statusUpdate);
      });
      this.globalSubscriptions.push(subscription); // Use global subscriptions
    }
  }

  subscribeToTypingIndicators(roomId: number, onTypingUpdate: (typingUpdate: any) => void) {
    if (this.client && this.connected) {
      console.log('Subscribing to typing indicators for room:', roomId);
      const subscription = this.client.subscribe(`/topic/chatroom/${roomId}/typing`, (message) => {
        console.log('Received typing indicator WebSocket message:', message.body);
        const typingUpdate = JSON.parse(message.body);
        onTypingUpdate(typingUpdate);
      });
      this.subscriptions.push(subscription);
      console.log('Typing indicator subscription added');
    } else {
      console.warn('Cannot subscribe to typing indicators - WebSocket not connected');
    }
  }

  subscribeToMessageStatus(roomId: number, onStatusUpdate: (statusUpdate: any) => void) {
    if (this.client && this.connected) {
      console.log('Subscribing to message status for room:', roomId);
      const subscription = this.client.subscribe(`/topic/chatroom/${roomId}/status`, (message) => {
        console.log('Received message status WebSocket message:', message.body);
        const statusUpdate = JSON.parse(message.body);
        onStatusUpdate(statusUpdate);
      });
      this.subscriptions.push(subscription);
      console.log('Message status subscription added');
    }
  }

  clearSubscriptions() {
    // Only clear room-specific subscriptions, not global ones
    this.subscriptions.forEach(sub => {
      if (sub && sub.unsubscribe) {
        sub.unsubscribe();
      }
    });
    this.subscriptions = [];
  }

  clearAllSubscriptions() {
    // Clear all subscriptions including global ones
    this.clearSubscriptions();
    this.globalSubscriptions.forEach(sub => {
      if (sub && sub.unsubscribe) {
        sub.unsubscribe();
      }
    });
    this.globalSubscriptions = [];
  }

  sendMessage(roomId: number, content: string, username: string) {
    if (this.client && this.connected) {
      this.client.publish({
        destination: '/app/chat.sendMessage',
        body: JSON.stringify({
          content,
          chatRoomId: roomId,
          type: 'CHAT'
        })
      });
    }
  }

  addUser(roomId: number, username: string) {
    if (this.client && this.connected) {
      this.client.publish({
        destination: '/app/chat.addUser',
        body: JSON.stringify({
          content: username,
          chatRoomId: roomId,
          type: 'JOIN'
        })
      });
    }
  }

  disconnect() {
    this.clearAllSubscriptions();
    if (this.client) {
      this.client.deactivate();
      this.connected = false;
    }
  }
}

export const websocketService = new WebSocketService();