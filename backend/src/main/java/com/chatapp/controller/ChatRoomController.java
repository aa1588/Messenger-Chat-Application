package com.chatapp.controller;

import com.chatapp.dto.ChatRoomRequest;
import com.chatapp.dto.ChatRoomWithUnreadCount;
import com.chatapp.model.ChatRoom;
import com.chatapp.model.Message;
import com.chatapp.service.ChatRoomService;
import com.chatapp.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/chatrooms")
public class ChatRoomController {

    @Autowired
    private ChatRoomService chatRoomService;

    @Autowired
    private MessageService messageService;

    @GetMapping
    public ResponseEntity<List<ChatRoomWithUnreadCount>> getUserChatRooms(Authentication authentication) {
        List<ChatRoomWithUnreadCount> chatRooms = chatRoomService.getUserChatRoomsWithUnreadCount(authentication.getName());
        return ResponseEntity.ok(chatRooms);
    }

    @PostMapping
    public ResponseEntity<ChatRoom> createChatRoom(@Valid @RequestBody ChatRoomRequest request, 
                                                   Authentication authentication) {
        ChatRoom chatRoom = chatRoomService.createChatRoom(request, authentication.getName());
        return ResponseEntity.ok(chatRoom);
    }

    @GetMapping("/{chatRoomId}/messages")
    public ResponseEntity<List<Message>> getChatRoomMessages(@PathVariable Long chatRoomId) {
        List<Message> messages = messageService.getChatRoomMessages(chatRoomId);
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/{chatRoomId}/join")
    public ResponseEntity<?> joinChatRoom(@PathVariable Long chatRoomId, Authentication authentication) {
        chatRoomService.joinChatRoom(chatRoomId, authentication.getName());
        return ResponseEntity.ok("Joined chat room successfully");
    }

    @PostMapping("/{chatRoomId}/leave")
    public ResponseEntity<?> leaveChatRoom(@PathVariable Long chatRoomId, Authentication authentication) {
        chatRoomService.leaveChatRoom(chatRoomId, authentication.getName());
        return ResponseEntity.ok("Left chat room successfully");
    }

    @GetMapping("/{chatRoomId}/last-message")
    public ResponseEntity<Message> getLastMessage(@PathVariable Long chatRoomId) {
        Message lastMessage = messageService.getLastMessage(chatRoomId);
        return lastMessage != null ? ResponseEntity.ok(lastMessage) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{chatRoomId}")
    public ResponseEntity<?> deleteChatForUser(@PathVariable Long chatRoomId, Authentication authentication) {
        chatRoomService.deleteChatForUser(chatRoomId, authentication.getName());
        return ResponseEntity.ok("Chat deleted for user");
    }

    @PostMapping("/{chatRoomId}/messages/{messageId}/read")
    public ResponseEntity<?> markMessageAsRead(@PathVariable Long chatRoomId, 
                                               @PathVariable Long messageId, 
                                               Authentication authentication) {
        messageService.markMessageAsRead(messageId, authentication.getName());
        return ResponseEntity.ok("Message marked as read");
    }

    @PostMapping("/{chatRoomId}/typing")
    public ResponseEntity<?> sendTypingIndicator(@PathVariable Long chatRoomId, 
                                                 @RequestParam Boolean isTyping,
                                                 Authentication authentication) {
        chatRoomService.broadcastTypingIndicator(chatRoomId, authentication.getName(), isTyping);
        return ResponseEntity.ok("Typing indicator sent");
    }
}