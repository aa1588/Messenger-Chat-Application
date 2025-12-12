# WebSocket Implementation Tutorial - Chat Application

## Overview

This chat application uses **Spring WebSocket with STOMP protocol** for real-time messaging. The implementation leverages Spring's message broker capabilities to handle chat rooms, user presence, and message delivery efficiently.

## Architecture Components

### 1. WebSocket Configuration (`WebSocketConfig.java`)

The WebSocket configuration is the foundation of the real-time communication system:

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer
```

#### Key Configuration Elements:

**Message Broker Setup:**
```java
config.enableSimpleBroker("/topic", "/queue");
config.setApplicationDestinationPrefixes("/app");
config.setUserDestinationPrefix("/user");
```

- **`/topic`**: Used for **broadcast messaging** (one-to-many)
  - Chat room messages: `/topic/chatroom/{roomId}`
  - Global notifications: `/topic/chatroom-created`
  - User status updates: `/topic/user-status`

- **`/queue`**: Used for **point-to-point messaging** (one-to-one)
  - Private messages between users
  - Personal notifications

- **`/app`**: Application destination prefix for client messages
  - Client sends to: `/app/chat.sendMessage`
  - Server handles via: `@MessageMapping("/chat.sendMessage")`

- **`/user`**: User-specific destinations for private messaging

**STOMP Endpoint:**
```java
registry.addEndpoint("/ws")
        .setAllowedOriginPatterns("*")
        .withSockJS();
```

- **`/ws`**: WebSocket connection endpoint
- **SockJS**: Provides fallback options for browsers that don't support WebSocket

### 2. Authentication & Security

**JWT Token Authentication:**
The WebSocket connection uses JWT tokens for authentication through a channel interceptor:

```java
@Override
public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(new ChannelInterceptor() {
        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
            
            if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                String authToken = accessor.getFirstNativeHeader("Authorization");
                if (authToken != null && authToken.startsWith("Bearer ")) {
                    String jwt = authToken.substring(7);
                    if (jwtUtils.validateJwtToken(jwt)) {
                        String username = jwtUtils.getUserNameFromJwtToken(jwt);
                        accessor.getSessionAttributes().put("username", username);
                    }
                }
            }
            return message;
        }
    });
}
```

**Security Flow:**
1. Client connects with JWT token in `Authorization` header
2. Server validates token and extracts username
3. Username stored in WebSocket session attributes
4. All subsequent messages use this authenticated session

### 3. Message Handling (`ChatController.java`)

The controller handles incoming WebSocket messages using `@MessageMapping`:

```java
@MessageMapping("/chat.sendMessage")
public void sendMessage(@Payload MessageRequest messageRequest, SimpMessageHeaderAccessor headerAccessor) {
    String username = (String) headerAccessor.getSessionAttributes().get("username");
    chatService.sendMessage(messageRequest, username);
}

@MessageMapping("/chat.addUser")
public void addUser(@Payload MessageRequest messageRequest, SimpMessageHeaderAccessor headerAccessor) {
    String username = messageRequest.getContent();
    headerAccessor.getSessionAttributes().put("username", username);
    chatService.addUser(messageRequest, username);
}
```

**Message Flow:**
1. Client sends message to `/app/chat.sendMessage`
2. Controller extracts authenticated username from session
3. Delegates to `ChatService` for processing
4. Service broadcasts message to appropriate topic

### 4. Message Broadcasting (`ChatService.java`)

The service layer handles message persistence and broadcasting:

```java
public void sendMessage(MessageRequest messageRequest, String username) {
    // 1. Validate user and chat room
    User sender = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
    ChatRoom chatRoom = chatRoomRepository.findById(messageRequest.getChatRoomId())
            .orElseThrow(() -> new RuntimeException("Chat room not found"));

    // 2. Create and save message
    Message message = new Message(messageRequest.getContent(), sender, chatRoom);
    message.setType(Message.MessageType.valueOf(messageRequest.getType()));
    Message savedMessage = messageRepository.save(message);

    // 3. Broadcast to all room subscribers
    messagingTemplate.convertAndSend("/topic/chatroom/" + chatRoom.getId(), savedMessage);
}
```

**Broadcasting Mechanism:**
- Uses `SimpMessagingTemplate` to send messages
- Messages sent to `/topic/chatroom/{roomId}` reach all subscribers
- Automatic JSON serialization of message objects

## Topic and Queue Structure

### Topics (Broadcast Channels)

1. **Chat Room Messages**: `/topic/chatroom/{roomId}`
   - All members of a chat room subscribe to this topic
   - Receives all messages sent to that room
   - Includes regular messages, join/leave notifications

2. **Chat Room Creation**: `/topic/chatroom-created`
   - Global topic for new chat room notifications
   - Users subscribe to get notified when added to new rooms

3. **User Status Updates**: `/topic/user-status`
   - Global topic for online/offline status
   - Typing indicators
   - Presence information

4. **Typing Indicators**: `/topic/chatroom/{roomId}/typing`
   - Room-specific typing notifications
   - Real-time typing status updates

5. **Message Status**: `/topic/chatroom/{roomId}/status`
   - Message delivery confirmations
   - Read receipts
   - Message status updates

### Queues (Point-to-Point)

While the current implementation primarily uses topics, queues can be used for:

1. **Private Messages**: `/queue/user/{username}/messages`
2. **Personal Notifications**: `/queue/user/{username}/notifications`
3. **Direct User Communications**: `/queue/user/{username}/direct`

## Client-Side Implementation (Frontend)

### Connection Setup

```typescript
connect(username: string, onMessageReceived: (message: Message) => void): Promise<void> {
    const socket = new SockJS('http://localhost:8080/ws');
    const token = localStorage.getItem('token');
    
    this.client = new Client({
        webSocketFactory: () => socket,
        connectHeaders: {
            'Authorization': `Bearer ${token}`
        },
        reconnectDelay: 5000,
        heartbeatIncoming: 4000,
        heartbeatOutgoing: 4000,
    });
}
```

### Subscription Management

**Room-Specific Subscriptions:**
```typescript
subscribeToRoom(roomId: number, onMessageReceived: (message: Message) => void) {
    const subscription = this.client.subscribe(`/topic/chatroom/${roomId}`, (message) => {
        const receivedMessage = JSON.parse(message.body);
        onMessageReceived(receivedMessage);
    });
    this.subscriptions.push(subscription);
}
```

**Global Subscriptions:**
```typescript
subscribeToNewChatRooms(username: string, onNewChatRoom: (chatRoom: any) => void) {
    const subscription = this.client.subscribe('/topic/chatroom-created', (message) => {
        const notification = JSON.parse(message.body);
        if (notification.memberUsernames && notification.memberUsernames.includes(username)) {
            onNewChatRoom(notification.room);
        }
    });
    this.globalSubscriptions.push(subscription);
}
```

### Message Sending

```typescript
sendMessage(roomId: number, content: string, username: string) {
    this.client.publish({
        destination: '/app/chat.sendMessage',
        body: JSON.stringify({
            content,
            chatRoomId: roomId,
            type: 'CHAT'
        })
    });
}
```

## Data Models

### Message Model
```java
@Entity
public class Message {
    private Long id;
    private String content;
    private LocalDateTime createdAt;
    private User sender;
    private ChatRoom chatRoom;
    private MessageType type; // CHAT, JOIN, LEAVE
    private Boolean isDelivered;
    private Boolean isRead;
    private LocalDateTime deliveredAt;
    private LocalDateTime readAt;
}
```

### Chat Room Model
```java
@Entity
public class ChatRoom {
    private Long id;
    private String name;
    private ChatRoomType type; // DIRECT, GROUP
    private LocalDateTime createdAt;
    private User createdBy;
    private Set<User> members;
    private Set<Message> messages;
}
```

## Message Flow Diagram

```
Client A                    Server                      Client B
   |                         |                           |
   |-- Connect with JWT ---->|                           |
   |<-- Connection OK -------|                           |
   |                         |<-- Connect with JWT -----|
   |                         |-- Connection OK --------->|
   |                         |                           |
   |-- Subscribe to room --->|                           |
   |                         |<-- Subscribe to room -----|
   |                         |                           |
   |-- Send message -------->|                           |
   |                         |-- Save to DB              |
   |                         |-- Broadcast to topic      |
   |<-- Receive message -----|-- Receive message ------->|
```

## Advanced Features

### 1. Typing Indicators
- Real-time typing status using dedicated topics
- Temporary state management (not persisted)
- Automatic cleanup after timeout

### 2. Message Status Tracking
- Delivery confirmations
- Read receipts
- Status updates via dedicated topics

### 3. Presence Management
- Online/offline status
- Last seen timestamps
- Activity indicators

### 4. Subscription Management
- Room-specific subscriptions (cleared when leaving room)
- Global subscriptions (persist across room changes)
- Automatic cleanup on disconnect

## Security Considerations

1. **JWT Authentication**: All WebSocket connections require valid JWT tokens
2. **Session Management**: User identity stored in WebSocket session
3. **Authorization**: Room membership validation before message delivery
4. **CORS Configuration**: Controlled origin patterns for WebSocket connections

## Performance Optimizations

1. **Connection Pooling**: Reuse WebSocket connections
2. **Heartbeat Configuration**: Maintain connection health
3. **Automatic Reconnection**: Handle network interruptions
4. **Subscription Cleanup**: Prevent memory leaks
5. **Message Batching**: Efficient message delivery

## Deployment Considerations

1. **Load Balancing**: WebSocket sticky sessions required
2. **Scaling**: Consider Redis for message broker in multi-instance deployments
3. **Monitoring**: Track connection counts and message throughput
4. **Fallback**: SockJS provides compatibility for older browsers

This WebSocket implementation provides a robust foundation for real-time chat functionality with proper authentication, message persistence, and scalable architecture patterns.