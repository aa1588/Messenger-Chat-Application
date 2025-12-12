package com.chatapp.service;

import com.chatapp.model.ChatRoom;
import com.chatapp.model.Message;
import com.chatapp.model.User;
import com.chatapp.repository.ChatRoomRepository;
import com.chatapp.repository.MessageRepository;
import com.chatapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public List<Message> getChatRoomMessages(Long chatRoomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));
        
        return messageRepository.findByChatRoomOrderByCreatedAtAsc(chatRoom);
    }

    public Message getLastMessage(Long chatRoomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new RuntimeException("Chat room not found"));
        
        return messageRepository.findTopByChatRoomOrderByCreatedAtDesc(chatRoom).orElse(null);
    }

    @Transactional
    public void markMessageAsRead(Long messageId, String username) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Only mark as read if the user is not the sender
        if (!message.getSender().getId().equals(user.getId())) {
            message.setIsRead(true);
            message.setReadAt(LocalDateTime.now());
            Message savedMessage = messageRepository.save(message);
            
            // Broadcast read status to the chat room
            broadcastMessageStatus(savedMessage, "READ");
        }
    }

    private void broadcastMessageStatus(Message message, String status) {
        var statusUpdate = new Object() {
            public final Long messageId = message.getId();
            public final String statusType = status;
            public final String readAt = message.getReadAt() != null ? message.getReadAt().toString() : null;
            public final Long chatRoomId = message.getChatRoom().getId();
        };
        
        messagingTemplate.convertAndSend("/topic/chatroom/" + message.getChatRoom().getId() + "/status", statusUpdate);
    }
}