# WebSocket Implementation Guide

## Overview

This chat application uses **STOMP (Simple Text Oriented Messaging Protocol)** over WebSocket with **Spring Boot** on the backend and **@stomp/stompjs** on the frontend. The implementation provides real-time messaging, typing indicators, user status updates, and message read receipts.

## Architecture Diagram

```
┌─────────────────┐    WebSocket/STOMP     ┌─────────────────┐
│   React Client  │ ◄─────────────────────► │  Spring Boot    │
│                 │    /ws endpoint         │   Server        │
│  @stomp/stompjs │                         │                 │
└─────────────────┘                         └─────────────────┘
         │                                           │
         │                                           │
         ▼                                           ▼
┌─────────────────┐                         ┌─────────────────┐
│   SockJS        │                         │ Simple Broker   │
│   Fallback      │                         │ (/topic, /queue)│
└─────────────────┘                         └─────────────────┘
```

## Backend Configuration

### 1. WebSocket Configuration (`WebSocketConfig.java`)

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable simple in-memory broker for topics and queues
        config.enableSimpleBroker("/topic", "/queue");
        
        // Prefix for client-to-server messages
        config.setApplicationDestinationPrefixes("/app");
        
        // Prefix for user-specific destinations
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket endpoint with SockJS fallback
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
```

**Key Components:**
- **Simple Broker**: In-memory message broker handling `/topic` and `/queue` destinations
- **Application Prefix**: `/app` - routes client messages to `@MessageMapping` methods
- **User Prefix**: `/user` - enables user-specific messaging
- **STOMP Endpoint**: `/ws` - WebSocket connection endpoint with SockJS fallback

### 2. Authentication Interceptor

The configuration includes a channel interceptor that:
1. Intercepts STOMP CONNECT commands
2. Extracts JWT token from `Authorization` header
3. Validates token and stores username in session attributes
4. Sets Spring Security context

## Message Flow Architecture

### 1. Connection Establishment

```
Client                          Server
  │                              │
  │ 1. HTTP Upgrade Request      │
  │ ──────────────────────────► │
  │    GET /ws                   │
  │    Upgrade: websocket        │
  │                              │
  │ 2. WebSocket Handshake       │
  │ ◄────────────────────────── │
  │    101 Switching Protocols   │
  │                              │
  │ 3. STOMP CONNECT Frame       │
  │ ──────────────────────────► │
  │    Authorization: Bearer JWT │
  │                              │
  │ 4. STOMP CONNECTED Frame     │
  │ ◄────────────────────────── │
  │                              │
```

### 2. Subscription Flow

```
Client                          Server
  │                              │
  │ SUBSCRIBE Frame              │
  │ ──────────────────────────► │
  │ destination: /topic/chatroom/1│
  │ id: sub-1                    │
  │                              │
  │ RECEIPT Frame                │
  │ ◄────────────────────────── │
  │ receipt-id: sub-1            │
  │                              │
```

### 3. Message Publishing Flow

```
Client                          Server                     Database
  │                              │                           │
  │ SEND Frame                   │                           │
  │ ──────────────────────────► │                           │
  │ destination: /app/chat.sendMessage                       │
  │ body: {"content":"Hello"}    │                           │
  │                              │                           │
  │                              │ 1. Save Message          │
  │                              │ ────────────────────────► │
  │                              │                           │
  │                              │ 2. Broadcast to Topic    │
  │                              │ /topic/chatroom/1         │
  │                              │                           │
  │ MESSAGE Frame                │                           │
  │ ◄────────────────────────── │                           │
  │ destination: /topic/chatroom/1                           │
  │ body: {message object}       │                           │
```

## Topics and Destinations

### 1. Chat Room Messages
- **Destination**: `/topic/chatroom/{roomId}`
- **Purpose**: Real-time chat messages for specific rooms
- **Message Types**: CHAT, JOIN, LEAVE
- **Triggered by**: `/app/chat.sendMessage`, `/app/chat.addUser`

### 2. Typing Indicators
- **Destination**: `/topic/chatroom/{roomId}/typing`
- **Purpose**: Show when users are typing
- **Message Format**: `{userId, username, typing: boolean}`
- **Triggered by**: REST API call to `/api/chat-rooms/{id}/typing`

### 3. Message Status Updates
- **Destination**: `/topic/chatroom/{roomId}/status`
- **Purpose**: Message read receipts
- **Message Format**: `{messageId, statusType: "read", readAt}`
- **Triggered by**: REST API call to mark message as read

### 4. User Status Updates
- **Destination**: `/topic/user-status`
- **Purpose**: Online/offline status broadcasting
- **Message Format**: `{userId, username, isOnline, lastSeen}`
- **Triggered by**: REST API calls to update user status

### 5. New Chat Room Notifications
- **Destination**: `/topic/chatroom-created`
- **Purpose**: Notify users when added to new chat rooms
- **Message Format**: `{room: ChatRoom, memberUsernames: string[]}`
- **Triggered by**: Creating new chat rooms via REST API

## Client-Side Implementation

### 1. Connection Setup

```typescript
const socket = new SockJS('http://localhost:8080/ws');
const client = new Client({
  webSocketFactory: () => socket,
  connectHeaders: {
    'Authorization': `Bearer ${token}`
  },
  reconnectDelay: 5000,
  heartbeatIncoming: 4000,
  heartbeatOutgoing: 4000,
});
```

### 2. Subscription Management

The client maintains two types of subscriptions:

**Room-Specific Subscriptions** (cleared when switching rooms):
- Chat messages: `/topic/chatroom/{roomId}`
- Typing indicators: `/topic/chatroom/{roomId}/typing`
- Message status: `/topic/chatroom/{roomId}/status`

**Global Subscriptions** (persistent):
- User status: `/topic/user-status`
- New chat rooms: `/topic/chatroom-created`

### 3. Message Publishing

```typescript
// Send chat message
client.publish({
  destination: '/app/chat.sendMessage',
  body: JSON.stringify({
    content: "Hello World",
    chatRoomId: 1,
    type: 'CHAT'
  })
});
```

## Connection Lifecycle

### 1. Connection Establishment
1. **HTTP Upgrade**: Client requests WebSocket upgrade at `/ws`
2. **SockJS Negotiation**: If WebSocket fails, SockJS provides fallbacks
3. **STOMP Connect**: Client sends CONNECT frame with JWT token
4. **Authentication**: Server validates JWT and stores user session
5. **Connected**: Server responds with CONNECTED frame

### 2. Active Session
1. **Heartbeats**: Bi-directional heartbeats every 4 seconds
2. **Subscriptions**: Client subscribes to relevant topics
3. **Message Exchange**: Real-time message publishing and receiving
4. **Automatic Reconnection**: Client reconnects on connection loss

### 3. Connection Termination

**Graceful Disconnect**:
```typescript
websocketService.disconnect();
// 1. Unsubscribes from all topics
// 2. Sends DISCONNECT frame
// 3. Closes WebSocket connection
```

**Ungraceful Disconnect**:
- Network failure or browser close
- Server detects via missed heartbeats
- Client automatically attempts reconnection

## Error Handling

### 1. Connection Errors
- **Authentication Failure**: Invalid JWT token
- **Network Issues**: Connection timeout or failure
- **Server Unavailable**: Backend service down

### 2. Message Errors
- **Invalid Destination**: Subscribing to non-existent topic
- **Malformed Message**: Invalid JSON in message body
- **Authorization**: Accessing unauthorized chat rooms

### 3. Recovery Mechanisms
- **Automatic Reconnection**: 5-second delay with exponential backoff
- **Subscription Recovery**: Re-subscribe to topics after reconnection
- **Message Queuing**: Client-side queuing during disconnection

## Security Considerations

### 1. Authentication
- JWT token required in CONNECT frame
- Token validation on every connection
- Session-based user identification

### 2. Authorization
- Room-based message filtering
- User-specific subscriptions
- Server-side permission checks

### 3. Rate Limiting
- Heartbeat configuration prevents flooding
- Message size limits via STOMP configuration
- Connection limits per user

## Performance Optimizations

### 1. Subscription Management
- Clear room subscriptions when switching rooms
- Maintain global subscriptions across room changes
- Batch subscription operations

### 2. Message Filtering
- Server-side filtering for relevant messages
- Client-side duplicate message detection
- Efficient JSON serialization/deserialization

### 3. Connection Pooling
- Single WebSocket connection per client
- Multiplexed messaging over single connection
- Efficient resource utilization

## Debugging and Monitoring

### 1. Client-Side Debugging
```typescript
debug: (str) => {
  console.log('STOMP: ' + str);
}
```

### 2. Server-Side Logging
- Spring WebSocket debug logging
- Message broker statistics
- Connection lifecycle events

### 3. Common Issues
- **CORS Problems**: Ensure proper origin configuration
- **Token Expiry**: Handle JWT token refresh
- **Memory Leaks**: Proper subscription cleanup
- **Connection Limits**: Monitor concurrent connections

## Message Types and Formats

### Chat Message
```json
{
  "id": 123,
  "content": "Hello World",
  "sender": {
    "id": 1,
    "username": "john_doe"
  },
  "chatRoom": {
    "id": 1,
    "name": "General"
  },
  "type": "CHAT",
  "createdAt": "2023-12-12T10:30:00Z",
  "isRead": false
}
```

### Typing Indicator
```json
{
  "userId": 1,
  "username": "john_doe",
  "typing": true
}
```

### User Status Update
```json
{
  "userId": 1,
  "username": "john_doe",
  "isOnline": true,
  "lastSeen": "2023-12-12T10:30:00Z"
}
```

### Message Status Update
```json
{
  "messageId": 123,
  "statusType": "read",
  "readAt": "2023-12-12T10:30:00Z"
}
```

This WebSocket implementation provides a robust, scalable real-time messaging system with proper authentication, error handling, and performance optimizations.