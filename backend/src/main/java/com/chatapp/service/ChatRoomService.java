package com.chatapp.service;

import com.chatapp.dto.ChatRoomRequest;
import com.chatapp.dto.ChatRoomWithUnreadCount;
import com.chatapp.model.ChatRoom;
import com.chatapp.model.Message;
import com.chatapp.model.User;
import com.chatapp.repository.ChatRoomRepository;
import com.chatapp.repository.MessageRepository;
import com.chatapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ChatRoomService {

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public List<ChatRoom> getUserChatRooms(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<ChatRoom> chatRooms = chatRoomRepository.findByMembersContaining(user);
        
        // Load the last message for each chat room
        for (ChatRoom chatRoom : chatRooms) {
            Optional<Message> lastMessage = messageRepository.findTopByChatRoomOrderByCreatedAtDesc(chatRoom);
            if (lastMessage.isPresent()) {
                // We'll handle this in the frontend for now to avoid circular reference issues
            }
        }
        
        return chatRooms;
    }

    public List<ChatRoomWithUnreadCount> getUserChatRoomsWithUnreadCount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<ChatRoom> chatRooms = chatRoomRepository.findByMembersContaining(user);
        
        return chatRooms.stream().map(chatRoom -> {
            ChatRoomWithUnreadCount roomWithCount = new ChatRoomWithUnreadCount(chatRoom, 0);
            
            // Get last message
            Optional<Message> lastMessage = messageRepository.findTopByChatRoomOrderByCreatedAtDesc(chatRoom);
            if (lastMessage.isPresent()) {
                roomWithCount.setLastMessage(lastMessage.get());
            }
            
            // Count unread messages
            int unreadCount = messageRepository.countUnreadMessagesForUser(chatRoom, user);
            roomWithCount.setUnreadCount(unreadCount);
            
            return roomWithCount;
        }).collect(Collectors.toList());
    }

    public ChatRoom createChatRoom(ChatRoomRequest request, String creatorUsername) {
        User creator = userRepository.findByUsername(creatorUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ChatRoom.ChatRoomType type = ChatRoom.ChatRoomType.valueOf(request.getType());
        
        // For direct messages, check if chat room already exists
        if (type == ChatRoom.ChatRoomType.DIRECT && request.getMemberIds().size() == 1) {
            User otherUser = userRepository.findById(request.getMemberIds().get(0))
                    .orElseThrow(() -> new RuntimeException("Other user not found"));
            
            Optional<ChatRoom> existingRoom = chatRoomRepository.findDirectChatRoom(creator, otherUser);
            if (existingRoom.isPresent()) {
                return existingRoom.get();
            }
            
            // Set the name to the other user's username for direct messages
            request.setName(otherUser.getUsername());
        }

        ChatRoom chatRoom = new ChatRoom(request.getName(), type, creator);
        chatRoom.getMembers().add(creator);

        // Add other members
        if (request.getMemberIds() != null) {
            for (Long memberId : request.getMemberIds()) {
                User member = userRepository.findById(memberId)
                        .orElseThrow(() -> new RuntimeException("Member not found: " + memberId));
                chatRoom.getMembers().add(member);
            }
        }

        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);
        
        // Notify all members about the new chat room
        notifyMembersAboutNewChatRoom(savedChatRoom);
        
        return savedChatRoom;
    }

    public void joinChatRoom(Long chatRoomId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));

        if (!chatRoom.getMembers().contains(user)) {
            chatRoom.getMembers().add(user);
            chatRoomRepository.save(chatRoom);
            
            // Create system message for join
            createSystemMessage(chatRoom, user, user.getUsername() + " joined the chat", Message.MessageType.JOIN);
        }
    }

    public void leaveChatRoom(Long chatRoomId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));

        if (chatRoom.getMembers().contains(user)) {
            chatRoom.getMembers().remove(user);
            chatRoomRepository.save(chatRoom);
            
            // Create system message for leave
            createSystemMessage(chatRoom, user, user.getUsername() + " left the chat", Message.MessageType.LEAVE);
        }
    }

    private void createSystemMessage(ChatRoom chatRoom, User user, String content, Message.MessageType type) {
        Message systemMessage = new Message();
        systemMessage.setContent(content);
        systemMessage.setSender(user);
        systemMessage.setChatRoom(chatRoom);
        systemMessage.setType(type);
        messageRepository.save(systemMessage);
    }

    public void deleteChatForUser(Long chatRoomId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));

        // Remove user from chat room members
        if (chatRoom.getMembers().contains(user)) {
            chatRoom.getMembers().remove(user);
            
            // If no members left, delete the entire chat room
            if (chatRoom.getMembers().isEmpty()) {
                chatRoomRepository.delete(chatRoom);
            } else {
                chatRoomRepository.save(chatRoom);
            }
        }
    }

    private void notifyMembersAboutNewChatRoom(ChatRoom chatRoom) {
        // Create notification object with member information
        var notification = new Object() {
            public final ChatRoom room = chatRoom;
            public final java.util.List<String> memberUsernames = chatRoom.getMembers()
                .stream()
                .map(User::getUsername)
                .collect(java.util.stream.Collectors.toList());
        };
        
        System.out.println("Broadcasting new chat room to all users: " + chatRoom.getName());
        System.out.println("Members: " + notification.memberUsernames);
        
        // Broadcast to all users, let frontend filter
        messagingTemplate.convertAndSend("/topic/chatroom-created", notification);
    }

    public void broadcastTypingIndicator(Long chatRoomId, String username, Boolean isTyping) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Create typing indicator object
        var typingIndicator = new Object() {
            public final Long userId = user.getId();
            public final String username = user.getUsername();
            public final Boolean typing = isTyping;
            public final Long chatRoomId = chatRoom.getId();
        };
        
        // Broadcast to all users in the chat room
        messagingTemplate.convertAndSend("/topic/chatroom/" + chatRoomId + "/typing", typingIndicator);
    }
}