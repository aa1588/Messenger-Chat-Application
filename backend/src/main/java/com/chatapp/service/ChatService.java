package com.chatapp.service;

import com.chatapp.dto.MessageRequest;
import com.chatapp.model.ChatRoom;
import com.chatapp.model.Message;
import com.chatapp.model.User;
import com.chatapp.repository.ChatRoomRepository;
import com.chatapp.repository.MessageRepository;
import com.chatapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    public void sendMessage(MessageRequest messageRequest, String username) {
        User sender = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ChatRoom chatRoom = chatRoomRepository.findById(messageRequest.getChatRoomId())
                .orElseThrow(() -> new RuntimeException("Chat room not found"));

        Message message = new Message(messageRequest.getContent(), sender, chatRoom);
        message.setType(Message.MessageType.valueOf(messageRequest.getType()));
        
        Message savedMessage = messageRepository.save(message);

        // Send to chat room topic
        messagingTemplate.convertAndSend("/topic/chatroom/" + chatRoom.getId(), savedMessage);
    }

    public void addUser(MessageRequest messageRequest, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ChatRoom chatRoom = chatRoomRepository.findById(messageRequest.getChatRoomId())
                .orElseThrow(() -> new RuntimeException("Chat room not found"));

        Message joinMessage = new Message(username + " joined the chat", user, chatRoom);
        joinMessage.setType(Message.MessageType.JOIN);
        
        Message savedMessage = messageRepository.save(joinMessage);

        // Send join notification
        messagingTemplate.convertAndSend("/topic/chatroom/" + chatRoom.getId(), savedMessage);
    }
}